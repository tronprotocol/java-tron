package org.tron.common.logsfilter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.log.layout.DesensitizedConverter;

public class DesensitizedConverterTest {

  @Test
  public void testReplace()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    DesensitizedConverter converter = new DesensitizedConverter();
    DesensitizedConverter.addSensitive("192.168.1.10", "address1");
    DesensitizedConverter.addSensitive("197.168.1.10", "address2");

    Method method = converter.getClass().getDeclaredMethod(
        "desensitization", String.class);
    method.setAccessible(true);

    String logStr1 = "This is test log /192.168.1.10:100, /197.168.1.10:200, /197.168.1.10:100";
    String result1 = (String) method.invoke(converter, logStr1);
    Assert.assertEquals("This is test log /address1:100, /address2:200, /address2:100",
        result1);

    String logStr2 = "This is test log /192.168.1.100:100, /197.168.1.10:200, /197.168.1.10:100";
    String result2 = (String) method.invoke(converter, logStr2);
    Assert.assertEquals("This is test log /IP:100, /address2:200, /address2:100",
        result2);
  }
}
