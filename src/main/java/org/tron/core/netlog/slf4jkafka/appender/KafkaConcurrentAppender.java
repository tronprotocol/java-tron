package org.tron.core.netlog.slf4jkafka.appender;

import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.status.ErrorStatus;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.core.netlog.slf4jkafka.config.Slf4jKafkaParamParse;
import org.tron.core.netlog.slf4jkafka.exceptions.KafkaConfigException;
import org.tron.core.netlog.slf4jkafka.exceptions.KafkaProducerCreateException;
import org.tron.core.netlog.slf4jkafka.kafkaproducer.KafkaProducerConcurrentProcessor;
import org.tron.core.netlog.slf4jkafka.kafkaproducer.KafkaProducerContext;
import org.tron.core.netlog.slf4jkafka.recorder.NetLogRecord;
import org.tron.core.netlog.slf4jkafka.recorder.TronProducerRecord;

@Slf4j(topic = "kafka-log")
public class KafkaConcurrentAppender<E> extends OutputStreamAppender<E> {

  private String topic = "";
  private String bootstrapServers = "";

  private Integer retryInterval;
  public Integer count = 0;

  private String kafkaParamString;
  private Properties kafkaConfig = new Properties();

  private KafkaProducerConcurrentProcessor processor;

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public String getBootstrapServers() {
    return bootstrapServers;
  }

  public void setBootstrapServers(String bootstrapServers) {
    Preconditions.checkNotNull(bootstrapServers);
    this.bootstrapServers = bootstrapServers;
  }

  public String getKafkaParamString() {
    return kafkaParamString;
  }

  public void setKafkaParamString(String kafkaParamString) {
    this.kafkaParamString = kafkaParamString;
  }

  public Integer getRetryInterval() {
    return retryInterval;
  }

  public void setRetryInterval(Integer retryInterval) {
    this.retryInterval = Math.max(5000, retryInterval);
  }

  @Override
  public void start() {
    Map<Integer, String> errorsMap = new HashMap<>();

    if (StringUtils.isEmpty(topic)) {
      addStatus(new ErrorStatus("topic is null", this));
      errorsMap.put(errorsMap.size() + 1, "topic is null.");
    }

    try {
      kafkaConfig = Slf4jKafkaParamParse.INSTANCE.parse(kafkaParamString);
      try {
        kafkaConfig.setProperty("bootstrap.servers", bootstrapServers);
      } catch (NullPointerException npe) {
        addStatus(new ErrorStatus("bootstrap.server is null", this));
        errorsMap.put(errorsMap.size() + 1, "bootstrapServer is null.");
      }
    } catch (KafkaConfigException e) {
      addStatus(new ErrorStatus("kafkaParamString is parsed failure.", this));
      errorsMap.put(errorsMap.size() + 1, "kafkaParamString is parsed failure.");
    }

    if (retryInterval == null) {
      addStatus(new ErrorStatus("retryInterval is null", this));
      errorsMap.put(errorsMap.size() + 1, "retryInterval is null.");
    }

    try {
      processor = new KafkaProducerConcurrentProcessor();
      processor.setKafkaProducer(KafkaProducerContext.makeKafkaProducer(kafkaConfig));
    } catch (KafkaProducerCreateException e) {
      addStatus(new ErrorStatus(e.getMessage(), this));
      errorsMap.put(errorsMap.size() + 1, "makeKafkaProducer is failure.");
    }

    if (errorsMap.size() == 0) {
      started = true;
      logger.info("Net Log Startup successfully.");
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append("Net Log Startup failure, Reason:\n");
      errorsMap.forEach((k, v) -> {
        sb.append(k).append(".").append(v).append("\n");
      });
      logger.error(sb.toString());
    }
  }

  @Override
  protected void subAppend(E event) {
    LayoutWrappingEncoder<E> encoder = (LayoutWrappingEncoder<E>) this.getEncoder();
    String msg = encoder.getLayout().doLayout(event);
    TronProducerRecord record = new NetLogRecord(
        this.getTopic(), msg, null,
        null, null);

    retryWithInterval((obj) -> processor.send(obj), record);
  }

  protected void retryWithInterval(Predicate<TronProducerRecord> predicate,
      TronProducerRecord record) {
    if (count % this.getRetryInterval() == 0) {
      if (predicate.test(record)) {
        count = 0;
      } else {
        count = 1;
      }
    } else {
      count++;
    }
  }
}