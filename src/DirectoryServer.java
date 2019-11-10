import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    // Manage threads
    private ThreadPoolExecutor threadPoolExecutor;

    public DirectoryServer(String name, String address, int port, List<String> addressPortList) throws IOException {
        this.name = name;
        this.address = address;
        this.port = port;
        this.addressPortList = addressPortList;

        filesList = new ArrayList<>();

        this.serverSocket = new ServerSocket(port);
        this.threadPoolExecutor = new ThreadPoolExecutor(4, 8, 1000,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        System.out.println("Activate " + name + " " + address + " " + String.valueOf(port));
        new Thread(this).start();
    }

    synchronized public void register(RequestPackage rp) throws Exception {
        List<String> filesList = (List<String>) rp.getContent();
        for (String fileName : filesList) {
            if (!this.filesList.contains(fileName)) {
                this.filesList.add(fileName);
            }
        }

        Socket _socket = new Socket(rp.getRequestAddress(), rp.getRequestPort());
        ObjectOutputStream oos = new ObjectOutputStream(_socket.getOutputStream());
        oos.writeObject(new RequestPackage(0, this.address, this.port, this.addressPortList));
        oos.flush();
        oos.close();

    }

    public void getAllNodes(Socket socket) {
        RequestPackage rp = new RequestPackage(3, this.getAddress(), this.getPort(), this.addressPortList);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(rp);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect(Socket socket) {
        List<String> content = new ArrayList<>();
        content.add(this.addressPortList.get(0));
        RequestPackage rp = new RequestPackage(2, this.address, this.port, content);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(rp);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getFilesList(Socket socket) {
        RequestPackage rp = new RequestPackage(4, this.address, this.port, this.filesList);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(rp);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deadServerRecover(){

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
                } else if (rp.getRequestType() == 1) {

                } else if (rp.getRequestType() == 2) {
                    connect(socket);
                } else if (rp.getRequestType() == 3) {
                    getAllNodes(socket);
                }else if (rp.getRequestType() == 4) {
                    getFilesList(socket);
                }
                ois.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
