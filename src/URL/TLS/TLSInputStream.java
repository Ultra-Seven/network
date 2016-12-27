package URL.TLS;

import javax.crypto.ShortBufferException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by Administrator on 2016/12/8.
 */
public class TLSInputStream extends InputStream {
    private TLSSocket tlsSocket;
    private byte[] buffer = new byte[4096];
    private int start = 0;
    private int end = 0;
    private int available = 0;
    public TLSInputStream(TLSSocket socket) {
        this.tlsSocket = socket;
    }
    @Override
    public int read() throws IOException {
        byte[] buf = {0};
        int len = read(buf, 0, 1);
        if (len == -1) {
            return -1;
        }
        return buf[0];
    }
    public int read(byte[] buf, int offset, int len) throws IOException {
        try {
            tlsSocket.readAvailable();
        } catch (ShortBufferException e) {
            e.printStackTrace();
        }
        if (available == 0) {
            try {
                tlsSocket.readFragment();
            } catch (ShortBufferException e) {
                e.printStackTrace();
            }
            try {
                tlsSocket.readAvailable();
            } catch (ShortBufferException e) {
                e.printStackTrace();
            }
        }
        if (available == 0) {
            return -1;
        }
        int readLength = Math.min(available, len);
        System.arraycopy(buffer, start, buf, offset, readLength);
        start += readLength;
        available -= readLength;
        return readLength;
    }

    void addBytes(byte[] fragment) {
        if (available + fragment.length > buffer.length) {
            byte[] temp = new byte[buffer.length + (fragment.length << 1)];
            System.arraycopy(buffer, start, temp, 0, available);
            buffer = temp;
        }
        if (end + fragment.length > buffer.length) {
            byte[] temp = new byte[buffer.length];
            System.arraycopy(buffer, start, temp, 0, available);
            start = 0; end = available;
            buffer = temp;
        }
        System.arraycopy(fragment, 0, buffer, end, fragment.length);
        end += fragment.length;
        available += fragment.length;
    }
    public int available() {
        return available;
    }

}
