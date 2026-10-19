package org.tron.common.prometheus;

import com.google.protobuf.ByteString;
import java.util.List;
import org.tron.common.utils.StringUtil;

public class SRMetrics {

  private SRMetrics() {
    throw new IllegalStateException("SRMetrics");
  }

  public static void recordNodeInfo(String version, String chainId, String address,
      String role) {
    if (!Metrics.enabled()) {
      return;
    }
    Metrics.info(MetricKeys.Info.NODE_INFO, version, chainId, address, role);
  }

  public static void recordSrStatus(boolean active, long votes, long rank) {
    if (!Metrics.enabled()) {
      return;
    }
    Metrics.gaugeSet(MetricKeys.Gauge.SR_IS_ACTIVE, active ? 1 : 0);
    Metrics.gaugeSet(MetricKeys.Gauge.SR_VOTES, votes);
    Metrics.gaugeSet(MetricKeys.Gauge.SR_RANK, rank);
  }

  public static void recordSrSetChange(List<ByteString> currentWits, List<ByteString> newWits) {
    if (!Metrics.enabled()) {
      return;
    }
    newWits.stream()
        .filter(w -> !currentWits.contains(w))
        .forEach(w -> Metrics.counterInc(MetricKeys.Counter.SR_SET_CHANGE, 1,
            MetricLabels.Counter.SR_ADD, StringUtil.encode58Check(w.toByteArray())));
    currentWits.stream()
        .filter(w -> !newWits.contains(w))
        .forEach(w -> Metrics.counterInc(MetricKeys.Counter.SR_SET_CHANGE, 1,
            MetricLabels.Counter.SR_REMOVE, StringUtil.encode58Check(w.toByteArray())));
  }
}
