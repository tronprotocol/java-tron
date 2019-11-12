package org.tron.common.crypto.sm2;

import org.spongycastle.crypto.digests.SM3Digest;
import org.spongycastle.util.encoders.Hex;

public class SM3 {
    public static byte[] hash(String message) {
        SM3Digest digest = new SM3Digest();
        byte[] msg = Hex.decode(message);
        digest.update(msg,0,msg.length);

        byte[] eHash = new byte[digest.getDigestSize()];

        digest.doFinal(eHash, 0);

        return eHash;
    }

    public static byte[] hash(byte[] message) {
        SM3Digest digest = new SM3Digest();
        digest.update(message,0,message.length);

        byte[] eHash = new byte[digest.getDigestSize()];

        digest.doFinal(eHash, 0);

        return eHash;
    }
}
