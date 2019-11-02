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
        System.out.println("Activate " + name + " " + address + " " + String.valueOf(port));
        new Thread(this).start();
    }

    public List<String> getFilesList() {
        return filesList;

    }

    public void addFileToSystem(String fileName, File file){

    }

    public void receiveFileToLocal(String fileName, File file) throws Exception{
        String path = dataFolder + "/" + fileName;
        File destFile = new File(path);
        FileChannel inputChannel = new FileInputStream(file).getChannel();
        FileChannel outputChannel = new FileOutputStream(destFile).getChannel();
        outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        inputChannel.close();
        outputChannel.close();
    }

    public void readFile(String fileName){

    }

    public void register() throws IOException {
        Socket socket = new Socket(this.dsAddress, this.dsPort);
        RequestPackage rp = new RequestPackage(0, this.address, this.port, this.filesList);
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(rp);
        oos.flush();
        oos.close();
    }

    public void setDsAddress(String dsAddress) {
        this.dsAddress = dsAddress;
    }

    public void setDsPort(int dsPort) {
        this.dsPort = dsPort;
    }

    @Override
    public void run() {

    }
}
