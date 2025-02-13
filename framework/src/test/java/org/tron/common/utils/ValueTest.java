package org.tron.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;

public class ValueTest {

  private Value value;

  @Before
  public void setUp() {
    value = new Value();
    value.init(new byte[100]);
  }

  @Test
  public void testAsObj() {
    Object obj = new Object();
    value = new Value(obj);
    assertEquals("asObj should return the encapsulated object", obj, value.asObj());
  }

  @Test
  public void testAsList() {
    Object[] array = { "element1", "element2" };
    value = new Value(array);
    List<Object> list = value.asList();
    assertEquals(2, list.size());
    assertEquals("element1", list.get(0));
  }

  @Test
  public void testAsInt() {
    value = new Value(123);
    assertEquals(123, value.asInt());
    assertEquals(0, new Value(new byte[0]).asInt());
    assertEquals(0, new Value("test").asInt());
  }

  @Test
  public void testAsLong() {
    value = new Value(123456789L);
    assertEquals(123456789L, value.asLong());
    assertEquals(0L, new Value(new byte[0]).asLong());
    assertEquals(0L, new Value("TEST").asLong());
  }

  @Test
  public void testAsBigInt() {
    BigInteger bigInteger = new BigInteger("12345678901234567890");
    value = new Value(bigInteger);
    assertEquals(bigInteger, value.asBigInt());
  }

  @Test
  public void testAsString() {
    value = new Value("test string");
    assertEquals("test string", value.asString());
    assertNotNull(new Value(new byte[0]).asString());
    assertEquals("", new Value(100).asString());
    assertNotNull(value.get(0));

  }

  @Test
  public void testAsBytes() {
    byte[] bytes = { 0x01, 0x02, 0x03 };
    value = new Value(bytes);
    assertArrayEquals("asBytes should return the correct byte array", bytes, value.asBytes());
    assertEquals(EMPTY_BYTE_ARRAY, new Value(100).asBytes());
  }

  @Test
  public void testGet() {
    List<Object> list = Arrays.asList("element1", "element2");
    value = new Value(list.toArray());
    Value element = value.get(0);
    assertEquals("element1", element.asString());
    assertNotNull(value.get(2));
  }

  @Test(expected = Exception.class)
  public void testGetNegativeIndex() {
    value = new Value(new Object[] { "element1", "element2" });
    value.get(-1);
  }

  @Test
  public void testCmp() {
    Value value1 = new Value("test");
    Value value2 = new Value("test");
    assertTrue("cmp should return true for equal values", value1.cmp(value2));
  }

  @Test
  public void testIsList() {
    value = new Value(new Object[] { "element1", "element2" });
    assertTrue("isList should return true for an array", value.isList());
    assertNotNull(value.toString());
  }

  @Test
  public void testIsString() {
    value = new Value("test string");
    assertTrue("isString should return true for a string", value.isString());
  }

  @Test
  public void testIsInt() {
    value = new Value(123);
    assertTrue("isInt should return true for an integer", value.isInt());
  }

  @Test
  public void testIsLong() {
    value = new Value(123456789L);
    assertTrue("isLong should return true for a long", value.isLong());
  }

  @Test
  public void testIsBigInt() {
    value = new Value(new BigInteger("12345678901234567890"));
    assertTrue("isBigInt should return true for a BigInteger", value.isBigInt());
  }

  @Test
  public void testIsBytes() {
    byte[] bytes = { 0x01, 0x02, 0x03 };
    value = new Value(bytes);
    assertTrue("isBytes should return true for a byte array", value.isBytes());
  }

  @Test
  public void testIsReadableString() {
    byte[] readableBytes = "Hello World".getBytes();
    value = new Value(readableBytes);
    assertTrue(value.isReadableString());
  }

  @Test
  public void testIsHexString() {
    byte[] hexBytes = Hex.decode("48656c6c6f20576f726c64");
    value = new Value(hexBytes);
    assertFalse("isHexString should return true for hex byte array", value.isHexString());
  }

  @Test
  public void testIsHashCode() {
    byte[] hashBytes = new byte[32];
    value = new Value(hashBytes);
    assertTrue("isHashCode should return true for a 32 byte array", value.isHashCode());
  }

  @Test
  public void testIsNull() {
    value = new Value(null);
    assertTrue("isNull should return true for null", value.isNull());
  }

  @Test
  public void testIsEmpty() {
    value = new Value("");
    assertTrue("isEmpty should return true for an empty string", value.isEmpty());
  }

  @Test
  public void testLength() {
    value = new Value("test string");
    assertEquals(11, value.length());
  }

  @Test
  public void testToString() {
    value = new Value("test string");
    assertEquals("test string", value.toString());
  }

  @Test
  public void testCountBranchNodes() {
    Object[] array = { new Value("element1"), new Value("element2") };
    value = new Value(array);
    assertEquals(0, value.countBranchNodes());
    value = new Value(new byte[0]);
    assertEquals(0, value.countBranchNodes());

  }
}