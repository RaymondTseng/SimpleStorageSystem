import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client extends Server {
    private String nodeAddress;
    private int nodePort;
    private String serverAddress;
    private int serverPort;
    private String backupAddress;
    private int backupPort;

    public static void main(String[] args){
        Client client = new Client("localhost", 23333);
        client.connectToSystem("localhost", 8123, "localhost", 8888);
        client.getFilesListFromNode();
    }

    public Client(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public void connectToSystem(String address, int port, String backupAddress, int backupPort) {
        this.serverAddress = address; //record the information of the directory server
        this.serverPort = port;
        this.backupAddress = backupAddress;
        this.backupPort = backupPort;
        SocketUtils socketUtils = new SocketUtils(address, port, backupAddress, backupPort,
                new RequestPackage(2, this.address, this.port, null));
        socketUtils.send();

        RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(true);

        String addressPort = rp.getContent().get(0);
        String[] array = addressPort.split(";");
        nodeAddress = array[0];
        nodePort = Integer.parseInt(array[1]);
    }

    public void createNewFile(String fileName) {
        List<String> content = new ArrayList<>();
        content.add(fileName);
        new SocketUtils(nodeAddress, nodePort,
                new RequestPackage(3, this.address, this.port, content)).send();

    }
    public void getFilesListFromDirectoryServer() {
        SocketUtils socketUtils = new SocketUtils(this.serverAddress, this.serverPort, backupAddress, backupPort,
                new RequestPackage(4, this.address, this.port, null));
        socketUtils.send();

        RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
        List<String> fileList = rp.getContent();

        for (String fileName : fileList) {
            System.out.println(fileName);
        }

    }


    public void getFilesListFromNode() {
        SocketUtils socketUtils = new SocketUtils(this.nodeAddress, this.nodePort,
                new RequestPackage(2, this.address, this.port, null));
        socketUtils.send();

        RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
        List<String> fileList = rp.getContent();

        for (String fileName : fileList) {
            System.out.println(fileName);
        }
    }
}
