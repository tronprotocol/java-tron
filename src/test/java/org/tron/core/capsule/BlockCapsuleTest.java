package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocal.Block;
import org.tron.protos.Protocal.BlockHeader;
import org.tron.protos.Protocal.BlockHeader.raw;

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
    blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
            .setTimestamp(currentTimestamp)
            .build())).build());
  }

  @Test
  public void testSign() {
    ECKey key = ECKey.fromPrivate(new BigInteger(
        "48720541756297624231117183381585618702966411811775628910886100667008198869515"));

    byte[] privKeyBytes = key.getPrivKeyBytes();

    blockCapsule.sign(privKeyBytes);

    Assert.assertTrue(blockCapsule.verifySign(privKeyBytes));
  }

  @Test
  public void testGetTimeStamp() {
    logger.info("test get timeStamp = {}", sdf.format(blockCapsule.getTimeStamp()));

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    String sysCurrentTime = simpleDateFormat.format(currentTimestamp);

    Assert.assertEquals(sysCurrentTime, sdf.format(blockCapsule.getTimeStamp()));

  }

}
