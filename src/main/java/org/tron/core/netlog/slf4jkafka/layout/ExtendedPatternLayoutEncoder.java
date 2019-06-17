package org.tron.core.netlog.slf4jkafka.layout;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import org.tron.core.netlog.slf4jkafka.converter.EncodedMessageConverter;
import org.tron.core.netlog.slf4jkafka.converter.NodeIdConverter;
import org.tron.core.netlog.slf4jkafka.converter.TimestampConvert;

public class ExtendedPatternLayoutEncoder extends PatternLayoutEncoder {
  @Override
  public void start() {
    // put your converter
    PatternLayout.defaultConverterMap.put(
        "nodeid", NodeIdConverter.class.getName());
    PatternLayout.defaultConverterMap.put(
        "raw", EncodedMessageConverter.class.getName());
    PatternLayout.defaultConverterMap.put(
        "timestamp", TimestampConvert.class.getName());
    super.start();
  }
}