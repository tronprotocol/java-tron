package org.tron.core.exception;

import org.junit.Assert;
import org.junit.Test;
import org.tron.core.exception.jsonrpc.JsonRpcException;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;

public class JsonRpcExceptionTest {
  @Test
  public void testJsonRpcExceptionWithData() {
    String testData = "test_data";
    JsonRpcException exception = new JsonRpcException("test message", testData);
    Assert.assertEquals(testData, exception.getData());

    String hexData = "0x1234";
    JsonRpcInternalException rpcException = new JsonRpcInternalException("test", hexData);
    Assert.assertEquals(hexData, rpcException.getData());

    try {
      throw new JsonRpcInternalException("test", hexData);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof JsonRpcException);
      Assert.assertEquals(hexData, ((JsonRpcException)e).getData());
    }
  }
}
