package org.tron.core.consensus;

import static org.mockito.Mockito.mock;

import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.parameter.CommonParameter;
import org.tron.consensus.ConsensusDelegate;
import org.tron.consensus.dpos.DposService;
import org.tron.consensus.dpos.DposSlot;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.p2p.utils.NetUtil;
import org.tron.protos.Protocol;

public class DposServiceTest {
  DposService service = new DposService();


  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  @Test
  public void testValidBlockTime() throws Exception {
    long headTime = 1724036757000L;

    ConsensusDelegate consensusDelegate = mock(ConsensusDelegate.class);
    Field field = service.getClass().getDeclaredField("consensusDelegate");
    field.setAccessible(true);
    field.set(service, consensusDelegate);

    DynamicPropertiesStore store = mock(DynamicPropertiesStore.class);
    Mockito.when(consensusDelegate.getDynamicPropertiesStore()).thenReturn(store);

    Mockito.when(consensusDelegate.getLatestBlockHeaderNumber()).thenReturn(0L);
    boolean f = service.validBlock(null);
    Assert.assertTrue(f);

    Protocol.BlockHeader.raw raw = Protocol.BlockHeader.raw.newBuilder()
        .setTimestamp(headTime + 3001).build();
    Protocol.BlockHeader header = Protocol.BlockHeader.newBuilder().setRawData(raw).build();
    Protocol.Block block = Protocol.Block.newBuilder().setBlockHeader(header).build();

    Mockito.when(consensusDelegate.getLatestBlockHeaderNumber()).thenReturn(100L);
    Mockito.when(store.allowConsensusLogicOptimization()).thenReturn(true);

    Mockito.when(consensusDelegate.getLatestBlockHeaderTimestamp()).thenReturn(headTime + 3000);
    f = service.validBlock(new BlockCapsule(block));
    Assert.assertTrue(!f);

  }

  @Test
  public void testValidSlot() throws Exception {
    Args.setParam(new String[] {}, Constant.TEST_CONF);
    long headTime = 1724036757000L;
    ByteString witness = ByteString.copyFrom(NetUtil.getNodeId());
    ByteString witness2 = ByteString.copyFrom(NetUtil.getNodeId());

    ConsensusDelegate consensusDelegate = mock(ConsensusDelegate.class);
    Field field = service.getClass().getDeclaredField("consensusDelegate");
    field.setAccessible(true);
    field.set(service, consensusDelegate);

    DposSlot dposSlot = mock(DposSlot.class);
    field = service.getClass().getDeclaredField("dposSlot");
    field.setAccessible(true);
    field.set(service, dposSlot);

    Mockito.when(dposSlot.getAbSlot(headTime)).thenReturn(headTime / 3000);
    Mockito.when(dposSlot.getAbSlot(headTime + 3000)).thenReturn((headTime + 3000) / 3000);

    DynamicPropertiesStore store = mock(DynamicPropertiesStore.class);
    Mockito.when(consensusDelegate.getDynamicPropertiesStore()).thenReturn(store);

    Mockito.when(consensusDelegate.getLatestBlockHeaderNumber()).thenReturn(0L);
    boolean f = service.validBlock(null);
    Assert.assertTrue(f);

    Mockito.when(consensusDelegate.getLatestBlockHeaderNumber()).thenReturn(100L);

    Protocol.BlockHeader.raw raw = Protocol.BlockHeader.raw.newBuilder()
        .setTimestamp(headTime + 3000)
        .setWitnessAddress(witness).build();
    Protocol.BlockHeader header = Protocol.BlockHeader.newBuilder().setRawData(raw).build();
    Protocol.Block block = Protocol.Block.newBuilder().setBlockHeader(header).build();

    Mockito.when(consensusDelegate.getLatestBlockHeaderTimestamp()).thenReturn(headTime + 3000);
    f = service.validBlock(new BlockCapsule(block));
    Assert.assertTrue(!f);

    Mockito.when(consensusDelegate.getLatestBlockHeaderTimestamp()).thenReturn(headTime);

    Mockito.when(dposSlot.getSlot(headTime + 3000)).thenReturn(0L);

    Mockito.when(dposSlot.getScheduledWitness(0L)).thenReturn(witness2);
    f = service.validBlock(new BlockCapsule(block));
    Assert.assertTrue(!f);

    Mockito.when(dposSlot.getScheduledWitness(0L)).thenReturn(witness);
    f = service.validBlock(new BlockCapsule(block));
    Assert.assertTrue(f);

    Mockito.when(store.allowConsensusLogicOptimization()).thenReturn(true);
    f = service.validBlock(new BlockCapsule(block));
    Assert.assertTrue(!f);
  }

}