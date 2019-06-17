package org.tron.core.netlog.slf4jkafka.converter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Assert;
import org.testng.annotations.Test;

public class EncodedMessageConverterTest {

  @Test
  public void testEncodedMessageConverter() {
    EncodedMessageConverter converter = new EncodedMessageConverter();
    ILoggingEvent event1 = new LoggingEvent();

    ((LoggingEvent) event1).setMessage("this is a normal msg.");
    String convertedStr = converter.convert(event1);
    Assert.assertTrue("this is a normal msg.".equals(convertedStr));

    ILoggingEvent event2 = new LoggingEvent();

    ((LoggingEvent) event2).setMessage( "this is a normal msg has a \".");
    String convertedStrWith = converter.convert(event2);
    Assert.assertTrue("this is a normal msg has a \\\".".equals(convertedStrWith));
  }
}