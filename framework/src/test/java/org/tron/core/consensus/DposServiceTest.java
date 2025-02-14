package org.tron.core.consensus;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.consensus.ConsensusDelegate;
import org.tron.consensus.dpos.DposService;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol;

public class DposServiceTest {
  DposService service = new DposService();

  @Test
  public void test() throws Exception {
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
}