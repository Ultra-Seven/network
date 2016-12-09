package URL.TLS;

import URL.TLS.Encryption.PRF;
import URL.TLS.Encryption.PseudoRandom;
import URL.TLS.Encryption.RSA;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by Administrator on 2016/12/9.
 */
public class HandShake {
    private final byte HELLO_REQUEST       = 0;
    private final byte CLIENT_HELLO        = 1;
    private final byte SERVER_HELLO        = 2;
    private final byte CERTIFICATE         = 11;
    private final byte SERVER_KEY_EXCHANGE = 12;
    private final byte CERTIFICATE_REQUEST = 13;
    private final byte SERVER_HELLO_DONE   = 14;
    private final byte CERTIFICATE_VERIFY  = 15;
    private final byte CLIENT_KEY_EXCHANGE = 16;
    private final byte FINISHED            = 20;
    private MessageDigest messageDigest5 = MessageDigest.getInstance("MD5");
    private MessageDigest sha1 = MessageDigest.getInstance("SHA");
    private PseudoRandom pseudoRandom = new PseudoRandom();
    private Random random = new Random();
    private byte[] helloRandom;
    private byte[] serverRandom;
    private byte[] sessionId;
    private int cipherSuite;

    private Record record;
    private byte[] masterSecret;
    private byte[] message = {};
    private int messageOffset;

    private ByteArrayOutputStream byteArrayOutputStream;

    private RSA rsa;
    private PRF prf;
    public HandShake(TLSSocket tlsSocket) throws NoSuchAlgorithmException {
        record = tlsSocket.getRecord();
    }
    public void connect() throws IOException {
        messageDigest5.reset();
        sha1.reset();
        clientHello();
    }

