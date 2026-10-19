package org.tron.core.metrics;

import com.google.protobuf.ByteString;
import java.util.List;
import org.tron.common.backup.BackupManager;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;
import org.tron.common.prometheus.SRMetrics;
import org.tron.common.utils.LocalWitnesses;
import org.tron.common.utils.StringUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.WitnessStore;
import org.tron.program.Version;

/**
 * Publishes the status of the super representative (SR) configured locally: whether the node
 * runs as a witness, its role in the backup election ({@code master} when it currently produces
 * blocks, {@code slaver} when it stands by for a master), whether that witness is in the active
 * witness set, its vote count and vote-count rank, plus the local address itself.
 */
public final class SrMetricsService {

  private SrMetricsService() {
    throw new IllegalStateException("SrMetricsService");
  }

  /**
   * Records the full node status. Called once at startup; afterwards each part refreshes
   * at its own point of change: the role on backup-status switches
   * ({@link #onRoleChanged}), the witness gauges at maintenance blocks
   * ({@link #recordWitnessStatus}).
   */
  public static void recordNodeStatus(ChainBaseManager chainBaseManager,
      BackupManager backupManager) {
    if (!Metrics.enabled()) {
      return;
    }
    recordNodeInfoLabels(backupManager.getStatus());
    recordWitnessStatus(chainBaseManager);
  }

  /**
   * Records the witness gauges (active-set membership, settled votes, settled rank).
   * Called at maintenance blocks, where the active witness set and the settled vote
   * counts are updated. Reads no backup state: the role only changes on backup-status
   * switches and is refreshed by {@link #onRoleChanged}.
   */
  public static void recordWitnessStatus(ChainBaseManager chainBaseManager) {
    if (!Metrics.enabled()) {
      return;
    }
    byte[] witnessAddress = localWitnessAddress();
    boolean active = false;
    long votes = 0;
    long rank = 0;
    if (witnessAddress != null && witnessAddress.length > 0) {
      ByteString witnessAddressBs = ByteString.copyFrom(witnessAddress);
      active = chainBaseManager.getWitnessScheduleStore().getActiveWitnesses()
          .contains(witnessAddressBs);
      WitnessCapsule witness = chainBaseManager.getWitnessStore().get(witnessAddress);
      votes = witness == null ? 0 : witness.getVoteCount();
      rank = witnessRank(chainBaseManager.getWitnessStore(), witnessAddressBs);
    }
    SRMetrics.recordSrStatus(active, votes, rank);
  }

  /**
   * Publishes the {@code node_info} labels immediately after a backup-election state
   * change. Reads no store: votes, rank and active-set membership only change at
   * maintenance blocks and are refreshed there.
   */
  public static void onRoleChanged(BackupManager.BackupStatusEnum status) {
    if (!Metrics.enabled()) {
      return;
    }
    recordNodeInfoLabels(status);
  }

  private static void recordNodeInfoLabels(BackupManager.BackupStatusEnum status) {
    byte[] witnessAddress = localWitnessAddress();
    String witnessAddress58 = "";
    String role = MetricLabels.Info.ROLE_FULLNODE;
    if (witnessAddress != null && witnessAddress.length > 0) {
      witnessAddress58 = StringUtil.encode58Check(witnessAddress);
      role = status == BackupManager.BackupStatusEnum.MASTER
          ? MetricLabels.Info.ROLE_MASTER : MetricLabels.Info.ROLE_SLAVER;
    }
    SRMetrics.recordNodeInfo(Version.getVersion(), Args.getInstance().getChainId(),
        witnessAddress58, role);
  }

  private static byte[] localWitnessAddress() {
    LocalWitnesses localWitnesses = Args.getLocalWitnesses();
    return localWitnesses == null ? null : localWitnesses.getWitnessAccountAddress();
  }

  private static long witnessRank(WitnessStore witnessStore, ByteString witnessAddress) {
    List<WitnessCapsule> witnesses = witnessStore.getAllWitnesses();
    WitnessStore.sortWitnesses(witnesses, false);
    for (int i = 0; i < witnesses.size(); i++) {
      if (witnesses.get(i).getAddress().equals(witnessAddress)) {
        return i + 1L;
      }
    }
    return 0L;
  }
}
