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

  /**
   * No witness configured: with an empty miner list a plain fullnode publishes none of
   * the sr_* series, and node_role reports 0 because the node started without witness
   * mode, while node_info keeps its two static labels.
   */
  @Test
  public void testSrStatusFullNode() {
    Args.getInstance().witness = false;
    Param.getInstance().setMiners(new ArrayList<>());

    maintenanceManager.recordWitnessMetrics();
    Assert.assertNull("empty miner list must not write an empty-address sr series",
        srGauge(MetricKeys.Gauge.SR_IS_ACTIVE, ""));

    backupManager.recordNodeRole();
    Assert.assertEquals("a node started without witness mode reports role 0",
        SRMetrics.NODE_ROLE_FULLNODE, roleGauge(), 0.0);

    Metrics.info(MetricKeys.Info.NODE_INFO, Version.getVersion(),
        Args.getInstance().getChainId());
    Assert.assertEquals(1.0, info(), 0.0);
  }

  /**
   * Witness configured without a backup election (backup members unset): the node
   * always holds the primary key itself, so the single role series reports master (2);
   * node_info stays a static two-label identity record.
   */
  @Test
  public void testSrStatusMaster() {
    Args.getInstance().witness = true;
    ByteString witness = registerWitnessWithVotes(1000);
    registerWitnessWithVotes(50);
    chainBaseManager.getWitnessScheduleStore()
        .saveActiveWitnesses(Collections.singletonList(witness));

    seedMiners(witness);

    backupManager.recordNodeRole();
    maintenanceManager.recordWitnessMetrics();

    String address = StringUtil.encode58Check(witness.toByteArray());
    Assert.assertEquals(1.0, srGauge(MetricKeys.Gauge.SR_IS_ACTIVE, address), 0.0);
    Assert.assertEquals(1000.0, srGauge(MetricKeys.Gauge.SR_VOTES, address), 0.0);
    Assert.assertEquals(1.0, srGauge(MetricKeys.Gauge.SR_RANK, address), 0.0);
    Assert.assertEquals(SRMetrics.NODE_ROLE_MASTER, roleGauge(), 0.0);
    Metrics.info(MetricKeys.Info.NODE_INFO, Version.getVersion(),
        Args.getInstance().getChainId());
    Assert.assertEquals(1.0, info(), 0.0);
  }

  /**
   * Backup election slaver: the single role series reports slaver (3) while is_active
   * reflects the active witness set.
   */
  @Test
  public void testSrStatusSlaver() throws Exception {
    Args.getInstance().witness = true;
    configureBackup();
    backupManager.setStatus(BackupManager.BackupStatusEnum.SLAVER);
    ByteString activeWitness = registerWitnessWithVotes(10);
    ByteString witness = registerWitnessWithVotes(7);
    chainBaseManager.getWitnessScheduleStore()
        .saveActiveWitnesses(Collections.singletonList(activeWitness));

    seedMiners(witness);
    markBackupInitiated(backupManager);

    backupManager.recordNodeRole();
    maintenanceManager.recordWitnessMetrics();

    String address = StringUtil.encode58Check(witness.toByteArray());
    Assert.assertEquals(0.0, srGauge(MetricKeys.Gauge.SR_IS_ACTIVE, address), 0.0);
    Assert.assertEquals(7.0, srGauge(MetricKeys.Gauge.SR_VOTES, address), 0.0);
    Assert.assertTrue(srGauge(MetricKeys.Gauge.SR_RANK, address) > 0);
    Assert.assertEquals(SRMetrics.NODE_ROLE_SLAVER, roleGauge(), 0.0);
  }

  /**
   * Before the first election round (backup configured, BackupManager.init() not yet run,
   * which in production only happens after the first fresh block) the node must report
   * init (1) instead of falling back to the MASTER field default; the first real status
   * after init() rewrites the same single series to the elected role.
   */
  @Test
  public void testRoleInitBeforeBackupElectionRuns() throws Exception {
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
   * setStatus() itself republishes the single label-less node_role series without
   * touching any store, so a backup-election transition is reflected immediately instead
   * of at the next maintenance.
   */
  @Test
  public void testRoleRefreshOnBackupStatusChange() throws Exception {
    Args.getInstance().witness = true;
    configureBackup();
    markBackupInitiated(backupManager);

    backupManager.setStatus(BackupManager.BackupStatusEnum.SLAVER);
    Assert.assertEquals(SRMetrics.NODE_ROLE_SLAVER, roleGauge(), 0.0);

    backupManager.setStatus(BackupManager.BackupStatusEnum.MASTER);
    Assert.assertEquals(SRMetrics.NODE_ROLE_MASTER, roleGauge(), 0.0);
  }

  /**
   * After init() has run, status INIT is a real election state — the transport is up
   * but no keep-alive timeout has been judged yet, or the master lost keep-alive and a
   * re-election is running — and maps 1:1 to init (1). It must not be misreported as
   * slaver (3), the pre-fix behavior.
   */
  @Test
  public void testRoleInitAfterElectionStarted() throws Exception {
    Args.getInstance().witness = true;
    configureBackup();
    markBackupInitiated(backupManager);

    backupManager.setStatus(BackupManager.BackupStatusEnum.INIT);
    Assert.assertEquals(SRMetrics.NODE_ROLE_INIT, roleGauge(), 0.0);
  }

  /**
   * Testnet multi-key setup: the consensus layer assembles one miner per configured
   * witness private key, so every sr gauge is written as one series per key address while
   * node_role stays a single series regardless of the key count. Role recording only
   * touches the role series; maintenance-time recording gives each address its own sr
   * values (here distinct votes, activity and rank) and leaves the role series alone. A
   * status change rewrites the single role series without touching the sr series.
   */
  @Test
  public void testRoleMultiWitnessKeys() throws Exception {
    Args.getInstance().witness = true;
    configureBackup();
    String key1 = PublicMethod.getRandomPrivateKey();
    String key2 = PublicMethod.getRandomPrivateKey();
    ByteString miner1 = addressOf(key1);
    ByteString miner2 = addressOf(key2);
    seedMiners(miner1, miner2);
    String address1 = StringUtil.encode58Check(miner1.toByteArray());
    String address2 = StringUtil.encode58Check(miner2.toByteArray());
    ByteString witness1 = registerWitnessWithVotes(miner1.toByteArray(), 100);
    ByteString witness2 = registerWitnessWithVotes(miner2.toByteArray(), 40);
    chainBaseManager.getWitnessScheduleStore()
        .saveActiveWitnesses(Collections.singletonList(witness1));
    markBackupInitiated(backupManager);

    backupManager.recordNodeRole();
    Assert.assertEquals(SRMetrics.NODE_ROLE_MASTER, roleGauge(), 0.0);
    // role recording never writes sr_* series
    Assert.assertNull(srGauge(MetricKeys.Gauge.SR_IS_ACTIVE, address1));
    Assert.assertNull(srGauge(MetricKeys.Gauge.SR_IS_ACTIVE, address2));

    // one sr series per address, with per-address values: witness1 is active with the
    // higher vote count, witness2 stands inactive on the lower rank
    maintenanceManager.recordWitnessMetrics();
    Assert.assertEquals(1.0, srGauge(MetricKeys.Gauge.SR_IS_ACTIVE, address1), 0.0);
    Assert.assertEquals(100.0, srGauge(MetricKeys.Gauge.SR_VOTES, address1), 0.0);
    Assert.assertEquals(0.0, srGauge(MetricKeys.Gauge.SR_IS_ACTIVE, address2), 0.0);
    Assert.assertEquals(40.0, srGauge(MetricKeys.Gauge.SR_VOTES, address2), 0.0);
    // rank is absolute within the full witness store, which also holds genesis
    // witnesses; the two local series must at least order by their votes
    Assert.assertTrue(srGauge(MetricKeys.Gauge.SR_RANK, address1) > 0);
    Assert.assertTrue(srGauge(MetricKeys.Gauge.SR_RANK, address2) > 0);
    Assert.assertTrue("higher votes must give the better rank",
        srGauge(MetricKeys.Gauge.SR_RANK, address1)
            < srGauge(MetricKeys.Gauge.SR_RANK, address2));

    // sr recording never touches the role series; a status change flips the single
    // role series and leaves the sr series alone
    backupManager.setStatus(BackupManager.BackupStatusEnum.SLAVER);
    Assert.assertEquals(SRMetrics.NODE_ROLE_SLAVER, roleGauge(), 0.0);
    Assert.assertEquals(100.0, srGauge(MetricKeys.Gauge.SR_VOTES, address1), 0.0);
    Assert.assertEquals(40.0, srGauge(MetricKeys.Gauge.SR_VOTES, address2), 0.0);
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
   * Builds the consensus-miner identity the node assembles from a configured private
   * key: the address derived from that key.
   */
  private ByteString addressOf(String privateKeyHex) {
    return ByteString.copyFrom(
        SignUtils.fromPrivate(ByteArray.fromHexString(privateKeyHex)).getAddress());
  }

  /**
   * Seeds the consensus miners the metrics path reads its identities from, one miner
   * per address. The private key bytes are never read by the metrics path; tearDown
   * clears the miner list again.
   */
  private void seedMiners(ByteString... addresses) {
    Param param = Param.getInstance();
    List<Param.Miner> miners = new ArrayList<>();
    for (ByteString address : addresses) {
      miners.add(param.new Miner(new byte[0], address, address));
    }
    param.setMiners(miners);
  }

  private ByteString registerWitnessWithVotes(long votes) {
    ByteString address = uniqueAddress();
    chainBaseManager.getWitnessStore().put(address.toByteArray(),
        new WitnessCapsule(address, votes, ""));
    chainBaseManager.getAccountStore().put(address.toByteArray(),
        new AccountCapsule(Protocol.Account.newBuilder().setAddress(address).build()));
    return address;
  }

  /**
   * Registers a witness for a locally configured miner's address so the multi-key test
   * can assert distinguishable per-address sr values.
   */
  private ByteString registerWitnessWithVotes(byte[] addressBytes, long votes) {
    ByteString address = ByteString.copyFrom(addressBytes);
    chainBaseManager.getWitnessStore().put(address.toByteArray(),
        new WitnessCapsule(address, votes, ""));
    chainBaseManager.getAccountStore().put(address.toByteArray(),
        new AccountCapsule(Protocol.Account.newBuilder().setAddress(address).build()));
    return address;
  }

  private Double srGauge(String name, String address) {
    return CollectorRegistry.defaultRegistry.getSampleValue(
        name,
        new String[] {MetricLabels.Gauge.ADDRESS},
        new String[] {address});
  }

  private Double info() {
    return CollectorRegistry.defaultRegistry.getSampleValue(
        "tron:node_info",
        new String[] {MetricLabels.Info.VERSION, MetricLabels.Info.CHAIN_ID},
        new String[] {Version.getVersion(), Args.getInstance().getChainId()});
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
