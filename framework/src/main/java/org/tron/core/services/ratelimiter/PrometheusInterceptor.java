
package org.tron.core.services.ratelimiter;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.prometheus.client.Histogram;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;

/**
 * A {@link ServerInterceptor} which sends latency stats about incoming grpc calls to Prometheus.
 */
@Slf4j(topic = "metrics")
@Component
public class PrometheusInterceptor implements ServerInterceptor {

  @Override
  public <R, S> ServerCall.Listener<R> interceptCall(
      ServerCall<R, S> call, Metadata requestMetadata, ServerCallHandler<R, S> next) {
    return next.startCall(new MonitoringServerCall<>(call), requestMetadata);
  }

  static class MonitoringServerCall<R, S> extends ForwardingServerCall
      .SimpleForwardingServerCall<R, S> {

    private final Histogram.Timer requestTimer;

    MonitoringServerCall(ServerCall<R, S> delegate) {
      super(delegate);
      this.requestTimer = Metrics.histogramStartTimer(
          MetricKeys.Histogram.GRPC_SERVICE_LATENCY, getMethodDescriptor().getFullMethodName());
    }

    @Override
    public void close(Status status, Metadata responseHeaders) {
      Metrics.histogramObserve(requestTimer);
      super.close(status, responseHeaders);
    }
  }
}
