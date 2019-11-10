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
        DirectoryServer directoryServer = new DirectoryServer("directoryServer", "localhost",
                8123, null, "localhost", 8888, false);
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
            return;

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
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
