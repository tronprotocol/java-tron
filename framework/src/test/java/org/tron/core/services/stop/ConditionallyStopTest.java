package org.tron.core.services.stop;

import com.google.protobuf.ByteString;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.consensus.dpos.DposSlot;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.witness.Witness;
import org.tron.protos.Protocol;
import org.tron.test.Env;

@Slf4j
public abstract class ConditionallyStopTest extends Witness {


  private final String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
  private final byte[] privateKey = ByteArray.fromHexString(key);
  protected String dbPath;
  long currentHeader = -1;
  private TronNetDelegate tronNetDelegate;
  private AnnotationConfigApplicationContext context;

  static LocalDateTime localDateTime = LocalDateTime.now();
  private final long time = ZonedDateTime.of(localDateTime,
      ZoneId.systemDefault()).toInstant().toEpochMilli();

  protected abstract void initParameter(CommonParameter parameter);

  protected abstract void check() throws Exception;

  protected abstract void initDbPath();


  @Before
  public void init() throws Exception {

    initDbPath();
    initParameter(Args.getInstance());
    context = Env.init(new String[] {"-d", dbPath, "-w"}, Constant.TEST_CONF, dbPath);

    dbManager = context.getBean(Manager.class);
    dposSlot = context.getBean(DposSlot.class);
    ConsensusService consensusService = context.getBean(ConsensusService.class);
    consensusService.start();
    chainManager = dbManager.getChainBaseManager();
    tronNetDelegate = context.getBean(TronNetDelegate.class);
    tronNetDelegate.setTest(true);
    currentHeader = dbManager.getDynamicPropertiesStore()
        .getLatestBlockHeaderNumberFromDB();
  }

  @After
  public void destroy() {
    Env.destroy(context, dbPath);
  }

  @Test
  public void testStop() throws Exception {
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    Assert.assertNotNull(ecKey);
    byte[] address = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    chainManager.getWitnessScheduleStore().saveActiveWitnesses(new ArrayList<>());
    chainManager.addWitness(ByteString.copyFrom(address));

    Protocol.Block block = getSignedBlock(witnessCapsule.getAddress(), time, privateKey);

    tronNetDelegate.processBlock(new BlockCapsule(block), false);

    Map<ByteString, String> witnessList = addWitnessAndAccount();
    witnessList.put(ByteString.copyFrom(address), key);
    while (!tronNetDelegate.isHitDown()) {
      tronNetDelegate.processBlock(getSignedBlock(witnessList), false);
    }
    Assert.assertTrue(tronNetDelegate.isHitDown());
    check();
  }

}
