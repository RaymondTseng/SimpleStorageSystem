import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class StorageSystem {
    private DirectoryServer directoryServer;
    private List<StorageNode> nodesList;
    public static void main(String[] args) throws Exception{
        String configFilePath = "./config.txt";
        List<String[]> systemInformation = readConfigFile(configFilePath);
        StorageSystem storageSystem = new StorageSystem(systemInformation);
        Thread.sleep(3000);
        System.out.println("done!");

    }

    public StorageSystem(List<String[]> systemInformation){
        initializeSystem(systemInformation);
    }

    public void initializeSystem(List<String[]> systemInformation){
        this.nodesList = new ArrayList<>();
        List<String> addressPortList = new ArrayList<>();
        String dsName = "";
        String dsAddress = "";
        int dsPort = -1;
        try {
            for (int i = 0; i < systemInformation.size(); i++) {
                String[] configArray = systemInformation.get(i);
                if (i == 0) {
                    dsName = configArray[0];
                    dsAddress = configArray[1];
                    dsPort = Integer.parseInt(configArray[2]);
                } else {
                    this.nodesList.add(new StorageNode(configArray[0], configArray[1], Integer.parseInt(configArray[2]), configArray[3]));
                    addressPortList.add(configArray[1] + ";" + configArray[2]);
                }
            }
            this.directoryServer = new DirectoryServer(dsName, dsAddress, dsPort, addressPortList);
            for (StorageNode node : nodesList){
                node.setDirectoryServer(directoryServer.getAddress(), directoryServer.getPort());
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static List<String[]>  readConfigFile(String configFilePath){
        List<String[]> networkInformation = new ArrayList<String []>();
        BufferedReader bufferedReader;
        try {
            File file = new File(configFilePath);
            bufferedReader = new BufferedReader(new FileReader(file));
            String strLine = null;
            while(null != (strLine = bufferedReader.readLine())){
                String[] configArray = strLine.split(" ");
                if (configArray[0].equals("DirectoryServer")){
                    networkInformation.add(new String[] {configArray[0], configArray[1], configArray[2]});
                }else{
                    networkInformation.add(configArray);
                }
            }
            bufferedReader.close();
            return networkInformation;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public void manualMode() throws Exception{
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
                    break;
                }
                case "2": {
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
}
