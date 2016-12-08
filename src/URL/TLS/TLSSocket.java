package URL.TLS;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by Administrator on 2016/12/8.
 */
public class TLSSocket extends Socket {
    //static variables
    static final byte[] CLIENT_VERSION = {0x03, 0x01};
    public static final byte[] COMPRESSION_METHOD = {0x01, 0x00};
    public static final byte[] CIPHER_SUITES = {0x00, 0x06, 0x00, 0x04, 0x00, 0x0a, 0x00, 0x2f};

    private String host;
    private int port;
    private boolean connected = false;
    private TLSInputStream tlsInputStream;
    private TLSOutputStream tlsOutputStream;

    public TLSSocket(String host, int port) {

    }

    public void connect() throws IOException {
        Socket socket = new Socket(host, port);

    }
}
