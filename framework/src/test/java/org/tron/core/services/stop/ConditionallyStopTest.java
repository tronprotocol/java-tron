package org.tron.core.services.stop;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.consensus.ConsensusDelegate;
import org.tron.consensus.dpos.DposService;
import org.tron.consensus.dpos.DposSlot;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetDelegate;
import org.tron.protos.Protocol;

@Slf4j(topic = "test")
public abstract class ConditionallyStopTest  {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  static ChainBaseManager chainManager;
  private static DposSlot dposSlot;
  private final AtomicInteger port = new AtomicInteger(0);
  protected String dbPath;
  protected Manager dbManager;
  long currentHeader = -1;
  private TronNetDelegate tronNetDelegate;
  private TronApplicationContext context;

  private DposService dposService;
  private ConsensusDelegate consensusDelegate;

  private static final Instant instant = Instant.parse("2025-10-01T00:00:00Z");
  private final long time = instant.toEpochMilli();
  static LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);

  protected abstract void initParameter(CommonParameter parameter);

  protected abstract void check() throws Exception;

  protected void initDbPath() throws IOException {
    dbPath = temporaryFolder.newFolder().toString();
  }

  private Map<String, String> witnesses;


  @Before
  public void init() throws Exception {

    initDbPath();
    logger.info("Full node running.");
    Args.setParam(new String[] {"-d", dbPath}, Constant.TEST_CONF);
    Args.getInstance().setNodeListenPort(10000 + port.incrementAndGet());
    Args.getInstance().genesisBlock.setTimestamp(Long.toString(time));
    initParameter(Args.getInstance());
    context = new TronApplicationContext(DefaultConfig.class);

    dbManager = context.getBean(Manager.class);
    dposSlot = context.getBean(DposSlot.class);
    ConsensusService consensusService = context.getBean(ConsensusService.class);
    consensusService.start();
    chainManager = dbManager.getChainBaseManager();
    tronNetDelegate = context.getBean(TronNetDelegate.class);
    dposService = context.getBean(DposService.class);
    consensusDelegate = context.getBean(ConsensusDelegate.class);
    tronNetDelegate.setExit(false);
    currentHeader = dbManager.getDynamicPropertiesStore()
        .getLatestBlockHeaderNumberFromDB();

    chainManager.getWitnessScheduleStore().reset();
    chainManager.getWitnessStore().reset();
    witnesses = addTestWitnessAndAccount();

    List<ByteString> allWitnesses = new ArrayList<>();
    consensusDelegate.getAllWitnesses().forEach(witnessCapsule ->
        allWitnesses.add(witnessCapsule.getAddress()));
    dposService.updateWitness(allWitnesses);
    List<ByteString> activeWitnesses = consensusDelegate.getActiveWitnesses();
    activeWitnesses.forEach(address -> {
      WitnessCapsule witnessCapsule = consensusDelegate.getWitness(address.toByteArray());
      witnessCapsule.setIsJobs(true);
      consensusDelegate.saveWitness(witnessCapsule);
    });
    chainManager.getDynamicPropertiesStore().saveNextMaintenanceTime(time);
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
  }

  private void generateBlock() throws Exception {

    BlockCapsule block =
        createTestBlockCapsule(
            chainManager.getNextBlockSlotTime(),
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString());

    tronNetDelegate.processBlock(block, false);

    logger.info("headerNum: {} solidityNum: {}, dbNum: {}",
        block.getNum(), chainManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum(),
        chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumberFromDB());
  }

  @Test(timeout = 30_000) // milliseconds
  public void testStop() throws Exception {
    while (!tronNetDelegate.isHitDown()) {
      generateBlock();
    }
    check();
  }

  private Map<String, String> addTestWitnessAndAccount() {
    return IntStream.range(0, 27)
        .mapToObj(
            i -> {
              ECKey ecKey = new ECKey(Utils.getRandom());
              String privateKey = ByteArray.toHexString(ecKey.getPrivKey().toByteArray());
              ByteString address = ByteString.copyFrom(ecKey.getAddress());

              WitnessCapsule witnessCapsule = new WitnessCapsule(address, 27 - i, "SR" + i);
              chainManager.getWitnessStore().put(address.toByteArray(), witnessCapsule);
              AccountCapsule accountCapsule =
                  new AccountCapsule(Protocol.Account.newBuilder().setAddress(address).build());
              chainManager.getAccountStore().put(address.toByteArray(), accountCapsule);

              return Maps.immutableEntry(ByteArray.toHexString(ecKey.getAddress()), privateKey);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private BlockCapsule createTestBlockCapsule(long time,
                                              long number, ByteString hash) {
    long slot = dposSlot.getSlot(time);
    ByteString witness = dposSlot.getScheduledWitness(slot);
    BlockCapsule blockCapsule = new BlockCapsule(number, Sha256Hash.wrap(hash), time, witness);
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    String pri = witnesses.get(ByteArray.toHexString(witness.toByteArray()));
    blockCapsule.sign(ByteArray.fromHexString(pri));
    return blockCapsule;
  }
}
