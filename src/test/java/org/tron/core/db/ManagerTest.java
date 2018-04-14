package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.exception.UnLinkedBlockException;

@Slf4j
public class ManagerTest {

  private static Manager dbManager;
  private static AnnotationConfigApplicationContext context;
  private static BlockCapsule blockCapsule2;
  private static String dbPath = "output_manager_test";

  static {
    Args.setParam(new String[]{"-d", dbPath, "-w"},
        Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {

    dbManager = context.getBean(Manager.class);

    blockCapsule2 = new BlockCapsule(1, ByteString.copyFrom(ByteArray
        .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")),
        0,
        ByteString.copyFrom(
            ECKey.fromPrivate(ByteArray
                .fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()))
                .getAddress()));
    blockCapsule2.setMerkleRoot();
    blockCapsule2.sign(
        ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));

  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }

  @Test
  public void pushBlock() {
    boolean isUnlinked = false;
    try {
      dbManager.pushBlock(blockCapsule2);
    } catch (UnLinkedBlockException e) {
      isUnlinked = true;
    } catch (Exception e) {
      Assert.assertTrue("pushBlock is error", false);
    }

    Assert.assertTrue("containBlock is error", dbManager.containBlock(Sha256Hash.wrap(ByteArray
        .fromHexString(blockCapsule2.getBlockId().toString()))));

    if (isUnlinked) {
      Assert.assertEquals("getBlockIdByNum is error", dbManager.getHeadBlockNum(),
          0);
    } else {
      Assert.assertEquals("getBlockIdByNum is error", blockCapsule2.getBlockId().toString(),
          dbManager.getBlockIdByNum(1).toString());
    }

    Assert.assertTrue("hasBlocks is error", dbManager.hasBlocks());

    dbManager.deleteBlock(Sha256Hash.wrap(ByteArray
        .fromHexString(blockCapsule2.getBlockId().toString())));

    Assert.assertFalse("deleteBlock is error", dbManager.containBlock(Sha256Hash.wrap(ByteArray
        .fromHexString(blockCapsule2.getBlockId().toString()))));
  }

  /*@Test
  public void testPushTransactions() {
    TransactionCapsule transactionCapsule = new TransactionCapsule(
        "2c0937534dd1b3832d05d865e8e6f2bf23218300b33a992740d45ccab7d4f519", 123);
    try {
      dbManager.pushTransactions(transactionCapsule);
    } catch (Exception e) {
      Assert.assertTrue("pushTransaction is error", false);
    }
    Assert.assertEquals("pushTransaction is error", 123,
        transactionCapsule.getInstance().getRawData().getVout(0).getValue());
  }*/

  //  @Test
  public void updateWits() {
    int sizePrv = dbManager.getWitnesses().size();
    dbManager.getWitnesses().forEach(witnessCapsule -> {
      logger.info("witness address is {}",
          ByteArray.toHexString(witnessCapsule.getAddress().toByteArray()));
    });
    logger.info("------------");
    WitnessCapsule witnessCapsulef = new WitnessCapsule(
        ByteString.copyFrom(ByteArray.fromHexString("0x0011")), "www.tron.net/first");
    witnessCapsulef.setIsJobs(true);
    WitnessCapsule witnessCapsules = new WitnessCapsule(
        ByteString.copyFrom(ByteArray.fromHexString("0x0012")), "www.tron.net/second");
    witnessCapsules.setIsJobs(true);
    WitnessCapsule witnessCapsulet = new WitnessCapsule(
        ByteString.copyFrom(ByteArray.fromHexString("0x0013")), "www.tron.net/three");
    witnessCapsulet.setIsJobs(false);

    dbManager.getWitnesses().forEach(witnessCapsule -> {
      logger.info("witness address is {}",
          ByteArray.toHexString(witnessCapsule.getAddress().toByteArray()));
    });
    logger.info("---------");
    dbManager.getWitnessStore().put(witnessCapsulef.getAddress().toByteArray(), witnessCapsulef);
    dbManager.getWitnessStore().put(witnessCapsules.getAddress().toByteArray(), witnessCapsules);
    dbManager.getWitnessStore().put(witnessCapsulet.getAddress().toByteArray(), witnessCapsulet);
    dbManager.getWitnessController().initWits();
    dbManager.getWitnesses().forEach(witnessCapsule -> {
      logger.info("witness address is {}",
          ByteArray.toHexString(witnessCapsule.getAddress().toByteArray()));
    });
    int sizeTis = dbManager.getWitnesses().size();
    Assert.assertEquals("update add witness size is ", 2, sizeTis - sizePrv);
  }

  @Test
  public void fork() {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_CONF);
    long size = dbManager.getBlockStore().dbSource.allKeys().size();
    String key = "040df7d5a0641eb901f4a3b08bb49f968f8dc14306e4cbfb2a455e37a91d6"
        + "0a7c8a5ff4f0814afa044a403f01cb462628bea1b62afc8fe3a6f3d42d3852591b059";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(
        ByteString.copyFrom(address));
    dbManager.addWitness(witnessCapsule);
    dbManager.addWitness(witnessCapsule);
    dbManager.addWitness(witnessCapsule);
    IntStream.range(0, 1).forEach(i -> {
      try {
        dbManager.generateBlock(witnessCapsule, System.currentTimeMillis(), privateKey);
      } catch (Exception e) {
        logger.debug(e.getMessage(), e);
      }
    });

    try {
      long num = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
      BlockCapsule blockCapsule1 = new BlockCapsule(num,
          dbManager.getHead().getParentHash().getByteString(),
          System.currentTimeMillis(),
          witnessCapsule.getAddress());
      blockCapsule1.generatedByMyself = true;
      BlockCapsule blockCapsule2 = new BlockCapsule(num + 1,
          blockCapsule1.getBlockId().getByteString(),
          System.currentTimeMillis(),
          witnessCapsule.getAddress());
      blockCapsule2.generatedByMyself = true;

      logger.error("******1*******" + "block1 id:" + blockCapsule1.getBlockId());
      logger.error("******2*******" + "block2 id:" + blockCapsule2.getBlockId());
      dbManager.pushBlock(blockCapsule1);
      dbManager.pushBlock(blockCapsule2);
      logger.error("******in blockStore block size:"
          + dbManager.getBlockStore().dbSource.allKeys().size());
      logger.error("******in blockStore block:"
          + dbManager.getBlockStore().dbSource.allKeys().stream().map(ByteArray::toHexString)
          .collect(Collectors.toList()));

      Assert.assertNotNull(dbManager.getBlockStore().get(blockCapsule1.getBlockId().getBytes()));
      Assert.assertNotNull(dbManager.getBlockStore().get(blockCapsule2.getBlockId().getBytes()));

      Assert.assertEquals(
          dbManager.getBlockStore().get(blockCapsule2.getBlockId().getBytes()).getParentHash(),
          blockCapsule1.getBlockId());

      Assert.assertEquals(dbManager.getBlockStore().dbSource.allKeys().size(), size + 2);

      Assert.assertEquals(dbManager.getBlockIdByNum(dbManager.getHead().getNum() - 1),
          blockCapsule1.getBlockId());
      Assert.assertEquals(dbManager.getBlockIdByNum(dbManager.getHead().getNum() - 2),
          blockCapsule1.getParentHash());

      Assert.assertEquals(blockCapsule2.getBlockId().getByteString(),
          dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
      Assert.assertEquals(dbManager.getHead().getBlockId().getByteString(),
          dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());

    } catch (Exception e) {
      logger.debug(e.getMessage(), e);
    }
    dbManager.getWitnesses().clear();
  }
}
