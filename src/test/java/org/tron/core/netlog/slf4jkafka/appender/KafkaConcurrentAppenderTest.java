package org.tron.core.netlog.slf4jkafka.appender;


import static org.slf4j.Logger.ROOT_LOGGER_NAME;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.tron.core.netlog.slf4jkafka.recorder.NetLogRecord;
import org.tron.core.netlog.slf4jkafka.recorder.TronProducerRecord;

public class KafkaConcurrentAppenderTest {

  @Test
  public void testKafkaConcurrentAppenderSuccess() throws IOException, JoranException {

    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

    File externalConfigFile = new File(
        getClass().getClassLoader().getResource("logback-test-kafka.xml").getFile());

    if (!externalConfigFile.exists()) {
      throw new IOException(
          "Logback External Config File Parameter does not reference a file that exists");
    } else {
      if (!externalConfigFile.isFile()) {
        throw new IOException(
            "Logback External Config File Parameter exists, but does not reference a file");
      } else {
        if (!externalConfigFile.canRead()) {
          throw new IOException(
              "Logback External Config File exists and is a file, but cannot be read.");
        } else {
          JoranConfigurator configurator = new JoranConfigurator();
          configurator.setContext(lc);
          lc.reset();
          configurator.doConfigure(externalConfigFile);
        }
      }
    }

    Logger rootLogger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);

    KafkaConcurrentAppender appender =
        ((KafkaConcurrentAppender) rootLogger.getAppender("KAFKA-LOG"));

    appender.setTopic(null);
    appender.setBootstrapServers(null);
    appender.start();
    Assert.assertFalse(appender.isStarted());
  }

  @Test
  public void testKafkaConcurrentAppenderFailure() throws IOException, JoranException {

    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

    File externalConfigFile = new File(
        getClass().getClassLoader().getResource("logback-test-kafka.xml").getFile());

    if (!externalConfigFile.exists()) {
      throw new IOException(
          "Logback External Config File Parameter does not reference a file that exists");
    } else {
      if (!externalConfigFile.isFile()) {
        throw new IOException(
            "Logback External Config File Parameter exists, but does not reference a file");
      } else {
        if (!externalConfigFile.canRead()) {
          throw new IOException(
              "Logback External Config File exists and is a file, but cannot be read.");
        } else {
          JoranConfigurator configurator = new JoranConfigurator();
          configurator.setContext(lc);
          lc.reset();
          configurator.doConfigure(externalConfigFile);
        }
      }
    }

    Logger rootLogger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);

    KafkaConcurrentAppender appender =
        ((KafkaConcurrentAppender) rootLogger.getAppender("KAFKA-LOG"));

    Assert.assertTrue("kafka-test".equals(appender.getTopic()));
    Assert.assertTrue("1.2.3.4:18888".equals(appender.getBootstrapServers()));
    Assert.assertTrue(6000 == appender.getRetryInterval());
    Assert.assertTrue(null == appender.getKafkaParamString());
    Assert.assertTrue(appender.isStarted());
  }

  @Test
  public void testRetryWithInterval() {
    TronProducerRecord record = new NetLogRecord("topic", "value", 0, System.currentTimeMillis(),
        "key");
    KafkaConcurrentAppender append = new KafkaConcurrentAppender();
    append.count = 0;
    append.setRetryInterval(5000);
    append.retryWithInterval((e) -> {
      return true;
    }, record);
    Assert.assertTrue(append.count == 0);

    append.count = 1;
    append.retryWithInterval((e) -> {
      return true;
    }, record);
    Assert.assertTrue(append.count == 2);

    append.count = 2;
    append.retryWithInterval((e) -> {
      return false;
    }, record);
    Assert.assertTrue(append.count == 3);

    append.count = 4999;
    append.retryWithInterval((e) -> {
      return false;
    }, record);
    Assert.assertTrue(append.count == 5000);

    append.count = 5000;
    append.retryWithInterval((e) -> {
      return false;
    }, record);
    Assert.assertTrue(append.count == 1);

    append.count = 5000;
    append.retryWithInterval((e) -> {
      return true;
    }, record);
    Assert.assertTrue(append.count == 0);
  }
}