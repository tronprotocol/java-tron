package org.tron.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.charset.Charset;
import java.security.SecureRandom;
import org.junit.Test;

public class UtilsTest {

  @Test
  public void testGetRandom() {
    SecureRandom random = Utils.getRandom();
    assertNotNull("SecureRandom should not be null", random);
  }

  @Test
  public void testGetBytes() {
    char[] chars = "hello".toCharArray();
    byte[] bytes = Utils.getBytes(chars);

    // Convert back to String to check if it's the same
    String result = new String(bytes, Charset.forName("UTF-8"));
    assertEquals("Converted bytes should match the original string", "hello", result);
  }

  @Test
  public void testGetIdShort() {
    String longId = "12345678901234567890";
    String shortId = Utils.getIdShort(longId);
    assertEquals("Short ID should be the first 8 characters of the long ID", "12345678", shortId);

    String nullId = Utils.getIdShort(null);
    assertEquals("ID should be '<null>' for null input", "<null>", nullId);
  }

  @Test
  public void testClone() {
    byte[] original = {1, 2, 3, 4, 5};
    byte[] clone = Utils.clone(original);

    assertArrayEquals("Clone should be equal to the original", original, clone);

    // Modify the clone to ensure it's a new array
    clone[0] = 99;
    assertNotEquals("Modifying the clone should not affect the original", original[0], clone[0]);
  }

  @Test
  public void testAlignLeft() {
    String result = Utils.align("abc", '-', 10, false);
    String result1 = Utils.align("abc", '-', 2, false);
    assertEquals("abc-------", result);
    assertEquals("abc", result1);
  }

  @Test
  public void testAlignRight() {
    String result = Utils.align("abc", '-', 10, true);
    assertEquals("-------abc", result);
  }

  @Test
  public void testRepeat() {
    String result = Utils.repeat("a", 5);
    assertEquals("aaaaa", result);

    result = Utils.repeat("abc", 3);
    assertEquals("abcabcabc", result);
  }
}
