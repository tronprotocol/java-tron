package org.tron.common.prometheus;

import com.google.protobuf.ByteString;
import java.util.List;
import org.tron.common.utils.StringUtil;

public class SRMetrics {

  private SRMetrics() {
    throw new IllegalStateException("SRMetrics");
  }

  // node_type: 0 = fullnode, 1 = witness
  public static final int NODE_TYPE_FULLNODE = 0;
  public static final int NODE_TYPE_WITNESS = 1;

  // node_role: 0 = init (pre-election / re-election), 1 = master, 2 = slaver
  public static final int NODE_ROLE_INIT = 0;
  public static final int NODE_ROLE_MASTER = 1;
  public static final int NODE_ROLE_SLAVER = 2;

  /** Publishes node_type as one label-less series; 0 = fullnode, 1 = witness. */
  public static void recordNodeType(int nodeType) {
    if (!Metrics.enabled()) {
      return;
    }
    Metrics.gaugeSet(MetricKeys.Gauge.NODE_TYPE, nodeType);
  }

  /** Publishes node_role as one label-less series; role changes rewrite it in place. */
  public static void recordNodeRole(int role) {
    if (!Metrics.enabled()) {
      return;
    }
    Metrics.gaugeSet(MetricKeys.Gauge.NODE_ROLE, role);
  }

  /** Publishes one tron:witness_address series (value 1) for a local witness address. */
  public static void recordWitnessAddress(String address) {
    if (!Metrics.enabled()) {
      return;
    }
    Metrics.gaugeSet(MetricKeys.Gauge.WITNESS_ADDRESS, 1, address);
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
