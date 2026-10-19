package org.tron.core.metrics;

import com.google.protobuf.ByteString;
import java.util.List;
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
 * runs as a witness, whether that witness is in the active set (primary) or only registered as a
 * candidate (backup), its vote count and vote-count rank, plus the local address itself.
 */
public final class SrMetricsService {

  private SrMetricsService() {
    throw new IllegalStateException("SrMetricsService");
  }

  /**
   * Recomputes and publishes the local SR status. Called once at startup and at maintenance
   * blocks, where the active witness set and the witness vote counts are updated.
   */
  public static void refresh(ChainBaseManager chainBaseManager) {
    if (!Metrics.enabled()) {
      return;
    }
    LocalWitnesses localWitnesses = Args.getLocalWitnesses();
    byte[] address = localWitnesses == null ? null : localWitnesses.getWitnessAccountAddress();
    String address58 = "";
    String role = MetricLabels.Info.ROLE_FULLNODE;
    boolean active = false;
    long votes = 0;
    long rank = 0;
    if (address != null && address.length > 0) {
      address58 = StringUtil.encode58Check(address);
      ByteString witnessAddress = ByteString.copyFrom(address);
      active = chainBaseManager.getWitnessScheduleStore().getActiveWitnesses()
          .contains(witnessAddress);
      WitnessCapsule witness = chainBaseManager.getWitnessStore().get(address);
      votes = witness == null ? 0 : witness.getVoteCount();
      rank = rankOf(chainBaseManager.getWitnessStore(), witnessAddress);
      role = active ? MetricLabels.Info.ROLE_PRIMARY : MetricLabels.Info.ROLE_BACKUP;
    }
    SRMetrics.recordNodeInfo(Version.getVersion(), Args.getInstance().getChainId(),
        address58, role);
    SRMetrics.recordSrStatus(active, votes, rank);
  }

  private static long rankOf(WitnessStore witnessStore, ByteString address) {
    List<WitnessCapsule> witnesses = witnessStore.getAllWitnesses();
    WitnessStore.sortWitnesses(witnesses, false);
    for (int i = 0; i < witnesses.size(); i++) {
      if (witnesses.get(i).getAddress().equals(address)) {
        return i + 1L;
      }
    }
    return 0L;
  }
}
