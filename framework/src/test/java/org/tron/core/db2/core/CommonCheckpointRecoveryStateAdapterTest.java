package org.tron.core.db2.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.DynamicPropertiesStore;

public class CommonCheckpointRecoveryStateAdapterTest {

  @Test
  public void usesPersistentDynamicRootAndReturnsFullBlockMeta() throws Exception {
    long number = 73L;
    BlockId id = new BlockId(Sha256Hash.wrap(hash(7)), number);
    Sha256Hash parent = Sha256Hash.wrap(hash(6));
    DynamicPropertiesStore dynamic = mock(DynamicPropertiesStore.class);
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(99L);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(
        new BlockId(Sha256Hash.wrap(hash(9)), 99L));
    when(dynamic.getLatestBlockHeaderNumberFromDB()).thenReturn(number);
    when(dynamic.getLatestBlockHeaderHashFromDB()).thenReturn(id);

    BlockCapsule block = mock(BlockCapsule.class);
    when(block.getNum()).thenReturn(number);
    when(block.getBlockId()).thenReturn(id);
    when(block.getParentHash()).thenReturn(parent);
    when(block.getTimeStamp()).thenReturn(1234L);
    ChainBaseManager chainBase = mock(ChainBaseManager.class);
    when(chainBase.getBlockByNum(number)).thenReturn(block);

    CommonCheckpointRecoveryStateAdapter adapter =
        new CommonCheckpointRecoveryStateAdapter(dynamic, chainBase);
    CommonCheckpointHotRecovery.PersistentDynamicHead head = adapter.load();
    BlockSnapshotMeta meta = adapter.loadIfPresent(number);

    assertEquals(number, head.getBlockNumber());
    assertArrayEquals(id.getBytes(), head.getBlockHash());
    assertEquals(number, meta.getBlockNumber());
    assertArrayEquals(id.getBytes(), meta.getBlockHash());
    assertArrayEquals(parent.getBytes(), meta.getParentHash());
    assertEquals(1234L, meta.getTimestamp());
  }

  @Test
  public void rejectsUnavailablePersistentDynamicIdentity() throws Exception {
    DynamicPropertiesStore dynamic = mock(DynamicPropertiesStore.class);
    when(dynamic.getLatestBlockHeaderNumberFromDB()).thenReturn(-1L);
    CommonCheckpointRecoveryStateAdapter adapter =
        new CommonCheckpointRecoveryStateAdapter(dynamic, mock(ChainBaseManager.class));

    assertThrows(java.io.IOException.class, adapter::load);
  }

  @Test
  public void distinguishesMissingAndCorruptBlockStoreRecords() throws Exception {
    long number = 73L;
    ChainBaseManager chainBase = mock(ChainBaseManager.class);
    when(chainBase.getBlockByNum(number)).thenThrow(new ItemNotFoundException());
    CommonCheckpointRecoveryStateAdapter adapter = new CommonCheckpointRecoveryStateAdapter(
        mock(DynamicPropertiesStore.class), chainBase);
    assertNull(adapter.loadIfPresent(number));

    doThrow(new BadItemException()).when(chainBase).getBlockByNum(number);
    assertThrows(java.io.IOException.class, () -> adapter.loadIfPresent(number));
  }

  private static byte[] hash(int seed) {
    byte[] value = new byte[32];
    java.util.Arrays.fill(value, (byte) seed);
    return value;
  }
}
