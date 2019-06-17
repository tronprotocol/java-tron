package org.tron.core.netlog.slf4jkafka.exceptions;

import org.tron.core.exception.TronException;

public class KafkaServerUnavailableException extends TronException {

  public KafkaServerUnavailableException() {
    super("Kafka Server Unavailable.");
  }
}