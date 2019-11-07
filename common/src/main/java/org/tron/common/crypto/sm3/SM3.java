package org.tron.common.crypto.sm3;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

/**
 * Implement Chinese Commercial Cryptographic Standard of SM3
 *
 */
public class SM3 {
    public SM3(){
    }

    /**
     * Input some bytes and get the hash value
     *
     * @param msg input message
     * @return  the hash output
     * @throws Exception
     */
    public byte[] hash(byte[] msg) throws Exception {
        if(null == msg) {
            throw new Exception("Input parameter is null");
        }
        if (msg.length == 0) {
            throw new Exception("Input parameter's length is empty");
        }
        Security.addProvider(new BouncyCastleProvider());
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("sm3");
        } catch (NoSuchAlgorithmException e) {
            throw new Exception(e);
        }
        messageDigest.update(msg);
        byte[] digest = messageDigest.digest();
        return digest;
    }
}
