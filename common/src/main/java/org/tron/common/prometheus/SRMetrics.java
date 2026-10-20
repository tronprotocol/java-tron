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

  // numeric encoding of the backup-election role in the tron:node_role gauge:
  // 0 = init (before the first election round), 1 = master (producing blocks),
  // 2 = slaver (standing by). Kept package-visible for tests; the mapping is
  // documented for operators in docs/metrics-changelog.md
  static final int NODE_ROLE_INIT = 0;
  static final int NODE_ROLE_MASTER = 1;
  static final int NODE_ROLE_SLAVER = 2;

  /**
   * Publishes the node-role gauge as a single series per witness address, encoding the
   * current role as a number (0 = init, 1 = master, 2 = slaver). A role change rewrites
   * the value of the same series, so no stale series are left behind. A no-op when no
   * witness address is given: nodes without a local witness expose no role series.
   */
  public static void recordNodeRole(String address, String role) {
    if (!Metrics.enabled() || address.isEmpty()) {
      return;
    }
    Metrics.gaugeSet(MetricKeys.Gauge.NODE_ROLE, roleValue(role), address);
  }

  /**
   * Maps a role name to its {@code tron:node_role} value; an unknown role falls back to
   * init so a bad argument can never fake an elected state.
   */
  private static int roleValue(String role) {
    if (MetricLabels.Gauge.ROLE_MASTER.equals(role)) {
      return NODE_ROLE_MASTER;
    }
    if (MetricLabels.Gauge.ROLE_SLAVER.equals(role)) {
      return NODE_ROLE_SLAVER;
    }
    return NODE_ROLE_INIT;
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
