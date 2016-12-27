package URL.TLS.Encryption;

import URL.TLS.HMAC;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Administrator on 2016/12/9.
 * pseudo random function
 */
public class PRF extends Encryptor{
    //md5
    private MessageDigest md5;
    //sha1
    private MessageDigest sha;

    public PRF() throws NoSuchAlgorithmException {
        md5 = MessageDigest.getInstance("MD5");
        sha = MessageDigest.getInstance("SHA");
    }
    public byte[] getBytes(byte[] secret, String s, byte[] random, int length) throws IOException {
        byte[] result = new byte[length];
        byte[] K1 = new byte[secret.length / 2 + secret.length % 2];
        byte[] K2 = new byte[K1.length];
        System.arraycopy(secret, 0, K1, 0, K1.length);
        System.arraycopy(secret, secret.length - K1.length, K2, 0, K1.length);

        // concatenate the label and the seed
        byte[] stringSeed = new byte[s.length() + random.length];
        System.arraycopy(s.getBytes(), 0, stringSeed, 0, s.length());
        System.arraycopy(random, 0, stringSeed, s.length(), random.length);
        byte[] md5Output = prfHash(md5, 16, K1, stringSeed, length);
        byte[] shaOutput = prfHash(sha, 20, K2, stringSeed, length);

        // XOR md5 and sha and save th result into the return array
        for (int i = 0; i < length; i++) {
            result[i] = (byte) (md5Output[i] ^ shaOutput[i]);
        }
        return result;
    }
    private byte[] prfHash(MessageDigest md, int digestLength, byte[] secret, byte[] seed, int length) throws IOException {
        HMAC hmac = new HMAC(md, secret);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(length);
        // concatenation seeds
        byte[] A_seed = new byte[digestLength + seed.length];
        System.arraycopy(seed, 0, A_seed, digestLength, seed.length);
        byte[] A = seed;
        while (byteArrayOutputStream.size() < length) {
            //calculate hmac digest
            A = hmac.digest(A);
            // concatenate A and seed
            System.arraycopy(A, 0, A_seed, 0, digestLength);
            byte[] temp = hmac.digest(A_seed);
            //copy length
            int writeLength = Math.min(temp.length, length - byteArrayOutputStream.size());
            byteArrayOutputStream.write(temp, 0, writeLength);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
