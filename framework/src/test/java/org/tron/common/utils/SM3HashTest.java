package org.tron.common.utils;

import org.junit.Test;
import org.spongycastle.crypto.digests.SM3Digest;
import org.testng.Assert;

public class SM3HashTest {

  @Test
  public void testHash() {
    //Example from https://github.com/tronprotocol/tips/blob/master/TWP-001.md
    byte[] input = ByteArray.fromHexString("A0E11973395042BA3C0B52B4CDF4E15EA77818F275");
    byte[] hash0 = SM3Hash.hash(input);
    byte[] hash1 = SM3Hash.hashTwice(input);
    byte[] hash4 = SM3Hash.hash(hash0);
    Assert.assertEquals(hash1, hash4);
  }
}
