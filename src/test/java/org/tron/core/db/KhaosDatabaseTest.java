package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.BlockHeader.raw;

public class KhaosDatabaseTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  private static KhaosDatabase khaosDatabase;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{}, Configuration.getByPath(Constant.TEST_CONF));
    khaosDatabase = new KhaosDatabase("test_khaos");
  }

  @Test
  public void testStartBlock() {
    BlockCapsule blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
        )).build());
    khaosDatabase.start(blockCapsule);

    Assert.assertEquals(blockCapsule, khaosDatabase.getBlock(blockCapsule.getBlockId()));
  }

  @Test
  public void testPushGetBlock() {
    BlockCapsule blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
        )).build());
    BlockCapsule blockCapsule2 = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("9938a342238077182498b464ac029222ae169360e540d1fd6aee7c2ae9575a06")))
        )).build());
    khaosDatabase.start(blockCapsule);
    khaosDatabase.push(blockCapsule2);

    Assert.assertEquals(blockCapsule2, khaosDatabase.getBlock(blockCapsule2.getBlockId()));
    Assert.assertTrue("conatain is error", khaosDatabase.containBlock(blockCapsule2.getBlockId()));

    khaosDatabase.removeBlk(blockCapsule2.getBlockId());

    Assert.assertNull("removeBlk is error", khaosDatabase.getBlock(blockCapsule2.getBlockId()));
  }

  @Test
  public void testGetBranch() {
    BlockCapsule blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
        )).build());
    BlockCapsule blockCapsule2 = new BlockCapsule(2, ByteString
        .copyFrom(blockCapsule.getBlockId().getBytes()), 1,
        ByteString.copyFrom("1234567".getBytes()));
    BlockCapsule blockCapsule3 = new BlockCapsule(2, ByteString
        .copyFrom(blockCapsule.getBlockId().getBytes()), 1,
        ByteString.copyFrom("6789".getBytes()));
    BlockCapsule blockCapsule2Next = new BlockCapsule(3, ByteString
        .copyFrom(blockCapsule2.getBlockId().getBytes()), 1,
        ByteString.copyFrom("211111".getBytes()));
    khaosDatabase.push(blockCapsule);
    khaosDatabase.push(blockCapsule2);
    khaosDatabase.push(blockCapsule3);
    khaosDatabase.push(blockCapsule2Next);

    int key1 = khaosDatabase.getBranch(blockCapsule2Next.getBlockId(), blockCapsule3.getBlockId())
        .getKey().size();

    Assert.assertEquals("getBranch is error", blockCapsule2Next, khaosDatabase
        .getBranch(blockCapsule2Next.getBlockId(), blockCapsule3.getBlockId())
        .getKey().get(key1 - 2));
    Assert.assertEquals("getBranch is error", blockCapsule2, khaosDatabase
        .getBranch(blockCapsule2Next.getBlockId(), blockCapsule3.getBlockId())
        .getKey().get(key1 - 1));

    int value1 = khaosDatabase.getBranch(blockCapsule2Next.getBlockId(), blockCapsule3.getBlockId())
        .getValue().size();

    Assert.assertEquals("getBranch is error", blockCapsule3, khaosDatabase
        .getBranch(blockCapsule2Next.getBlockId(), blockCapsule3.getBlockId())
        .getValue().get(value1 - 1));
  }


}