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

    public Client(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public void connectToSystem(String address, int port) {
        try {
            Socket socket = new Socket(address, port);
            RequestPackage rp = new RequestPackage(2, this.address, this.port, null);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(rp);
            oos.flush();

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            rp = (RequestPackage) ois.readObject();
            String addressPort = rp.getContent().get(0);
            String[] array = addressPort.split(";");
            nodeAddress = array[0];
            nodePort = Integer.parseInt(array[1]);
            oos.close();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createNewFile(String fileName) {
        try {
            Socket socket = new Socket(this.nodeAddress, this.nodePort);
            List<String> content = new ArrayList<>();
            content.add(fileName);
            RequestPackage rp = new RequestPackage(3, this.address, this.port, content);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(rp);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getFilesList() {
        try {
            Socket socket = new Socket(this.nodeAddress, this.nodePort);
            RequestPackage rp = new RequestPackage(2, this.address, this.port, null);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(rp);
            oos.flush();

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            rp = (RequestPackage) ois.readObject();
            List<String> fileList = rp.getContent();
            oos.close();
            ois.close();
            for (String fileName : fileList) {
                System.out.println(fileName);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
