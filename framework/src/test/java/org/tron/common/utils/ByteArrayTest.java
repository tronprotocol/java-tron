/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.tron.common.utils.ByteArray.fromHex;
import static org.tron.common.utils.ByteArray.jsonHexToInt;
import static org.tron.common.utils.ByteArray.jsonHexToLong;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;

@Slf4j
public class ByteArrayTest {

  @Test
  public void testToHexString() {
    //logger.info("Byte: byte 16 to hex string = {}", ByteArray.toHexString(new byte[]{16}));
    assertEquals("byte to hex string is wrong", "10", ByteArray.toHexString(new byte[]{16}));
  }

  @Test
  public void long2Bytes() {
    long a = 0x123456;
    byte[] bb = ByteArray.fromLong(a);
    System.out.println(bb[6]);
    System.out.println(bb[7]);
  }

  @Test
  public void testHexStringToByte() {
    //logger.info("Byte: hex string 0x11 to byte = {}", ByteArray.fromHexString("0x11"));
    byte[] expectedfirst = new byte[]{17};
    byte[] actualfirst = ByteArray.fromHexString("0x11");
    assertArrayEquals(expectedfirst, actualfirst);
    //logger.info("Byte: hex string 10 to byte = {}", ByteArray.fromHexString("10"));
    byte[] expectedsecond = new byte[]{16};
    byte[] actualsecond = ByteArray.fromHexString("10");
    assertArrayEquals(expectedsecond, actualsecond);
    //logger.info("Byte: hex string 1 to byte = {}", ByteArray.fromHexString("1"));
    byte[] expectedthird = new byte[]{1};
    byte[] actualthird = ByteArray.fromHexString("1");
    assertArrayEquals(expectedthird, actualthird);
  }

  @Test
  public void testToLong() {
    //logger.info("Byte: byte 13 to long = {}", ByteArray.toLong(new byte[]{13}));
    assertEquals("byte to long is wrong", 13L, ByteArray.toLong(new byte[]{13}));

  }

  @Test
  public void testFromLong() {
    //logger.info("Byte: long 127L to byte = {}", ByteArray.fromLong(127L));
    byte[] expected = new byte[]{0, 0, 0, 0, 0, 0, 0, 127};
    byte[] actual = ByteArray.fromLong(127L);
    assertArrayEquals(expected, actual);

  }

  @Test
  public void test2ToHexString() {
    //byte[] bs = new byte[]{};
    //logger.info("utils.ByteArray.toHexString: {}", ByteArray.toHexString(bs));
    //logger.info("Hex.toHexString: {}", Hex.toHexString(bs));
    byte[] bss = new byte[]{8, 9, 12, 13, 14, 15, 16};
    assertEquals("ByteArray.toHexString is not equals Hex.toHexString", ByteArray.toHexString(bss),
        Hex.toHexString(bss));
  }

  @Test
  public void testFromObject_SerializableObject() {
    String testString = "Hello, World!";
    byte[] result = ByteArray.fromObject(testString);
    assertNotNull(result);
    assertTrue(result.length > 0);
  }

  @Test
  public void testJsonHexToInt_ValidHex() {
    try {
      int result = jsonHexToInt("0x1A");
      assertEquals(26, result);
    } catch (Exception e) {
      fail("Exception should not have been thrown for valid hex string.");
    }
    assertThrows(Exception.class, () -> ByteArray.jsonHexToInt("1A"));
  }

  @Test
  public void testFromHexWithPrefix() {
    String input = "0x1A3F";
    String expected = "1A3F";
    String result = fromHex(input);
    assertEquals(expected, result);
    String input1 = "1A3";
    assertEquals("01A3", fromHex(input1));
  }

  @Test
  public void testJsonHexToLong_ValidInputs() {
    try {
      // Test basic hex conversion
      assertEquals(26L, jsonHexToLong("0x1A"));
      assertEquals(255L, jsonHexToLong("0xFF"));
      assertEquals(0L, jsonHexToLong("0x0"));
      assertEquals(1L, jsonHexToLong("0x1"));

      // Test large values
      assertEquals(4294967295L, jsonHexToLong("0xFFFFFFFF"));

      // Test maximum long value
      assertEquals(Long.MAX_VALUE, jsonHexToLong("0x7FFFFFFFFFFFFFFF"));
    } catch (JsonRpcInvalidParamsException e) {
      fail("Exception should not have been thrown for valid hex strings: " + e.getMessage());
    }
  }

