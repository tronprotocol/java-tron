package org.tron.common.utils;

import org.junit.Assert;

import org.junit.Test;


public class DbUtilCommonsTest {

  private byte[] arr = "albertoTest".getBytes();

  @Test
  public void testClone() {
    byte[] clone = arr.clone();
    Assert.assertArrayEquals(arr, clone);
  }
}
