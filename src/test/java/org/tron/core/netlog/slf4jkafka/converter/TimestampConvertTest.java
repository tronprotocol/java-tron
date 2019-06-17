package org.tron.core.netlog.slf4jkafka.converter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Test;

public class TimestampConvertTest {

  @Test
  public void testTimestampConverter() {
    TimestampConvert converter = new TimestampConvert();
    ILoggingEvent event = new LoggingEvent();

    String time = converter.convert(event);

    Assert.assertTrue(Long.valueOf(time) > 0);
  }
}