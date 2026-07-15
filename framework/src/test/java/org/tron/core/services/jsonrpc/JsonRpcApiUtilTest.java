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
    assertEquals("invalid address", e1.getMessage());
  }

  @Test
  public void addressCompatibleToByteArrayRejectsNull() {
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.addressCompatibleToByteArray(null));
    assertEquals("invalid address", e.getMessage());
  }

  @Test
  public void hashToByteArrayAcceptsValidHex() throws JsonRpcInvalidParamsException {
    String hash = "0x" + new String(new char[64]).replace('\0', 'a');
    assertEquals(32, JsonRpcApiUtil.hashToByteArray(hash).length);
  }

  @Test
  public void hashToByteArrayRejectsNonHexChars() {
    String hash = "0x" + new String(new char[64]).replace('\0', 'g');
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.hashToByteArray(hash));
    assertEquals("invalid hash value", e.getMessage());
  }

  @Test
  public void topicToByteArrayAcceptsFullLengthHex() throws JsonRpcInvalidParamsException {
    String topic = "0x" + new String(new char[64]).replace('\0', 'a');
    assertEquals(32, JsonRpcApiUtil.topicToByteArray(topic).length);
  }

  @Test
  public void topicToByteArrayPadsMissingLeadingZero() throws JsonRpcInvalidParamsException {
    String stripped = "0x" + new String(new char[63]).replace('\0', 'a');
    byte[] parsed = JsonRpcApiUtil.topicToByteArray(stripped);
    assertEquals(32, parsed.length);
    assertEquals(0x0a, parsed[0]);
  }

  @Test
  public void topicToByteArrayRejectsNonHexChars() {
    String topic = "0x" + new String(new char[64]).replace('\0', 'g');
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.topicToByteArray(topic));
    assertEquals("invalid topic: " + topic, e.getMessage());
  }

  @Test
  public void topicToByteArrayRejectsWrongLength() {
    // 62 chars (two zeros stripped) and 65 chars are both invalid
    String tooShort = "0x" + new String(new char[62]).replace('\0', 'a');
    assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.topicToByteArray(tooShort));
    String tooLong = "0x" + new String(new char[65]).replace('\0', 'a');
    assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.topicToByteArray(tooLong));
    assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.topicToByteArray(null));
  }

  @Test
  public void parseTxIndexAcceptsHex() throws JsonRpcInvalidParamsException {
    assertEquals(0x1a, JsonRpcApiUtil.parseTxIndex("0x1a"));
    assertEquals(0, JsonRpcApiUtil.parseTxIndex("0x0"));
    // 8 hex digits is the max width; 0x7fffffff is Integer.MAX_VALUE
    assertEquals(Integer.MAX_VALUE, JsonRpcApiUtil.parseTxIndex("0x7fffffff"));
  }

  @Test
  public void parseTxIndexRejectsMissingPrefix() {
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseTxIndex("1a"));
    assertEquals("invalid index value", e.getMessage());
  }

  @Test
  public void parseTxIndexAcceptsLeadingZeros() throws JsonRpcInvalidParamsException {
    // leading zeros are tolerated (only length is capped); "0x01"/"0x00" parse normally
    assertEquals(1, JsonRpcApiUtil.parseTxIndex("0x01"));
    assertEquals(0, JsonRpcApiUtil.parseTxIndex("0x00"));
  }

  @Test
  public void parseTxIndexRejectsOversized() {
    // 9 hex digits exceeds the 8-digit (0x + 8) limit -> rejected before parsing
    String tooLong = "0x" + new String(new char[9]).replace('\0', '1');
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseTxIndex(tooLong));
    assertEquals("invalid index value", e.getMessage());
  }

  @Test
  public void parseTxIndexRejectsEmptyAndNull() {
    JsonRpcInvalidParamsException e1 = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseTxIndex("0x"));
    assertEquals("invalid index value", e1.getMessage());
    JsonRpcInvalidParamsException e2 = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseTxIndex(null));
    assertEquals("invalid index value", e2.getMessage());
  }

  @Test
  public void parseTxIndexRejectsMalformedHex() {
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.parseTxIndex("0xGG"));
    assertEquals("invalid index value", e.getMessage());
  }

  @Test
  public void parseTxIndexParsesNegativeForCallerRangeCheck() throws JsonRpcInvalidParamsException {
    // "0x-1" is syntactically accepted and parsed to a negative int; the RPC handler maps
    // any out-of-range index (negative or >= tx count) to a null result.
    assertEquals(-1, JsonRpcApiUtil.parseTxIndex("0x-1"));
  }

  @Test
  public void calcFeeLimitNormal() throws JsonRpcInvalidParamsException {
    assertEquals(8400L, JsonRpcApiUtil.calcFeeLimit(20L, 420L));
    assertEquals(0L, JsonRpcApiUtil.calcFeeLimit(0L, 420L));
  }

  @Test
  public void calcFeeLimitRejectsOverflow() {
    // gas * energyFee overflows int64 -> rejected instead of silently wrapping to a bogus feeLimit
    JsonRpcInvalidParamsException e = assertThrows(JsonRpcInvalidParamsException.class,
        () -> JsonRpcApiUtil.calcFeeLimit(Long.MAX_VALUE, 420L));
    assertEquals("invalid gas: fee limit overflow", e.getMessage());
  }
}
