package org.tron.core.capsule.utils;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.ByteUtil;

public class DecodeResultTest {

  @Test
  public void testConstruct() {
    DecodeResult decodeResult = new DecodeResult(0, "decoded");
    Assert.assertEquals(decodeResult.getPos(), 0);
    Assert.assertEquals(decodeResult.getDecoded(), "decoded");
    Assert.assertEquals(decodeResult.toString(), "decoded");
  }

  @Test
  public void testToString() {
    DecodeResult decodeResult = new DecodeResult(0, "decoded");
    Assert.assertEquals(decodeResult.toString(), "decoded");
    decodeResult = new DecodeResult(0, ByteUtil.intToBytes(1000));
    Assert.assertEquals(Hex.toHexString(ByteUtil.intToBytes(1000)), decodeResult.toString());
    Object[] decodedData = {"aa","bb"};
    decodeResult = new DecodeResult(0, decodedData);
    Assert.assertEquals("aabb", decodeResult.toString());
  }
}
