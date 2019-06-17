package org.tron.core.netlog.slf4jkafka.kafkaproducer;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.tron.core.netlog.slf4jkafka.exceptions.KafkaProducerCreateException;

public class KafkaProducerContext {

  public static Properties getDefaultProperties() {
    Properties props = new Properties();
    props.put("acks", "0");
    props.put("retries", 0);
    props.put("batch.size", 16384);
    props.put("linger.ms", 1);
    props.put("buffer.memory", 33554432);
    props.put("max.block.ms", 6000);
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

    return props;
  }


  public static KafkaProducer makeKafkaProducer(Properties config)
      throws KafkaProducerCreateException {
    try {
      return new KafkaProducer(config);
    } catch (Exception e) {
      throw new KafkaProducerCreateException();
    }
  }
}
