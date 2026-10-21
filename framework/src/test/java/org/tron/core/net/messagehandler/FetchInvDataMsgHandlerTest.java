package org.tron.core.net.messagehandler;

import static org.tron.core.net.message.MessageTypes.FETCH_INV_DATA;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.Parameter;
import org.tron.core.exception.P2pException;
import org.tron.core.net.P2pRateLimiter;
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
    P2pRateLimiter p2pRateLimiter = new P2pRateLimiter();
    p2pRateLimiter.register(FETCH_INV_DATA.asByte(), 2);
    Mockito.when(peer.getP2pRateLimiter()).thenReturn(p2pRateLimiter);

    fetchInvDataMsgHandler.processMessage(peer,
        new FetchInvDataMessage(blockIds, Protocol.Inventory.InventoryType.BLOCK));
    Assert.assertNotNull(syncBlockIdCache.getIfPresent(blockId));
  }

  @Test
  public void testSyncBlockIdCacheRetainsFullWindowAndRejectsDuplicates() throws Exception {
    assertFullWindowRetainedAndDuplicatesRejected(false);
  }

  @Test
  public void testSyncBlockIdCacheRetainsShuffledWindowAndRejectsDuplicates() throws Exception {
    assertFullWindowRetainedAndDuplicatesRejected(true);
  }

  private void assertFullWindowRetainedAndDuplicatesRejected(boolean shuffled) throws Exception {
    PeerConnection peer = Mockito.spy(new PeerConnection());
    Mockito.doNothing().when(peer).sendMessage(Mockito.any());
    int windowSize = 2 * (int) Parameter.NetConstants.SYNC_FETCH_BATCH_NUM + 1;
    long lastHeight = 10000L;
    peer.setNeedSyncFromUs(true);
    peer.setLastSyncBlockId(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, lastHeight));

    FetchInvDataMsgHandler handler = new FetchInvDataMsgHandler();
    TronNetDelegate delegate = Mockito.mock(TronNetDelegate.class);
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(handler, "advService", Mockito.mock(AdvService.class));
    Method check = FetchInvDataMsgHandler.class.getDeclaredMethod(
        "check", PeerConnection.class, FetchInvDataMessage.class, boolean.class);
    check.setAccessible(true);
    List<Sha256Hash> hashes = new ArrayList<>();
    for (int i = 0; i < windowSize; i++) {
      BlockCapsule.BlockId id =
          new BlockCapsule.BlockId(createHash(i), lastHeight - windowSize + 1 + i);
      hashes.add(id);
    }
    if (shuffled) {
      Collections.shuffle(hashes, new Random(6966L));
    }

    for (Sha256Hash hash : hashes) {
      check.invoke(handler, peer, new FetchInvDataMessage(Collections.singletonList(hash),
          Protocol.Inventory.InventoryType.BLOCK), false);
    }
    peer.getSyncBlockIdCache().cleanUp();

    // Retained forks at the same height also consume cache entries.
    BlockCapsule fork = new BlockCapsule(lastHeight, Sha256Hash.ZERO_HASH, 1,
        ByteString.EMPTY);
    BlockMessage forkMessage = new BlockMessage(fork);
    Mockito.when(delegate.getData(fork.getBlockId(), Protocol.Inventory.InventoryType.BLOCK))
        .thenReturn(forkMessage);
    handler.processMessage(peer, new FetchInvDataMessage(
        Collections.singletonList(fork.getBlockId()), Protocol.Inventory.InventoryType.BLOCK));
    Assert.assertNotNull(peer.getSyncBlockIdCache().getIfPresent(fork.getBlockId()));
    Mockito.verify(peer).sendMessage(forkMessage);

    for (Sha256Hash hash : hashes) {
      Assert.assertNotNull("Missing block " + new BlockCapsule.BlockId(hash).getNum(),
          peer.getSyncBlockIdCache().getIfPresent(hash));
    }
    for (Sha256Hash hash : hashes) {
      FetchInvDataMessage message = new FetchInvDataMessage(Collections.singletonList(hash),
          Protocol.Inventory.InventoryType.BLOCK);
      InvocationTargetException exception = Assert.assertThrows(InvocationTargetException.class,
          () -> check.invoke(handler, peer, message, false));
      Assert.assertTrue(exception.getCause() instanceof P2pException);
      P2pException cause = (P2pException) exception.getCause();
      Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, cause.getType());
      Assert.assertEquals(new BlockCapsule.BlockId(hash).getString() + " is exist",
          cause.getMessage());
    }
  }

  @Test
  public void testSyncBlockIdCacheRejectsDuplicatesAcrossAdvancingWindows() throws Exception {
    PeerConnection peer = new PeerConnection();
    peer.setNeedSyncFromUs(true);
    FetchInvDataMsgHandler handler = new FetchInvDataMsgHandler();
    Method check = FetchInvDataMsgHandler.class.getDeclaredMethod(
        "check", PeerConnection.class, FetchInvDataMessage.class, boolean.class);
    check.setAccessible(true);

    // Fetch the highest block first so older heights become more recent cache entries.
    // Continue beyond the cache capacity to exercise eviction over several windows.
    for (long lastHeight = 10000; lastHeight <= 22000; lastHeight += 2000) {
      BlockCapsule.BlockId lastId =
          new BlockCapsule.BlockId(createHash((int) lastHeight), lastHeight);
      peer.setLastSyncBlockId(lastId);
      check.invoke(handler, peer, new FetchInvDataMessage(Collections.singletonList(lastId),
          Protocol.Inventory.InventoryType.BLOCK), false);
      long firstNewHeight = lastHeight == 10000 ? 6000 : lastHeight - 1999;
      for (long height = firstNewHeight; height < lastHeight; height++) {
        BlockCapsule.BlockId id = new BlockCapsule.BlockId(createHash((int) height), height);
        check.invoke(handler, peer, new FetchInvDataMessage(Collections.singletonList(id),
            Protocol.Inventory.InventoryType.BLOCK), false);
      }

      // containsKey does not refresh access order, preserving the adversarial request order.
      for (long height = lastHeight - 4000; height <= lastHeight; height++) {
        BlockCapsule.BlockId id = new BlockCapsule.BlockId(createHash((int) height), height);
        Assert.assertTrue("Missing eligible block " + height + " at window " + lastHeight,
            peer.getSyncBlockIdCache().asMap().containsKey(id));
      }
      long firstHeight = lastHeight - 4000;
      BlockCapsule.BlockId firstId =
          new BlockCapsule.BlockId(createHash((int) firstHeight), firstHeight);
      InvocationTargetException exception = Assert.assertThrows(InvocationTargetException.class,
          () -> check.invoke(handler, peer, new FetchInvDataMessage(
              Collections.singletonList(firstId), Protocol.Inventory.InventoryType.BLOCK), false));
      Assert.assertTrue(exception.getCause() instanceof P2pException);
      Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE,
          ((P2pException) exception.getCause()).getType());
    }
  }

  @Test
  public void testSyncBlockAfterReorgCanBeFetchedOnce() throws Exception {
    FetchInvDataMsgHandler handler = new FetchInvDataMsgHandler();
    TronNetDelegate delegate = Mockito.mock(TronNetDelegate.class);
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(handler, "advService", Mockito.mock(AdvService.class));
    PeerConnection peer = Mockito.spy(new PeerConnection());
    Mockito.doNothing().when(peer).sendMessage(Mockito.any());
    BlockCapsule oldBlock = new BlockCapsule(10000L, Sha256Hash.ZERO_HASH, 1,
        ByteString.EMPTY);
    BlockCapsule.BlockId oldId = oldBlock.getBlockId();
    BlockCapsule.BlockId newId = new BlockCapsule.BlockId(createHash(2), 10000L);
    peer.setNeedSyncFromUs(true);
    // The sync inventory was sent before the local main chain switched to newId.
    peer.setLastSyncBlockId(oldId);
    Mockito.when(delegate.getBlockIdByNum(10000L)).thenReturn(newId);
    BlockMessage blockMessage = new BlockMessage(oldBlock);
    Mockito.when(delegate.getData(oldId, Protocol.Inventory.InventoryType.BLOCK))
        .thenReturn(blockMessage);
    FetchInvDataMessage request = new FetchInvDataMessage(
        Collections.singletonList(oldId), Protocol.Inventory.InventoryType.BLOCK);

    handler.processMessage(peer, request);

    Mockito.verify(peer).sendMessage(blockMessage);
    Assert.assertEquals(oldId, peer.getBlockBothHave());
    Assert.assertNotNull(peer.getSyncBlockIdCache().getIfPresent(oldId));
    P2pException exception = Assert.assertThrows(P2pException.class,
        () -> handler.processMessage(peer, request));
    Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, exception.getType());
    Mockito.verify(delegate, Mockito.times(1))
        .getData(oldId, Protocol.Inventory.InventoryType.BLOCK);
    Mockito.verify(peer, Mockito.times(1)).sendMessage(blockMessage);
  }

  @Test
  public void testAdvertisedForkBlockCanStillBeFetched() throws Exception {
    FetchInvDataMsgHandler handler = new FetchInvDataMsgHandler();
    TronNetDelegate delegate = Mockito.mock(TronNetDelegate.class);
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(handler, "advService", Mockito.mock(AdvService.class));
    PeerConnection peer = Mockito.spy(new PeerConnection());
    Mockito.doNothing().when(peer).sendMessage(Mockito.any());
    BlockCapsule fork = new BlockCapsule(10000L, Sha256Hash.ZERO_HASH, 1,
        ByteString.EMPTY);
    Item item = new Item(fork.getBlockId(), Protocol.Inventory.InventoryType.BLOCK);
    peer.getAdvInvSpread().put(item, 1L);
    BlockMessage message = new BlockMessage(fork);
    Mockito.when(delegate.getData(fork.getBlockId(), Protocol.Inventory.InventoryType.BLOCK))
        .thenReturn(message);

    handler.processMessage(peer, new FetchInvDataMessage(
        Collections.singletonList(fork.getBlockId()), Protocol.Inventory.InventoryType.BLOCK));

    Mockito.verify(peer).sendMessage(message);
    Mockito.verify(delegate, Mockito.never()).getBlockIdByNum(Mockito.anyLong());
    Assert.assertNull(peer.getAdvInvSpread().getIfPresent(item));
    Assert.assertEquals(0, peer.getSyncBlockIdCache().size());
  }

  private Sha256Hash createHash(int value) {
    byte[] bytes = new byte[Sha256Hash.LENGTH];
    bytes[28] = (byte) (value >>> 24);
    bytes[29] = (byte) (value >>> 16);
    bytes[30] = (byte) (value >>> 8);
    bytes[31] = (byte) value;
    return Sha256Hash.wrap(bytes);
  }

  @Test
  public void testIsAdvInv() {
    FetchInvDataMsgHandler fetchInvDataMsgHandler = new FetchInvDataMsgHandler();

    List<Sha256Hash> list = new LinkedList<>();
    list.add(Sha256Hash.ZERO_HASH);
    FetchInvDataMessage msg =
        new FetchInvDataMessage(list, Protocol.Inventory.InventoryType.TRX);

    boolean isAdv = fetchInvDataMsgHandler.isAdvInv(null, msg);
    Assert.assertTrue(isAdv);

    PeerConnection peer = Mockito.mock(PeerConnection.class);
    Cache<Item, Long> advInvSpread = CacheBuilder.newBuilder().build();
    Mockito.when(peer.getAdvInvSpread()).thenReturn(advInvSpread);

    msg = new FetchInvDataMessage(list, Protocol.Inventory.InventoryType.BLOCK);
    isAdv = fetchInvDataMsgHandler.isAdvInv(peer, msg);
    Assert.assertTrue(!isAdv);

    advInvSpread.put(new Item(Sha256Hash.ZERO_HASH, Protocol.Inventory.InventoryType.BLOCK), 1L);
    isAdv = fetchInvDataMsgHandler.isAdvInv(peer, msg);
    Assert.assertTrue(isAdv);
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
    P2pRateLimiter p2pRateLimiter = new P2pRateLimiter();
    p2pRateLimiter.register(FETCH_INV_DATA.asByte(), 2);
    Mockito.when(peer.getP2pRateLimiter()).thenReturn(p2pRateLimiter);

    FetchInvDataMsgHandler fetchInvDataMsgHandler = new FetchInvDataMsgHandler();

    Mockito.when(peer.getLastSyncBlockId())
        .thenReturn(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 1000L));
    Exception e1 = Assert.assertThrows(Exception.class,
        () -> fetchInvDataMsgHandler.processMessage(peer, msg));
    Assert.assertEquals("maxBlockNum: 1000, blockNum: 10000", e1.getMessage());

    Mockito.when(peer.getLastSyncBlockId())
        .thenReturn(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 20000L));
    Exception e2 = Assert.assertThrows(Exception.class,
        () -> fetchInvDataMsgHandler.processMessage(peer, msg));
    Assert.assertEquals("minBlockNum: 16000, blockNum: 10000", e2.getMessage());
  }

  @Test
  public void testDuplicateHashRejected() throws Exception {
    FetchInvDataMsgHandler handler = new FetchInvDataMsgHandler();
    PeerConnection peer = Mockito.mock(PeerConnection.class);
    AdvService advService = Mockito.mock(AdvService.class);
    TronNetDelegate tronNetDelegate = Mockito.mock(TronNetDelegate.class);

    ReflectUtils.setFieldValue(handler, "advService", advService);
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", tronNetDelegate);

    Sha256Hash hash = Sha256Hash.ZERO_HASH;
    List<Sha256Hash> hashList = new LinkedList<>();
    hashList.add(hash);
    hashList.add(hash); // duplicate

    FetchInvDataMessage msg = new FetchInvDataMessage(hashList,
        Protocol.Inventory.InventoryType.TRX);

    Cache<Item, Long> advInvSpread = CacheBuilder.newBuilder()
        .maximumSize(20000).expireAfterWrite(1, TimeUnit.HOURS).build();
    advInvSpread.put(new Item(hash, Protocol.Inventory.InventoryType.TRX), 1L);
    Mockito.when(peer.getAdvInvSpread()).thenReturn(advInvSpread);

    try {
      handler.processMessage(peer, msg);
      Assert.fail("Expected P2pException for duplicate hash");
    } catch (P2pException e) {
      Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, e.getType());
    }
  }

  @Test
  public void testUnknownInventoryTypeRejected() throws Exception {
    FetchInvDataMsgHandler handler = new FetchInvDataMsgHandler();
    PeerConnection peer = Mockito.mock(PeerConnection.class);

    // craft a FetchInvData whose enum type holds an undefined value (2)
    Protocol.Inventory inv = Protocol.Inventory.newBuilder()
        .setTypeValue(2)
        .addIds(com.google.protobuf.ByteString.copyFrom(new byte[32]))
        .build();
    FetchInvDataMessage msg = new FetchInvDataMessage(inv.toByteArray());
    Assert.assertEquals(Protocol.Inventory.InventoryType.UNRECOGNIZED, msg.getInventoryType());

    try {
      handler.processMessage(peer, msg);
      Assert.fail("Expected P2pException for unknown inventory type");
    } catch (P2pException e) {
      Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, e.getType());
    }
  }

  @Test
  public void testRateLimiter() {
    List<Sha256Hash> blockIds = new LinkedList<>();
    for (int i = 0; i <= 100; i++) {
      blockIds.add(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, (long) i));
    }
    FetchInvDataMessage msg =
        new FetchInvDataMessage(blockIds, Protocol.Inventory.InventoryType.BLOCK);
    PeerConnection peer = Mockito.mock(PeerConnection.class);
    Mockito.when(peer.isNeedSyncFromUs()).thenReturn(true);
    Cache<Item, Long> advInvSpread = CacheBuilder.newBuilder().maximumSize(100)
        .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();
    Mockito.when(peer.getAdvInvSpread()).thenReturn(advInvSpread);
    P2pRateLimiter p2pRateLimiter = new P2pRateLimiter();
    p2pRateLimiter.register(FETCH_INV_DATA.asByte(), 1);
    p2pRateLimiter.acquire(FETCH_INV_DATA.asByte());
    Mockito.when(peer.getP2pRateLimiter()).thenReturn(p2pRateLimiter);
    FetchInvDataMsgHandler fetchInvDataMsgHandler = new FetchInvDataMsgHandler();

    Exception e1 = Assert.assertThrows(Exception.class,
        () -> fetchInvDataMsgHandler.processMessage(peer, msg));
    Assert.assertEquals("fetch too many blocks, size:101", e1.getMessage());

    Exception e2 = Assert.assertThrows(Exception.class,
        () -> fetchInvDataMsgHandler.processMessage(peer, msg));
    Assert.assertTrue(e2.getMessage().endsWith("rate limit"));
  }
}
