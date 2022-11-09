package org.tron.core.exception;

import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;

public class ScriptsException extends TronException {

  public ScriptsException() {
    super();
  }

  public ScriptsException(String message) {
    super(message);
  }

  public ScriptsException(String message, Throwable cause) {
    super(message, cause);
  }

  protected void report() {
    Metrics.counterInc(MetricKeys.Counter.TXS, 1,
        MetricLabels.Counter.TXS_FAIL, MetricLabels.Counter.TXS_FAIL_SCRIPT);
  }

}
