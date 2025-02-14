package org.tron.core.event;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.logsfilter.EventPluginConfig;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.TriggerConfig;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.PublicMethod;
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
import org.tron.core.services.event.BlockEventGet;
import org.tron.core.services.event.bo.BlockEvent;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol;

@Slf4j
public class BlockEventGetTest extends BlockGenerate {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  static ChainBaseManager chainManager;

  private final String key = PublicMethod.getRandomPrivateKey();
  private final byte[] privateKey = ByteArray.fromHexString(key);
  private final byte[] address = PublicMethod.getAddressByteByPrivateKey(key);

  private final AtomicInteger port = new AtomicInteger(0);
  protected String dbPath;
  protected Manager dbManager;
  long currentHeader = -1;
  private TronNetDelegate tronNetDelegate;
  private TronApplicationContext context;


  static LocalDateTime localDateTime = LocalDateTime.now();
  private long time = ZonedDateTime.of(localDateTime,
      ZoneId.systemDefault()).toInstant().toEpochMilli();

  protected void initDbPath() throws IOException {
    dbPath = temporaryFolder.newFolder().toString();
  }

  @Before
  public void before() throws IOException {
    initDbPath();
    logger.info("Full node running.");
    Args.setParam(new String[] {"-d", dbPath, "-w"}, Constant.TEST_CONF);
    Args.getInstance().setNodeListenPort(10000 + port.incrementAndGet());

    context = new TronApplicationContext(DefaultConfig.class);

    dbManager = context.getBean(Manager.class);
    setManager(dbManager);

    context.getBean(ConsensusService.class).start();
    chainManager = dbManager.getChainBaseManager();
    tronNetDelegate = context.getBean(TronNetDelegate.class);
    tronNetDelegate.setExit(false);
    currentHeader = dbManager.getDynamicPropertiesStore()
      .getLatestBlockHeaderNumberFromDB();

    ByteString addressBS = ByteString.copyFrom(address);
    WitnessCapsule witnessCapsule = new WitnessCapsule(addressBS);
    chainManager.getWitnessStore().put(address, witnessCapsule);
    chainManager.addWitness(addressBS);

    AccountCapsule accountCapsule = new AccountCapsule(Protocol.Account.newBuilder()
        .setAddress(addressBS).setBalance((long) 1e10).build());
    chainManager.getAccountStore().put(address, accountCapsule);

    DynamicPropertiesStore dps = dbManager.getDynamicPropertiesStore();
    dps.saveAllowTvmTransferTrc10(1);
    dps.saveAllowTvmConstantinople(1);
    dps.saveAllowTvmShangHai(1);
  }

  @After
  public void after() throws IOException {
  }

  @Test
  public void test() throws Exception {
    BlockEventGet blockEventGet = context.getBean(BlockEventGet.class);
    Manager manager = context.getBean(Manager.class);

    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    ChainBaseManager.getChainBaseManager()
        .getWitnessScheduleStore().saveActiveWitnesses(new ArrayList<>());
    ChainBaseManager.getChainBaseManager().addWitness(ByteString.copyFrom(address));

    String code = "608060405234801561000f575f80fd5b50d3801561001b575f80fd5b50d28015610027575f"
        + "80fd5b503373ffffffffffffffffffffffffffffffffffffffff165f73ffffffffffffffffffffffff"
        + "ffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3"
        + "ef6402540be40060405161008a91906100e2565b60405180910390a36100fb565b5f81905091905056"
        + "5b5f819050919050565b5f819050919050565b5f6100cc6100c76100c284610097565b6100a9565b61"
        + "00a0565b9050919050565b6100dc816100b2565b82525050565b5f6020820190506100f55f83018461"
        + "00d3565b92915050565b603e806101075f395ff3fe60806040525f80fdfea26474726f6e582212200c"
        + "57c973388f044038eff0e6474425b38037e75e66d6b3047647290605449c7764736f6c63430008140033";
    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        "TestTRC20", address, "[{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\""
        + ":\"from\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"to\",\"type\":\"address\"}"
        +   ",{\"indexed\":false,\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"Transfer\","
        +  "\"type\":\"event\"}]", code, 0, (long) 1e9, 100, null, 1);
    trx = trx.toBuilder().addRet(
      Protocol.Transaction.Result.newBuilder()
          .setContractRetValue(Protocol.Transaction.Result.contractResult.SUCCESS_VALUE)
          .build()).build();

    Protocol.Block block = getSignedBlock(witnessCapsule.getAddress(), time, privateKey);
    BlockCapsule blockCapsule = new BlockCapsule(block.toBuilder().addTransactions(trx).build());
    blockCapsule.generatedByMyself = true;
    blockCapsule.getTransactions().forEach(txCap -> {
      txCap.setVerified(true);
      chainManager.setBlockReference(txCap);
      txCap.setExpiration(3000);
    });
    manager.pushBlock(blockCapsule);

    EventPluginConfig config = new EventPluginConfig();
    config.setSendQueueLength(1000);
    config.setBindPort(5555);
    config.setUseNativeQueue(true);
    config.setTriggerConfigList(new ArrayList<>());

    TriggerConfig blockTriggerConfig = new TriggerConfig();
    blockTriggerConfig.setTriggerName("block");
    blockTriggerConfig.setEnabled(true);
    config.getTriggerConfigList().add(blockTriggerConfig);

    TriggerConfig txTriggerConfig = new TriggerConfig();
    txTriggerConfig.setTriggerName("transaction");
    txTriggerConfig.setEnabled(true);
    txTriggerConfig.setEthCompatible(true);
    config.getTriggerConfigList().add(txTriggerConfig);

    TriggerConfig solidityTriggerConfig = new TriggerConfig();
    solidityTriggerConfig.setTriggerName("solidity");
    solidityTriggerConfig.setEnabled(true);
    config.getTriggerConfigList().add(solidityTriggerConfig);

    TriggerConfig contracteventTriggerConfig = new TriggerConfig();
    contracteventTriggerConfig.setTriggerName("contractevent");
    contracteventTriggerConfig.setEnabled(true);
    config.getTriggerConfigList().add(contracteventTriggerConfig);

    TriggerConfig contractlogTriggerConfig = new TriggerConfig();
    contractlogTriggerConfig.setTriggerName("contractlog");
    contractlogTriggerConfig.setEnabled(true);
    contractlogTriggerConfig.setRedundancy(true);
    config.getTriggerConfigList().add(contractlogTriggerConfig);

    EventPluginLoader.getInstance().start(config);
    try {
      BlockEvent blockEvent = blockEventGet.getBlockEvent(1);
      Assert.assertNotNull(blockEvent);
    } catch (Exception e) {
      Assert.fail();
    }
  }
}