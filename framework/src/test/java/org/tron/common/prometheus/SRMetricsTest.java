package org.tron.common.prometheus;

import com.google.protobuf.ByteString;
import io.prometheus.client.CollectorRegistry;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.backup.BackupManager;
import org.tron.common.utils.LocalWitnesses;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.StringUtil;
import org.tron.consensus.dpos.MaintenanceManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.metrics.SrMetricsService;
import org.tron.program.Version;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Vote;

@Slf4j(topic = "metric")
public class SRMetricsTest extends BaseTest {

  private static final AtomicInteger PORT = new AtomicInteger(0);
  private static final AtomicInteger UNIQUE = new AtomicInteger(0);

  @Resource
  private MaintenanceManager maintenanceManager;
  @Resource
  private ConsensusService consensusService;
  @Resource
  private BackupManager backupManager;

  static {
    Args.setParam(new String[]{"-d", dbPath()}, TestConstants.TEST_CONF);
    Args.getInstance().setNodeListenPort(20000 + PORT.incrementAndGet());
    Args.getInstance().setMetricsPrometheusEnable(true);
    Args.getInstance().setMetricsPrometheusPort(PublicMethod.chooseRandomPort());
    Metrics.init();
  }

  @Before
  public void setUp() {
    Args.getInstance().setMetricsPrometheusEnable(true);
    consensusService.start();
  }

  @After
  public void tearDown() {
    Args.getInstance().setMetricsPrometheusEnable(true);
    Args.setLocalWitnesses(new LocalWitnesses());
    backupManager.setStatus(BackupManager.BackupStatusEnum.MASTER);
  }

  /**
   * Drive the full maintenance flow: starting with a single active witness while WitnessStore
   * contains additional ones, doMaintenance() should expand active witnesses to the full set and
   * emit SR_ADD for each newly active witness.
   */
  @Test
  public void testSrAddViaMaintenance() {
    ByteString stableWit = registerWitness();
    ByteString newWit1 = registerWitness();
    ByteString newWit2 = registerWitness();

    chainBaseManager.getWitnessScheduleStore()
        .saveActiveWitnesses(Collections.singletonList(stableWit));

    seedVote(stableWit);

    maintenanceManager.doMaintenance();

    Assert.assertEquals(1, sample(MetricLabels.Counter.SR_ADD, newWit1).intValue());
    Assert.assertEquals(1, sample(MetricLabels.Counter.SR_ADD, newWit2).intValue());
    Assert.assertNull(sample(MetricLabels.Counter.SR_ADD, stableWit));
    Assert.assertNull(sample(MetricLabels.Counter.SR_REMOVE, stableWit));
  }

  /**
   * Active witness set already matches WitnessStore → no metric emitted.
   */
  @Test
  public void testNoMetricWhenSetUnchanged() {
    ByteString witA = registerWitness();
    ByteString witB = registerWitness();

    chainBaseManager.getWitnessScheduleStore()
        .saveActiveWitnesses(Arrays.asList(witA, witB));

    seedVote(witA);

    maintenanceManager.doMaintenance();

    Assert.assertNull(sample(MetricLabels.Counter.SR_ADD, witA));
    Assert.assertNull(sample(MetricLabels.Counter.SR_ADD, witB));
    Assert.assertNull(sample(MetricLabels.Counter.SR_REMOVE, witA));
    Assert.assertNull(sample(MetricLabels.Counter.SR_REMOVE, witB));
  }

  /**
   * Empty VotesStore → countVote() is empty → SR change check is skipped, even when the active
   * set differs from the full witness store.
   */
  @Test
  public void testNoMetricWhenNoVotes() {
    ByteString stableWit = registerWitness();
    ByteString newWit = registerWitness();

    chainBaseManager.getWitnessScheduleStore()
        .saveActiveWitnesses(Collections.singletonList(stableWit));

    maintenanceManager.doMaintenance();

    Assert.assertNull(sample(MetricLabels.Counter.SR_ADD, newWit));
  }