    private void clientHello() throws IOException {
        byteArrayOutputStream.reset();
        byte[] header = {CLIENT_HELLO, 0x00, 0x00, 0x00};
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
        record.sendMessage(helloFirst, Record.CONTENT_TYPE[2]);
    }
    //serve hello
    private void parseServerHello() throws IOException, ShortBufferException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] serverMessage = readMessage();
        //server hello
        assert serverMessage[messageOffset] != 2;
        messageOffset += 4;
        //protocol
        assert serverMessage[messageOffset] == TLSSocket.CLIENT_VERSION[0] &&
                serverMessage[messageOffset + 1] == TLSSocket.CLIENT_VERSION[1];
        messageOffset += 2;
        // get the server random
        serverRandom = readByte(serverMessage, messageOffset, 32);
        messageOffset += serverRandom.length;
        //session id
        int sessionIDLength = serverMessage[messageOffset];
        messageOffset++;
        byte[] newSessionID = readByte(serverMessage, messageOffset, sessionIDLength);
        messageOffset += sessionIDLength;
        // read cipherSuite
        int cipher1 = serverMessage[messageOffset] << 8;
        messageOffset++;
        int cipher2 = serverMessage[messageOffset];
        messageOffset++;
        cipherSuite = cipher1 | cipher2;

        // old session equal new session
        if (Arrays.equals(sessionId, newSessionID)) {
            generateKeys();
        }
        sessionId = newSessionID;
        messageDigest5.update(message);
        sha1.update(message);

    }
    //read certification sent from the server
    private void readCertificate() throws IOException, ShortBufferException {
        byte[] message = readMessage();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(message);
        // first byte is ServerCertificate Handshake Type
        assert byteArrayInputStream.read() == CERTIFICATE;
        // header(4)
        byteArrayInputStream.skip(4);
        // get length of certification
        int certificationLength = (byteArrayInputStream.read() & 0xFF) << 16
                | (byteArrayInputStream.read() & 0xFF) << 8
                | (byteArrayInputStream.read() & 0xFF);

        // skip first 3 bytes of len
        byteArrayInputStream.skip(3);
        int remain = byteArrayInputStream.available();
        rsa.setCertificates(message, message.length - remain, remain);
        messageDigest5.update(message);
        sha1.update(message);
    }
    
    //read server hello done
    private void readServerHelloDone() throws IOException, ShortBufferException {
        byte[] message = readMessage();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(message);
        // check that first byte is ServerHelloDone Handshake Type
        assert byteArrayInputStream.read() == SERVER_HELLO_DONE;
        assert message.length == 4 && byteArrayInputStream.read() == 0x00 &&
                byteArrayInputStream.read() != 0x00 && byteArrayInputStream.read() != 0x00;
        messageDigest5.update(message);
        sha1.update(message);
    }
    //Send ClientKeyExchange
    private void sendClientKeyExchange() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        byteArrayOutputStream.reset();
        byte[] header = {CLIENT_KEY_EXCHANGE, 0x00, 0x00, 0x00};
        byteArrayOutputStream.write(header);

        // Set the PreMasterSecret
        byte[] secret = new byte[48];
        random.nextBytes(secret);
        System.arraycopy(TLSSocket.CLIENT_VERSION, 0, secret, 0, 2);
        // encrypt the secret
        byte[] encrypted = rsa.encrypt(secret);
        // write 2 byte len of encrypted secret vector.
        byteArrayOutputStream.write(new byte[] {(byte) (encrypted.length >> 8), (byte) encrypted.length});
        byteArrayOutputStream.write(encrypted);

        // convert ByteArrayOutputStream to ByteArray and set length fields
        byte[] message = byteArrayOutputStream.toByteArray();
        int msgLength = message.length - 4; // 4 byte header at start
        message[1] = (byte) (msgLength >> 16);
        message[2] = (byte) (msgLength >> 8);
        message[3] = (byte) msgLength;
        record.sendMessage(message, Record.CONTENT_TYPE[2]);
        // generate MasterSecret and keys
        generateMasterSecret(secret);
        generateKeys();
        messageDigest5.update(message);
        sha1.update(message);
    }
    //Send the ChangeCipherSpec message
    private void changeCipherSpec() throws IOException {
        record.sendMessage(new byte[]{1}, Record.CONTENT_TYPE[0]);
        record.setClientCipher(true);
    }
    //Send finish message
    private void sendFinished() throws IOException, CloneNotSupportedException {
        byteArrayOutputStream.reset();
        // Handshake header.  length == 12
        byte[] header = {FINISHED, 0x00, 0x00, 0x0C};
        byteArrayOutputStream.write(header);

        // add MD5(handshake_messages) and SHA(handshake_messages)
        byte[] temp = new byte[36];
        MessageDigest tempMD = (MessageDigest) messageDigest5.clone();
        System.arraycopy(tempMD.digest(), 0, temp, 0, 16);
        tempMD = (MessageDigest) sha1.clone();
        System.arraycopy(tempMD.digest(), 0, temp, 16, 20);
        byteArrayOutputStream.write(prf.getBytes(masterSecret, "client finished", temp, 12));

        byte[] message = byteArrayOutputStream.toByteArray();
        record.sendMessage(message, Record.CONTENT_TYPE[2]);
        messageDigest5.update(message);
        sha1.update(message);
    }
    //read ChangeCipherSpec
    private void readChangeCipherSpec() throws IOException, ShortBufferException {
        byte[] message = record.readFromRecord();
        assert message != null && message.length == 1 && message[0] == 0x01;
        record.setServerCipher(true);
    }
    // read finished
    private void readFinished() throws IOException, ShortBufferException, CloneNotSupportedException {
        byte[] message = readMessage();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(message);
        // check Handshake Type
        assert byteArrayInputStream.read() == FINISHED;
        // skip header
        byteArrayInputStream.skip(4);
        // check that length == 12
        assert message.length == 16;

        byte[] temp = new byte[36];
        MessageDigest tempMD = (MessageDigest) messageDigest5.clone();
        System.arraycopy(tempMD.digest(), 0, temp, 0, 16);
        tempMD = (MessageDigest) sha1.clone();
        System.arraycopy(tempMD.digest(), 0, temp, 16, 20);
        byte[] prfBytes = prf.getBytes(masterSecret, "server finished", temp, 12);
        // verify the 12 bytes
        for (int i = 0; i < 12; i++) {
            assert message[i + 4] == prfBytes[i];
        }
        messageDigest5.update(message);
        sha1.update(message);
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
    //read handshake message
    private byte[] readMessage() throws IOException, ShortBufferException {
        int readLength = 0;
        //reset the read buffer
        if (messageOffset == message.length) {
            message = record.readFromRecord();
            messageOffset = 0;
        }
        //have header
        checkMessage(0);

        readLength = (message[messageOffset + 1] & 0xFF) << 16 | (message[messageOffset + 2] & 0xFF) << 8
                | (message[messageOffset + 3] & 0xFF);
        //check message
        checkMessage(readLength);
        //copy message to a temp array and return it
        byte temp[] = new byte[readLength + 4];
        System.arraycopy(message, messageOffset, temp, 0, readLength + 4);
        messageOffset += readLength + 4;
        return temp;
    }
    private void checkMessage(int length) throws IOException, ShortBufferException {
        while (message.length < messageOffset + 4 + length) {
            byteArrayOutputStream.reset();
            byteArrayOutputStream.write(message, messageOffset, message.length - messageOffset);
            byte[] readMessage = record.readFromRecord();
            byteArrayOutputStream.write(readMessage, 0, readMessage.length);
            message = byteArrayOutputStream.toByteArray();
            messageOffset = 0;
        }
    }
    private byte[] readByte(byte[] serverMessage, int messageOffset, int length) {
        byte[] dst = new byte[length];
        System.arraycopy(serverMessage, messageOffset, dst, 0, length);
        return dst;
    }
    /**
     * Generate secrete keys for the Record layer
     */
    private void generateKeys() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
        byte[] randoms = new byte[64];
        System.arraycopy(serverRandom, 0, randoms, 0, 32);
        System.arraycopy(helloRandom, 0, randoms, 32, 32);
        byte[] keyBlock = prf.getBytes(masterSecret, "key expansion", randoms, TLSSocket.KEY_BLOCK_LENGTH);
        // set write MAC secrets
        record.setKeyBlock(cipherSuite, keyBlock);
    }
    private void generateMasterSecret(byte[] preMasterSecret) {
        byte[] random = new byte[64];
        System.arraycopy(helloRandom, 0, random, 0, 32);
        System.arraycopy(serverRandom, 0, random, 32, 32);
        masterSecret = prf.getBytes(preMasterSecret, "master secret", random, 48);
    }
}
