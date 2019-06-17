package org.tron.core.netlog.slf4jkafka.config;

import java.util.HashMap;
import java.util.Map;

public class Slf4jKafkaConfig {

  private String bootstrapServers;
  private String kafkaTopic;

  private Long timestamp;
  private Integer partition;
  private String kafkaKey;


  public void setParamMap(Map<String, String> paramMap) {
    this.paramMap = paramMap;
  }

  private Map<String, String> paramMap = new HashMap<>();

  public Map<String, String> getParamMap() {
    return paramMap;
  }

  public String getBootstrapServers() {
    return bootstrapServers;
  }

  public void setBootstrapServers(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
  }

  public String getKafkaTopic() {
    return kafkaTopic;
  }

  public void setKafkaTopic(String kafkaTopic) {
    this.kafkaTopic = kafkaTopic;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public Integer getPartition() {
    return partition;
  }

  public void setPartition(Integer partition) {
    this.partition = partition;
  }

  public String getKafkaKey() {
    return kafkaKey;
  }

  public void setKafkaKey(String kafkaKey) {
    this.kafkaKey = kafkaKey;
  }
}
