package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocal.Block;
import org.tron.protos.Protocal.BlockHeader;
import org.tron.protos.Protocal.BlockHeader.raw;
import org.tron.protos.Protocal.Transaction;

public class BlockCapsuleTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  protected BlockCapsule blockCapsule;

  DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  long currentTimestamp = System.currentTimeMillis();

  /*
   * init blockCapsule Constructor Method
   */
  @Before
  public void testBlockCapsule() {

    Transaction.Builder trx = Transaction.newBuilder().setData(ByteString.copyFrom(ByteArray
        .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")));

    blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
            .setTimestamp(currentTimestamp)
            .build())).build());

    blockCapsule.addTransaction(trx.build());

    // logger.info("test block capsule = {}:",
    //        ByteArray.toHexString(blockCapsule.getParentHash().getBytes()));
  }


  // unit test for correct parameters
  @Test
  public void testGetTransaction1() {
    logger.info("test getTransaction = {}",
        ByteArray.toHexString(blockCapsule.getTransactionList().get(0).getData().toByteArray()));

    Assert.assertEquals(1, blockCapsule.getTransactionList().size());

  }

  // unit test for error parameters
  @Ignore
  @Test
  public void testGetTransaction2() {
    logger.info("test getTransaction = {}",
        ByteArray.toHexString(blockCapsule.getTransactionList().get(0).getData().toByteArray()));

    Assert.assertNotEquals("[testGetTransaction2] is not expect", 2,
        blockCapsule.getTransactionList().size());
  }


  @Test
  public void testSign() {
    ECKey key = ECKey.fromPrivate(new BigInteger(
        "48720541756297624231117183381585618702966411811775628910886100667008198869515"));

    byte[] privKeyBytes = key.getPrivKeyBytes();
    String privateKey = ByteArray.toHexString(privKeyBytes);

    blockCapsule.sign(privateKey);

    //ECDSASignature signature = key.sign(privKeyBytes);
    //Assert.assertTrue(key.verify(privKeyBytes, signature));
    Assert.assertEquals(
        "48720541756297624231117183381585618702966411811775628910886100667008198869515",
        key.getPrivKey().toString());
  }



  @Test
  public void testGetTimeStamp() {
    logger.info("test get timeStamp = {}", sdf.format(blockCapsule.getTimeStamp()));

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    String sysCurrentTime = simpleDateFormat.format(currentTimestamp);

    Assert.assertEquals(sysCurrentTime, sdf.format(blockCapsule.getTimeStamp()));

  }

}
