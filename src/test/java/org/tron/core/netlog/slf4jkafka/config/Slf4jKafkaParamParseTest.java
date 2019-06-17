package org.tron.core.netlog.slf4jkafka.config;


import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.netlog.slf4jkafka.exceptions.KafkaConfigException;

@Slf4j
public class Slf4jKafkaParamParseTest {

  @Test
  public void testSlf4jKafkaParamParse() {
    Properties props = new Properties();
    String paramsString = " prop1=1 prop2=2   prop3=3";
    try {
      props = Slf4jKafkaParamParse.parse(paramsString);
    } catch (KafkaConfigException e) {
      e.printStackTrace();
    }

    Assert.assertTrue("1".equals(props.getProperty("prop1")));
    Assert.assertTrue("2".equals(props.getProperty("prop2")));
    Assert.assertTrue("3".equals(props.getProperty("prop3")));
  }
}