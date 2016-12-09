package URL.TLS;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Administrator on 2016/12/9.
 */
public class Record {
    public static final int MAX_RECORD_SIZE = 18442;
    //CONTENTTYPE_CHANGE_CIPHER_SPEC, CONTENTTYPE_ALERT, CONTENTTYPE_HANDSHAKE, CONTENTTYPE_APPLICATION_DATA
    public static final byte[] CONTENT_TYPE = {20, 21, 22, 23};
    private TLSSocket tlsSocket;
    private Cipher encrypt;
    private Cipher decrypt;
    private HMacMD clientMAC;
    private HMacMD serverMAC;
    //state of reading and writing
    private long clientNum = 0;
    private long serverNum = 0;
    private boolean clientCipher = false;
    private boolean serverCipher = false;

    //fragment parameters
    private int blockSize;
    private int macSize;

    //the buf for reading
    private byte[] readBuf = new byte[MAX_RECORD_SIZE];
    private int readBufOffset;

    //input and output stream
    OutputStream outputStream;
    InputStream inputStream;

    public Record(TLSSocket tlsSocket) {
        this.tlsSocket = tlsSocket;
    }

    //send message from the client to a server
    public void sendMessage(byte[] message, byte contentType) throws IOException {
        int messageLength = message.length;
        int offset = 0;
        byte[] range = null;
        while (messageLength > 0) {
            int sendingLength = messageLength > MAX_RECORD_SIZE ? MAX_RECORD_SIZE : messageLength;
            //encrypt the message
            if (clientCipher) {
                try {
                    range = encryptMessage(contentType, message, offset, sendingLength);
                } catch (ShortBufferException e) {
                    e.printStackTrace();
                }
            }
            else {
                range = new byte[sendingLength];
                System.arraycopy(message, offset, range, 0, sendingLength);
            }
            byte[] length = {(byte) (range.length >> 8), (byte) range.length};
            //write message content into stream orderly
            outputStream.write(contentType);
            outputStream.write(TLSSocket.CLIENT_VERSION);
            outputStream.write(length);
            outputStream.write(range);
            outputStream.flush();

            //add length
            offset += sendingLength;
            messageLength -= sendingLength;
        }
    }
    /*
     * read from a record. if the record is too large, return a single segment.
     * if invalid, return null
     */
    public byte[] readFromRecord() throws IOException, ShortBufferException {
        int recordLength = 0;
        // read header with the size of 5 bytes
        while (readBufOffset < 5) {
            int len = inputStream.read(readBuf, readBufOffset, 5 - readBufOffset);
            if (len == -1) {
                tlsSocket.setConnected(false);
                return null;
            }
            readBufOffset += len;
        }

        // check ProtocolVersion
        assert readBuf[1] == TLSSocket.CLIENT_VERSION[0] && readBuf[2] == TLSSocket.CLIENT_VERSION[1];
        // set the length
        recordLength = (readBuf[3] & 0xFF) << 8 | (readBuf[4] & 0xFF);
        //return fragment
        byte[] fragment = new byte[recordLength];
        // read the rest.
        //total length = head length(5) + read length
        while (readBufOffset < recordLength + 5) {
            // extra 5 bytes here to see if more fragments ready
            int len = inputStream.read(readBuf, readBufOffset, recordLength + 10 - readBufOffset);
            if (len < 0) {
                throw new IOException("The record is invalid");
            }
            readBufOffset += len;
        }
        // decrypt if the server encrypts the record
        if (serverCipher) {
            decrypt.update(readBuf, 5, recordLength, fragment);
            int fragmentLength = recordLength - macSize;
            // subtract padding from fragmentLength
            if (blockSize > 0) {
                fragmentLength -= ((fragment[recordLength - 1] & 0xff) + 1);
            }
            byte[] sequenceNumber = longToByte(serverNum++);
            byte[] mac = getMAC(serverMAC, sequenceNumber, readBuf[0], fragment, 0, fragmentLength);
            //check MAC
            for (int i = 0; i < mac.length; i++) {
                if (fragment[fragmentLength + i] != mac[i])
                    System.out.println("Wrong MAC");
            }
            byte[] fragmentWithoutMAC = new byte[fragmentLength];
            System.arraycopy(fragment, 0, fragmentWithoutMAC, 0, fragmentLength);
            fragment = fragmentWithoutMAC;
        } else {
            System.arraycopy(readBuf, 5, fragment, 0, recordLength);
        }
        // check ContentType
        if (readBuf[0] == CONTENT_TYPE[1]) {
            assert fragment.length == 2 && fragment[1] == 0;;
            sendMessage(new byte[] {1, 0}, CONTENT_TYPE[1]);
            tlsSocket.setConnected(false);
            return null;
        }
        // copy extra data (next 5 bytes of a record header)
        int delta = readBufOffset - (recordLength + 5);
        if (delta > 0) {
            System.arraycopy(readBuf, recordLength + 5, readBuf, 0, delta);
        }
        readBufOffset = delta;
        return fragment;
    }
    private byte[] encryptMessage(byte contentType, byte[] message, int offset, int length) throws ShortBufferException {
        byte[] sequence = longToByte(clientNum++);
        byte[] mac = getMAC(clientMAC, sequence, contentType, message, offset, length);
        int paddingLength = (blockSize == 0) ? 0 : blockSize - ((length + mac.length) % blockSize);
        byte[] messageMaced = new byte[length + mac.length + paddingLength];
        System.arraycopy(message, offset, messageMaced, 0, length);
        System.arraycopy(mac, 0, messageMaced, length, mac.length);
        int macLength = messageMaced.length;
        //coordinate the byte array with padding
        for (int i = 0; i < paddingLength; i++) {
            messageMaced[macLength - 1 - i] = (byte) (paddingLength - 1);
        }
        encrypt.update(messageMaced, 0, messageMaced.length, messageMaced);
        return messageMaced;
    }

