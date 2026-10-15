package org.tron.core.db;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Pair;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.BlockHeader.raw;

public class KhaosDatabaseTest extends BaseTest {

  @Resource
  private KhaosDatabase khaosDatabase;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
  }


  @Test
  public void testStartBlock() {
    BlockCapsule blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))))).build());
    khaosDatabase.start(blockCapsule);

    Assert.assertEquals(blockCapsule, khaosDatabase.getBlock(blockCapsule.getBlockId()));
  }

  @Test
  public void testPushGetBlock() throws UnLinkedBlockException, BadNumberBlockException {
    BlockCapsule blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))))).build());
    BlockCapsule blockCapsule2 = new BlockCapsule(
        1, blockCapsule.getBlockId(), 1, ByteString.EMPTY);
    khaosDatabase.start(blockCapsule);
    khaosDatabase.push(blockCapsule2);

    Assert.assertEquals(blockCapsule2, khaosDatabase.getBlock(blockCapsule2.getBlockId()));
    Assert.assertTrue("contain is error",
        khaosDatabase.containBlockInMiniStore(blockCapsule2.getBlockId()));

    khaosDatabase.removeBlk(blockCapsule2.getBlockId());

    Assert.assertNull("removeBlk is error", khaosDatabase.getBlock(blockCapsule2.getBlockId()));
  }

  @Test
  public void testUnlinkedBlockIsNotRetained() throws Exception {
    BlockCapsule root = new BlockCapsule(100, Sha256Hash.ZERO_HASH, 100, ByteString.EMPTY);
    BlockCapsule parent = new BlockCapsule(101, root.getBlockId(), 101, ByteString.EMPTY);
    BlockCapsule child = new BlockCapsule(102, parent.getBlockId(), 102, ByteString.EMPTY);
    khaosDatabase.start(root);

    for (int i = 0; i < 2; i++) {
      Assert.assertThrows(UnLinkedBlockException.class, () -> khaosDatabase.push(child));
      Assert.assertNull(khaosDatabase.getBlock(child.getBlockId()));
      Assert.assertFalse(khaosDatabase.containBlockInMiniStore(child.getBlockId()));
      Assert.assertNull(khaosDatabase.getMiniStore().getByHash(child.getBlockId()));
      Assert.assertSame(root, khaosDatabase.getHead());
    }

    khaosDatabase.push(parent);
    Assert.assertNull(khaosDatabase.getBlock(child.getBlockId()));
    Assert.assertSame(child, khaosDatabase.push(child));
    Assert.assertSame(child, khaosDatabase.getBlock(child.getBlockId()));
    Assert.assertSame(parent,
        khaosDatabase.getMiniStore().getByHash(child.getBlockId()).getParent().getBlk());

    BlockCapsule fork = new BlockCapsule(101, root.getBlockId(), 103, ByteString.EMPTY);
    khaosDatabase.push(fork);
    Pair<List<KhaosDatabase.KhaosBlock>, List<KhaosDatabase.KhaosBlock>> expected = new Pair<>(
        Lists.newArrayList(new KhaosDatabase.KhaosBlock(child),
            new KhaosDatabase.KhaosBlock(parent)),
        Lists.newArrayList(new KhaosDatabase.KhaosBlock(fork)));
    Pair<?, ?> branches = khaosDatabase.getBranch(child.getBlockId(), fork.getBlockId());
    Assert.assertEquals(expected.getKey(), branches.getKey());
    Assert.assertEquals(expected.getValue(), branches.getValue());
  }


  @Test
  public void checkWeakReference() throws UnLinkedBlockException, BadNumberBlockException {
    BlockCapsule blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b82")))
            .setNumber(0))).build());
    BlockCapsule blockCapsule2 = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder()
            .setParentHash(ByteString.copyFrom(blockCapsule.getBlockId().getBytes())).setNumber(1)))
        .build());
    Assert.assertEquals(blockCapsule.getBlockId(), blockCapsule2.getParentHash());

    khaosDatabase.start(blockCapsule);
    khaosDatabase.push(blockCapsule2);

    Reference<KhaosDatabase.KhaosBlock> parentReference =
        new WeakReference<>(khaosDatabase.getMiniStore().getByHash(blockCapsule.getBlockId()));
    khaosDatabase.removeBlk(blockCapsule.getBlockId());
    Assert.assertNull(khaosDatabase.getMiniStore().getByHash(blockCapsule.getBlockId()));
    blockCapsule = null;
    System.gc();
    Assert.assertNull(parentReference.get());
    Assert.assertNull(
        khaosDatabase.getMiniStore().getByHash(blockCapsule2.getBlockId()).getParent());
  }

  @Test
  public void testGetBranch()
      throws UnLinkedBlockException, BadNumberBlockException, NonCommonBlockException {
    final String mockedHash = "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b82";
    // common parent block
    BlockCapsule parentBlock = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray.fromHexString(mockedHash)))
            .setNumber(0))).build());
    // fork-chain-A
    // longer than chainB, share the common parent block with fork-chain-B
    BlockCapsule block1OnforkA = new BlockCapsule(
        1, parentBlock.getBlockId(), 0, ByteString.EMPTY);
    BlockCapsule block2OnforkA = new BlockCapsule(
        2, block1OnforkA.getBlockId(), 0, ByteString.EMPTY);
    List<KhaosDatabase.KhaosBlock> forkA = Lists.newLinkedList();
    forkA.add(new KhaosDatabase.KhaosBlock(block2OnforkA));
    forkA.add(new KhaosDatabase.KhaosBlock(block1OnforkA));
    // fork-chain-B
    BlockCapsule block1OnforkB = new BlockCapsule(
        1, parentBlock.getBlockId(), 1, ByteString.EMPTY);
    List<KhaosDatabase.KhaosBlock> forkB = Lists.newLinkedList();
    forkB.add(new KhaosDatabase.KhaosBlock(block1OnforkB));

    khaosDatabase.start(parentBlock);
    khaosDatabase.push(block1OnforkA);
    khaosDatabase.push(block2OnforkA);
    khaosDatabase.push(block1OnforkB);
    // case: block num of param1 > block num of param2
    Pair<?, ?> result1 = khaosDatabase.getBranch(
        block2OnforkA.getBlockId(), block1OnforkB.getBlockId());
    Assert.assertEquals(forkA, result1.getKey());
    Assert.assertEquals(forkB, result1.getValue());
    // case: block num of param2 > block num of param1
    Pair<?, ?> result2 = khaosDatabase.getBranch(
        block1OnforkB.getBlockId(), block2OnforkA.getBlockId());
    Assert.assertEquals(forkB, result2.getKey());
    Assert.assertEquals(forkA, result2.getValue());
  }
}