  /**
   * Metrics disabled → record() short-circuits even though the active set changes.
   */
  @Test
  public void testNoMetricWhenMetricsDisabled() {
    Args.getInstance().setMetricsPrometheusEnable(false);
    try {
      ByteString stableWit = registerWitness();
      ByteString newWit = registerWitness();

      chainBaseManager.getWitnessScheduleStore()
          .saveActiveWitnesses(Collections.singletonList(stableWit));

      seedVote(stableWit);

      maintenanceManager.doMaintenance();

      Assert.assertNull(sample(MetricLabels.Counter.SR_ADD, newWit));
    } finally {
      Args.getInstance().setMetricsPrometheusEnable(true);
    }
  }

  /**
   * SR_REMOVE is verified by directly calling record() instead of going through doMaintenance(),
   * because driving a removal through the real flow is impractical here:
   *
   * <p>Inside doMaintenance(), the block before SRMetrics.recordSrSetChange() iterates currentWits
   * and calls setIsJobs(false) on each WitnessCapsule fetched from WitnessStore. If currentWits
   * contains any address that is not present in WitnessStore, getWitness() returns null and the
   * code NPEs — so SR_REMOVE cannot be triggered by simply pointing the active set at an
   * "obsolete" address.
   *
   * <p>The only other path to SR_REMOVE is rank-based eviction: with more than
   * MAX_ACTIVE_WITNESS_NUM (27) witnesses, sorting drops the lowest-ranked one. Building that
   * setup just to exercise this branch is heavy and adds little value, since SR_ADD and
   * SR_REMOVE share the exact same emit logic in record() — verifying SR_ADD via doMaintenance
   * already proves the wiring is correct, and this direct call covers the symmetric branch.
   */
  @Test
  public void testSrRemoveDirect() {
    ByteString stableWit = uniqueAddress();
    ByteString removedWit = uniqueAddress();

    SRMetrics.recordSrSetChange(
        Arrays.asList(stableWit, removedWit),
        Collections.singletonList(stableWit));

    Assert.assertEquals(1, sample(MetricLabels.Counter.SR_REMOVE, removedWit).intValue());
    Assert.assertNull(sample(MetricLabels.Counter.SR_ADD, removedWit));
    Assert.assertNull(sample(MetricLabels.Counter.SR_REMOVE, stableWit));
  }

  /**
   * No witness configured: role is fullnode, gauges are zero, address label is empty.
   */
  @Test
  public void testSrStatusFullNode() {
    Args.setLocalWitnesses(new LocalWitnesses());

    SrMetricsService.refresh(chainBaseManager, backupManager);

    Assert.assertEquals(0.0, gauge(MetricKeys.Gauge.SR_IS_ACTIVE), 0.0);
    Assert.assertEquals(0.0, gauge(MetricKeys.Gauge.SR_VOTES), 0.0);
    Assert.assertEquals(0.0, gauge(MetricKeys.Gauge.SR_RANK), 0.0);
    Assert.assertEquals(1.0, info("", MetricLabels.Info.ROLE_FULLNODE), 0.0);
  }

  /**
   * Witness in the active set: role is master, gauges expose its votes and 1-based rank, and
   * the previously published fullnode series is dropped when the label values change.
   */
  @Test
  public void testSrStatusMaster() {
    backupManager.setStatus(BackupManager.BackupStatusEnum.MASTER);
    Args.setLocalWitnesses(new LocalWitnesses());
    SrMetricsService.refresh(chainBaseManager, backupManager);
    Assert.assertEquals(1.0, info("", MetricLabels.Info.ROLE_FULLNODE), 0.0);

    ByteString witness = registerWitnessWithVotes(1000);
    registerWitnessWithVotes(50);
    chainBaseManager.getWitnessScheduleStore()
        .saveActiveWitnesses(Collections.singletonList(witness));

    LocalWitnesses localWitnesses = new LocalWitnesses();
    localWitnesses.initWitnessAccountAddress(witness.toByteArray());
    Args.setLocalWitnesses(localWitnesses);

    SrMetricsService.refresh(chainBaseManager, backupManager);

    String address = StringUtil.encode58Check(witness.toByteArray());
    Assert.assertEquals(1.0, gauge(MetricKeys.Gauge.SR_IS_ACTIVE), 0.0);
    Assert.assertEquals(1000.0, gauge(MetricKeys.Gauge.SR_VOTES), 0.0);
    Assert.assertEquals(1.0, gauge(MetricKeys.Gauge.SR_RANK), 0.0);
    Assert.assertEquals(1.0, info(address, MetricLabels.Info.ROLE_MASTER), 0.0);
    Assert.assertNull(info("", MetricLabels.Info.ROLE_FULLNODE));
  }

