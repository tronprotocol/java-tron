package org.tron.core.netlog.slf4jkafka.converter;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class TimestampConvert extends ClassicConverter {

  @Override
  public String convert(ILoggingEvent event) {
    return String.valueOf(System.currentTimeMillis());
  }
}