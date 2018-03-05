package org.tron.utils;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.DecodeResult;

public class DecodeResultTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testToString() {
    DecodeResult decodeResult = new DecodeResult(2, "It is String");
    logger.info("decoded instanceof String={}", decodeResult.toString());
    Assert.assertEquals("It is String", decodeResult.toString());

    byte[] barray = new byte[]{1, 2, 3, 4};
    DecodeResult decodeResult2 = new DecodeResult(1, barray);
    logger.info("decoded instanceof byte[]={}", decodeResult2.toString());
    Assert.assertEquals("01020304", decodeResult2.toString());

    String[] s = new String[]{"hello,", "java-tron", "!"};
    DecodeResult decodeResult3 = new DecodeResult(3, s);
    logger.info("decoded instanceof Object[]={}", decodeResult3.toString());
    Assert.assertEquals("hello,java-tron!", decodeResult3.toString());

  }

}
