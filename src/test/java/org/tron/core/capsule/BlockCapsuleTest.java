package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;

@Slf4j
public class BlockCapsuleTest {

  private static String dbPath = "block_capsule_test_database";
  private BlockCapsule blockCapsule;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath}, Configuration.getByPath(Constant.TEST_CONF));
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Before
  public void setup() {
    this.blockCapsule = createBlockCapsule();
  }

  @Test
  public void calculateMerkleRoot_ok() {
    blockCapsule.setMerkleRoot();
    Assert.assertEquals(Sha256Hash.wrap(Sha256Hash.ZERO_HASH.getByteString()).toString(),
        blockCapsule.getMerkleRoot().toString());

    logger.info("Transaction[X] Merkle Root : {}", blockCapsule.getMerkleRoot().toString());

    TransactionCapsule transactionCapsule1 = new TransactionCapsule("123", 1L);
    TransactionCapsule transactionCapsule2 = new TransactionCapsule("124", 2L);
    blockCapsule.addTransaction(transactionCapsule1.getInstance());
    blockCapsule.addTransaction(transactionCapsule2.getInstance());
    blockCapsule.setMerkleRoot();

    Assert.assertEquals("fbf357d2f8c5db313e87bf0cb67dc69db4e11aef31bdfe6c2faa4519d91372a1",
        blockCapsule.getMerkleRoot().toString());

    logger.info("Transaction[O] Merkle Root : {}", blockCapsule.getMerkleRoot().toString());
  }

  @Test
  public void addTransaction_ok() {
    TransactionCapsule transactionCapsule = new TransactionCapsule("123", 1L);
    blockCapsule.addTransaction(transactionCapsule.getInstance());

    Assert.assertArrayEquals(blockCapsule.getTransactions().get(0).getHash().getBytes(),
        transactionCapsule.getHash().getBytes());
    Assert.assertEquals(transactionCapsule.getInstance().getRawData().getVout(0).getValue(),
        blockCapsule.getTransactions().get(0).getInstance().getRawData().getVout(0).getValue());
  }

  @Test
  public void createBlockFromByteArray_ok() {
    byte[] data = blockCapsule.getData();
    BlockCapsule newBlockCapsule = new BlockCapsule(data);
    Assert.assertEquals(blockCapsule.getBlockId(), newBlockCapsule.getBlockId());
    Assert.assertEquals(blockCapsule.getNumber(), newBlockCapsule.getNumber());
    Assert.assertEquals(blockCapsule.getParentHash(), newBlockCapsule.getParentHash());
    Assert.assertEquals(blockCapsule.isGeneratedByMyself(),
        newBlockCapsule.isGeneratedByMyself());
  }

  @Test
  public void getNumber_ok() {
    Assert.assertEquals(1,
        blockCapsule.getInstance().getBlockHeader().getRawData().getNumber());
  }

  @Test
  public void wrapParentHash_ok() {
    Assert.assertEquals(blockCapsule.getHashedParentHash(),
        Sha256Hash.wrap(blockCapsule.getParentHash()));
  }

  @Test
  public void getTimestamp_ok() {
    Assert.assertEquals(1234L, blockCapsule.getTimestamp());
  }

  private static BlockCapsule createBlockCapsule() {
    return new BlockCapsule(1, ByteString
        .copyFrom(ByteArray
            .fromHexString("9938a342238077182498b464ac0292229938a342238077182498b464ac029222")),
        1234,
        ByteString.copyFrom("1234567".getBytes()));
  }

}
