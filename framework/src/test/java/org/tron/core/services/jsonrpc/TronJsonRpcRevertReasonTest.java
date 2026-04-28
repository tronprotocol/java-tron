package org.tron.core.services.jsonrpc;

import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.ByteArray;

public class TronJsonRpcRevertReasonTest {

  @Test
  public void testTryDecodeRevertReasonWithMalformedLength() {
    // Error(string) selector + offset=0x20 + length=0x7FFFFFFF + 3 bytes of payload.
    // parseDataBytes throws because the declared length exceeds the buffer.
    // The helper should return "" and leave the raw revert hex untouched.
    byte[] resData = ByteArray.fromHexString("08c379a0"
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "000000000000000000000000000000000000000000000000000000007fffffff"
        + "414243");
    Assert.assertEquals("", TronJsonRpcImpl.tryDecodeRevertReason(resData));
  }

  @Test
  public void testTryDecodeRevertReasonWithNegativeLength() {
    byte[] resData = ByteArray.fromHexString("08c379a0"
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        + "414243");
    Assert.assertEquals("", TronJsonRpcImpl.tryDecodeRevertReason(resData));
  }

  @Test
  public void testTryDecodeRevertReasonWithValidData() {
    byte[] resData = ByteArray.fromHexString("08c379a0"
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000016"
        + "6e6f7420656e6f75676820696e7075742076616c756500000000000000000000");
    Assert.assertEquals(": not enough input value",
        TronJsonRpcImpl.tryDecodeRevertReason(resData));
  }

  @Test
  public void testTryDecodeRevertReasonWithEmptyString() {
    // require(cond, "") yields a empty string
    byte[] resData = ByteArray.fromHexString("08c379a0"
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000000");
    Assert.assertEquals("", TronJsonRpcImpl.tryDecodeRevertReason(resData));
  }

  @Test
  public void testTryDecodeRevertReasonWithOversizedPayload() {
    // selector(4) + payload(4097) one byte over the 4096 limit: must be rejected before parse.
    byte[] resData = new byte[4101];
    resData[0] = 0x08;
    resData[1] = (byte) 0xc3;
    resData[2] = 0x79;
    resData[3] = (byte) 0xa0;
    Assert.assertEquals("", TronJsonRpcImpl.tryDecodeRevertReason(resData));
  }

  @Test
  public void testTryDecodeRevertReasonWithNullData() {
    Assert.assertEquals("", TronJsonRpcImpl.tryDecodeRevertReason(null));
  }

  @Test
  public void testTryDecodeRevertReasonWithShortSelector() {
    // length == selector length (4): not enough bytes for any payload, reject.
    Assert.assertEquals("", TronJsonRpcImpl.tryDecodeRevertReason(new byte[]{
        0x08, (byte) 0xc3, 0x79, (byte) 0xa0}));
  }

  @Test
  public void testTryDecodeRevertReasonWithNonErrorSelector() {
    // Non-Error(string) selector (e.g. Panic(uint256) = 0x4e487b71) must be rejected.
    byte[] resData = ByteArray.fromHexString("4e487b71"
        + "0000000000000000000000000000000000000000000000000000000000000001");
    Assert.assertEquals("", TronJsonRpcImpl.tryDecodeRevertReason(resData));
  }

  @Test
  public void testTryDecodeRevertReasonAtPayloadLimit() {
    // selector(4) + payload(4096) exactly at the limit: must go through parse, not size-reject.
    byte[] resData = new byte[4100];
    resData[0] = 0x08;
    resData[1] = (byte) 0xc3;
    resData[2] = 0x79;
    resData[3] = (byte) 0xa0;
    // ABI offset = 0x20
    resData[4 + 31] = 0x20;
    // ABI string length = 2
    resData[4 + 32 + 31] = 0x02;
    // data "ok", remaining bytes stay zero-padded
    resData[4 + 64] = 'o';
    resData[4 + 65] = 'k';
    Assert.assertEquals(": ok", TronJsonRpcImpl.tryDecodeRevertReason(resData));
  }
}
