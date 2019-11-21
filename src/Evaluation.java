import javafx.scene.input.KeyCode;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * A class for evaluate this system
 */
public class Evaluation {
    public static void main(String[] args) throws Exception{
        if (args.length != 0){
            int M = Integer.parseInt(args[0]);
            int N = Integer.parseInt(args[1]);
            int f = Integer.parseInt(args[2]);
            int C = Integer.parseInt(args[3]);

            String[] nodeNames = {"a", "b", "c", "d", "e"};
            String folderPath = "./nodesData";
            String configPath = "./config.txt";

            initializeAllFiles(M, folderPath, nodeNames);
            List<List<Integer>> partitionRequestsList = initializeAllRequests(N, C);

            runAllRequests(configPath, folderPath, nodeNames, C, f, M, partitionRequestsList);
        }
    }

    public static void runAllRequests(String configPath, String folderPath, String[] nodeNames, int C, int f, int M,
                                      List<List<Integer>> partitionRequestsList) throws Exception{
        List<String[]> networkInformation = Utils.readConfigFile(configPath);
        String[] mainConfig = networkInformation.get(0);
        String[] backupConfig = networkInformation.get(1);
        DirectoryServer mainDS = new DirectoryServer(mainConfig[0], mainConfig[1], Integer.parseInt(mainConfig[2]),
                    new ArrayList<>(), backupConfig[1], Integer.parseInt(backupConfig[2]), false);
        DirectoryServer backupDS = new DirectoryServer(backupConfig[0], backupConfig[1], Integer.parseInt(backupConfig[2]),
                    new ArrayList<>(), mainConfig[1], Integer.parseInt(mainConfig[2]), true);
        int[] ports = new int[]{8124, 8125, 8126, 8127, 8128};
        List<StorageNode> nodesList = new ArrayList<>();
        for (int i = 0; i < nodeNames.length; i++){
            StorageNode node = new StorageNode(nodeNames[i], "localhost", ports[i], folderPath + "/" + nodeNames[i],
                    mainConfig[1], Integer.parseInt(mainConfig[2]), backupConfig[1], Integer.parseInt(backupConfig[2]));
            nodesList.add(node);
        }
        List<Client> clientsList = new ArrayList<>();
        for (int i = 0; i < C; i++){
            Client client = new Client("localhost", 30000+i, mainConfig[1], Integer.parseInt(mainConfig[2]),
                    backupConfig[1], Integer.parseInt(backupConfig[2]));
            clientsList.add(client);
        }
        Thread.sleep(2000);
        // simulate fault tolerance
//        mainDS.stop();
//        Random r = new Random();
//        int index1 = r.nextInt(5);
//        int index2 = r.nextInt(5);
//        while (index2 == index1){
//            index2 = r.nextInt(5);
//        }
//        nodesList.get(index1).stop();
//        nodesList.get(index2).stop();

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(20, 100, 1000,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        for (int i = 0; i < C; i++){
            threadPoolExecutor.execute(new TestTask(String.valueOf(i), clientsList.get(i), partitionRequestsList.get(i), M, f));
        }
        threadPoolExecutor.shutdown();
        while (true){
            if (threadPoolExecutor.isTerminated()){
                System.out.println("done!");

                // metric
                int messagesExchanged = 0;
                int bytesTransferred = 0;
                long responseTime = 0;

                messagesExchanged += mainDS.getMessagesExchanged();
                messagesExchanged += backupDS.getMessagesExchanged();
                bytesTransferred += mainDS.getBytesTransferred();
                bytesTransferred += backupDS.getBytesTransferred();

                for (int i = 0; i < nodesList.size(); i++){
                    messagesExchanged += nodesList.get(i).getMessagesExchanged();
                    bytesTransferred += nodesList.get(i).getBytesTransferred();
                }

                for (int i = 0; i < C; i++){
                    messagesExchanged += clientsList.get(i).getMessagesExchanged();
                    bytesTransferred += clientsList.get(i).getBytesTransferred();
                    responseTime += clientsList.get(i).getResponseTime();
                }
                System.out.println("Metric:");
                System.out.println("Messages Changed: " + String.valueOf(messagesExchanged));
                System.out.println("Bytes Transferred: " + String.valueOf(bytesTransferred));
                System.out.println("Response Time: " + String.valueOf(responseTime / (long) C));
                break;
            }
        }
        System.exit(0);


    }

    /**
     * initialize all requests randomly
     * @param N
     * @param C
     * @return
     */
    public static List<List<Integer>> initializeAllRequests(int N, int C){
        // 0 -> get file list from directory server
        // 1 -> get file list from storage node
        // 2 -> read file
        // 3 -> add new file
        Random r = new Random();
        List<Integer> allRequestsList = new ArrayList<>();
        while (allRequestsList.size() < N){
            allRequestsList.add(r.nextInt(4));
        }
        List<List<Integer>> partitionRequestsList = new ArrayList<>();
        int part = N / C;
        for (int i = 0; i < C; i++){
            List<Integer> temp = new ArrayList<>();
            for (int j = i * part; j < (i+1) * part; j++){
                temp.add(allRequestsList.get(j));
            }
            partitionRequestsList.add(temp);
        }
        return partitionRequestsList;

    }

    /**
     * initialize all files
     * @param M
     * @param folderPath
     * @param nodesName
     */
    public static void initializeAllFiles(int M, String folderPath, String[] nodesName){
        int part = M / 5;
        for (int i = 0; i < nodesName.length; i++){
            String path = folderPath + "/" + nodesName[i];
            deleteAllFiles(path);
            for (int j = 0; j < M; j++){
                if (i * part <= j && j < (i+1) * part){
                    try {
                        File file = new File(path + "/" + String.valueOf(j) + ".txt");  //create a new file locally with fileName
                        RandomAccessFile raf = new RandomAccessFile(file, "rw");
                        raf.setLength(100000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    /**
     * delete all files in this path before initialize all files
     * @param path
     * @return
     */
    public static boolean deleteAllFiles(String path){
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                flag = temp.delete();
            }
        }
        return flag;
    }

    /**
     * A class for evaluating this system
     */
    static class TestTask implements Runnable{
        private String name;
        private Client client;
        private List<Integer> requestsList;
        private int M;
        private int f;

        public TestTask(String name, Client client, List<Integer> requestsList, int M, int f){
            this.name = name;
            this.client = client;
            this.requestsList = requestsList;
            this.M = M;
            this.f = f;
            this.client.connectToSystem();
        }
        @Override
        public void run() {
            int counter = 0;
            Random r = new Random();
            for (int i = 0; i < requestsList.size(); i++){
                int taskId = requestsList.get(i);
                if (taskId == 0){
                    client.getFilesListFromDirectoryServer();
                }else if (taskId == 1){
                    client.getFilesListFromNode();
                }else if (taskId == 2){
                    String fileName = String.valueOf(r.nextInt(M) + ".txt");
                    client.readFile(fileName);
                }else{
                    client.createNewFile(name + "_" + String.valueOf(counter) + ".txt");
                    counter ++;
                }

                try {
                    Thread.sleep(f);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
