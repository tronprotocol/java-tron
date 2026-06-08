package org.tron.core.services.jsonrpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;

public class JsonRpcApiUtilTest {

  @Test
  public void parseBlockNumberAcceptsHex() throws JsonRpcInvalidParamsException {
    assertEquals(0x1aL, JsonRpcApiUtil.parseBlockNumber("0x1a"));
    assertEquals(0L, JsonRpcApiUtil.parseBlockNumber("0x0"));
  }

  @Test
  public void parseBlockNumberAcceptsDecimal() throws JsonRpcInvalidParamsException {
    assertEquals(12345L, JsonRpcApiUtil.parseBlockNumber("12345"));
  }

  @Test
  public void parseBlockNumberAcceptsMaxLongValue() throws JsonRpcInvalidParamsException {
    assertEquals(Long.MAX_VALUE,
        JsonRpcApiUtil.parseBlockNumber("0x7fffffffffffffff"));
  }

  @Test
  public void parseBlockNumberRejectsNegative() {
    JsonRpcInvalidParamsException e1 = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseBlockNumber("-1"));
    assertEquals("invalid block number", e1.getMessage());
    JsonRpcInvalidParamsException e2 = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseBlockNumber("0x-1"));
    assertEquals("invalid block number", e2.getMessage());
  }

  @Test
  public void parseBlockNumberRejectsOverflow() {
    // 2^64 - 1: fits uint64 but overflows signed long
    JsonRpcInvalidParamsException e1 = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseBlockNumber("0xffffffffffffffff"));
    assertEquals("invalid block number", e1.getMessage());
    // 2^63: just past Long.MAX_VALUE
    JsonRpcInvalidParamsException e2 = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseBlockNumber("0x8000000000000000"));
    assertEquals("invalid block number", e2.getMessage());
  }

  @Test
  public void parseBlockNumberRejectsOversized() {
    // 21 chars exceeds the 20-char limit
    String tooLong = "0x" + new String(new char[18]).replace('\0', '0') + "1";
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

  @Test
  public void addressCompatibleToByteArrayNormal()
      throws JsonRpcInvalidParamsException {
    String addr = "0xabd4b9367799eaa3197fecb144eb71de1e049abc";
    assertEquals(21, JsonRpcApiUtil.addressCompatibleToByteArray(addr).length);
  }

  @Test
  public void addressCompatibleToByteArrayRejectsOversized() {
    // 45 chars
    String justOver = "0x" + new String(new char[43]).replace('\0', 'a');
    JsonRpcInvalidParamsException e1 = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.addressCompatibleToByteArray(justOver));
    assertEquals("invalid address hash value", e1.getMessage());
  }

  @Test
  public void addressCompatibleToByteArrayRejectsNull() {
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.addressCompatibleToByteArray(null));
    assertEquals("invalid address hash value", e.getMessage());
  }
}
