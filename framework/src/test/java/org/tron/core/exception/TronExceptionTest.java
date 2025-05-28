package org.tron.core.exception;

import org.junit.Assert;
import org.junit.Test;

public class TronExceptionTest {
  @Test
  public void testTronExceptionWithData() {
    String testData = "test_data";
    TronException exception = new TronException("test message", testData);
    Assert.assertEquals(testData, exception.getData());

    String hexData = "0x1234";
    JsonRpcInternalException rpcException = new JsonRpcInternalException("test", hexData);
    Assert.assertEquals(hexData, rpcException.getData());

    try {
      throw new JsonRpcInternalException("test", hexData);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof TronException);
      Assert.assertEquals(hexData, ((TronException)e).getData());
    }
  }
}
