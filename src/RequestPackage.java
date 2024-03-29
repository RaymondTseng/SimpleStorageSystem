import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * Class for building a request
 */
public class RequestPackage<T> implements Serializable {
    private static final long serialVersionUID = 1386583756403881124L;
    // Directory Server 0 -> register, 1 -> recovery, 2 -> client connect, 3 -> get all nodes information, 4 -> get files list， 5 -> send server information, 6 -> dead node information, 7-> dead node information from server
    // Storage Node 0 -> register, 1 -> receive file, 2 -> get files list, 3 -> create new file, 4 -> send all local files, 5 -> read file, 6->dead node recover
    private int requestType;
    private String requestAddress;
    private int requestPort;
    private T content;

    public RequestPackage(int requestType, String address, int port, T content) {
        this.requestType = requestType;
        this.requestAddress = address;
        this.requestPort = port;
        this.content = content;
    }


    public int getRequestType() {
        return requestType;
    }

    public String getRequestAddress() {
        return requestAddress;
    }

    public int getRequestPort() {
        return requestPort;
    }

    public T getContent() {
        return content;
    }


}

