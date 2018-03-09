package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ValidateSignatureException;


public class ManagerTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  private static Manager dbManager = new Manager();
  private static BlockCapsule blockCapsule2;
  //private static KhaosDatabase khaosDatabase;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", "output_manager"},
        Configuration.getByPath(Constant.TEST_CONF));

    dbManager.init();
    blockCapsule2 = new BlockCapsule(0, ByteString.copyFrom(ByteArray
        .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")),
        0,
        ByteString.copyFrom(
            ECKey.fromPrivate(ByteArray.fromHexString(Args.getInstance().getPrivateKey()))
                .getAddress()));
    blockCapsule2.setMerklerRoot();
    blockCapsule2.sign(Args.getInstance().getPrivateKey().getBytes());
    //khaosDatabase = dbManager.getKhaosDb();
    //khaosDatabase.push(blockCapsule2);
  }

  @Test
  public void pushBlock() {
    try {
      dbManager.pushBlock(blockCapsule2);
    } catch (Exception e) {
      Assert.assertNotNull(e);
      Assert.assertTrue(e instanceof ValidateSignatureException);
    }
    dbManager.getBlockById(dbManager.getBlockIdByNum(0)).getParentHash();
//    Assert.assertTrue(dbManager.containBlock(Sha256Hash.wrap(ByteArray.fromHexString("c37fea1dec8048180911c6cf075348f93a524336c47e97317eb59b91bd485ca4"))));
    Assert.assertTrue(dbManager.containBlock(Sha256Hash.wrap(ByteArray.fromHexString("b77ad0695b94e4f96e5927260cf18b20067159a6a5f6e62c193b35f9443a5237"))));
//    Assert.assertEquals("c37fea1dec8048180911c6cf075348f93a524336c47e97317eb59b91bd485ca4", dbManager.getBlockIdByNum(1).toString());
    Assert.assertEquals("9d5daf1c368d84fe2731de78d3d073a8668893a68c3d989490eb745eaef9529c", dbManager.getBlockIdByNum(1).toString());
    //Assert.assertEquals("[c37fea1dec8048180911c6cf075348f93a524336c47e97317eb59b91bd485ca4]",
    //dbManager.getBlockChainHashesOnFork(khaosDatabase.getHead().getBlockId()).toString());
    Assert.assertTrue(dbManager.hasBlocks());

//    dbManager.deleteBlock(Sha256Hash.wrap(ByteArray.fromHexString("c37fea1dec8048180911c6cf075348f93a524336c47e97317eb59b91bd485ca4")));
    dbManager.deleteBlock(Sha256Hash.wrap(ByteArray.fromHexString("b77ad0695b94e4f96e5927260cf18b20067159a6a5f6e62c193b35f9443a5237")));
//    Assert.assertFalse(dbManager.containBlock(Sha256Hash.wrap(ByteArray.fromHexString("c37fea1dec8048180911c6cf075348f93a52417eb59b91bd485ca4")));
    Assert.assertFalse(dbManager.containBlock(Sha256Hash.wrap(ByteArray.fromHexString("c37fea1dec8048180911c6cf075348f93a524336c47e97317eb59b91bd485ca4"))));
  }

  @Test
  public void testPushTransactions() {
    TransactionCapsule transactionCapsule = new TransactionCapsule(
        "2c0937534dd1b3832d05d865e8e6f2bf23218300b33a992740d45ccab7d4f519", 123);
    try {
      dbManager.pushTransactions(transactionCapsule);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Assert
        .assertEquals(123, transactionCapsule.getInstance().getRawData().getVout(0).getValue());
  }
}