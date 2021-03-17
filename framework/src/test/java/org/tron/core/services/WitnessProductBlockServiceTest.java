package org.tron.core.services;

import com.google.protobuf.ByteString;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;

public class WitnessProductBlockServiceTest {

  @Test
  public void GetSetCheatWitnessInfoTest() {
    WitnessProductBlockService.CheatWitnessInfo cheatWitnessInfo =
        new WitnessProductBlockService.CheatWitnessInfo();
    long time = System.currentTimeMillis();
    cheatWitnessInfo.setTime(time);
    Assert.assertEquals(time, cheatWitnessInfo.getTime());
    long latestBlockNum = 100L;
    cheatWitnessInfo.setLatestBlockNum(latestBlockNum);
    Assert.assertEquals(latestBlockNum, cheatWitnessInfo.getLatestBlockNum());
    Set<BlockCapsule> blockCapsuleSet = new HashSet<BlockCapsule>();
    cheatWitnessInfo.setBlockCapsuleSet(blockCapsuleSet);
    Assert.assertEquals(blockCapsuleSet, cheatWitnessInfo.getBlockCapsuleSet());
    BlockCapsule blockCapsule1 = new BlockCapsule(
        1,
        Sha256Hash.wrap(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
        1,
        ByteString.copyFromUtf8("testAddress"));
    // check after add one block
    cheatWitnessInfo.add(blockCapsule1);
    blockCapsuleSet.add(blockCapsule1);
    Assert.assertEquals(blockCapsuleSet, cheatWitnessInfo.getBlockCapsuleSet());
    cheatWitnessInfo.clear();
    blockCapsuleSet.clear();
    Assert.assertEquals(blockCapsuleSet, cheatWitnessInfo.getBlockCapsuleSet());
    // times increment check
    AtomicInteger times = new AtomicInteger(0);
    cheatWitnessInfo.setTimes(times);
    Assert.assertEquals(times, cheatWitnessInfo.getTimes());
    cheatWitnessInfo.increment();
    times.incrementAndGet();
    Assert.assertEquals(times, cheatWitnessInfo.getTimes());

    Assert.assertEquals("{"
        + "times=" + times.get()
        + ", time=" + time
        + ", latestBlockNum=" + latestBlockNum
        + ", blockCapsuleSet=" + blockCapsuleSet
        + '}', cheatWitnessInfo.toString());
  }

  @Test
  public void validWitnessProductTwoBlockTest() {
    WitnessProductBlockService witnessProductBlockService = new WitnessProductBlockService();
    BlockCapsule blockCapsule1 = new BlockCapsule(
        1,
        Sha256Hash.wrap(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
        1,
        ByteString.copyFromUtf8("testAddress"));
    witnessProductBlockService.validWitnessProductTwoBlock(blockCapsule1);
    Assert.assertEquals(0, witnessProductBlockService.queryCheatWitnessInfo().size());
    // different hash, same time and number
    BlockCapsule blockCapsule2 = new BlockCapsule(
        1,
        Sha256Hash.wrap(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b82"))),
        1,
        ByteString.copyFromUtf8("testAddress"));

    witnessProductBlockService.validWitnessProductTwoBlock(blockCapsule2);
    String key = ByteArray.toHexString(blockCapsule2.getWitnessAddress().toByteArray());
    WitnessProductBlockService.CheatWitnessInfo block =
        witnessProductBlockService.queryCheatWitnessInfo().get(key);
    Assert.assertEquals(1, witnessProductBlockService.queryCheatWitnessInfo().size());
    Assert.assertEquals(2, block.getBlockCapsuleSet().size());
    Assert.assertEquals(blockCapsule2.getNum(), block.getLatestBlockNum());

    Assert.assertEquals(block.getBlockCapsuleSet().contains(blockCapsule2), true);

    Iterator<BlockCapsule> iterator = block.getBlockCapsuleSet()
        .iterator();
    boolean isInner = false;
    while (iterator.hasNext()) {
      BlockCapsule blockCapsule = iterator.next();
      blockCapsule.getBlockId();
      if (blockCapsule.getBlockId().equals(blockCapsule1.getBlockId())) {
        isInner = true;
      }
    }
    Assert.assertTrue(isInner);
  }
}
