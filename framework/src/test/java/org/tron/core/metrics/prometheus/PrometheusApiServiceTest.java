package org.tron.core.metrics.prometheus;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import io.prometheus.client.CollectorRegistry;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.consensus.dpos.DposSlot;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.BlockGenerate;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetDelegate;
import org.tron.protos.Protocol;

@Slf4j(topic = "metric")
public class PrometheusApiServiceTest extends BlockGenerate {


  static ChainBaseManager chainManager;
  static LocalDateTime localDateTime = LocalDateTime.now();
  private static DposSlot dposSlot;
  final int blocks = 512;
  private final String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
  private final byte[] privateKey = ByteArray.fromHexString(key);
  private final AtomicInteger port = new AtomicInteger(0);
  private final long time = ZonedDateTime.of(localDateTime,
      ZoneId.systemDefault()).toInstant().toEpochMilli();
  protected String dbPath;
  protected String dbEngine;
  protected Manager dbManager;
  private TronNetDelegate tronNetDelegate;
  private TronApplicationContext context;

  protected void initParameter(CommonParameter parameter) {
    parameter.setMetricsPrometheusEnable(true);
  }

  protected void check() throws Exception {
    Double memoryBytes = CollectorRegistry.defaultRegistry.getSampleValue(
        "system_total_physical_memory_bytes");
    Assert.assertNotNull(memoryBytes);
    Assert.assertTrue(memoryBytes.intValue() > 0);

    Double cpus = CollectorRegistry.defaultRegistry.getSampleValue("system_available_cpus");
    Assert.assertNotNull(cpus);
    Assert.assertEquals(cpus.intValue(), Runtime.getRuntime().availableProcessors());

    Double pushBlock = CollectorRegistry.defaultRegistry.getSampleValue(
        "tron:block_process_latency_seconds_count",
        new String[] {"sync"}, new String[] {"false"});
    Assert.assertNotNull(pushBlock);
    Assert.assertEquals(pushBlock.intValue(), blocks + 1);
    Double errorLogs = CollectorRegistry.defaultRegistry.getSampleValue(
        "tron:error_info_total", new String[] {"net"}, new String[] {MetricLabels.UNDEFINED});
    Assert.assertNull(errorLogs);
  }

  protected void initDb() {
    dbPath = "output-prometheus-metric";
    dbEngine = "LEVELDB";
  }


  @Before
  public void init() throws Exception {

    initDb();
    FileUtil.deleteDir(new File(dbPath));
    logger.info("Full node running.");
    Args.setParam(new String[] {"-d", dbPath, "-w"}, Constant.TEST_CONF);
    Args.getInstance().setNodeListenPort(10000 + port.incrementAndGet());
    initParameter(Args.getInstance());
    Metrics.init();
    context = new TronApplicationContext(DefaultConfig.class);

    dbManager = context.getBean(Manager.class);
    setManager(dbManager);
    dposSlot = context.getBean(DposSlot.class);
    ConsensusService consensusService = context.getBean(ConsensusService.class);
    consensusService.start();
    chainManager = dbManager.getChainBaseManager();
    tronNetDelegate = context.getBean(TronNetDelegate.class);
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  private void generateBlock(Map<ByteString, String> witnessAndAccount) throws Exception {

    BlockCapsule block =
        createTestBlockCapsule(
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 3000,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            witnessAndAccount);

    tronNetDelegate.processBlock(block, false);
  }

  @Test
  public void testMetric() throws Exception {

    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    Assert.assertNotNull(ecKey);
    byte[] address = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    chainManager.getWitnessScheduleStore().saveActiveWitnesses(new ArrayList<>());
    chainManager.addWitness(ByteString.copyFrom(address));

    Protocol.Block block = getSignedBlock(witnessCapsule.getAddress(), time, privateKey);

    tronNetDelegate.processBlock(new BlockCapsule(block), false);

    Map<ByteString, String> witnessAndAccount = addTestWitnessAndAccount();
    witnessAndAccount.put(ByteString.copyFrom(address), key);
    for (int i = 0; i < blocks; i++) {
      generateBlock(witnessAndAccount);
    }
    check();
  }

  private Map<ByteString, String> addTestWitnessAndAccount() {
    chainManager.getWitnesses().clear();
    return IntStream.range(0, 2)
        .mapToObj(
            i -> {
              ECKey ecKey = new ECKey(Utils.getRandom());
              String privateKey = ByteArray.toHexString(ecKey.getPrivKey().toByteArray());
              ByteString address = ByteString.copyFrom(ecKey.getAddress());

              WitnessCapsule witnessCapsule = new WitnessCapsule(address);
              chainManager.getWitnessStore().put(address.toByteArray(), witnessCapsule);
              chainManager.addWitness(address);

              AccountCapsule accountCapsule =
                  new AccountCapsule(Protocol.Account.newBuilder().setAddress(address).build());
              chainManager.getAccountStore().put(address.toByteArray(), accountCapsule);

              return Maps.immutableEntry(address, privateKey);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private BlockCapsule createTestBlockCapsule(long time,
                                              long number, ByteString hash,
                                              Map<ByteString, String> witnessAddressMap) {
    ByteString witnessAddress = dposSlot.getScheduledWitness(dposSlot.getSlot(time));
    BlockCapsule blockCapsule = new BlockCapsule(number, Sha256Hash.wrap(hash), time,
        witnessAddress);
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(ByteArray.fromHexString(witnessAddressMap.get(witnessAddress)));
    return blockCapsule;
  }

}