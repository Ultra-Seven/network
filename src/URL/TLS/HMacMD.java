package URL.TLS;

import java.security.MessageDigest;

/**
 * Created by Administrator on 2016/12/9.
 * HMAC defined in RFC 2104
 */
public class HMacMD {
    //for mdX, the length of ipad and opad is 64
    private byte[] ipad = new byte[64];
    private byte[] opad = new byte[64];
    private MessageDigest messageDigest;
    public HMacMD(MessageDigest messageDigest, byte[] key) {
        this.messageDigest = messageDigest;
        //set key
        int keyLength = 0;
        if (key != null)
            keyLength = key.length;
        //if the length of key is longer than 64, then use the message digest to hash it. if shorter than, fill it will 0
        byte[] newKey = keyLength > ipad.length ? messageDigest.digest(key) : key;
        //set ipad and opad
        /*
        ipad = the byte 0x36 repeated B times
        opad = the byte 0x5C repeated B times
         */
        for (int i = 0; i < keyLength; i++) {
            ipad[i] = (byte) (0x36 ^ newKey[i]);
            opad[i] = (byte) (0x5C ^ newKey[i]);
        }
        for (int i = keyLength; i < ipad.length; i++) {
            ipad[i] = 0x36;
            opad[i] = 0x5C;
        }
    }
    //get HMAC value
    public byte[] digest(byte[] input) {
        messageDigest.reset();
        messageDigest.update(ipad);
        messageDigest.update(input);
        //XOR with ipad
        byte[] firstPhase = messageDigest.digest();
        messageDigest.update(opad);
        messageDigest.update(firstPhase);
        //XOR with ipod
        return messageDigest.digest();
    }
}
