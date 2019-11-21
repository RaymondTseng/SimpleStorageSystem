

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.List;


public class Utils {

    public static List<String[]> readConfigFile(String configFilePath){
        List<String[]> networkInformation = new ArrayList<String[]>();
        BufferedReader bufferedReader;
        try {
            File file = new File(configFilePath);
            bufferedReader = new BufferedReader(new FileReader(file));
            String strLine = null;
            while (null != (strLine = bufferedReader.readLine())) {
                String[] configArray = strLine.split(" ");
                if (configArray[0].contains("Server")) { //record the information of the Servers
                    networkInformation.add(configArray);
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


}
