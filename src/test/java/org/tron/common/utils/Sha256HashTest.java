package org.tron.common.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

public class Sha256HashTest {

  @Test
  public void testHash() throws Exception {
    //Example from https://github.com/tronprotocol/tips/blob/master/TWP-001.md
    byte[] input = ByteArray.fromHexString("A0E11973395042BA3C0B52B4CDF4E15EA77818F275");
    byte[] hash0 = Sha256Hash.hash(input);
    byte[] hash1 = Sha256Hash.hash(hash0);
    Assert.assertEquals(hash0, ByteArray.fromHexString("CD5D4A7E8BE869C00E17F8F7712F41DBE2DDBD4D8EC36A7280CD578863717084"));
    Assert.assertEquals(hash1, ByteArray.fromHexString("10AE21E887E8FE30C591A22A5F8BB20EB32B2A739486DC5F3810E00BBDB58C5C"));

  }
}