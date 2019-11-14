import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client extends Server {
    private String nodeAddress;
    private int nodePort;
    private String dsAddress;
    private int dsPort;
    private String backupAddress;
    private int backupPort;
    private String cache = "./ClientCache";

    // metric
    private int messagesExchanged = 0;
    private int bytesTransferred = 0;
    private long responseTime = 0;

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
        this.dsAddress = dsAddress; //record the information of the directory server
        this.dsPort = dsPort;
        this.backupAddress = backupAddress;
        this.backupPort = backupPort;
    }

    public void connectToSystem() {

        SocketUtils socketUtils = new SocketUtils(dsAddress, dsPort, backupAddress, backupPort,
                new RequestPackage(2, this.address, this.port, null));
        socketUtils.send();

        RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
        this.messagesExchanged += 1;
        this.bytesTransferred += 1;
        String addressPort = rp.getContent().get(0);
        String[] array = addressPort.split(";");
        nodeAddress = array[0];
        nodePort = Integer.parseInt(array[1]);
        System.out.println("Connect storage node " + array[0] + ":" + array[1]);
    }

    public void createNewFile(String fileName) {
        List<String> content = new ArrayList<>();
        content.add(fileName);
        long startTime = System.nanoTime();
        boolean flag = new SocketUtils(nodeAddress, nodePort,
                new RequestPackage(3, this.address, this.port, content)).send();
        if (!flag){
            notifyServerNodeDead();
            connectToSystem();
            long endTime = System.nanoTime();
            this.responseTime += endTime - startTime;
            createNewFile(fileName);
        } else{
            long endTime = System.nanoTime();
            this.responseTime += endTime - startTime;
        }


    }

    public void readFile(String fileName){
        List<String> content = new ArrayList<String>();
        content.add(fileName);
        long startTime = System.nanoTime();
        SocketUtils socketUtils = new SocketUtils(this.nodeAddress, this.nodePort,
                new RequestPackage(5, this.address, this.port, content));
        boolean flag = socketUtils.send();
        if (flag){
            String tempPath = cache + "/" + fileName;
            socketUtils.readFileFromSocket(tempPath);
            long endTime = System.nanoTime();
            this.responseTime += endTime - startTime;
            this.messagesExchanged += 1;
            this.bytesTransferred += socketUtils.getBytesTransferred();
            try  { //print file
                BufferedReader br = new BufferedReader(new FileReader(tempPath));
                String line = null;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
                br.close();
                File file = new File(tempPath);
                file.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }else{
            notifyServerNodeDead();
            connectToSystem();
            long endTime = System.nanoTime();
            this.responseTime += endTime - startTime;
            readFile(fileName);
        }

    }

    public void getFilesListFromDirectoryServer() {
        long startTime = System.nanoTime();
        SocketUtils socketUtils = new SocketUtils(this.dsAddress, this.dsPort, backupAddress, backupPort,
                new RequestPackage(4, this.address, this.port, null));
        socketUtils.send();

        RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
        long endTime = System.nanoTime();
        this.responseTime += endTime - startTime;
        this.messagesExchanged += 1;
        this.bytesTransferred = socketUtils.getBytesTransferred();
        List<String> fileList = rp.getContent();

        for (String fileName : fileList) {
            System.out.println(fileName);
        }

    }


    public void getFilesListFromNode() {
        long startTime = System.nanoTime();
        SocketUtils socketUtils = new SocketUtils(this.nodeAddress, this.nodePort,
                new RequestPackage(2, this.address, this.port, null));
        boolean flag = socketUtils.send();

        if (flag){
            RequestPackage rp = (RequestPackage) socketUtils.readObjectFromSocket(true);
            long endTime = System.nanoTime();
            this.responseTime += endTime - startTime;
            this.messagesExchanged += 1;
            this.bytesTransferred += socketUtils.getBytesTransferred();

            List<String> fileList = rp.getContent();

            for (String fileName : fileList) {
                System.out.println(fileName);
            }
        }else{
            notifyServerNodeDead();
            connectToSystem();
            long endTime = System.nanoTime();
            this.responseTime += endTime - startTime;
            getFilesListFromNode();
        }

    }

    public void notifyServerNodeDead(){
        List<String> content = new ArrayList<>();
        content.add(nodeAddress + ";" + String.valueOf(nodePort)); //pass the dead node information to both directory server
        SocketUtils socketUtils = new SocketUtils(this.dsAddress, this.dsPort, backupAddress, backupPort,
                new RequestPackage(6, this.address, this.port, content));
        socketUtils.send();
        System.out.println("node" + content.get(0) + "is dead!");

    }

    public int getMessagesExchanged() {
        return messagesExchanged;
    }

    public int getBytesTransferred() {
        return bytesTransferred;
    }

    public long getResponseTime() {
        return responseTime;
    }
}
