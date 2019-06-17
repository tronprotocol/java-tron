package org.tron.core.netlog.slf4jkafka.kafkaproducer;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.netlog.slf4jkafka.exceptions.KafkaProducerCreateException;

public class KafkaProducerContextTest {

  @Test(expected = KafkaProducerCreateException.class)
  public void testMakeKafkaProducerFailure() throws KafkaProducerCreateException {
    Properties prop = KafkaProducerContext.getDefaultProperties();
    KafkaProducerCreateException e = null;
    KafkaProducerContext.makeKafkaProducer(prop);
  }

  @Test()
  public void testMakeKafkaProducerSuccess() throws KafkaProducerCreateException {
    Properties prop = KafkaProducerContext.getDefaultProperties();
    prop.setProperty("bootstrap.servers", "1.2.3.4:9003");
    KafkaProducer producer = KafkaProducerContext.makeKafkaProducer(prop);
    Assert.assertNotNull(producer);
  }
}