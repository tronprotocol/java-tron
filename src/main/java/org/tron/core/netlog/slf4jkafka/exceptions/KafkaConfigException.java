package org.tron.core.netlog.slf4jkafka.exceptions;

import org.tron.core.exception.TronException;

public class KafkaConfigException extends TronException {

  public KafkaConfigException(String message) {
    super(message);
  }

  public KafkaConfigException(String message, Throwable cause) {
    super(message, cause);
  }

  public KafkaConfigException() {
    super("Kafka Config Error.");
  }
}