import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class StorageSystem {
    private DirectoryServer directoryServer;
    private List<StorageNode> nodesList;
    private DirectoryServer backupServer;
    public static void main(String[] args) throws Exception {
        String configFilePath = "./config.txt";
        List<String[]> systemInformation = readConfigFile(configFilePath);
        StorageSystem storageSystem = new StorageSystem(systemInformation);
        Thread.sleep(3000);
        storageSystem.manualMode();
        System.out.println("done!");

    }

    public StorageSystem(List<String[]> systemInformation) {
        initializeSystem(systemInformation);
    }

    public void initializeSystem(List<String[]> systemInformation) {
        this.nodesList = new ArrayList<>();
        List<String> addressPortList = new ArrayList<>();
        String dsName = "";
        String dsAddress = "";
        int dsPort = -1;
        String backupName = "";
        String backupAddress = "";
        int backupPort = -1;

        try {
            for (int i = 0; i < systemInformation.size(); i++) {
                String[] configArray = systemInformation.get(i);
                if (i == 0) {  //initialize the directory server
                    dsName = configArray[0];
                    dsAddress = configArray[1];
                    dsPort = Integer.parseInt(configArray[2]);
                }
                else if(i == 1){  //initialize the backup server
                    backupName = configArray[0];
                    backupAddress = configArray[1];
                    backupPort = Integer.parseInt(configArray[2]);
                }


                else {       //initialize every node in the system
                    this.nodesList.add(new StorageNode(configArray[0], configArray[1], Integer.parseInt(configArray[2]), configArray[3]));
                    addressPortList.add(configArray[1] + ";" + configArray[2]);
                }
            }
            this.directoryServer = new DirectoryServer(dsName, dsAddress, dsPort, addressPortList, backupAddress, backupPort, false);
            this.backupServer = new DirectoryServer(backupName, backupAddress, backupPort, addressPortList, dsAddress, dsPort, true);
            for (StorageNode node : nodesList) {
                node.setDirectoryServer(directoryServer.getAddress(), directoryServer.getPort());
                node.setBackupServer(backupServer.getAddress(), backupServer.getPort());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String[]> readConfigFile(String configFilePath) {
        List<String[]> networkInformation = new ArrayList<String[]>();
        BufferedReader bufferedReader;
        try {
            File file = new File(configFilePath);
            bufferedReader = new BufferedReader(new FileReader(file));
            String strLine = null;
            while (null != (strLine = bufferedReader.readLine())) {
                String[] configArray = strLine.split(" ");
                if (configArray[0].contains("Server")) { //record the information of the Servers
                    networkInformation.add(new String[]{configArray[0], configArray[1], configArray[2]});
                } else {
                    networkInformation.add(configArray);
                }
            }
            bufferedReader.close();
            return networkInformation;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void manualMode() throws Exception {
        while (true) {
            System.out.println("*******************************************************");
            System.out.println("Enter 1 : Add a new storage node to current system.");
            System.out.println("Enter 2 : Build a client to connect current system.");
            System.out.println("Enter 3 : Exit the program.");
            System.out.println("*******************************************************");

            Scanner userInput = new Scanner(System.in);
            String varInput = userInput.nextLine();

            switch (varInput) {
                case "1": {
                    System.out.println("Please enter the new node's name");
                    String nodeName = userInput.nextLine();
                    System.out.println("Please enter the new node's folder");
                    String nodeFolder = userInput.nextLine();
                    System.out.println("Please enter the new node's port");
                    String nodePort = userInput.nextLine();
                    createNewNode(nodeName, "localhost", nodeFolder, Integer.parseInt(nodePort));
                    break;
                }
                case "2": {
                    Client client = new Client("localhost", 23333);
                    client.connectToSystem(directoryServer.getAddress(), directoryServer.getPort(),
                            backupServer.getAddress(), backupServer.getPort());
                    while (true) {
                        System.out.println("*******************************************************");
                        System.out.println("Enter 1 : Get Files List From Directory Server.");
                        System.out.println("Enter 2 : Add a new File.");
                        System.out.println("Enter 3 : Get Files List connected Storage Node.");
                        System.out.println("Enter 4 : Go back.");
                        System.out.println("*******************************************************");

                        varInput = userInput.nextLine();

                        if (varInput.equals("1")) {
                            client.getFilesListFromDirectoryServer();
                        } else if (varInput.equals("2")) {
                            System.out.println("Enter new file name.");
                            varInput = userInput.nextLine();
                            client.createNewFile(varInput);
                            continue;
                        } else if (varInput.equals("3")){
                            client.getFilesListFromNode();
                        }
                        else if (varInput.equals("4")) {
                            break;
                        } else {
                            System.out.println("Wrong command, Please try again!!");
                        }
                    }

                    break;
                }
                case "3":
                    System.exit(0);
                    break;
                default:
                    System.out.println("Wrong command, Please try again!!");
                    break;
            }
        }

    }

    private void createNewNode(String name, String address, String folder, int port) throws Exception {
        for (StorageNode node : nodesList) {
            if (name.equals(node.getName())) {
                System.out.println("Node name already exists!");
                return;
            }
        }
        File file = new File(folder);
        if (file.exists()) {
            System.out.println("Node folder already exists!");
            return;
        }
        boolean res = isPortUsing(port);
        if (res) {
            System.out.println("This port is using by other program!");
            return;
        }
        res = file.mkdirs();
        if (!res) {
            System.out.println("Fail to create node folder!");
            return;
        }
        StorageNode node = new StorageNode(name, address, port, folder);
        this.nodesList.add(node);
        node.setDirectoryServer(directoryServer.getAddress(), directoryServer.getPort());
        node.setBackupServer(backupServer.getAddress(), backupServer.getPort());
    }

    private boolean isPortUsing(int port) {
        boolean flag = false;
        try {
            Socket socket = new Socket("localhost", port);
            flag = true;
            socket.close();
        } catch (Exception e) {

        }
        return flag;
    }
}
