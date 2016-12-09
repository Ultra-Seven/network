package URL.TLS.Encryption;

import URL.TLS.HMacMD;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Administrator on 2016/12/9.
 */
public class PRF {
    private MessageDigest md5;
    private MessageDigest sha;
    private HMacMD hmac;
    public PRF() throws NoSuchAlgorithmException {
        md5 = MessageDigest.getInstance("MD5");
        sha = MessageDigest.getInstance("SHA");
        hmac = new HMacMD(null, null);
    }
    public byte[] getBytes(byte[] secret, String s, byte[] random, int length) {
        byte[] result = new byte[length];
        byte[] K1 = new byte[length / 2];
        byte[] K2 = new byte[length - K1.length];

        return result;
    }
    private byte[] prfHash(MessageDigest md, int digestLength, byte[] secret, byte[] seed, int length) {
        return null;
    }
}
