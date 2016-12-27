package URL.TLS.Encryption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

/**
 * Created by Administrator on 2016/12/9.
 */
public class RSA extends Encryptor{
    private Random random = new Random();
    //certificate
    private ByteArrayInputStream certificate;
    //exponent
    private BigInteger exponent;
    //module
    private BigInteger mod;
    //length of the key
    private int keyLength;
    //the function of encryption
    public byte[] encrypt(byte[] secret) {
        byte[] magnitude = new byte[keyLength - 1];
        for (int i = 0; i < magnitude.length; i++) {
            magnitude[i] = (byte) (Math.abs(random.nextInt()) % 255 + 1);
        }
        int delta = magnitude.length - secret.length;
        magnitude[0] = 0x02;
        magnitude[delta - 1] = 0x00;
        System.arraycopy(secret, 0, magnitude, delta, secret.length);
        BigInteger integer = new BigInteger(1, magnitude);
        byte[] result = integer.modPow(exponent, mod).toByteArray();
        if (result.length > keyLength) {
            byte[] value = new byte[keyLength];
            System.arraycopy(result, result.length - keyLength, value, 0, keyLength);
            return value;
        }
        return result;
    }
    //parse certificate and set exponent and module
    public void setCertificates(byte[] message, int offset) throws IOException {
        certificate = new ByteArrayInputStream(message);
        certificate.skip(offset);
        int asnLength;
        // handle certificate
        for (int i = 0; i < 2; i++) {
            certificate.skip(1);
            readLengthFromBuf();
        }
        // check the version
        if ((certificate.read() & 0x80) > 0) {
            certificate.skip(2);
            asnLength = readLengthFromBuf();
            certificate.skip(asnLength);
        }
        // serialNumber, signature, issuer, validity, subject
        for (int i = 0; i < 5; i++) {
            certificate.skip(1);
            asnLength = readLengthFromBuf();
            certificate.skip(asnLength);
        }
        certificate.skip(1);
        readLengthFromBuf();
        // handle algorithm
        certificate.skip(1);
        asnLength = readLengthFromBuf();
        certificate.skip(asnLength);
        // handle bit string
        certificate.skip(1);
        readLengthFromBuf();
        // SubjectPublicKeyInfo - BitString - RSAPublicKey
        certificate.skip(2);
        readLengthFromBuf();
        // handle modulus
        certificate.skip(1);
        int modLen = readLengthFromBuf();
        byte[] mod = new byte[modLen];
        certificate.read(mod, 0, modLen);
        this.mod = new BigInteger(1, mod);
        this.keyLength = modLen;
        for (int i = 0; i < modLen; i++) {
            if (mod[i] != 0)
                break;
            else
                keyLength--;
        }
        // handle exponent
        certificate.skip(1);
        int exponentLength = readLengthFromBuf();
        byte[] exp = new byte[exponentLength];
        certificate.read(exp);
        exponent = new BigInteger(1, exp);
    }

    private int readLengthFromBuf() {
        int len = certificate.read();
        if (len < 128) {
            return len;
        }
        else {
            len %= 128;
            int length = 0;
            for (int i = 0; i < len; i++) {
                length <<= 8;
                length += certificate.read();
            }
            return length;
        }
    }
}
