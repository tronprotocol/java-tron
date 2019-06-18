package org.tron.core.netlog.slf4jkafka.config;


import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class Slf4jKafkaParamParseTest {

  @Test
  public void testSlf4jKafkaParamParse() {
    Properties props1 = new Properties();
    String paramsString = " prop1=1 prop2=2   prop3=3";
    props1 = Slf4jKafkaParamParse.parse(paramsString);

    Assert.assertTrue("1".equals(props1.getProperty("prop1")));
    Assert.assertTrue("2".equals(props1.getProperty("prop2")));
    Assert.assertTrue("3".equals(props1.getProperty("prop3")));
  }
}