package org.tron.common.utils;

import org.junit.Test;
import org.testng.Assert;
import org.tron.common.crypto.sm2.SM3;

public class SM3HashTest {

    @Test
    public void testHash() {
        //Example from https://github.com/tronprotocol/tips/blob/master/TWP-001.md
        byte[] input = ByteArray.fromHexString("A0E11973395042BA3C0B52B4CDF4E15EA77818F275");
        byte[] hash0 = SM3Hash.hash(input);
        byte[] hash1 = SM3Hash.hashTwice(input);
        byte[] hash2 = SM3.hash(input);
        byte[] hash3 = SM3.hash(hash2);
        Assert.assertEquals(hash0,hash2);
        Assert.assertEquals(hash1,hash3);
    }

}
