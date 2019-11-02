import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class StorageSystem {
    private DirectoryServer directoryServer;
    private List<StorageNode> nodesList;
    public static void main(String[] args){
        String configFilePath = "./config.txt";
        List<String[]> systemInformation = readConfigFile(configFilePath);
        StorageSystem storageSystem = new StorageSystem(systemInformation);

    }

    public StorageSystem(List<String[]> systemInformation){
        initializeSystem(systemInformation);
    }

    public void initializeSystem(List<String[]> systemInformation){
        this.nodesList = new ArrayList<>();
        try {
            for (int i = 0; i < systemInformation.size(); i++) {
                String[] configArray = systemInformation.get(i);
                if (i == 0) {
                    this.directoryServer = new DirectoryServer(configArray[0], configArray[1], Integer.parseInt(configArray[2]));
                } else {
                    this.nodesList.add(new StorageNode(configArray[0], configArray[1], Integer.parseInt(configArray[2]), configArray[3]));
                }
            }
            for (StorageNode node : nodesList){
                node.setDsAddress(directoryServer.getAddress());
                node.setDsPort(directoryServer.getPort());
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
                if (configArray[0].equals("DirectorySystem")){
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
}
