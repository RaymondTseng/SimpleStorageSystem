import javax.management.ObjectName;
import java.io.IOException;
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
    // Manage threads
    private ThreadPoolExecutor threadPoolExecutor;

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

        this.synchronizeWithBackupServer();

        if (this.addressPortList == null || this.filesList == null){
            this.addressPortList = addressPortList;
            this.filesList = new ArrayList<>();
        }


        this.serverSocket = new ServerSocket(port);
        this.threadPoolExecutor = new ThreadPoolExecutor(4, 8, 1000,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        System.out.println("Activate " + name + " " + address + " " + String.valueOf(port));
        new Thread(this).start();
    }

    private void synchronizeWithBackupServer(){
        SocketUtils socketUtils = new SocketUtils(backupAddress, backupPort,
                new RequestPackage(5, this.address, this.port, null));
        boolean flag = socketUtils.send();

        if (!flag)
            return;  //if it is the main server, stop here. In case of the backup Server is dead?

        RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(false);
        this.filesList = rp.getContent();

        rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
        this.addressPortList = rp.getContent();
    }

    synchronized public void register(RequestPackage rp) {
        List<String> filesList = (List<String>) rp.getContent();
        for (String fileName : filesList) {
            if (!this.filesList.contains(fileName)) {
                this.filesList.add(fileName);
            }
        }
        if (this.addressPortList.size() == 0){
            this.addressPortList.add(rp.getRequestAddress() + ";" + String.valueOf(rp.getRequestPort()));
            return;
        }
        this.addressPortList.add(rp.getRequestAddress() + ";" + String.valueOf(rp.getRequestPort()));
        new SocketUtils(rp.getRequestAddress(), rp.getRequestPort(),
                new RequestPackage(0, this.address, this.port, this.addressPortList)).send();

        String[] array = this.addressPortList.get(0).split(";");
        String address = array[0];
        int port = Integer.parseInt(array[1]);
        List<String> content = new ArrayList<>();
        content.add(rp.getRequestAddress() + ";" + String.valueOf(rp.getRequestPort()));
        new SocketUtils(address, port, new RequestPackage(4, this.address, this.port, content)).send();

    }

    public void getAllNodes(Socket socket) {
        new SocketUtils(socket,
                new RequestPackage(3, this.getAddress(), this.getPort(), this.addressPortList)).send();
    }

    public void connect(Socket socket) {
        List<String> content = new ArrayList<>();
        content.add(this.addressPortList.get(0));
       // System.out.println(this.addressPortList.get(0));
        new SocketUtils(socket,
                new RequestPackage(2, this.address, this.port, content)).send();
    }

    public void getFilesList(Socket socket) {
        new SocketUtils(socket,
                new RequestPackage(4, this.address, this.port, this.filesList)).send();
    }

    public void getServerInformation(Socket socket){
        SocketUtils socketUtils = new SocketUtils(socket, new RequestPackage(5, this.address, this.port, this.filesList));
        socketUtils.send();
        socketUtils.setRequestPackage(new RequestPackage(5, this.address, this.port, this.addressPortList));
        socketUtils.send();
    }
    public void deleteDeadNodeFromList(Socket socket){
        SocketUtils socketUtils = new SocketUtils(socket, null);
        RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
        String deadNodeAddressPort = rp.getContent().get(0);
        addressPortList.remove(deadNodeAddressPort);
        System.out.println("Main Server has deleted node" + deadNodeAddressPort);
        notifyBackupDeadNode(deadNodeAddressPort);

    }

    public void notifyBackupDeadNode(String deadNodeAddressPort){
        List<String> content = new ArrayList<>();
        content.add(deadNodeAddressPort);
        SocketUtils socketUtils = new SocketUtils(backupAddress, backupPort, new RequestPackage(7, this.address, this.port, content));
        socketUtils.send();
        System.out.println("Main server has notified the backup server");
    }

    public void backupDeleteNodeFromList(Socket socket){
        SocketUtils socketUtils = new SocketUtils(socket, null);
        RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
        String deadNodeAddressPort = rp.getContent().get(0);
        addressPortList.remove(deadNodeAddressPort);
        System.out.println("Backup Server has deleted node" + deadNodeAddressPort);
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
                    register(rp);
                    ois.close();
                } else if (rp.getRequestType() == 1) {

                } else if (rp.getRequestType() == 2) {
                    connect(socket);
                } else if (rp.getRequestType() == 3) {
                    filesList.add(rp.getContent().get(0));
                    getAllNodes(socket);
                    synchronizeWithBackupServer();
                }else if (rp.getRequestType() == 4) {
                    getFilesList(socket);
                }else if (rp.getRequestType() == 5){
                    getServerInformation(socket);
                }else if (rp.getRequestType() == 6){
                    deleteDeadNodeFromList(socket);
                }else if (rp.getRequestType() == 7){
                    backupDeleteNodeFromList(socket);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
