package org.tron.common.logsfilter;

import org.junit.Assert;
import org.junit.Test;

public class DesensitizedConverterTest {

  @Test
  public void testReplace() {
    DesensitizedConverter converter = new DesensitizedConverter();
    DesensitizedConverter.addSensitive("/192.168.1.10", "address1");
    DesensitizedConverter.addSensitive("/197.168.1.10", "address2");

    String logStr1 = "This is test log /192.168.1.10:100, /197.168.1.10:200, /197.168.1.10:100";
    Assert.assertEquals("This is test log address1:100, address2:200, address2:100",
        converter.desensitization(logStr1));

    String logStr2 = "This is test log /192.168.1.100:100, /197.168.1.10:200, /197.168.1.10:100";
    Assert.assertEquals("This is test log unknown:100, address2:200, address2:100",
        converter.desensitization(logStr2));
  }
}
