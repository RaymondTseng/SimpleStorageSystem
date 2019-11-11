import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class StorageNode extends Server implements Runnable {
    private List<String> filesList;
    private String dataFolder;
    private ServerSocket serverSocket;
    private ThreadPoolExecutor threadPoolExecutor;
    private String dsAddress;
    private int dsPort;
    private String backupAddress;
    private int backupPort;

    public static void main(String[] args) throws IOException{
        if (args.length != 0){
            new StorageNode(args[0], args[1], Integer.parseInt(args[2]), args[3], args[4], Integer.parseInt(args[5]),
                    args[6], Integer.parseInt(args[7]));
        }
    }


    public StorageNode(String name, String address, int port, String dataFolder, String dsAddress,
                       int dsPort, String backupAddress, int backupPort) throws IOException {
        this.name = name;
        this.address = address;
        this.port = port;
        this.dataFolder = dataFolder;
        this.dsAddress = dsAddress;
        this.dsPort = dsPort;
        this.backupAddress = backupAddress;
        this.backupPort = backupPort;

        this.serverSocket = new ServerSocket(port);
        this.threadPoolExecutor = new ThreadPoolExecutor(4, 8, 1000,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        initializeLocalFiles();
        setDirectoryServer();
        setBackupServer();
        System.out.println("Activate " + name + " " + address + " " + String.valueOf(port));
        new Thread(this).start();
    }

    /**
     * Check the folder and put all file names in hash map
     */
    public void initializeLocalFiles() {
        filesList = new ArrayList<>();
        File file = new File(this.dataFolder);
        File[] files = file.listFiles();
        if (files == null)
            return;
        for (int i = 0; i < files.length; i++) {
            System.out.println("Register " + files[i].getName());
            filesList.add(files[i].getName().trim());
        }
    }

    public void getFilesList(Socket socket) {
        new SocketUtils(socket,
                new RequestPackage(2, this.address, this.port, this.filesList)).send();

    }

    public void addFileToSystem(String fileName) {
        filesList.add(fileName);
        List<String> content = new ArrayList<>();
        content.add(fileName);
        SocketUtils socketUtils = new SocketUtils(this.dsAddress, this.dsPort, backupAddress, backupPort,
                new RequestPackage(3, this.getAddress(), this.getPort(), content));
        socketUtils.send();


        RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
        for (String addressPort : rp.getContent()) {  //content should also contain this node's address and port: minus self
            String[] array = addressPort.split(";");
            String address = array[0];
            int port = Integer.parseInt(array[1]);
            sendFile(fileName, address, port);
        }
    }

    public void createFile(String fileName) {
        if (filesList.contains(fileName)) {
            System.out.println("File name already exists!");
            return;
        }
        try {
            File file = new File(this.dataFolder + "/" + fileName);  //create a new file locally with fileName
            if (file.exists()) {
                System.out.println("File already exists!");
                return;
            } else {
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                raf.setLength(10000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(fileName + " create successfully!");
        addFileToSystem(fileName);

    }

    public void receiveFileToLocal(String fileName, Socket socket) throws Exception {
        if (filesList.contains(fileName)) {
            socket.close();
            return;
        }

        SocketUtils socketUtils = new SocketUtils(socket, null);
        socketUtils.readFileFromSocket(dataFolder + "/" + fileName);

        filesList.add(fileName);
        System.out.println("Download " + fileName + " successfully!");


    }


    public void sendAllLocalFiles(String address, int port){
        for (String fileName : filesList){
            sendFile(fileName, address, port);
        }
    }

    public void sendFile(String fileName, String address, int port) {  //send a certain file to the input address and port
        if (port == this.port && address.equals(this.address))
            return;
        List<String> content = new ArrayList<>();
        content.add(fileName);
        SocketUtils socketUtils = new SocketUtils(address, port, new RequestPackage(1, this.address, this.port, content));
        socketUtils.send();

        socketUtils.sendFileBySocket(this.dataFolder + "/" + fileName);

    }

    public void readFile(String fileName, String clientAddress, int clientPort){
        File content = new File(this.dataFolder + "/" + fileName);
        SocketUtils socketUtils = new SocketUtils(clientAddress, clientPort, new RequestPackage(5, this.address, this.port, content));
        socketUtils.send();
        
    }

    public void register(List<String> addressPortList) throws Exception {
        for (String addressPort : addressPortList) {
            String[] array = addressPort.split(";");
            String address = array[0];
            int port = Integer.parseInt(array[1]);
            for (String fileName : filesList) {
                sendFile(fileName, address, port);

            }
        }
    }

    private void setDirectoryServer() {
        new SocketUtils(dsAddress, dsPort,
                new RequestPackage(0, this.address, this.port, this.filesList)).send();
    }

    private void setBackupServer() {
        new SocketUtils(backupAddress, backupPort,
                new RequestPackage(0, this.address, this.port, this.filesList)).send();
    }

    @Override
    public void run() {
        Socket socket = null;
        while (true) {
            try {
                socket = this.serverSocket.accept();
                threadPoolExecutor.execute(new Task(socket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Task implements Runnable {
        private Socket socket;

        public Task(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(this.socket.getInputStream());
                RequestPackage rp = (RequestPackage) ois.readObject();
                // different types mean different requests
                if (rp.getRequestType() == 0) {
                    register(rp.getContent());
                    ois.close();
                } else if (rp.getRequestType() == 1) {
                    receiveFileToLocal(rp.getContent().get(0), socket);
                } else if (rp.getRequestType() == 2) {
                    getFilesList(socket);
                } else if (rp.getRequestType() == 3) {
                    createFile(rp.getContent().get(0));
                    ois.close();
                }else if (rp.getRequestType() == 4){
                    String[] array = rp.getContent().get(0).split(";");
                    sendAllLocalFiles(array[0], Integer.parseInt(array[1]));
                }else if (rp.getRequestType() == 5){
                    readFile(rp.getContent().get(0),rp.getRequestAddress(),rp.getRequestPort());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
