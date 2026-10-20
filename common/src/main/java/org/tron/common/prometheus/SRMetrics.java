package org.tron.common.prometheus;

import com.google.protobuf.ByteString;
import java.util.List;
import org.tron.common.utils.StringUtil;

public class SRMetrics {

  private SRMetrics() {
    throw new IllegalStateException("SRMetrics");
  }

  private static volatile byte[] localWitnessAddress;

  /**
   * Caches the local witness address once it is resolved at startup, so maintenance-time
   * recording can read it without reaching back into node configuration.
   */
  public static void setLocalWitnessAddress(byte[] address) {
    localWitnessAddress = address;
  }

  public static byte[] getLocalWitnessAddress() {
    return localWitnessAddress;
  }

  /**
   * Publishes the node-role gauge as three one-hot series (init, master, slaver) for the
   * given witness address, with the current role set to 1, so a role change rewrites the
   * same series instead of leaving stale ones behind. A no-op when no witness address is
   * given: nodes without a local witness expose no role series.
   */
  public static void recordNodeRole(String address, String role) {
    if (!Metrics.enabled() || address.isEmpty()) {
      return;
    }
    String[] roles = {MetricLabels.Gauge.ROLE_INIT, MetricLabels.Gauge.ROLE_MASTER,
        MetricLabels.Gauge.ROLE_SLAVER};
    for (String r : roles) {
      Metrics.gaugeSet(MetricKeys.Gauge.NODE_ROLE, r.equals(role) ? 1 : 0, address, r);
    }
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
