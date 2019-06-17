package org.tron.core.netlog.slf4jkafka.kafkaproducer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.TimeoutException;
import org.tron.core.netlog.slf4jkafka.appender.KafkaConcurrentAppender;
import org.tron.core.netlog.slf4jkafka.recorder.TronProducerRecord;

@Slf4j(topic = "kafka-log")
public class KafkaProducerConcurrentProcessor {

  public KafkaProducer kafkaProducer;
  private KafkaConcurrentAppender appender;
  private boolean flag = true;

  public void setKafkaProducer(KafkaProducer kafkaProducer) {
    this.kafkaProducer = kafkaProducer;
  }

  public <K, V> boolean send(TronProducerRecord<K, V> record) {

    this.kafkaProducer.send(record.getProducerRecord(), new Callback() {
      @Override
      public void onCompletion(RecordMetadata metadata, Exception e) {
        if (e != null) {
          if (e instanceof TimeoutException) {
            flag = false;
            logger.warn("timeout of kafka server.");
          }
        } else {
          flag = true;
        }
      }
    });
    return flag;
  }
}