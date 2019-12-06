package org.tron.common.utils;


import org.junit.Assert;
import org.junit.Test;


public class DbCommonsTest {

  private byte[] arr= "alberto test".getBytes();


  @Test
  public void testClone() {
    byte[] clone =  arr.clone();
    Assert.assertArrayEquals(arr, clone);
  }

}
