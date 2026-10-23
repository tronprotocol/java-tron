package org.tron.common.prometheus;

import com.google.protobuf.ByteString;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.StringWriter;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.backup.BackupManager;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.StringUtil;
import org.tron.consensus.Consensus;
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

  // ---- tron:is_witness: process flag, published at startup and independent of the role ----

  /**
   * Flag is published once at startup by ConsensusService: 0 for a node started without
   * witness mode, 1 for a node started with -w.
   */
  @Test
  public void testIsWitnessBothValues() {
    for (boolean witness : new boolean[] {false, true}) {
      Args.getInstance().witness = witness;
      publishIsWitness();
      int expected = witness ? SRMetrics.WITNESS : SRMetrics.NOT_WITNESS;
      Assert.assertEquals(expected, isWitnessGauge(), 0.0);
    }
  }

  /**
   * Metrics disabled: recordIsWitness() short-circuits, so the previously published value stands
   * even when the flag would have changed.
   */
  @Test
  public void testIsWitnessNotPublishedWhenMetricsDisabled() {
    Args.getInstance().witness = false;
    publishIsWitness();
    Assert.assertEquals(SRMetrics.NOT_WITNESS, isWitnessGauge(), 0.0);

    Args.getInstance().setMetricsPrometheusEnable(false);
    try {
      Args.getInstance().witness = true;
      publishIsWitness();
      Assert.assertEquals(SRMetrics.NOT_WITNESS, isWitnessGauge(), 0.0);
    } finally {
      Args.getInstance().setMetricsPrometheusEnable(true);
    }
  }

  /**
   * Metrics disabled: recordNodeRole() short-circuits, so the previously published role stands
   * even when the backup configuration would resolve to a different one.
   */
  @Test
  public void testNodeRoleNotPublishedWhenMetricsDisabled() {
    Args.getInstance().witness = false;
    // no backup configured: baseline master (1)
    backupManager.recordNodeRole();
    Assert.assertEquals(SRMetrics.NODE_ROLE_MASTER, roleGauge(), 0.0);

    // a configuration that would resolve to init (0) if it were published
    Args.getInstance().setBackupPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setBackupMembers(Collections.singletonList("127.0.0.2"));
    Args.getInstance().setMetricsPrometheusEnable(false);
    try {
      backupManager.recordNodeRole();
      Assert.assertEquals(SRMetrics.NODE_ROLE_MASTER, roleGauge(), 0.0);
    } finally {
      Args.getInstance().setMetricsPrometheusEnable(true);
      Args.getInstance().setBackupPort(0);
      Args.getInstance().setBackupMembers(Collections.emptyList());
    }
  }

  /**
   * A failure while publishing is_witness must not propagate: ConsensusService catches it, so it
   * can neither skip the witness-address publication running right beside it nor abort startup.
   */
  @Test
  public void testIsWitnessFailureDoesNotBlockWitnessPublication() {
    Args.getInstance().witness = false;
    ByteString address = uniqueAddress();
    List<Param.Miner> miners = Collections.singletonList(miner(address, address));

    try (MockedStatic<SRMetrics> mocked = Mockito.mockStatic(SRMetrics.class)) {
      mocked.when(() -> SRMetrics.recordIsWitness(Mockito.anyInt()))
          .thenThrow(new RuntimeException("is_witness failure"));
      mocked.when(() -> SRMetrics.recordWitnessAddress(Mockito.anyString()))
          .thenCallRealMethod();

      // must not propagate the is_witness failure
      publishIsWitness();

      // the witness-address publication still runs independently on the same startup path
      ReflectionTestUtils.invokeMethod(consensusService, "recordWitnessAddresses", miners);
    }

    Assert.assertEquals(1.0, witnessGauge(encode(address)), 0.0);
  }

  /**
   * The flag metric must be published by {@link ConsensusService#start()} itself, not by a
   * caller: with the election engine stubbed out, a real start() has to write tron:is_witness
   * from Args.isWitness(), even for a node with zero miners. Pre-setting the opposite value pins
   * the call site, so dropping the publication from start() fails here.
   */
  @Test
  public void testStartPublishesIsWitnessForZeroMiners() {
    Consensus consensusMock = Mockito.mock(Consensus.class);
    ConsensusService service = new ConsensusService();
    ReflectionTestUtils.setField(service, "consensus", consensusMock);

    for (boolean witness : new boolean[] {false, true}) {
      Args.getInstance().witness = witness;
      // opposite of the expected value, so a start() that skipped the publication cannot pass
      SRMetrics.recordIsWitness(witness ? SRMetrics.NOT_WITNESS
          : SRMetrics.WITNESS);

      service.start();

      int expected = witness ? SRMetrics.WITNESS : SRMetrics.NOT_WITNESS;
      Assert.assertEquals(expected, isWitnessGauge(), 0.0);
    }

    Mockito.verify(consensusMock, Mockito.times(2)).start(Mockito.any(Param.class));
  }

  // ---- tron:node_role: the three states, the pre-election window and status transitions ----

  /**
   * No backup configured: both identities report master (1) as the standalone default; the
   * backup status defaults to MASTER before any election runs.
   */
  @Test
  public void testNodeRoleMasterWhenNoBackupBothIdentities() {
    Args.getInstance().witness = false;
    backupManager.recordNodeRole();
    Assert.assertEquals(SRMetrics.NODE_ROLE_MASTER, roleGauge(), 0.0);

    Args.getInstance().witness = true;
    backupManager.recordNodeRole();
    Assert.assertEquals(SRMetrics.NODE_ROLE_MASTER, roleGauge(), 0.0);
  }

  /**
   * A partial backup configuration — a usable port without members, or members without a usable
   * port — is not an election setup (the same condition BackupServer.initServer() uses), so the
   * node reports master (1) for both identities and no transport is started.
   */
  @Test
  public void testNodeRoleMasterWhenBackupPartiallyConfigured() {
    for (boolean witness : new boolean[] {false, true}) {
      for (boolean portUsable : new boolean[] {false, true}) {
        Args.getInstance().witness = witness;
        Args.getInstance().setBackupPort(portUsable ? PublicMethod.chooseRandomPort() : 0);
        Args.getInstance().setBackupMembers(portUsable
            ? Collections.emptyList() : Collections.singletonList("127.0.0.2"));

        // one half of the pair is always unset, so this is never a usable election config
        BackupManager fresh = new BackupManager();
        try {
          fresh.recordNodeRole();
          Assert.assertEquals(SRMetrics.NODE_ROLE_MASTER, roleGauge(), 0.0);
          Assert.assertEquals(BackupManager.BackupStatusEnum.MASTER, fresh.getStatus());
        } finally {
          fresh.stop();
        }
      }
    }
  }

  /**
   * Backup configured but the election has not run yet: even though the status field still
   * defaults to MASTER, the node reports init (0), for both identities; a status change before
   * init() stays inside the window, and after init() the status maps to the elected role.
   */
  @Test
  public void testNodeRoleInitBeforeBackupElectionBothIdentities() throws Exception {
    configureBackup();

    for (boolean witness : new boolean[] {false, true}) {
      BackupManager fresh = new BackupManager();
      try {
        Args.getInstance().witness = witness;

        // a directly constructed manager has not gone through init(): default status is MASTER
        Assert.assertEquals(BackupManager.BackupStatusEnum.MASTER, fresh.getStatus());
        fresh.recordNodeRole();
        Assert.assertEquals(SRMetrics.NODE_ROLE_INIT, roleGauge(), 0.0);

        // a status change before init() stays inside the window
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
  }

  /**
   * Flag and role are fully independent: for both flag values the startup publication is
   * not-witness/witness + init, and the role then cycles INIT -> MASTER -> SLAVER -> MASTER while
   * is_witness never changes. Every publication rewrites the single label-less series in place,
   * so no duplicate registration or stale role series accumulates.
   */
  @Test
  public void testIsWitnessIndependentOfRoleBothValues() throws Exception {
    configureBackup();

    BackupManager.BackupStatusEnum[] statuses = {
        BackupManager.BackupStatusEnum.INIT,
        BackupManager.BackupStatusEnum.MASTER,
        BackupManager.BackupStatusEnum.SLAVER,
        BackupManager.BackupStatusEnum.MASTER};
    int[] roles = {
        SRMetrics.NODE_ROLE_INIT,
        SRMetrics.NODE_ROLE_MASTER,
        SRMetrics.NODE_ROLE_SLAVER,
        SRMetrics.NODE_ROLE_MASTER};

    for (boolean witness : new boolean[] {false, true}) {
      BackupManager fresh = new BackupManager();
      try {
        Args.getInstance().witness = witness;
        int expectedIsWitness = witness ? SRMetrics.WITNESS : SRMetrics.NOT_WITNESS;

        // startup: is_witness comes from ConsensusService, node_role from BackupManager;
        // configured-but-not-init is INIT, not the MASTER default
        publishIsWitness();
        fresh.recordNodeRole();
        Assert.assertEquals(expectedIsWitness, isWitnessGauge(), 0.0);
        Assert.assertEquals(SRMetrics.NODE_ROLE_INIT, roleGauge(), 0.0);

        // flip the startup flag: setStatus() must only republish the role, never recompute
        // is_witness, so the originally published flag has to keep standing
        Args.getInstance().witness = !witness;
        markBackupInitiated(fresh);
        for (int i = 0; i < statuses.length; i++) {
          fresh.setStatus(statuses[i]);
          Assert.assertEquals(roles[i], roleGauge(), 0.0);
          Assert.assertEquals("is_witness must not change with the role", expectedIsWitness,
              isWitnessGauge(), 0.0);
        }

        Assert.assertEquals(1, sampleCount(MetricKeys.Gauge.NODE_ROLE));
        Assert.assertEquals(1, sampleCount(MetricKeys.Gauge.IS_WITNESS));
      } finally {
        fresh.stop();
      }
    }
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
   * Drives the startup flag publication the way production does: ConsensusService.start()
   * calls its private recordIsWitness() hook right after assembling the miners.
   */
  private void publishIsWitness() {
    ReflectionTestUtils.invokeMethod(consensusService, "recordIsWitness");
  }

  /**
   * Drives the startup publication the way production does: the miners are assembled by
   * ConsensusService, which publishes the witness identities once during startup.
   */
  private void recordWitnessMetrics(List<Param.Miner> miners) {
    Param.getInstance().setMiners(miners);
    ReflectionTestUtils.invokeMethod(consensusService, "recordWitnessAddresses", miners);
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

  private Double isWitnessGauge() {
    return CollectorRegistry.defaultRegistry.getSampleValue(
        MetricKeys.Gauge.IS_WITNESS,
        new String[] {},
        new String[] {});
  }

  /** Counts the exported sample lines of a label-less metric, to detect duplicate registration. */
  private int sampleCount(String metricName) {
    StringWriter writer = new StringWriter();
    try {
      TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    int count = 0;
    for (String line : writer.toString().split("\n")) {
      if (line.startsWith(metricName + " ") || line.startsWith(metricName + "{")) {
        count++;
      }
    }
    return count;
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
