import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DirectoryServer extends Server implements Runnable{
    private String name;
    private List<String> filesList;
    private List<String> addressPortList;
    private ServerSocket serverSocket;
    // Manage threads
    private ThreadPoolExecutor threadPoolExecutor;
    public DirectoryServer(String name, String address, int port) throws IOException {
        this.name = name;
        this.address = address;
        this.port = port;

        filesList = new ArrayList<>();
        addressPortList = new ArrayList<>();

        this.serverSocket = new ServerSocket(port);
        this.threadPoolExecutor = new ThreadPoolExecutor(4, 8, 1000,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        System.out.println("Activate " + name + " " + address + " " + String.valueOf(port));
        new Thread(this).start();
    }

    public void register(Socket socket) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        RequestPackage rp = (RequestPackage) ois.readObject();
        List<String> filesList = (List<String>) rp.getContent();

    }

    public void newFile(String fileName, File file){
        for (String addressPort : addressPortList){
            String[] array = addressPort.split(";");

        }
        filesList.add(fileName);
    }

    public void connect(){

    }

    public List<String> getFilesList(){
        return filesList;
    }

    @Override
    public void run() {
        Socket socket = null;
        while (true){
            try {
                socket = this.serverSocket.accept();
                threadPoolExecutor.execute(new Task(socket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Task implements Runnable{
        private Socket socket;
        public Task(Socket socket){
            this.socket = socket;
        }
        @Override
        public void run() {
            ObjectInputStream ois = null;
            try{
                ois = new ObjectInputStream(this.socket.getInputStream());
                RequestPackage rp = (RequestPackage) ois.readObject();
                // different types mean different requests
                if (rp.getRequestType() == 0){
                    register(socket);
                }else if (rp.getRequestType() == 1){

                }
                ois.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
