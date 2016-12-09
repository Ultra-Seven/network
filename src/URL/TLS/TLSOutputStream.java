package URL.TLS;

import javax.crypto.ShortBufferException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Administrator on 2016/12/9.
 */
public class TLSOutputStream extends OutputStream {
    private TLSSocket tlsSocket;
    public TLSOutputStream(TLSSocket tlsSocket) {
        this.tlsSocket = tlsSocket;
    }
    public void write(byte[] buf, int offset, int len) throws IOException {
        try {
            tlsSocket.readAvailable();
        } catch (ShortBufferException e) {
            e.printStackTrace();
        }

        if (!tlsSocket.isConnect()) {
            tlsSocket.connect();
        }
        tlsSocket.getRecord().sendMessage(buf, Record.CONTENT_TYPE[3]);
    }
    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte)b}, 0, 1);
    }
}
