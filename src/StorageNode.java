

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A class for storage node
 */
public class StorageNode extends Server implements Runnable {
    private List<String> filesList;
    private String dataFolder;
    private ServerSocket serverSocket;
    private ThreadPoolExecutor threadPoolExecutor;
    private String dsAddress;
    private int dsPort;
    private String backupAddress;
    private int backupPort;
    private Thread currentThread;

    // metric
    private int messagesExchanged = 0;
    private int bytesTransferred = 0;

    /**
     * main function for launching a storage node
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException{
        if (args.length != 0){
            new StorageNode(args[0], args[1], Integer.parseInt(args[2]), args[3], args[4], Integer.parseInt(args[5]),
                    args[6], Integer.parseInt(args[7]));
        }
    }

    /**
     * construct function
     * @param name
     * @param address
     * @param port
     * @param dataFolder
     * @param dsAddress
     * @param dsPort
     * @param backupAddress
     * @param backupPort
     * @throws IOException
     */
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
        System.out.println("Activate " + name + " " + address + " " + String.valueOf(port));
        this.currentThread = new Thread(this);
        currentThread.start();

    }

    /**
     * Check the folder and put all file names in hash map(locally)
     */
    public void initializeLocalFiles() {
        filesList = new ArrayList<>();
        File file = new File(this.dataFolder);
        File[] files = file.listFiles();
        if (files == null)
            return;
        synchronized (filesList){
            for (int i = 0; i < files.length; i++) {
                filesList.add(files[i].getName().trim());
            }
        }
    }

    /**
     * return all files list
     * @param socket
     */
    public void getFilesList(Socket socket) {
        synchronized (this.filesList) {
            new SocketUtils(socket,
                    new RequestPackage(2, this.address, this.port, this.filesList)).send();
        }

    }

    /**
     * Adding a file to directory server as well as sending this file to other storage nodes
     * @param fileName
     */
    public void addFileToSystem(String fileName) {
        synchronized (this.filesList){
            filesList.add(fileName);
        }

        List<String> content = new ArrayList<>();
        content.add(fileName);
        SocketUtils socketUtils = new SocketUtils(this.dsAddress, this.dsPort, backupAddress, backupPort,
                new RequestPackage(3, this.getAddress(), this.getPort(), content));
        boolean flag = socketUtils.send();
        if (!flag){
            System.out.println("Fail to add file to directory server!");
        }

        RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
        this.bytesTransferred += socketUtils.getBytesTransferred();
        content = (List<String>) rp.getContent();
        for (String addressPort : content) {  //content should also contain this node's address and port: minus self
            String[] array = addressPort.split(";");
            String address = array[0];
            int port = Integer.parseInt(array[1]);
            sendFile(fileName, address, port);
        }
    }

    /**
     * creating a file with fixed size
     * @param fileName
     */
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

    /**
     * Receive a file from other storage nodes
     * @param fileName
     * @param socket
     * @throws Exception
     */
    public void receiveFileToLocal(String fileName, Socket socket) throws Exception {
        synchronized (this.filesList){
            if (filesList.contains(fileName)) {
                socket.close();
                return;
            }
        }


        SocketUtils socketUtils = new SocketUtils(socket, null);
        socketUtils.readFileFromSocket(dataFolder + "/" + fileName);
        this.bytesTransferred += socketUtils.getBytesTransferred();
        synchronized (this.filesList){
            filesList.add(fileName);
        }
        System.out.println("Download " + fileName + " successfully!");
        new SocketUtils(dsAddress, dsPort, new RequestPackage(7, this.address, this.port, fileName)).send();


    }


    public void sendFile(String fileName, String address, int port) {  //send a certain file to the input address and port
        if (port == this.port && address.equals(this.address))
            return;
        if (!this.filesList.contains(fileName))
            return;;
        List<String> content = new ArrayList<>();
        content.add(fileName);
        SocketUtils socketUtils = new SocketUtils(address, port, new RequestPackage(1, this.address, this.port, content));
        System.out.println("Storage node " + this.name + " send file " + fileName + " to " + address + String.valueOf(port));
        boolean flag = socketUtils.send();
        if (flag){
            socketUtils.sendFileBySocket(this.dataFolder + "/" + fileName);
        }else{
            List<String> temp = new ArrayList<>();
            temp.add(address + ";" + String.valueOf(port));
            new SocketUtils(dsAddress, dsPort, new RequestPackage(6, this.address, this.port, temp)).send();
        }


    }

    /**
     * Requested by a client that send the file to this client
     * @param fileName
     * @param socket
     */
    public void readFile(String fileName, Socket socket){  //send a certain file to the input address and port
        SocketUtils socketUtils = new SocketUtils(socket, null);
        socketUtils.sendFileBySocket(this.dataFolder + "/" + fileName);
        System.out.println("Storage node send: " + fileName);

    }

    /**
     * Sending all local files to other storage nodes
     * @param addressPortList
     */
    public void register(List<String> addressPortList) {
        for (String addressPort : addressPortList) {
            String[] array = addressPort.split(";");
            String address = array[0];
            int port = Integer.parseInt(array[1]);
            synchronized (filesList){
                for (String fileName : filesList) {
                    sendFile(fileName, address, port);

                }
            }

        }
    }

    /**
     * Setting current node's directory server
     */
    private void setDirectoryServer() {
        new SocketUtils(dsAddress, dsPort,
                new RequestPackage(0, this.address, this.port, this.filesList)).send();
    }

    public int getMessagesExchanged() {
        return messagesExchanged;
    }

    public int getBytesTransferred() {
        return bytesTransferred;
    }

    public void stop() throws IOException {
        this.serverSocket.close();
        this.currentThread.stop();
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
                InputStream is = this.socket.getInputStream();
                ois = new ObjectInputStream(is);
                messagesExchanged += 1;
                RequestPackage rp = (RequestPackage) ois.readObject();
                bytesTransferred += is.available();
                // different types mean different requests
                if (rp.getRequestType() == 0) {
                    register((List<String>) rp.getContent());
                    ois.close();
                } else if (rp.getRequestType() == 1) {
                    List<String> content = (List<String>) rp.getContent();
                    receiveFileToLocal(content.get(0), socket);
                } else if (rp.getRequestType() == 2) {
                    getFilesList(socket);
                } else if (rp.getRequestType() == 3) {
                    List<String> content = (List<String>) rp.getContent();
                    createFile(content.get(0));
                    ois.close();
                }else if (rp.getRequestType() == 4){
                    List<String> content = (List<String>) rp.getContent();
                    String[] array = (content.get(0).split(";"));
                    String fileName = content.get(1);
                    sendFile(fileName, array[0], Integer.parseInt(array[1]));
                }else if (rp.getRequestType() == 5){
                    List<String> content = (List<String>) rp.getContent();
                    readFile(content.get(0), socket);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
