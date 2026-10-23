package org.tron.common.prometheus;

import com.google.protobuf.ByteString;
import io.prometheus.client.CollectorRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.StringUtil;
import org.tron.consensus.base.Param;
import org.tron.consensus.dpos.MaintenanceManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
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
    Args.getInstance().setBackupPort(0);
    Args.getInstance().setBackupMembers(Collections.emptyList());
    Args.getInstance().witness = false;
    Param.getInstance().setMiners(new ArrayList<>());
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

  // ---- node_role: the four states, the pre-election window and status transitions ----

  /**
   * Started without witness mode: a plain fullnode reports role 0.
   */
  @Test
  public void testNodeRoleFullNode() {
    Args.getInstance().witness = false;
    backupManager.recordNodeRole();
    Assert.assertEquals(SRMetrics.NODE_ROLE_FULLNODE, roleGauge(), 0.0);
  }

  /**
   * Witness configured without a backup election (backup members unset): the node always
   * holds the primary key itself, so the single role series reports master (2).
   */
  @Test
  public void testNodeRoleMaster() {
    Args.getInstance().witness = true;
    backupManager.recordNodeRole();
    Assert.assertEquals(SRMetrics.NODE_ROLE_MASTER, roleGauge(), 0.0);
  }

  /**
   * Backup election slaver: the node stands by for a master and reports slaver (3).
   */
  @Test
  public void testNodeRoleSlaver() throws Exception {
    Args.getInstance().witness = true;
    configureBackup();
    markBackupInitiated(backupManager);
    backupManager.setStatus(BackupManager.BackupStatusEnum.SLAVER);
    Assert.assertEquals(SRMetrics.NODE_ROLE_SLAVER, roleGauge(), 0.0);
  }

  /**
   * Before the first election round (backup configured, BackupManager.init() not yet run,
   * which in production only happens after the first fresh block) the node must report
   * init (1) instead of falling back to the MASTER field default; the first real status
   * after init() rewrites the same single series to the elected role.
   */
  @Test
  public void testNodeRoleInitBeforeBackupElectionRuns() throws Exception {
    Args.getInstance().witness = true;
    configureBackup();

    // a directly constructed manager has not gone through init(): pre-election window
    BackupManager fresh = new BackupManager();
    try {
      fresh.recordNodeRole();
      Assert.assertEquals(SRMetrics.NODE_ROLE_INIT, roleGauge(), 0.0);

      // a status change while init() has not run stays inside the window
      fresh.setStatus(BackupManager.BackupStatusEnum.SLAVER);
      Assert.assertEquals(SRMetrics.NODE_ROLE_INIT, roleGauge(), 0.0);

      // once the election has run, the status maps to the elected role
      markBackupInitiated(fresh);
      fresh.setStatus(BackupManager.BackupStatusEnum.SLAVER);
      Assert.assertEquals(SRMetrics.NODE_ROLE_SLAVER, roleGauge(), 0.0);
    } finally {
      fresh.stop();
    }
  }

  /**
   * After init() has run, status INIT is a real election state — the transport is up but no
   * keep-alive timeout has been judged yet, or the master lost keep-alive and a re-election
   * is running — and maps 1:1 to init (1). It must not be misreported as slaver (3).
   */
  @Test
  public void testNodeRoleInitAfterElectionStarted() throws Exception {
    Args.getInstance().witness = true;
    configureBackup();
    markBackupInitiated(backupManager);
    backupManager.setStatus(BackupManager.BackupStatusEnum.INIT);
    Assert.assertEquals(SRMetrics.NODE_ROLE_INIT, roleGauge(), 0.0);
  }

  /**
   * setStatus() itself republishes the single label-less node_role series, so a
   * backup-election transition is reflected immediately.
   */
  @Test
  public void testNodeRoleRefreshOnBackupStatusChange() throws Exception {
    Args.getInstance().witness = true;
    configureBackup();
    markBackupInitiated(backupManager);

    backupManager.setStatus(BackupManager.BackupStatusEnum.SLAVER);
    Assert.assertEquals(SRMetrics.NODE_ROLE_SLAVER, roleGauge(), 0.0);

    backupManager.setStatus(BackupManager.BackupStatusEnum.MASTER);
    Assert.assertEquals(SRMetrics.NODE_ROLE_MASTER, roleGauge(), 0.0);
  }

  // ---- tron:witness_address: one series per consensus miner, value 1 ----

  /**
   * A single consensus miner publishes exactly one tron:witness_address series with value 1.
   */
  @Test
  public void testWitnessAddressValueOne() {
    ByteString address = uniqueAddress();
    recordWitnessMetrics(
        Collections.singletonList(miner(address, address)));
    Assert.assertEquals(1.0, witnessGauge(encode(address)), 0.0);
  }

  /**
   * Multi-key setup: every consensus miner address gets its own series with value 1.
   */
  @Test
  public void testWitnessAddressMultipleMiners() {
    ByteString address1 = uniqueAddress();
    ByteString address2 = uniqueAddress();
    List<Param.Miner> miners = new ArrayList<>();
    miners.add(miner(address1, address1));
    miners.add(miner(address2, address2));

    recordWitnessMetrics(miners);

    Assert.assertEquals(1.0, witnessGauge(encode(address1)), 0.0);
    Assert.assertEquals(1.0, witnessGauge(encode(address2)), 0.0);
  }

  /**
   * Single-key multisig: the witness account address may differ from the address derived
   * from the signing key. Only the witness account address is published; the signing-key
   * address must not appear.
   */
  @Test
  public void testWitnessAddressUsesWitnessAccountNotSigningKey() {
    ByteString signingKeyAddress = addressOf(PublicMethod.getRandomPrivateKey());
    ByteString witnessAccountAddress = uniqueAddress();

    recordWitnessMetrics(
        Collections.singletonList(miner(signingKeyAddress, witnessAccountAddress)));

    Assert.assertEquals(1.0, witnessGauge(encode(witnessAccountAddress)), 0.0);
    Assert.assertNull(witnessGauge(encode(signingKeyAddress)));
  }

  /**
   * No miner configured: a plain fullnode publishes no tron:witness_address series at all,
   * not even one with an empty address label.
   */
  @Test
  public void testNoWitnessAddressSeriesForEmptyMiners() {
    recordWitnessMetrics(new ArrayList<>());
    Assert.assertNull(witnessGauge(""));
  }

  /**
   * Metrics disabled: no tron:witness_address series is published.
   */
  @Test
  public void testWitnessAddressNotPublishedWhenMetricsDisabled() {
    ByteString address = uniqueAddress();
    Args.getInstance().setMetricsPrometheusEnable(false);
    try {
      recordWitnessMetrics(
          Collections.singletonList(miner(address, address)));
      Assert.assertNull(witnessGauge(encode(address)));
    } finally {
      Args.getInstance().setMetricsPrometheusEnable(true);
    }
  }

  /**
   * Backup must be seen as configured the same way BackupServer.initServer() decides to
   * start the election transport, otherwise the pre-election window is unreachable.
   */
  private void configureBackup() {
    Args.getInstance().setBackupPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setBackupMembers(Collections.singletonList("127.0.0.2"));
  }

  /**
   * Simulates that BackupManager.init() has run: in production the backup server only
   * starts after the first fresh block, so tests drive the isInit flag directly instead
   * of spinning up the election transport.
   */
  private void markBackupInitiated(BackupManager manager) throws Exception {
    java.lang.reflect.Field isInit = BackupManager.class.getDeclaredField("isInit");
    isInit.setAccessible(true);
    isInit.setBoolean(manager, true);
  }

  /**
   * Drives the startup publication the way production does: the miners are assembled first and
   * the maintenance manager publishes once from init().
   */
  private void recordWitnessMetrics(List<Param.Miner> miners) {
    Param.getInstance().setMiners(miners);
    maintenanceManager.recordWitnessMetrics();
  }

  /**
   * Builds the consensus-miner identity the node assembles: the signing-key address and the
   * witness account address are passed explicitly, exactly as ConsensusService does.
   */
  private Param.Miner miner(ByteString privateKeyAddress, ByteString witnessAddress) {
    return Param.getInstance().new Miner(new byte[0], privateKeyAddress, witnessAddress);
  }

  private ByteString addressOf(String privateKeyHex) {
    return ByteString.copyFrom(
        SignUtils.fromPrivate(ByteArray.fromHexString(privateKeyHex)).getAddress());
  }

  private String encode(ByteString address) {
    return StringUtil.encode58Check(address.toByteArray());
  }

  private Double witnessGauge(String address) {
    return CollectorRegistry.defaultRegistry.getSampleValue(
        MetricKeys.Gauge.WITNESS_ADDRESS,
        new String[] {MetricLabels.Gauge.ADDRESS},
        new String[] {address});
  }

  private Double roleGauge() {
    return CollectorRegistry.defaultRegistry.getSampleValue(
        MetricKeys.Gauge.NODE_ROLE,
        new String[] {},
        new String[] {});
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
