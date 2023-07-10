package org.tron.core;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ForkController;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.Parameter;
import org.tron.core.config.args.Args;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol;

public class ForkControllerTest {
  private static ChainBaseManager chainBaseManager;
  private static DynamicPropertiesStore dynamicPropertiesStore;
  private static final ForkController forkController = ForkController.instance();
  private static TronApplicationContext context;
  private static final String dbPath = "output_fork_test";
  private static long ENERGY_LIMIT_BLOCK_NUM = 4727890L;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    dynamicPropertiesStore = context.getBean(DynamicPropertiesStore.class);
    chainBaseManager = context.getBean(ChainBaseManager.class);
    forkController.init(chainBaseManager);
  }

  @Test
  public void testPass() {
    boolean flag = forkController.pass(Parameter.ForkBlockVersionEnum.ENERGY_LIMIT);
    Assert.assertFalse(flag);

    dynamicPropertiesStore.saveLatestBlockHeaderNumber(ENERGY_LIMIT_BLOCK_NUM);
    flag = forkController.pass(Parameter.ForkBlockVersionEnum.ENERGY_LIMIT);
    Assert.assertTrue(flag);

    flag = forkController.pass(Parameter.ForkBlockVersionEnum.VERSION_3_5);
    Assert.assertFalse(flag);

    byte[] stats = new byte[3];
    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_3_5.getValue(), stats);
    flag = forkController.pass(Parameter.ForkBlockVersionEnum.VERSION_3_5);
    Assert.assertFalse(flag);

    stats[0] = 1;
    stats[1] = 1;
    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_3_5.getValue(), stats);
    flag = forkController.pass(Parameter.ForkBlockVersionEnum.VERSION_3_5);
    Assert.assertFalse(flag);

    stats[2] = 1;
    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_3_5.getValue(), stats);
    flag = forkController.pass(Parameter.ForkBlockVersionEnum.VERSION_3_5);
    Assert.assertTrue(flag);

    stats = new byte[5];
    flag = forkController.pass(Parameter.ForkBlockVersionEnum.VERSION_4_4);
    Assert.assertFalse(flag);

    stats[0] = 1;
    stats[1] = 1;
    stats[2] = 1;
    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_4.getValue(), stats);
    flag = forkController.pass(Parameter.ForkBlockVersionEnum.VERSION_4_4);
    Assert.assertFalse(flag);

    stats[3] = 1;
    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_4.getValue(), stats);
    flag = forkController.pass(Parameter.ForkBlockVersionEnum.VERSION_4_4);
    Assert.assertFalse(flag);

    dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(1596780000000L);
    flag = forkController.pass(Parameter.ForkBlockVersionEnum.VERSION_4_4);
    Assert.assertTrue(flag);
  }

  @Test
  public void testReset() {
    List<ByteString> list = new ArrayList<>();
    list.add(ByteString.copyFrom(getBytes(0)));
    list.add(ByteString.copyFrom(getBytes(0)));
    list.add(ByteString.copyFrom(getBytes(0)));
    list.add(ByteString.copyFrom(getBytes(0)));
    list.add(ByteString.copyFrom(getBytes(0)));

    chainBaseManager.getWitnessScheduleStore().saveActiveWitnesses(list);

    byte[] stats1 = {1, 1, 1, 1, 1};
    byte[] stats2 = {1, 1, 1, 1, 0};
    byte[] stats3 = {1, 1, 1, 0, 0};
    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_3_5.getValue(), stats1);
    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_3_6.getValue(), stats2);
    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_4.getValue(), stats2);
    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_5.getValue(), stats3);

    dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(1596780000000L);
    forkController.reset();

    byte[] bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_3_5.getValue());
    Assert.assertEquals(getSum(bytes), 5);

    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_3_6.getValue());
    Assert.assertEquals(getSum(bytes), 0);

    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_4.getValue());
    Assert.assertEquals(getSum(bytes), 4);

    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_5.getValue());
    Assert.assertEquals(getSum(bytes), 0);
    list.add(ByteString.copyFrom(new byte[32]));
    chainBaseManager.getWitnessScheduleStore().saveActiveWitnesses(list);
    forkController.reset();
    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_4.getValue());
    Assert.assertEquals(bytes.length, 5);
    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_5.getValue());
    Assert.assertEquals(bytes.length, 6);
  }

  @Test
  public void testUpdate() {
    List<ByteString> list = new ArrayList<>();
    list.add(ByteString.copyFrom(getBytes(1)));
    list.add(ByteString.copyFrom(getBytes(2)));
    list.add(ByteString.copyFrom(getBytes(3)));
    list.add(ByteString.copyFrom(getBytes(4)));
    list.add(ByteString.copyFrom(getBytes(5)));

    chainBaseManager.getWitnessScheduleStore().saveActiveWitnesses(list);

    byte[] stats1 = {1, 1, 1, 1, 1};
    byte[] stats2 = {1, 1, 1, 1, 0};
    byte[] stats3 = {1, 1, 1, 0, 0};

    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_3_5.getValue(), stats1);
    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_3_6.getValue(), stats2);
    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_4.getValue(), stats2);
    dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_5.getValue(), stats3);

    BlockCapsule blockCapsule = getBlock(1, Parameter.ForkBlockVersionEnum.VERSION_3_5);

    forkController.update(blockCapsule);

    byte[] bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_3_6.getValue());
    Assert.assertEquals(0, bytes[0]);
    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_4.getValue());
    Assert.assertEquals(0, bytes[0]);
    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_5.getValue());
    Assert.assertEquals(0, bytes[0]);

    blockCapsule = getBlock(1, Parameter.ForkBlockVersionEnum.VERSION_4_5);
    forkController.update(blockCapsule);
    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_3_6.getValue());
    Assert.assertEquals(0, bytes[0]);
    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_4.getValue());
    Assert.assertEquals(0, bytes[0]);
    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_5.getValue());
    Assert.assertEquals(1, bytes[0]);

    blockCapsule = getBlock(4, Parameter.ForkBlockVersionEnum.VERSION_4_5);
    forkController.update(blockCapsule);
    blockCapsule = getBlock(5, Parameter.ForkBlockVersionEnum.VERSION_4_5);

    dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(1596780000000L);
    forkController.update(blockCapsule);

    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_3_6.getValue());
    Assert.assertEquals(getSum(bytes), 5);
    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_3.getValue());
    Assert.assertEquals(getSum(bytes), 5);
    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_4.getValue());
    Assert.assertEquals(getSum(bytes), 5);
    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_5.getValue());
    Assert.assertEquals(getSum(bytes), 4);

    blockCapsule = getBlock(1, Parameter.ForkBlockVersionEnum.VERSION_4_3);
    forkController.update(blockCapsule);
    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_4.getValue());
    Assert.assertEquals(getSum(bytes), 5);
    bytes = dynamicPropertiesStore
        .statsByVersion(Parameter.ForkBlockVersionEnum.VERSION_4_5.getValue());
    Assert.assertEquals(getSum(bytes), 4);
  }

  private BlockCapsule getBlock(int i, Parameter.ForkBlockVersionEnum versionEnum) {
    org.tron.protos.Protocol.BlockHeader.raw rawData =
        org.tron.protos.Protocol.BlockHeader.raw.newBuilder()
            .setVersion(versionEnum.getValue())
            .setWitnessAddress(ByteString.copyFrom(getBytes(i)))
            .build();

    Protocol.BlockHeader blockHeader = Protocol.BlockHeader.newBuilder()
        .setRawData(rawData).build();

    Protocol.Block block = Protocol.Block.newBuilder().setBlockHeader(blockHeader).build();

    return new BlockCapsule(block);
  }

  private int getSum(byte[] bytes) {
    int sum = 0;
    for (byte aByte : bytes) {
      sum += aByte;
    }
    return sum;
  }

  private byte[] getBytes(int i) {
    byte[] bytes = new byte[21];
    bytes[i] = 1;
    return bytes;
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

}
