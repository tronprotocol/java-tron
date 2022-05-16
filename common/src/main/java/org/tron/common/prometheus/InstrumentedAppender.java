package org.tron.common.prometheus;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.prometheus.client.Counter;

/**
 * a new instrumented appender using the default registry.
 */
public class InstrumentedAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
  public static final String COUNTER_NAME = "tron:error_info";

  private static final Counter defaultCounter = Counter.build().name(COUNTER_NAME)
      .help("tron log statements at error type levels")
      .labelNames("topic", "type")
      .register();

  @Override
  protected void append(ILoggingEvent event) {
    if (Metrics.enabled() && event.getLevel().toInt() == Level.ERROR_INT) {
      String type = MetricLabels.UNDEFINED;
      if (event.getThrowableProxy() != null) {
        type = event.getThrowableProxy().getClassName();
        type = type.substring(type.lastIndexOf(".") + 1);
      }
      defaultCounter.labels(event.getLoggerName(), type).inc();
    }
  }
}
