package org.tron.common.prometheus;

import com.google.protobuf.ByteString;
import java.util.List;
import org.tron.common.utils.StringUtil;

public class SRMetrics {

  private SRMetrics() {
    throw new IllegalStateException("SRMetrics");
  }

  // numeric encoding of the tron:node_role gauge value: 0 = init (before the first
  // election round), 1 = master (producing blocks), 2 = slaver (standing by). Read by
  // BackupManager when it reports the election state; documented for operators in
  // docs/metrics-changelog.md
  public static final int NODE_ROLE_INIT = 0;
  public static final int NODE_ROLE_MASTER = 1;
  public static final int NODE_ROLE_SLAVER = 2;

  /**
   * Publishes the node-role gauge as a single label-less series, encoding the current
   * role as a number (0 = init, 1 = master, 2 = slaver). A role change rewrites the
   * value of the same series, so no stale series are left behind.
   */
  public static void recordNodeRole(int role) {
    if (!Metrics.enabled()) {
      return;
    }
    Metrics.gaugeSet(MetricKeys.Gauge.NODE_ROLE, role);
  }

  /**
   * Publishes the sr_* gauges as one series per local witness address. An empty address
   * records the node-level zero series a fullnode exposes when no witness is configured.
   */
  public static void recordSrStatus(String address, boolean active, long votes, long rank) {
    if (!Metrics.enabled()) {
      return;
    }
    Metrics.gaugeSet(MetricKeys.Gauge.SR_IS_ACTIVE, active ? 1 : 0, address);
    Metrics.gaugeSet(MetricKeys.Gauge.SR_VOTES, votes, address);
    Metrics.gaugeSet(MetricKeys.Gauge.SR_RANK, rank, address);
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
