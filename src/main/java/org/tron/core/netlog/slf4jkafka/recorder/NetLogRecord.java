package org.tron.core.netlog.slf4jkafka.recorder;


public class NetLogRecord extends TronProducerRecord<String, String> {

  public NetLogRecord(String topic, String value, Integer partition, Long timestamp, String key) {
    super(topic, value, partition, timestamp, key);
  }

}