    private byte[] getMAC(HMacMD hMacMD, byte[] sequenceNumber, byte type, byte[] message, int offset, int length) {
        //sequenceNumber(8) + contentType(1) + protocolVersion(2) + messageVector(2 + messageLength)
        byte[] plainTxt = new byte[13 + length];
        System.arraycopy(sequenceNumber, 0, plainTxt, 0, 8);
        plainTxt[8] = type;
        System.arraycopy(TLSSocket.CLIENT_VERSION, 0, plainTxt, 9, 2);
        plainTxt[11] = (byte) (length >> 8);
        plainTxt[12] = (byte) (length);
        System.arraycopy(message, offset, plainTxt, 13, length);

        //MAC digest
        return hMacMD.digest(plainTxt);
    }
    private byte[] longToByte(long l) {
        byte[] byteVal = new byte[8];
        for (int i = 0; i < byteVal.length; i++) {
            byteVal[i] = (byte) (l >> (byteVal.length - 1 - i) * 8);
        }
        return byteVal;
    }
    //set key block  TLS_RSA_WITH_RC4_128_MD5
    public void setKeyBlock(int cipherSuite, byte[] keyBlock) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        TLS_ENCRYPT tls_encrypt = new TLS_ENCRYPT();
        if (cipherSuite == TLSSocket.TLS_RSA_WITH_3DES_EDE_CBC_SHA) {
            tls_encrypt = new TLS_RSA_WITH_3DES_EDE_CBC_SHA();
        }
        else if (cipherSuite == TLSSocket.TLS_RSA_WITH_AES_128_CBC_SHA) {
            tls_encrypt = new TLS_RSA_WITH_AES_128_CBC_SHA();
        }
        macSize = tls_encrypt.macSize;
        blockSize = tls_encrypt.blockSize;
        int keySize = tls_encrypt.keySize;
        int ivSize = tls_encrypt.ivSize;
        byte[] clientWriteMACSecret = sub(keyBlock, 0, macSize);
        byte[] serverWriteMACSecret = sub(keyBlock, macSize, macSize);
        byte[] clientWriteKey = sub(keyBlock, 2 * macSize, keySize);
        byte[] serverWriteKey = sub(keyBlock, 2 * macSize + keySize, keySize);
        byte[] clientWriteIV = sub(keyBlock, 2 * (macSize + keySize), ivSize);
        byte[] serverWriteIV = sub(keyBlock, 2 * (macSize + keySize) + ivSize, ivSize);

        clientMAC = new HMacMD(MessageDigest.getInstance(tls_encrypt.macAlg), clientWriteMACSecret);
        serverMAC = new HMacMD(MessageDigest.getInstance(tls_encrypt.macAlg), serverWriteMACSecret);

        encrypt = Cipher.getInstance(tls_encrypt.cipherAlg);
        decrypt = Cipher.getInstance(tls_encrypt.cipherAlg);

        encrypt.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(clientWriteKey, tls_encrypt.keyAlg));
        decrypt.init(Cipher.DECRYPT_MODE, new SecretKeySpec(serverWriteKey, tls_encrypt.keyAlg));
    }



    class TLS_ENCRYPT {
        String cipherAlg = "RC4";
        String keyAlg = "RC4";
        String macAlg = "MD5";
        int macSize = 16;
        int blockSize = 0;
        int keySize = 16;
        int ivSize = 0;
    }
    class TLS_RSA_WITH_3DES_EDE_CBC_SHA extends TLS_ENCRYPT {
        public TLS_RSA_WITH_3DES_EDE_CBC_SHA() {
            cipherAlg = "DESede/CBC/NoPadding";
            keyAlg = "DESede";
            macAlg = "SHA-1";
            macSize = 20;
            blockSize = 8;
            keySize = 24;
            ivSize = 8;
        }
    }
    class TLS_RSA_WITH_AES_128_CBC_SHA extends TLS_ENCRYPT {
        public TLS_RSA_WITH_AES_128_CBC_SHA() {
            cipherAlg = "AES/CBC/NoPadding";
            keyAlg = "AES";
            macAlg = "SHA-1";
            macSize = 20;
            blockSize = 16;
            keySize = 16;
            ivSize = 16;
        }
    }
    private static byte[] sub(byte[] buf, int offset, int len) {
        byte[] sub = new byte[len];
        System.arraycopy(buf, offset, sub, 0, len);
        return sub;
    }

    public void setClientCipher(boolean clientCipher) {
        this.clientCipher = clientCipher;
    }

    public void setServerCipher(boolean serverCipher) {
        this.serverCipher = serverCipher;
    }
}
