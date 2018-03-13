package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ValidateSignatureException;


public class ManagerTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  private static Manager dbManager = new Manager();
  private static BlockCapsule blockCapsule2;
  private static String dbPath = "output_manager";

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath},
        Configuration.getByPath(Constant.TEST_CONF));

    dbManager.init();
    blockCapsule2 = new BlockCapsule(1, ByteString.copyFrom(ByteArray
        .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")),
        0,
        ByteString.copyFrom(
            ECKey.fromPrivate(ByteArray.fromHexString(Args.getInstance().getPrivateKey()))
                .getAddress()));
    blockCapsule2.setMerkleRoot();
    blockCapsule2.sign(Args.getInstance().getPrivateKey().getBytes());
  }

  @AfterClass
  public static void removeDb() {
    File dbFolder = new File(dbPath);
    deleteFolder(dbFolder);
  }

  private static void deleteFolder(File index) {
    if (!index.isDirectory() || index.listFiles().length <= 0) {
      index.delete();
      return;
    }
    for (File file : index.listFiles()) {
      if (null != file) {
        deleteFolder(file);
      }
    }
    index.delete();
  }

  @Test
  public void pushBlock() {
    try {
      dbManager.pushBlock(blockCapsule2);
    } catch (Exception e) {
      Assert.assertTrue("pushBlock is error", false);
    }

    Assert.assertTrue("containBlock is error", dbManager.containBlock(Sha256Hash.wrap(ByteArray
        .fromHexString(blockCapsule2.getBlockId().toString()))));
    Assert.assertEquals("getBlockIdByNum is error", blockCapsule2.getBlockId().toString(),
        dbManager.getBlockIdByNum(1).toString());
    Assert.assertTrue("hasBlocks is error", dbManager.hasBlocks());

    dbManager.deleteBlock(Sha256Hash.wrap(ByteArray
        .fromHexString(blockCapsule2.getBlockId().toString())));

    Assert.assertFalse("deleteBlock is error", dbManager.containBlock(Sha256Hash.wrap(ByteArray
        .fromHexString(blockCapsule2.getBlockId().toString()))));
  }

  @Test
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
  }

  @Test
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
    dbManager.updateWits();
    dbManager.getWitnesses().forEach(witnessCapsule -> {
      logger.info("witness address is {}",
          ByteArray.toHexString(witnessCapsule.getAddress().toByteArray()));
    });
    int sizeTis = dbManager.getWitnesses().size();
    Assert.assertEquals("update add witness size is ", 2, sizeTis - sizePrv);
  }

  @Test
  public void testGetBlockChainHashesOnFork() {
    byte[] prikey = new byte[]{1, 2, 3, 4, 5};
    ECKey ecKey = ECKey.fromPrivate(prikey);
    byte[] address = ecKey.getAddress();

    BlockCapsule blockCapsule = new BlockCapsule(1, ByteString
        .copyFrom(dbManager.getGenesisBlockId().getBytes()), 0,
        ByteString.copyFrom(address));
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(prikey);
    BlockCapsule blockCapsule2 = new BlockCapsule(2, ByteString
        .copyFrom(blockCapsule.getBlockId().getBytes()), 1,
        ByteString.copyFrom(address));
    blockCapsule2.setMerkleRoot();
    blockCapsule2.sign(prikey);
    BlockCapsule blockCapsule3 = new BlockCapsule(3, ByteString
        .copyFrom(blockCapsule2.getBlockId().getBytes()), 2,
        ByteString.copyFrom(address));
    blockCapsule3.setMerkleRoot();
    blockCapsule3.sign(prikey);
    BlockCapsule blockCapsule4 = new BlockCapsule(3, ByteString
        .copyFrom(blockCapsule2.getBlockId().getBytes()), 1,
        ByteString.copyFrom(address));
    blockCapsule4.setMerkleRoot();
    blockCapsule4.sign(prikey);
    BlockCapsule blockCapsule5 = new BlockCapsule(4, ByteString
        .copyFrom(blockCapsule4.getBlockId().getBytes()), 1,
        ByteString.copyFrom(address));
    blockCapsule5.setMerkleRoot();
    blockCapsule5.sign(prikey);
    BlockCapsule blockCapsule6 = new BlockCapsule(4, ByteString
        .copyFrom(blockCapsule3.getBlockId().getBytes()), 2,
        ByteString.copyFrom(address));
    blockCapsule6.setMerkleRoot();
    blockCapsule6.sign(prikey);
    try {
      dbManager.pushBlock(blockCapsule);
      dbManager.pushBlock(blockCapsule2);
      dbManager.pushBlock(blockCapsule3);
      dbManager.pushBlock(blockCapsule4);
      dbManager.pushBlock(blockCapsule6);
      dbManager.pushBlock(blockCapsule5);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.assertTrue("pushBlock is error", false);
    }
    Assert.assertEquals(blockCapsule6.getBlockId(), dbManager.getHeadBlockId());
    Assert.assertEquals(4, dbManager.getHeadBlockNum());
    logger.info("{}", dbManager.getHeadBlockId());
    Assert.assertTrue(
        dbManager.getBlockChainHashesOnFork(blockCapsule5.getBlockId())
            .contains(blockCapsule4.getBlockId())
    );
    Assert.assertTrue(
        dbManager.getBlockChainHashesOnFork(blockCapsule5.getBlockId())
            .contains(blockCapsule5.getBlockId())
    );

    dbManager.initHeadBlock(Sha256Hash.wrap(ByteArray
        .fromHexString(blockCapsule5.getBlockId().toString())));
    Assert.assertEquals(blockCapsule5.getBlockId(), dbManager.getHeadBlockId());
  }

  @Test
  public void testGetGenesisBlockId() {
    Assert.assertEquals("9d5daf1c368d84fe2731de78d3d073a8668893a68c3d989490eb745eaef9529c",
        dbManager.getGenesisBlockId().toString());
    logger.info("getGenesisBlock={}", dbManager.getGenesisBlock());
  }

  @Test
  public void testGenerateBlock() {
    byte[] prikey = new byte[]{1, 2, 3, 4, 5};

    WitnessCapsule witnessCapsulef = new WitnessCapsule(
        ByteString.copyFrom(ByteArray.fromHexString("0x0011")), "www.tron.net/first");
    witnessCapsulef.setIsJobs(true);
    try {
      BlockCapsule blockCapsule = dbManager.generateBlock(witnessCapsulef, 0, prikey);
      Assert.assertEquals(blockCapsule, dbManager.getBlockById(blockCapsule.getBlockId()));
    } catch (ValidateSignatureException e) {
      Assert.assertTrue("generateBlock ValidateSignatureException", false);
    } catch (ContractValidateException e) {
      Assert.assertTrue("generateBlock ContractValidateException", false);
    } catch (ContractExeException e) {
      Assert.assertTrue("generateBlock ContractExeException", false);
    }
  }
}
