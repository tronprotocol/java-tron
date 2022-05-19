package org.tron.core.exception;

import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;

public class TransactionExpirationException extends TronException {

  public TransactionExpirationException() {
    super();
  }

  public TransactionExpirationException(String message) {
    super(message);
  }

  protected void report() {
    Metrics.counterInc(MetricKeys.Counter.TXS, 1,
        MetricLabels.Counter.TXS_FAIL, MetricLabels.Counter.TXS_FAIL_EXPIRED);

  }

}