import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DirectoryServer extends Server implements Runnable {
    private String name;
    private List<String> filesList;
    private List<String> addressPortList;
    private ServerSocket serverSocket;
    private String backupAddress;
    private int backupPort;
    private boolean ifBackup;
    private Thread currentThread;
    // Manage threads
    private ThreadPoolExecutor threadPoolExecutor;

    // metric
    private int messagesExchanged = 0;
    private int bytesTransferred = 0;


    public static void main(String[] args) throws IOException{
        if (args.length != 0){
            List<String[]> networkInformation = Utils.readConfigFile(args[0]);
            boolean ifBackup = Boolean.parseBoolean(args[1]);
            String[] mainConfig = networkInformation.get(0);
            String[] backupConfig = networkInformation.get(1);
            List<String> addressPortList = new ArrayList<>();
            if (!ifBackup){
                new DirectoryServer(mainConfig[0], mainConfig[1], Integer.parseInt(mainConfig[2]),
                        addressPortList, backupConfig[1], Integer.parseInt(backupConfig[2]), ifBackup, true);
            }else{
                new DirectoryServer(backupConfig[0], backupConfig[1], Integer.parseInt(backupConfig[2]),
                        addressPortList, mainConfig[1], Integer.parseInt(mainConfig[2]), ifBackup, true);
            }
        }

    }

    public DirectoryServer(String name, String address, int port, List<String> addressPortList, String backupAddress,
                           int backupPort, boolean ifBackup, boolean isInit) throws IOException {
        this.name = name;
        this.address = address;
        this.port = port;
        this.backupAddress = backupAddress;
        this.backupPort = backupPort;
        this.ifBackup = ifBackup;

        if(!isInit) {
            this.recoveryFromBackupServer();
        }

        if (this.addressPortList == null || this.filesList == null){
            this.addressPortList = addressPortList;
            this.filesList = new ArrayList<>();
        }


        this.serverSocket = new ServerSocket(port);
        this.threadPoolExecutor = new ThreadPoolExecutor(4, 8, 1000,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        System.out.println("Activate " + name + " " + address + " " + String.valueOf(port));
        this.currentThread = new Thread(this);
        currentThread.start();
    }

    private void recoveryFromBackupServer(){
        new SocketUtils(this.backupAddress, this.backupPort,
                new RequestPackage(1, this.address, this.port, null)).send();
    }


    synchronized private void synchronizeFromBackupServer(List<String> content, Socket socket){
        this.filesList = content;
        SocketUtils socketUtils = new SocketUtils(socket, null);
        RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
        this.bytesTransferred += socketUtils.getBytesTransferred();
        this.messagesExchanged += 1;
        this.addressPortList = rp.getContent();
    }

    private void synchronizeToBackupServer(){
        SocketUtils socketUtils = null;

        synchronized (this.filesList){
            socketUtils = new SocketUtils(this.backupAddress, this.backupPort,
                    new RequestPackage(5, this.address, this.port, this.filesList));
            socketUtils.send();
        }
        synchronized(this.addressPortList){
            socketUtils.setRequestPackage(new RequestPackage(5, this.address, this.port, this.addressPortList));
            socketUtils.send();
        }

    }

    synchronized public void register(RequestPackage rp) {
        List<String> filesList = (List<String>) rp.getContent();
        synchronized (this.filesList){
            for (String fileName : filesList) {
                if (!this.filesList.contains(fileName)) {
                    System.out.println("Register " + fileName);
                    this.filesList.add(fileName);
                }
            }
        }


        this.addressPortList.add(rp.getRequestAddress() + ";" + String.valueOf(rp.getRequestPort()));

        if (this.addressPortList.size() > 1){
            synchronized (this.addressPortList){
                new SocketUtils(rp.getRequestAddress(), rp.getRequestPort(),
                        new RequestPackage(0, this.address, this.port, this.addressPortList)).send();
            }

            String[] array = this.addressPortList.get(0).split(";");
            String address = array[0];
            int port = Integer.parseInt(array[1]);
            List<String> content = new ArrayList<>();
            content.add(rp.getRequestAddress() + ";" + String.valueOf(rp.getRequestPort()));
            new SocketUtils(address, port, new RequestPackage(4, this.address, this.port, content)).send();
        }
        synchronizeToBackupServer();


    }


    public void connect(Socket socket) {
        List<String> content = new ArrayList<>();
        content.add(this.addressPortList.get(0));
       // System.out.println(this.addressPortList.get(0));
        new SocketUtils(socket,
                new RequestPackage(2, this.address, this.port, content)).send();
    }

    public void getFilesList(Socket socket) {
        synchronized (this.filesList){
            new SocketUtils(socket,
                    new RequestPackage(4, this.address, this.port, this.filesList)).send();
        }
    }


    synchronized public void addNewFile(String fileName, Socket socket){
        synchronized (this.filesList){
            this.filesList.add(fileName);
        }
        System.out.println("Add " + fileName + " to Directory Server!");
        synchronized (this.addressPortList){
            new SocketUtils(socket,
                    new RequestPackage(3, this.getAddress(), this.getPort(), this.addressPortList)).send();
        }

        synchronizeToBackupServer();
    }

    public void deleteDeadNodeFromList(String addressPort){
        synchronized (this.addressPortList){
            if (this.addressPortList.contains(addressPort)){
                addressPortList.remove(addressPort);
            }
        }

        System.out.println("Main Server has deleted node " + addressPort);
        synchronizeToBackupServer();

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
                bytesTransferred += is.available();
                RequestPackage rp = (RequestPackage) ois.readObject();
                // different types mean different requests
                if (rp.getRequestType() == 0) {
                    register(rp);
                    ois.close();
                } else if (rp.getRequestType() == 1) {
                    synchronizeToBackupServer();
                } else if (rp.getRequestType() == 2) {
                    connect(socket);
                } else if (rp.getRequestType() == 3) {
                    addNewFile(rp.getContent().get(0), socket);
                }else if (rp.getRequestType() == 4) {
                    getFilesList(socket);
                }else if (rp.getRequestType() == 5){
                    synchronizeFromBackupServer(rp.getContent(), socket);
                }else if (rp.getRequestType() == 6){
                    deleteDeadNodeFromList(rp.getContent().get(0));
                    ois.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
