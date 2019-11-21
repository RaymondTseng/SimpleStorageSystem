import java.io.*;
import java.net.ConnectException;
import java.net.Socket;


/**
 * A class for handling socket
 */
public class SocketUtils {
    private String address;
    private int port;

    private String backupAddress;
    private int backupPort;

    private Socket socket;

    private RequestPackage rp;

    private int bytesTransferred = 0;

    /**
     * Sending a request package to address:port
     * @param address
     * @param port
     * @param rp
     */
    public SocketUtils(String address, int port, RequestPackage rp){
        this.address = address;
        this.port = port;
        this.rp = rp;
    }

    /**
     * Sending a request package to address:port. If fail, sending this package to backupAddress:backupPort
     * @param address
     * @param port
     * @param backupAddress
     * @param backupPort
     * @param rp
     */
    public SocketUtils(String address, int port, String backupAddress, int backupPort, RequestPackage rp){
        this.address = address;
        this.port = port;
        this.backupAddress = backupAddress;
        this.backupPort = backupPort;
        this.rp = rp;
    }

    /**
     * handling existed socket
     * @param socket
     * @param rp
     */
    public SocketUtils(Socket socket, RequestPackage rp){
        this.socket = socket;
        this.rp = rp;
    }

    /**
     * sending request package
     * @return
     */
    public boolean send(){
        boolean flag = false;
        try{
            if (this.socket == null) {
                this.socket = new Socket(this.address, this.port);
                this.socket.setSoTimeout(2000);
            }
            ObjectOutputStream oos = new ObjectOutputStream(this.socket.getOutputStream());
            oos.writeObject(this.rp);
            oos.flush();
            flag = true;
        }catch (ConnectException e){
            flag = sendBackup();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * sending request package to backup directory server
     * @return
     */
    public boolean sendBackup(){
        boolean flag = false;
        if (this.backupAddress == null)
            return false;
        try{
            this.socket = new Socket(this.backupAddress, this.backupPort);
            ObjectOutputStream oos = new ObjectOutputStream(this.socket.getOutputStream());
            oos.writeObject(this.rp);
            oos.flush();
            flag = true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * reading an object from an existed package
     * @param ifClose
     * @return
     */
    public Object readObjectFromSocket(boolean ifClose){
        try {
            InputStream is = this.socket.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            Object obj = ois.readObject();
            this.bytesTransferred += is.available();
            if (ifClose)
                ois.close();
            return obj;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Reading a file from an existed socket
     * @param filePath
     */
    synchronized public void readFileFromSocket(String filePath){
        try {
            InputStream is = this.socket.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));

            byte [] buf = new byte [1024];
            int len = 0;
            while((len = bis.read(buf))!=-1){
                bos.write(buf, 0, len);
                this.bytesTransferred += len;
            }
            bos.flush();
            bos.close();
            bis.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * sending a file by using an existed socket
     * @param filePath
     */
    synchronized public void sendFileBySocket(String filePath){
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath));
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

            byte [] buf = new byte [1024];
            int len = 0;
            while((len = bis.read(buf))!=-1){
                bos.write(buf, 0, len);
            }
            bos.flush();
            bos.close();
            bis.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public int getBytesTransferred() {
        return bytesTransferred;
    }

    public void setRequestPackage(RequestPackage rp) {
        this.rp = rp;
    }
}
