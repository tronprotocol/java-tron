package org.tron.core.net.messagehandler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.Parameter;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.protos.Protocol;

public class FetchInvDataMsgHandlerTest {

  @Test
  public void testProcessMessage() throws Exception {
    FetchInvDataMsgHandler fetchInvDataMsgHandler = new FetchInvDataMsgHandler();
    PeerConnection peer = Mockito.mock(PeerConnection.class);
    TronNetDelegate tronNetDelegate = Mockito.mock(TronNetDelegate.class);
    AdvService advService = Mockito.mock(AdvService.class);

    Field field = FetchInvDataMsgHandler.class.getDeclaredField("tronNetDelegate");
    field.setAccessible(true);
    field.set(fetchInvDataMsgHandler, tronNetDelegate);

    Mockito.when(tronNetDelegate.allowPBFT()).thenReturn(false);

    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId();
    List<Sha256Hash> blockIds = new LinkedList<>();
    blockIds.add(blockId);

    Cache<Item, Long> advInvSpread = CacheBuilder.newBuilder().maximumSize(20000)
        .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();
    Mockito.when(peer.getAdvInvSpread()).thenReturn(advInvSpread);
    Mockito.when(peer.isNeedSyncFromUs()).thenReturn(true);
    Mockito.when(peer.isSyncFinish()).thenReturn(false);
    Mockito.when(peer.getBlockBothHave()).thenReturn(blockId);
    Cache<Sha256Hash, Long> syncBlockIdCache = CacheBuilder.newBuilder()
        .maximumSize(2 * Parameter.NetConstants.SYNC_FETCH_BATCH_NUM).recordStats().build();
    Mockito.when(peer.getSyncBlockIdCache()).thenReturn(syncBlockIdCache);
    Mockito.when(peer.getLastSyncBlockId()).thenReturn(blockId);
    BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
    Mockito.when(advService.getMessage(new Item(blockId, Protocol.Inventory.InventoryType.BLOCK)))
        .thenReturn(new BlockMessage(blockCapsule));
    ReflectUtils.setFieldValue(fetchInvDataMsgHandler, "advService", advService);

    fetchInvDataMsgHandler.processMessage(peer,
        new FetchInvDataMessage(blockIds, Protocol.Inventory.InventoryType.BLOCK));
    Assert.assertNotNull(syncBlockIdCache.getIfPresent(blockId));
  }

  @Test
  public void testSyncFetchCheck() {
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 10000L);
    List<Sha256Hash> blockIds = new LinkedList<>();
    blockIds.add(blockId);
    FetchInvDataMessage msg =
        new FetchInvDataMessage(blockIds, Protocol.Inventory.InventoryType.BLOCK);

    PeerConnection peer = Mockito.mock(PeerConnection.class);
    Mockito.when(peer.isNeedSyncFromUs()).thenReturn(true);
    Cache<Item, Long> advInvSpread = CacheBuilder.newBuilder().maximumSize(100)
        .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();
    Mockito.when(peer.getAdvInvSpread()).thenReturn(advInvSpread);

    FetchInvDataMsgHandler fetchInvDataMsgHandler = new FetchInvDataMsgHandler();

    try {
      Mockito.when(peer.getLastSyncBlockId())
        .thenReturn(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 1000L));
      fetchInvDataMsgHandler.processMessage(peer, msg);
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "maxBlockNum: 1000, blockNum: 10000");
    }

    try {
      Mockito.when(peer.getLastSyncBlockId())
        .thenReturn(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 20000L));
      fetchInvDataMsgHandler.processMessage(peer, msg);
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "minBlockNum: 16000, blockNum: 10000");
    }
  }
}
