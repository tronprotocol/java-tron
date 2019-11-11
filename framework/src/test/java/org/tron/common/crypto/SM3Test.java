package org.tron.common.crypto;

import org.junit.Test;
import org.spongycastle.crypto.digests.SM3Digest;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.sm3.SM3;

public class SM3Test {
    private String message = "F4A38489E32B45B6F876E3AC2168CA392362DC8F23459C1D1146FC3DBFB7BC9A6D65737361676520646967657374";

    @Test
    public  void testSM3(){

        SM3Digest digest = new SM3Digest();
        byte[] msg = message.getBytes();
        digest.update(msg,0,msg.length);

        byte[] eHash = new byte[digest.getDigestSize()];

        digest.doFinal(eHash, 0);

        System.out.println(Hex.toHexString(eHash));

    }


    @Test
    public  void testSM3_2() throws Exception {

        SM3 sm3 = new SM3();
        byte[] msg = message.getBytes();
        byte[] hash =  sm3.hash(msg);

        System.out.println(Hex.toHexString(hash));

    }
}
