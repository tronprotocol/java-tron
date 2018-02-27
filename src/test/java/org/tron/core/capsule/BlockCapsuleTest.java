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

  @Before
  public void testBlockCapsule() {

    blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
            .setNumber(1).setTimestamp(System.currentTimeMillis())
            .build())).build());

//    logger.info("test block capsule = {}:" ,
//        ByteArray.toHexString(blockCapsule.getParentHash().getBytes()));
  }

  @Test
  public void testSign() {
    ECKey key = ECKey.fromPrivate(new BigInteger(
        "48720541756297624231117183381585618702966411811775628910886100667008198869515"));

    byte[] privKeyBytes = key.getPrivKeyBytes();
    String privateKey = ByteArray.toHexString(privKeyBytes);

    blockCapsule.sign(privateKey);

    logger.info("test sign = {}", ByteArray.toHexString(blockCapsule.getParentHash().getBytes()));

    Assert.assertEquals("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81",
        ByteArray.toHexString(blockCapsule.getParentHash().getBytes()));

  }

  @Test
  public void testGetParentHash() {
    logger.info("test get parent hash = {}", blockCapsule.getParentHash());
  }

  @Test
  public void testGetNumber() {
    logger.info("test get number = {}", blockCapsule.getNum());
  }

  @Test
  public void testGetTimeStamp() {
    logger.info("test get timeStamp = {}", sdf.format(blockCapsule.getTimeStamp()));
  }

  @Test
  public void testGetBlockId() {
    logger.info("test getBlockId = {}", blockCapsule.getBlockId());
  }


}
