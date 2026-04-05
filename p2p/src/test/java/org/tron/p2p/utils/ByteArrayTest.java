package org.tron.p2p.utils;

import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.dns.update.AwsClient;


public class ByteArrayTest {

  @Test
  public void testHexToString() {
    byte[] data = new byte[] {-128, -127, -1, 0, 1, 127};
    Assert.assertEquals("8081ff00017f", ByteArray.toHexString(data));
  }

  @Test
  public void testSubdomain(){
    Assert.assertTrue(AwsClient.isSubdomain("cde.abc.com","abc.com"));
    Assert.assertTrue(AwsClient.isSubdomain("cde.abc.com.","abc.com"));
    Assert.assertTrue(AwsClient.isSubdomain("cde.abc.com","abc.com."));
    Assert.assertTrue(AwsClient.isSubdomain("cde.abc.com.","abc.com."));

    Assert.assertFalse(AwsClient.isSubdomain("a-sub.abc.com","sub.abc.com"));
    Assert.assertTrue(AwsClient.isSubdomain(".sub.abc.com","sub.abc.com"));
  }
}
