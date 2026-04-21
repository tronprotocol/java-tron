package org.tron.core.services.jsonrpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigInteger;
import org.junit.Test;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;

public class JsonRpcApiUtilTest {

  @Test
  public void parseBlockNumberAcceptsHex() throws JsonRpcInvalidParamsException {
    assertEquals(BigInteger.valueOf(0x1a), JsonRpcApiUtil.parseBlockNumber("0x1a"));
    assertEquals(BigInteger.ZERO, JsonRpcApiUtil.parseBlockNumber("0x0"));
  }

  @Test
  public void parseBlockNumberAcceptsDecimal() throws JsonRpcInvalidParamsException {
    assertEquals(BigInteger.valueOf(12345), JsonRpcApiUtil.parseBlockNumber("12345"));
  }

  @Test
  public void parseBlockNumberAcceptsMaxLength() throws JsonRpcInvalidParamsException {
    // 0x + 98 hex chars = 100 chars total, at the limit
    String maxValid = "0x" + new String(new char[98]).replace('\0', 'f');
    assertEquals(100, maxValid.length());
    JsonRpcApiUtil.parseBlockNumber(maxValid);
  }

  @Test
  public void parseBlockNumberRejectsOversized() {
    // 101 chars exceeds the 100-char limit
    String tooLong = "0x" + new String(new char[99]).replace('\0', 'a');
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseBlockNumber(tooLong));
    assertEquals("invalid block number", e.getMessage());
  }

  @Test
  public void parseBlockNumberRejectsNull() {
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseBlockNumber(null));
    assertEquals("invalid block number", e.getMessage());
  }

  @Test
  public void parseBlockNumberRejectsMalformedHex() {
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseBlockNumber("0xGG"));
    assertEquals("invalid block number", e.getMessage());
  }

  @Test
  public void parseBlockNumberRejectsEmpty() {
    assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseBlockNumber(""));
  }
}
