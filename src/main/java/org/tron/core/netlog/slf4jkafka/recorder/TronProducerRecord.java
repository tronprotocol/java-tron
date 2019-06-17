package org.tron.core.netlog.slf4jkafka.recorder;

import org.apache.kafka.clients.producer.ProducerRecord;


public abstract class TronProducerRecord<K, V> {


  private String topic;
  private Integer partition;
  private Long timestamp;
  private K key;
  private V msg;

  protected ProducerRecord<K, V> producerRecord;

  public TronProducerRecord(String topic, V value, Integer partition, Long timestamp, K key) {
    this.producerRecord = new ProducerRecord(topic, partition, timestamp, key, value);
  }

  public ProducerRecord<K, V> getProducerRecord() {
    return this.producerRecord;
  }
}