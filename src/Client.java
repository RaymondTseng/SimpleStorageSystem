import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client extends Server {
    private String nodeAddress;
    private int nodePort;
    private String serverAddress;
    private int serverPort;
    private String backupAddress;
    private int backupPort;
    private String cache = "../ClientCache";

    public static void main(String[] args){
        Client client = new Client("localhost", 23333, "localhost", 8123, "localhost", 8888);
        client.connectToSystem();
        Scanner userInput = new Scanner(System.in);
        while (true){
            System.out.println("*******************************************************");
            System.out.println("Enter 1 : Get Files List From Storage Node.");
            System.out.println("Enter 2 : Get Files List From Directory Server.");
            System.out.println("Enter 3 : Read File");
            System.out.println("Enter 4 : Add new file.");
            System.out.println("Enter 5 : Exit.");
            System.out.println("*******************************************************");
            String varInput = userInput.nextLine();
            switch (varInput){
                case "1":
                    client.getFilesListFromNode();
                    break;
                case "2":
                    client.getFilesListFromDirectoryServer();
                    break;
                case "3":
                    System.out.println("Please enter the file's name");
                    varInput = userInput.nextLine();
                    client.readFile(varInput);
                    break;
                case "4":
                    System.out.println("Please enter the file's name");
                    varInput = userInput.nextLine();
                    client.createNewFile(varInput);
                    break;
                case "5":
                    System.exit(0);
                    break;
                default:
                    System.out.println("Wrong Command, Please try again!");
                    break;
            }
        }

    }

    public Client(String address, int port, String dsAddress, int dsPort, String backupAddress, int backupPort) {
        this.address = address;
        this.port = port;
        this.serverAddress = address; //record the information of the directory server
        this.serverPort = port;
        this.backupAddress = backupAddress;
        this.backupPort = backupPort;
    }

    public void connectToSystem() {

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
        boolean flag = new SocketUtils(nodeAddress, nodePort,
                new RequestPackage(3, this.address, this.port, content)).send();
        if (! flag){
            tellServerNodeDead();
            connectToSystem();
        }

    }

    public void readFile(String fileName){
        List<String> fileNames = new ArrayList<String>();
        fileNames.add(fileName);
        SocketUtils socketUtils = new SocketUtils(this.nodeAddress, this.nodePort,
                new RequestPackage(5, this.address, this.port, fileNames));
        boolean flag = socketUtils.send();
        if (flag){
            String tempPath = cache + "/" + fileName;
            String intellijAddress = tempPath.substring(1, tempPath.length());
            socketUtils.readFileFromSocket(intellijAddress);
            try (BufferedReader br = new BufferedReader(new FileReader(intellijAddress))) { //print file
                String line = null;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
                File file = new File(intellijAddress);
                file.delete();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }else{
            tellServerNodeDead();
            connectToSystem();
        }

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
        boolean flag = socketUtils.send();

        if (flag){
            RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
            List<String> fileList = rp.getContent();

            for (String fileName : fileList) {
                System.out.println(fileName);
            }
        }else{
            tellServerNodeDead();
            connectToSystem();
        }

    }

    public void tellServerNodeDead(){
        List<String> content = new ArrayList<>();
        content.add(nodeAddress + ";" + nodePort); //pass the dead node information to both directory server
        SocketUtils socketUtils = new SocketUtils(this.serverAddress, this.serverPort, backupAddress, backupPort,
                new RequestPackage(6, this.address, this.port, content));
        System.out.println("node" + content.get(0) + "is dead!");
        socketUtils.send();

    }
}
