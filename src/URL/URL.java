package URL;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Administrator on 2016/12/2.
 */
public class URL {
    private String url;
    private String host;
    private int port;
    private String path;
    private String protocol;
    private String charset;
    private String userName;
    private String password;
    private HashMap<String, List<String>> parameters = new HashMap<>();
    private String fragment;

    public URL(String url) {
        this.url = url;
    }
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public HashMap<String, List<String>> getParameters() {
        return parameters;
    }

    public void setParameters(HashMap<String, List<String>> parameters) {
        this.parameters = parameters;
    }

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }
}
