package org.web3j.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class StringsTest {

  // --- toCsv ---

  @Test
  public void testToCsvMultipleElements() {
    assertEquals("a, b, c", Strings.toCsv(Arrays.asList("a", "b", "c")));
  }

  @Test
  public void testToCsvSingleElement() {
    assertEquals("a", Strings.toCsv(Collections.singletonList("a")));
  }

  @Test
  public void testToCsvNull() {
    assertNull(Strings.toCsv(null));
  }

  @Test
  public void testToCsvEmpty() {
    assertEquals("", Strings.toCsv(Collections.<String>emptyList()));
  }

  // --- join ---

  @Test
  public void testJoinCustomDelimiter() {
    assertEquals("a|b|c", Strings.join(Arrays.asList("a", "b", "c"), "|"));
  }

  @Test
  public void testJoinNull() {
    assertNull(Strings.join(null, ","));
  }

  // --- capitaliseFirstLetter ---

  @Test
  public void testCapitaliseFirstLetter() {
    assertEquals("Hello", Strings.capitaliseFirstLetter("hello"));
    assertEquals("A", Strings.capitaliseFirstLetter("a"));
  }

  @Test
  public void testCapitaliseFirstLetterAlreadyCapital() {
    assertEquals("Hello", Strings.capitaliseFirstLetter("Hello"));
  }

  @Test
  public void testCapitaliseFirstLetterNull() {
    assertNull(Strings.capitaliseFirstLetter(null));
  }

  @Test
  public void testCapitaliseFirstLetterEmpty() {
    assertEquals("", Strings.capitaliseFirstLetter(""));
  }

  // --- lowercaseFirstLetter ---

  @Test
  public void testLowercaseFirstLetter() {
    assertEquals("hello", Strings.lowercaseFirstLetter("Hello"));
    assertEquals("a", Strings.lowercaseFirstLetter("A"));
  }

  @Test
  public void testLowercaseFirstLetterAlreadyLower() {
    assertEquals("hello", Strings.lowercaseFirstLetter("hello"));
  }

  @Test
  public void testLowercaseFirstLetterNull() {
    assertNull(Strings.lowercaseFirstLetter(null));
  }

  @Test
  public void testLowercaseFirstLetterEmpty() {
    assertEquals("", Strings.lowercaseFirstLetter(""));
  }

  // --- zeros ---

  @Test
  public void testZeros() {
    assertEquals("000", Strings.zeros(3));
    assertEquals("", Strings.zeros(0));
  }

  // --- repeat ---

  @Test
  public void testRepeat() {
    assertEquals("aaa", Strings.repeat('a', 3));
    assertEquals("", Strings.repeat('x', 0));
  }

  // --- isEmpty ---

  @Test
  public void testIsEmpty() {
    assertTrue(Strings.isEmpty(null));
    assertTrue(Strings.isEmpty(""));
    assertFalse(Strings.isEmpty("a"));
    assertFalse(Strings.isEmpty(" "));
  }
}
