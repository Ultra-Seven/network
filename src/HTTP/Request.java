package HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/12/4.
 */
public class Request {
    private String method;
    private String url;
    private int port;
    private String request;
    private Map<String, List<String>> parameters = new HashMap<>();
    private final static int BUF_SIZE = 8192;
    public Request() {

    }
    public Request(InputStream in) throws IOException {
        if (in != null) {
            byte[] request = new byte[BUF_SIZE];
            in.read(request);
            this.request = Arrays.toString(request);
        }
    }
}
