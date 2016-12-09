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
    public static final int TLS_RSA_WITH_RC4_128_MD5 = 0x04;
    public static final int TLS_RSA_WITH_3DES_EDE_CBC_SHA = 0x0A;
    public static final int TLS_RSA_WITH_AES_128_CBC_SHA = 0x2F;
    public static final int KEY_BLOCK_LENGTH = 104;
    private String host;
    private int port;
    private boolean connected = false;
    private TLSInputStream tlsInputStream;
    private TLSOutputStream tlsOutputStream;
    private Record record;
    private boolean connect = false;

    public TLSSocket(String host, int port) {

    }

    public void connect() throws IOException {
        Socket socket = new Socket(host, port);

    }

    public Record getRecord() {
        return record;
    }

    public void setConnected(boolean b) {
    }
}
