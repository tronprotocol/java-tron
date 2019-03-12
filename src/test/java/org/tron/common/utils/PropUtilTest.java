package org.tron.common.utils;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class PropUtilTest {

  @Test
  public void testWriteProperty() {
    String filename = "test_prop.properties";
    File file = new File(filename);
    try {
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    PropUtil.writeProperty(filename, "key", "value");
    Assert.assertTrue("value".equals(PropUtil.readProperty(filename, "key")));
    PropUtil.writeProperty(filename, "key", "value2");
    Assert.assertTrue("value2".equals(PropUtil.readProperty(filename, "key")));
    file.delete();
  }

  @Test
  public void testReadProperty() {
    String filename = "test_prop.properties";
    File file = new File(filename);
    try {
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    PropUtil.writeProperty(filename, "key", "value");
    Assert.assertTrue("value".equals(PropUtil.readProperty(filename, "key")));
    file.delete();
    Assert.assertTrue("".equals(PropUtil.readProperty(filename, "key")));
  }
}