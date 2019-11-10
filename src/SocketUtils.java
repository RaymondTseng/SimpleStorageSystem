import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketUtils {
    private String address;
    private int port;

    private String backupAddress;
    private int backupPort;

    private Socket socket;

    private RequestPackage rp;

    public SocketUtils(String address, int port, RequestPackage rp){
        this.address = address;
        this.port = port;
        this.rp = rp;
    }

    public SocketUtils(String address, int port, String backupAddress, int backupPort, RequestPackage rp){
        this.address = address;
        this.port = port;
        this.backupAddress = backupAddress;
        this.backupPort = backupPort;
        this.rp = rp;
    }

    public SocketUtils(Socket socket, RequestPackage rp){
        this.socket = socket;
        this.rp = rp;
    }


    public boolean send(){
        boolean flag = false;
        try{
            if (this.socket == null) {
                this.socket = new Socket(this.address, this.port);
            }
            ObjectOutputStream oos = new ObjectOutputStream(this.socket.getOutputStream());
            oos.writeObject(this.rp);
            oos.flush();
            flag = true;
        }catch (ConnectException e){
            System.out.println("use backup");
            flag = sendBackup();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    private boolean sendBackup(){
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

    public Object readObjectFromSocket(boolean ifClose){
        try {
            ObjectInputStream ois = new ObjectInputStream(this.socket.getInputStream());
            Object obj = ois.readObject();
            if (ifClose)
                ois.close();
            return obj;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    synchronized public void readFileFromSocket(String filePath){
        try {
            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));

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

    public void setRequestPackage(RequestPackage rp) {
        this.rp = rp;
    }
}
