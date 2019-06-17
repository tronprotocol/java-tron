package org.tron.core.netlog.slf4jkafka.exceptions;

import org.tron.core.exception.TronException;

public class KafkaProducerCreateException extends TronException {

  public KafkaProducerCreateException() {
    super("Kafka Producer Create Failure.");
  }
}