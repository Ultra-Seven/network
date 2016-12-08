package URL.TLS;

import URL.TLS.Encryption.PseudoRandom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Created by Administrator on 2016/12/9.
 */
public class HandShake {
    private static final byte CLIENTHELLO = 1;
    private MessageDigest messageDigest5 = MessageDigest.getInstance("MD5");
    private MessageDigest sha1 = MessageDigest.getInstance("SHA");
    private PseudoRandom pseudoRandom = new PseudoRandom();
    private Random random = new Random();
    private byte[] helloRandom;
    private byte[] sessionId;
    private Record record;
    public HandShake() throws NoSuchAlgorithmException {

    }
    public void connect() throws IOException {
        messageDigest5.reset();;
        sha1.reset();
        clientHello();
    }

    private void clientHello() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] header = {CLIENTHELLO, 0x00, 0x00, 0x00};
        byteArrayOutputStream.write(header);
        helloRandom = getRandom();
        //client version
        byteArrayOutputStream.write(TLSSocket.CLIENT_VERSION);
        //client random
        byteArrayOutputStream.write(helloRandom);
        //session id
        if (sessionId != null) {
            byteArrayOutputStream.write((byte) sessionId.length);
            byteArrayOutputStream.write(sessionId);
        }
        else
            byteArrayOutputStream.write(0);
        //cipher suits
        byteArrayOutputStream.write(TLSSocket.CIPHER_SUITES);
        //compression method
        byteArrayOutputStream.write(TLSSocket.COMPRESSION_METHOD);

        byte[] helloFirst = byteArrayOutputStream.toByteArray();
        int length = helloFirst.length;
        //set length
        helloFirst[1] = (byte) (length >> 16);
        helloFirst[2] = (byte) (length >> 8);
        helloFirst[3] = (byte) (length);

        //update
        messageDigest5.update(helloFirst);
        sha1.update(helloFirst);
        //send message

    }

    /**
     * get random byte array for client
     */
    private byte[] getRandom() {
        byte[] random = new byte[32];
        this.random.nextBytes(random);
        long gmt_unix_time = System.currentTimeMillis() / 1000;
        random[3] = (byte) gmt_unix_time;
        random[2] = (byte) (gmt_unix_time >> 8);
        random[1] = (byte) (gmt_unix_time >> 16);
        random[0] = (byte) (gmt_unix_time >> 24);
        return random;
    }
}