  /**
   * Backup election slaver: role is slaver and is_active reflects the active witness set.
   */
  @Test
  public void testSrStatusSlaver() {
    backupManager.setStatus(BackupManager.BackupStatusEnum.SLAVER);
    ByteString activeWitness = registerWitnessWithVotes(10);
    ByteString witness = registerWitnessWithVotes(7);
    chainBaseManager.getWitnessScheduleStore()
        .saveActiveWitnesses(Collections.singletonList(activeWitness));

    LocalWitnesses localWitnesses = new LocalWitnesses();
    localWitnesses.initWitnessAccountAddress(witness.toByteArray());
    Args.setLocalWitnesses(localWitnesses);

    SrMetricsService.refresh(chainBaseManager, backupManager);

    String address = StringUtil.encode58Check(witness.toByteArray());
    Assert.assertEquals(0.0, gauge(MetricKeys.Gauge.SR_IS_ACTIVE), 0.0);
    Assert.assertEquals(7.0, gauge(MetricKeys.Gauge.SR_VOTES), 0.0);
    Assert.assertTrue(gauge(MetricKeys.Gauge.SR_RANK) > 0);
    Assert.assertEquals(1.0, info(address, MetricLabels.Info.ROLE_SLAVER), 0.0);
  }

  private ByteString registerWitnessWithVotes(long votes) {
    ByteString address = uniqueAddress();
    chainBaseManager.getWitnessStore().put(address.toByteArray(),
        new WitnessCapsule(address, votes, ""));
    chainBaseManager.getAccountStore().put(address.toByteArray(),
        new AccountCapsule(Protocol.Account.newBuilder().setAddress(address).build()));
    return address;
  }

  private Double gauge(String name) {
    return CollectorRegistry.defaultRegistry.getSampleValue(name);
  }

  private Double info(String address, String role) {
    return CollectorRegistry.defaultRegistry.getSampleValue(
        "tron:node_info",
        new String[] {MetricLabels.Info.VERSION, MetricLabels.Info.CHAIN_ID,
            MetricLabels.Info.ADDRESS, MetricLabels.Info.ROLE},
        new String[] {Version.getVersion(), Args.getInstance().getChainId(), address, role});
  }

  private ByteString registerWitness() {
    ByteString address = uniqueAddress();
    chainBaseManager.getWitnessStore().put(address.toByteArray(), new WitnessCapsule(address));
    chainBaseManager.addWitness(address);
    chainBaseManager.getAccountStore().put(address.toByteArray(),
        new AccountCapsule(Protocol.Account.newBuilder().setAddress(address).build()));
    return address;
  }

  private void seedVote(ByteString voteFor) {
    ByteString voter = uniqueAddress();
    VotesCapsule votes = new VotesCapsule(voter, Collections.emptyList(),
        Collections.singletonList(Vote.newBuilder()
            .setVoteAddress(voteFor)
            .setVoteCount(1L)
            .build()));
    chainBaseManager.getVotesStore().put(voter.toByteArray(), votes);
  }

  private ByteString uniqueAddress() {
    int n = UNIQUE.incrementAndGet();
    byte[] bytes = new byte[21];
    bytes[0] = 0x41;
    bytes[17] = (byte) ((n >> 16) & 0xFF);
    bytes[18] = (byte) ((n >> 8) & 0xFF);
    bytes[19] = (byte) (n & 0xFF);
    bytes[20] = 0x01;
    return ByteString.copyFrom(bytes);
  }

  private Double sample(String action, ByteString witness) {
    return CollectorRegistry.defaultRegistry.getSampleValue(
        MetricKeys.Counter.SR_SET_CHANGE + "_total",
        new String[]{"action", "witness"},
        new String[]{action, StringUtil.encode58Check(witness.toByteArray())});
  }
}
