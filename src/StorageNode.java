import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class StorageNode extends Server implements Runnable{
    private List<String> filesList;
    private String dataFolder;
    private ServerSocket serverSocket;
    private ThreadPoolExecutor threadPoolExecutor;
    private String dsAddress;
    private int dsPort;
    public StorageNode(String name, String address, int port, String dataFolder) throws IOException {
        this.name = name;
        this.address = address;
        this.port = port;
        this.dataFolder = dataFolder;
        this.serverSocket = new ServerSocket(port);
        this.threadPoolExecutor = new ThreadPoolExecutor(4, 8, 1000,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        initializeLocalFiles();
        System.out.println("Activate " + name + " " + address + " " + String.valueOf(port));
        new Thread(this).start();
    }

    /**
     * Check the folder and put all file names in hash map
     */
    public void initializeLocalFiles(){
        filesList = new ArrayList<>();
        File file = new File(this.dataFolder);
        File[] files = file.listFiles();
        if (files == null)
            return;
        for (int i = 0; i < files.length; i++){
            System.out.println("Register " + files[i].getName());
            filesList.add(files[i].getName().trim());
        }
    }

    public List<String> getFilesList() {
        return filesList;

    }

    public void addFileToSystem(String fileName, File file){

    }

    synchronized public void receiveFileToLocal(String fileName, Socket socket) throws Exception{
        if (filesList.contains(fileName))
            return;
        String path = dataFolder + "/" + fileName;

        BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));

        byte [] buf = new byte [1024];
        int len = 0;
        while((len = bis.read(buf))!=-1){
            bos.write(buf, 0, len);
        }
        bos.flush();
        bos.close();
        bis.close();
        System.out.println("Download " + fileName + " successfully!");
        filesList.add(fileName);

    }

    public void readFile(String fileName){

    }

    public void register(List<String> addressPortList) throws Exception {
        Socket socket = null;
        for (String addressPort: addressPortList){
            String[] array = addressPort.split(";");
            String address = array[0];
            int port = Integer.parseInt(array[1]);
            for (String fileName : filesList){
                socket = new Socket(address, port);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                List<String> content = new ArrayList<>();
                content.add(fileName);
                oos.writeObject(new RequestPackage(1, this.address, this.port, content));
                oos.flush();

                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(this.dataFolder + "/" + fileName));

                BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

                byte[] buf = new byte[1024];
                int len = 0;
                while ((len = bis.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                bos.flush();
                oos.close();
                bos.close();
                bis.close();

            }
        }
    }

    public void setDirectoryServer(String dsAddress, int dsPort){
        this.dsAddress = dsAddress;
        this.dsPort = dsPort;
        try {
            Socket socket = new Socket(dsAddress, dsPort);
            RequestPackage rp = new RequestPackage(0, this.address, this.port, this.filesList);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(rp);
            oos.flush();
            oos.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    register(rp.getContent());
                }else if (rp.getRequestType() == 1){
                    receiveFileToLocal(rp.getContent().get(0), socket);
                }
                ois.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