  @Test
  public void testJsonHexToLong_InvalidInputs() {
    // Test null input
    assertThrows(JsonRpcInvalidParamsException.class, () -> jsonHexToLong(null));

    // Test missing 0x prefix
    assertThrows(JsonRpcInvalidParamsException.class, () -> jsonHexToLong("1A"));

    // Test too long input (DDoS protection)
    StringBuilder tooLongStr = new StringBuilder("0x");
    for (int i = 0; i < 20; i++) {
      tooLongStr.append("F");
    }
    String tooLongHex = tooLongStr.toString(); // 22 characters total, exceeds MAX_HEX_LONG_LENGTH
    assertThrows(JsonRpcInvalidParamsException.class, () -> jsonHexToLong(tooLongHex));

    // Test invalid hex characters
    assertThrows(NumberFormatException.class, () -> jsonHexToLong("0xGG"));
  }

  @Test
  public void testJsonHexToInt_ValidInputs() {
    try {
      // Test basic hex conversion
      assertEquals(26, jsonHexToInt("0x1A"));
      assertEquals(255, jsonHexToInt("0xFF"));
      assertEquals(0, jsonHexToInt("0x0"));
      assertEquals(1, jsonHexToInt("0x1"));

      // Test maximum int value
      assertEquals(Integer.MAX_VALUE, jsonHexToInt("0x7FFFFFFF"));

      // Test large values
      assertEquals(65535, jsonHexToInt("0xFFFF"));
    } catch (Exception e) {
      fail("Exception should not have been thrown for valid hex strings: " + e.getMessage());
    }
  }

  @Test
  public void testJsonHexToInt_InvalidInputs() {
    // Test null input
    assertThrows(Exception.class, () -> jsonHexToInt(null));

    // Test missing 0x prefix
    assertThrows(Exception.class, () -> jsonHexToInt("1A"));

    // Test too long input (DDoS protection)
    StringBuilder tooLongStr = new StringBuilder("0x");
    for (int i = 0; i < 12; i++) {
      tooLongStr.append("F");
    }
    String tooLongHex = tooLongStr.toString(); // 14 characters total, exceeds MAX_HEX_INT_LENGTH
    assertThrows(Exception.class, () -> jsonHexToInt(tooLongHex));

    // Test invalid hex characters
    assertThrows(NumberFormatException.class, () -> jsonHexToInt("0xGG"));
  }

  @Test
  public void testJsonHexToLong_EdgeCases() {
    try {
      // Test minimum length valid input
      assertEquals(0L, jsonHexToLong("0x0"));

      // Test a long hex string that's within limits but doesn't overflow
      assertEquals(4095L, jsonHexToLong("0xFFF")); // 3 F's = 4095, safe value

      // Test length validation - this should pass length check
      assertEquals(1048575L, jsonHexToLong("0xFFFFF")); // 5 F's = 1048575, safe value
    } catch (JsonRpcInvalidParamsException e) {
      fail("Exception should not have been thrown for edge case inputs: " + e.getMessage());
    }
  }

  @Test
  public void testJsonHexToInt_EdgeCases() {
    try {
      // Test minimum length valid input
      assertEquals(0, jsonHexToInt("0x0"));

      // Test a hex string that's within limits but doesn't overflow
      assertEquals(4095, jsonHexToInt("0xFFF")); // 3 F's = 4095, safe value

      // Test length validation - this should pass length check
      assertEquals(1048575, jsonHexToInt("0xFFFFF")); // 5 F's = 1048575, safe value
    } catch (Exception e) {
      fail("Exception should not have been thrown for edge case inputs: " + e.getMessage());
    }
  }

  @Test
  public void testJsonHexToLong_OverflowHandling() {
    // Test that Long.parseLong properly handles overflow by throwing NumberFormatException
    // This tests values that pass length validation but cause overflow
    assertThrows(NumberFormatException.class,
        () -> jsonHexToLong("0x8000000000000000")); // Long.MAX_VALUE + 1
  }

  @Test
  public void testJsonHexToInt_OverflowHandling() {
    // Test that Integer.parseInt properly handles overflow by throwing NumberFormatException
    // This tests values that pass length validation but cause overflow
    assertThrows(NumberFormatException.class,
        () -> jsonHexToInt("0x80000000")); // Integer.MAX_VALUE + 1
  }
}
