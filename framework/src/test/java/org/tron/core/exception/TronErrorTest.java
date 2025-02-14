package org.tron.core.exception;

import org.junit.Assert;
import org.junit.Test;

public class TronErrorTest {

  @Test
  public void testTronError() {
    TronError tronError = new TronError("message", TronError.ErrCode.WITNESS_KEYSTORE_LOAD);
    Assert.assertEquals(tronError.getErrCode(), TronError.ErrCode.WITNESS_KEYSTORE_LOAD);
    Assert.assertEquals(tronError.getErrCode().toString(), "WITNESS_KEYSTORE_LOAD(-1)");
    Assert.assertEquals(tronError.getErrCode().getCode(), -1);
    tronError = new TronError("message", new Throwable(), TronError.ErrCode.API_SERVER_INIT);
    Assert.assertEquals(tronError.getErrCode(), TronError.ErrCode.API_SERVER_INIT);
    tronError = new TronError(new Throwable(), TronError.ErrCode.LEVELDB_INIT);
    Assert.assertEquals(tronError.getErrCode(), TronError.ErrCode.LEVELDB_INIT);
  }
}
