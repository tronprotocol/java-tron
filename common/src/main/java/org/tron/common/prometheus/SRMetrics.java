package org.tron.common.prometheus;

import com.google.protobuf.ByteString;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.tron.common.utils.StringUtil;

public class SRMetrics {

  private SRMetrics() {
    throw new IllegalStateException("SRMetrics");
  }

  private static volatile Set<String> localWitnessAddresses = Collections.emptySet();

  /**
   * Caches the local witness addresses (58check-encoded) once they are resolved at
   * startup, so maintenance-time recording can read them without reaching back into node
   * configuration. Written once by BackupManager's {@code @PostConstruct} hook: an empty
   * collection (no witness configured) is cached as an empty set, a non-empty one as an
   * immutable snapshot covering the witness account address or every derived private-key
   * address.
   */
  public static void setLocalWitnessAddresses(Collection<String> addresses) {
    localWitnessAddresses = addresses.isEmpty()
        ? Collections.emptySet()
        : Collections.unmodifiableSet(new HashSet<>(addresses));
  }

  public static Set<String> getLocalWitnessAddresses() {
    return localWitnessAddresses;
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
