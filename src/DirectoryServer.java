import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;

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
    // deal with concurrent new nodes
    private Map<String, Set> filesRecorder;
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

    // implement polling algorithm
    private int counter = 0;


    public static void main(String[] args) throws IOException{
        if (args.length != 0){
            List<String[]> networkInformation = Utils.readConfigFile(args[0]);
            boolean ifBackup = Boolean.parseBoolean(args[1]);
            String[] mainConfig = networkInformation.get(0);
            String[] backupConfig = networkInformation.get(1);
            List<String> addressPortList = new ArrayList<>();
            if (!ifBackup){
                new DirectoryServer(mainConfig[0], mainConfig[1], Integer.parseInt(mainConfig[2]),
                        addressPortList, backupConfig[1], Integer.parseInt(backupConfig[2]), ifBackup);
            }else{
                new DirectoryServer(backupConfig[0], backupConfig[1], Integer.parseInt(backupConfig[2]),
                        addressPortList, mainConfig[1], Integer.parseInt(mainConfig[2]), ifBackup);
            }
        }

    }

    public DirectoryServer(String name, String address, int port, List<String> addressPortList, String backupAddress,
                           int backupPort, boolean ifBackup) throws IOException {
        this.name = name;
        this.address = address;
        this.port = port;
        this.backupAddress = backupAddress;
        this.backupPort = backupPort;
        this.ifBackup = ifBackup;


            this.recoveryFromBackupServer();


        if (this.addressPortList == null || this.filesList == null){
            this.addressPortList = addressPortList;
            this.filesList = new ArrayList<>();
            this.filesRecorder = new HashMap<>();
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
        RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(false);
        this.addressPortList = (List<String>) rp.getContent();
        rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
        this.bytesTransferred += socketUtils.getBytesTransferred();
        this.messagesExchanged += 1;
        this.filesRecorder = (Map<String, Set>) rp.getContent();
    }

    private void synchronizeToBackupServer(){
        SocketUtils socketUtils = null;

        synchronized (this.filesList){
            socketUtils = new SocketUtils(this.backupAddress, this.backupPort,
                    new RequestPackage<>(5, this.address, this.port, this.filesList));
            socketUtils.send();
        }
        synchronized(this.addressPortList){
            socketUtils.setRequestPackage(new RequestPackage<>(5, this.address, this.port, this.addressPortList));
            socketUtils.send();
        }
        synchronized(this.filesRecorder){
            socketUtils.setRequestPackage(new RequestPackage<>(5, this.address, this.port, this.filesRecorder));
            socketUtils.send();
        }

    }

    synchronized public void register(RequestPackage rp) {
        List<String> filesList = (List<String>) rp.getContent();
        synchronized (this.filesList){
            synchronized (this.filesRecorder){
                for (String fileName : filesList) {
                    if (!this.filesList.contains(fileName)) {
                        System.out.println("Register " + fileName);
                        this.filesList.add(fileName);
                        Set<String> set = new HashSet<>();
                        set.add(rp.getRequestAddress() + ";" + String.valueOf(rp.getRequestPort()));
                        this.filesRecorder.put(fileName, set);
                    }
                }
            }
        }


        this.addressPortList.add(rp.getRequestAddress() + ";" + String.valueOf(rp.getRequestPort()));

        if (this.addressPortList.size() > 1){
            synchronized (this.addressPortList){
                new SocketUtils(rp.getRequestAddress(), rp.getRequestPort(),
                        new RequestPackage(0, this.address, this.port, this.addressPortList)).send();
            }
            for (Map.Entry<String, Set> entry : this.filesRecorder.entrySet()){
                String fileName = entry.getKey();
                String[] array = ((String) entry.getValue().iterator().next()).split(";");
                String address = array[0];
                int port = Integer.parseInt(array[1]);
                List<String> content = new ArrayList<>();
                content.add(rp.getRequestAddress() + ";" + String.valueOf(rp.getRequestPort()));
                content.add(fileName);
                new SocketUtils(address, port, new RequestPackage(4, this.address, this.port, content)).send();
            }

        }
        synchronizeToBackupServer();


    }


    public void connect(Socket socket) {
        List<String> content = new ArrayList<>();
        content.add(this.addressPortList.get(counter % this.addressPortList.size()));
        counter ++;
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


    public void addNewFile(String fileName, Socket socket){
        synchronized (this.filesList){
            this.filesList.add(fileName);
        }
        System.out.println("Add " + fileName + " to Directory Server!");
        synchronized (this.addressPortList){
            new SocketUtils(socket,
                    new RequestPackage(3, this.getAddress(), this.getPort(), this.addressPortList)).send();
        }
        synchronized (this.filesRecorder){
            Set<String> set = new HashSet<>();
            set.add(socket.getInetAddress() + ";" + String.valueOf(socket.getPort()));
            this.filesRecorder.put(fileName, set);
        }

        synchronizeToBackupServer();
    }

    public void updateFilesRecorder(String fileName, String addressPort){
        synchronized (this.filesRecorder){
            this.filesRecorder.get(fileName).add(addressPort);
        }
    }

    public void deleteDeadNodeFromList(String addressPort){
        synchronized (this.addressPortList){
            if (this.addressPortList.contains(addressPort)){
                addressPortList.remove(addressPort);
            }
        }
        synchronized (this.filesRecorder){
            for (Map.Entry<String, Set> entry : this.filesRecorder.entrySet()){
                Set<String> set = entry.getValue();
                if(set.contains(addressPort))
                    set.remove(addressPort);
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
                RequestPackage rp = (RequestPackage) ois.readObject();
                bytesTransferred += ObjectSizeCalculator.getObjectSize(rp);
                // different types mean different requests
                if (rp.getRequestType() == 0) {
                    register(rp);
                    ois.close();
                } else if (rp.getRequestType() == 1) {
                    synchronizeToBackupServer();
                } else if (rp.getRequestType() == 2) {
                    connect(socket);
                } else if (rp.getRequestType() == 3) {
                    List<String> content = (List<String>) rp.getContent();
                    addNewFile(content.get(0), socket);
                }else if (rp.getRequestType() == 4) {
                    getFilesList(socket);
                }else if (rp.getRequestType() == 5){
                    synchronizeFromBackupServer((List<String>) rp.getContent(), socket);
                }else if (rp.getRequestType() == 6){
                    List<String> content = (List<String>) rp.getContent();
                    deleteDeadNodeFromList(content.get(0));
                    ois.close();
                }else if (rp.getRequestType() == 7){
                    String fileName = (String) rp.getContent();
                    updateFilesRecorder(fileName, rp.getRequestAddress() + ";" + String.valueOf(rp.getRequestPort()));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
