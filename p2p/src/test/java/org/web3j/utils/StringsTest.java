package org.web3j.utils;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class StringsTest {

  @Test
  public void toCsvAndJoin() {
    Assert.assertEquals("a, b, c", Strings.toCsv(Arrays.asList("a", "b", "c")));
    Assert.assertEquals("a", Strings.toCsv(Collections.singletonList("a")));
    Assert.assertEquals("", Strings.toCsv(Collections.<String>emptyList()));
    Assert.assertNull(Strings.toCsv(null));
    Assert.assertEquals("a|b", Strings.join(Arrays.asList("a", "b"), "|"));
    Assert.assertNull(Strings.join(null, "|"));
  }

  @Test
  public void capitaliseFirstLetter() {
    Assert.assertEquals("Abc", Strings.capitaliseFirstLetter("abc"));
    Assert.assertEquals("Abc", Strings.capitaliseFirstLetter("Abc"));
    Assert.assertEquals("", Strings.capitaliseFirstLetter(""));
    Assert.assertNull(Strings.capitaliseFirstLetter(null));
  }

  @Test
  public void lowercaseFirstLetter() {
    Assert.assertEquals("aBC", Strings.lowercaseFirstLetter("ABC"));
    Assert.assertEquals("abc", Strings.lowercaseFirstLetter("abc"));
    Assert.assertEquals("", Strings.lowercaseFirstLetter(""));
    Assert.assertNull(Strings.lowercaseFirstLetter(null));
  }

  @Test
  public void zerosAndRepeat() {
    Assert.assertEquals("", Strings.zeros(0));
    Assert.assertEquals("000", Strings.zeros(3));
    Assert.assertEquals("xxxx", Strings.repeat('x', 4));
  }

  @Test
  public void isEmpty() {
    Assert.assertTrue(Strings.isEmpty(null));
    Assert.assertTrue(Strings.isEmpty(""));
    Assert.assertFalse(Strings.isEmpty(" "));
    Assert.assertFalse(Strings.isEmpty("a"));
  }
}
