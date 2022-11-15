package org.tron.core.net.service.statistics;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;
import org.tron.core.net.TronNetService;
import org.tron.p2p.stats.P2pStats;

@Slf4j(topic = "net")
@Component
public class TronStatsManager {
  private volatile long TCP_TRAFFIC_IN = 0;
  private volatile long TCP_TRAFFIC_OUT = 0;
  private volatile long UDP_TRAFFIC_IN = 0;
  private volatile long UDP_TRAFFIC_OUT = 0;

  private static Cache<InetAddress, NodeStatistics> cache = CacheBuilder.newBuilder()
          .maximumSize(3000).recordStats().build();

  private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  public static NodeStatistics getNodeStatistics(InetAddress inetAddress) {
    NodeStatistics nodeStatistics = cache.getIfPresent(inetAddress);
    if (nodeStatistics == null) {
      nodeStatistics = new NodeStatistics();
      cache.put(inetAddress, nodeStatistics);
    }
    return nodeStatistics;
  }

  public void init() {
    executor.scheduleWithFixedDelay(() -> {
      try {
        work();
      } catch (Throwable t) {
        logger.error("Exception in traffic stats worker, {}", t.getMessage());
      }
    }, 1, 1, TimeUnit.SECONDS);
  }

  public void close() {
    try {
      executor.shutdownNow();
    } catch (Exception e) {
      logger.error("Exception in shutdown traffic stats worker, {}", e.getMessage());
    }
  }

  private void work() {
    P2pStats stats = TronNetService.getP2pService().getP2pStats();

    MetricsUtil.meterMark(MetricsKey.NET_TCP_IN_TRAFFIC,
            stats.getTcpInSize() - TCP_TRAFFIC_IN);
    Metrics.histogramObserve(MetricKeys.Histogram.TCP_BYTES,
            stats.getTcpInSize() - TCP_TRAFFIC_IN,
            MetricLabels.Histogram.TRAFFIC_IN);
    MetricsUtil.meterMark(MetricsKey.NET_TCP_OUT_TRAFFIC,
            stats.getTcpOutSize() - TCP_TRAFFIC_OUT);
    Metrics.histogramObserve(MetricKeys.Histogram.UDP_BYTES,
            stats.getUdpInSize() - UDP_TRAFFIC_IN,
            MetricLabels.Histogram.TRAFFIC_IN);
    MetricsUtil.meterMark(MetricsKey.NET_UDP_OUT_TRAFFIC,
            stats.getUdpOutSize() - UDP_TRAFFIC_OUT);
    Metrics.histogramObserve(MetricKeys.Histogram.UDP_BYTES,
            stats.getUdpOutSize() - UDP_TRAFFIC_OUT,
            MetricLabels.Histogram.TRAFFIC_OUT);

    TCP_TRAFFIC_IN = stats.getTcpInSize();
    TCP_TRAFFIC_OUT = stats.getTcpOutSize();
    UDP_TRAFFIC_IN = stats.getUdpInSize();
    UDP_TRAFFIC_OUT = stats.getUdpOutSize();
  }
}
