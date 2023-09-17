package org.tron.plugins;

import org.junit.Assert;
import org.junit.Test;

public class ToolKitTest {

  @Test
  public void testCall() {
    long expect = 0;
    Toolkit toolkit = new Toolkit();
    try {
      long call = toolkit.call();
      Assert.assertEquals(expect, call);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
