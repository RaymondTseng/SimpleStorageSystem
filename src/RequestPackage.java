import java.io.Serializable;
import java.util.List;

/**
 * Class for building a request
 */
public class RequestPackage implements Serializable {
    private static final long serialVersionUID = 1386583756403881124L;
    // -1 -> failure, 0 -> register, 1 -> send file, 2 -> get file list, 3 -> create new file
    private int requestType;
    private String requestAddress;
    private int requestPort;
    private List<String> content;

    public RequestPackage(int requestType, String address, int port, List<String> content) {
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

    public List<String> getContent() {
        return content;
    }

}

