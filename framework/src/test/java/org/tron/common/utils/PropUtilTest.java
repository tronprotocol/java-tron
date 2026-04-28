package org.tron.common.utils;

import java.io.File;
import org.junit.Assert;
import org.junit.Test;

public class PropUtilTest {

  @Test
  public void testWriteProperty() throws Exception {
    String filename = "test_prop.properties";
    File file = new File(filename);
    file.createNewFile();
    PropUtil.writeProperty(filename, "key", "value");
    Assert.assertTrue("value".equals(PropUtil.readProperty(filename, "key")));
    PropUtil.writeProperty(filename, "key", "value2");
    Assert.assertTrue("value2".equals(PropUtil.readProperty(filename, "key")));
    file.delete();
  }

  @Test
  public void testReadProperty() throws Exception {
    String filename = "test_prop.properties";
    File file = new File(filename);
    file.createNewFile();
    PropUtil.writeProperty(filename, "key", "value");
    Assert.assertTrue("value".equals(PropUtil.readProperty(filename, "key")));
    file.delete();
    Assert.assertTrue("".equals(PropUtil.readProperty(filename, "key")));
  }
}
