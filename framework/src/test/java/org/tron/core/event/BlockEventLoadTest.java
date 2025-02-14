package org.tron.core.event;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;
import org.tron.core.services.event.BlockEventCache;
import org.tron.core.services.event.BlockEventGet;
import org.tron.core.services.event.BlockEventLoad;
import org.tron.core.services.event.RealtimeEventService;
import org.tron.core.services.event.bo.BlockEvent;
import org.tron.core.services.event.bo.Event;
import org.tron.core.store.DynamicPropertiesStore;

public class BlockEventLoadTest {
  BlockEventLoad blockEventLoad = new BlockEventLoad();

  @Test
  public void test() throws Exception {
    Method method = blockEventLoad.getClass().getDeclaredMethod("load");
    method.setAccessible(true);

    RealtimeEventService realtimeEventService = new RealtimeEventService();
    Field field = realtimeEventService.getClass().getDeclaredField("queue");
    field.setAccessible(true);
    BlockingQueue<Event> queue = (BlockingQueue<Event>)field.get(BlockingQueue.class);

    BlockEventGet blockEventGet = mock(BlockEventGet.class);
    Manager manager = mock(Manager.class);
    ReflectUtils.setFieldValue(blockEventLoad, "realtimeEventService", realtimeEventService);
    ReflectUtils.setFieldValue(blockEventLoad, "blockEventGet", blockEventGet);
    ReflectUtils.setFieldValue(blockEventLoad, "manager", manager);

    DynamicPropertiesStore dynamicPropertiesStore = mock(DynamicPropertiesStore.class);
    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    Mockito.when(manager.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStore);
    Mockito.when(manager.getChainBaseManager()).thenReturn(chainBaseManager);

    BlockCapsule.BlockId b0 = new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 0);
    BlockEventCache.init(b0);

    /********** 1 ***********/
    Mockito.when(dynamicPropertiesStore.getLatestBlockHeaderNumber()).thenReturn(0L);
    method.invoke(blockEventLoad);
    Assert.assertEquals(0, queue.size());

    /********** 2 ***********/
    BlockEvent be1 = new BlockEvent();
    BlockCapsule.BlockId b1 = new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 1);
    be1.setBlockId(b1);
    be1.setParentId(b0);
    be1.setSolidId(b0);

    Mockito.when(dynamicPropertiesStore.getLatestBlockHeaderNumber()).thenReturn(1L);
    Mockito.when(blockEventGet.getBlockEvent(1L)).thenReturn(be1);
    method.invoke(blockEventLoad);
    Assert.assertEquals(1, queue.size());
    Assert.assertEquals(b1, BlockEventCache.getHead().getBlockId());

    /********** 3 ***********/
    BlockEventCache.init(b0);
    queue.clear();

    BlockEvent be2 = new BlockEvent();
    BlockCapsule.BlockId b2 = new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 2L);
    be2.setBlockId(b2);
    be2.setParentId(b1);
    be2.setSolidId(b0);

    Mockito.when(dynamicPropertiesStore.getLatestBlockHeaderNumber()).thenReturn(2L);
    Mockito.when(blockEventGet.getBlockEvent(2L)).thenReturn(be2);
    method.invoke(blockEventLoad);
    Assert.assertEquals(2, queue.size());
    Assert.assertEquals(b2, BlockEventCache.getHead().getBlockId());

    /********** 4 ***********/
    BlockEventCache.init(b0);
    queue.clear();

    Mockito.when(dynamicPropertiesStore.getLatestBlockHeaderNumber()).thenReturn(1L);
    method.invoke(blockEventLoad);
    Assert.assertEquals(1, queue.size());
    queue.clear();

    BlockEvent be21 = new BlockEvent();
    BlockCapsule.BlockId b21 = new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 1L);
    be21.setBlockId(b21);
    be21.setParentId(b0);
    be21.setSolidId(b0);

    BlockEvent be22 = new BlockEvent();
    BlockCapsule.BlockId b22 = new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 2L);
    be22.setBlockId(b22);
    be22.setParentId(b21);
    be22.setSolidId(b21);

    Mockito.when(dynamicPropertiesStore.getLatestBlockHeaderNumber()).thenReturn(2L);
    Mockito.when(blockEventGet.getBlockEvent(1L)).thenReturn(be21);
    Mockito.when(blockEventGet.getBlockEvent(2L)).thenReturn(be22);
    method.invoke(blockEventLoad);
    Assert.assertEquals(3, queue.size());
    Assert.assertEquals(b22, BlockEventCache.getHead().getBlockId());

    Event event = queue.poll();
    Assert.assertEquals(b1, event.getBlockEvent().getBlockId());
    Assert.assertEquals(true, event.isRemove());

    event = queue.poll();
    Assert.assertEquals(b21, event.getBlockEvent().getBlockId());
    Assert.assertEquals(false, event.isRemove());

    event = queue.poll();
    Assert.assertEquals(b22, event.getBlockEvent().getBlockId());
    Assert.assertEquals(false, event.isRemove());
  }
}
