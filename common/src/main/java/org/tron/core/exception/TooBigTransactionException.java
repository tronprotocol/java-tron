package org.tron.core.exception;

import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;

public class TooBigTransactionException extends TronException {

  public TooBigTransactionException() {
    super();
  }

  public TooBigTransactionException(String message) {
    super(message);
  }

  protected void report() {
    Metrics.counterInc(MetricKeys.Counter.TXS, 1,
        MetricLabels.Counter.TXS_FAIL, MetricLabels.Counter.TXS_FAIL_BIG);
  }

}
