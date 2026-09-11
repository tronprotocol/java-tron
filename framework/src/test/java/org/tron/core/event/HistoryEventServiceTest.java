package org.tron.core.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.db.Manager;
import org.tron.core.services.event.BlockEventCache;
import org.tron.core.services.event.BlockEventGet;
import org.tron.core.services.event.BlockEventLoad;
import org.tron.core.services.event.HistoryEventService;
import org.tron.core.services.event.RealtimeEventService;
import org.tron.core.services.event.SolidEventService;
import org.tron.core.services.event.bo.BlockEvent;
import org.tron.core.store.DynamicPropertiesStore;

public class HistoryEventServiceTest {

  private final HistoryEventService service = new HistoryEventService();
  private final EventPluginLoader plugin = mock(EventPluginLoader.class);
  private final SolidEventService solid = mock(SolidEventService.class);
  private final RealtimeEventService realtime = mock(RealtimeEventService.class);
  private final BlockEventLoad load = mock(BlockEventLoad.class);
  private final BlockEventGet get = mock(BlockEventGet.class);
  private final ChainBaseManager chain = mock(ChainBaseManager.class);
  private final DynamicPropertiesStore properties = mock(DynamicPropertiesStore.class);
  private final Map<String, Object> savedCacheState = new LinkedHashMap<>();

  @Before
  public void setUp() {
    for (String field : new String[]{"solidNum", "head", "solidId", "blockEventMap", "numMap"}) {
      savedCacheState.put(field, ReflectionTestUtils.getField(BlockEventCache.class, field));
    }
    // init() clears both maps, so use test-owned maps to preserve the original contents.
    ReflectionTestUtils.setField(BlockEventCache.class, "blockEventMap", new ConcurrentHashMap<>());
    ReflectionTestUtils.setField(BlockEventCache.class, "numMap", new ConcurrentHashMap<>());
    Manager manager = mock(Manager.class);
    when(manager.getChainBaseManager()).thenReturn(chain);
    when(manager.getDynamicPropertiesStore()).thenReturn(properties);
    when(chain.getHeadBlockId()).thenReturn(new BlockId());
    ReflectionTestUtils.setField(service, "manager", manager);
    ReflectionTestUtils.setField(service, "instance", plugin);
    ReflectionTestUtils.setField(service, "solidEventService", solid);
    ReflectionTestUtils.setField(service, "realtimeEventService", realtime);
    ReflectionTestUtils.setField(service, "blockEventLoad", load);
    ReflectionTestUtils.setField(service, "blockEventGet", get);
  }

  @After
  public void tearDown() {
    try {
      service.close();
    } finally {
      Thread worker = (Thread) ReflectionTestUtils.getField(service, "thread");
      if (worker != null) {
        worker.interrupt();
        Uninterruptibles.joinUninterruptibly(worker, 5, TimeUnit.SECONDS);
        Assert.assertFalse("History worker did not terminate", worker.isAlive());
      }
      // Restore only after the worker can no longer change the shared cache.
      savedCacheState.forEach((field, value) ->
          ReflectionTestUtils.setField(BlockEventCache.class, field, value));
      savedCacheState.clear();
    }
  }

  @Test
  public void testInitFromHead() {
    service.init();
    verify(realtime).init();
    verify(solid).init();
    verify(load).init();
  }

  @Test(timeout = 10_000)
  public void testSyncHistory() throws Exception {
    when(plugin.getStartSyncBlockNum()).thenReturn(1L);
    when(plugin.isUseNativeQueue()).thenReturn(true);
    when(properties.getLatestSolidifiedBlockNum()).thenReturn(2L);
    BlockId blockId = new BlockId(Sha256Hash.ZERO_HASH, 1);
    BlockEvent block = new BlockEvent(blockId);
    when(get.getBlockEvent(1L)).thenReturn(block);
    when(chain.getBlockIdByNum(1L)).thenReturn(blockId);

    service.init();
    Thread worker = (Thread) ReflectionTestUtils.getField(service, "thread");
    worker.join(5000);
    Assert.assertFalse("History sync did not complete", worker.isAlive());
    verify(realtime).flush(block, false);
    verify(solid).flush(block);
    verify(realtime).init();
    verify(solid).init();
    verify(load).init();
  }

  @Test(timeout = 10_000)
  public void testCloseWhilePluginBusy() throws Exception {
    when(plugin.getStartSyncBlockNum()).thenReturn(1L);
    when(properties.getLatestSolidifiedBlockNum()).thenReturn(2L);
    CountDownLatch busy = new CountDownLatch(1);
    when(plugin.isBusy()).thenAnswer(invocation -> {
      busy.countDown();
      return true;
    });

    service.init();
    Assert.assertTrue("Worker did not reach busy plugin", busy.await(5, TimeUnit.SECONDS));
    service.close();
    Thread worker = (Thread) ReflectionTestUtils.getField(service, "thread");
    Assert.assertFalse("History worker ignored close", worker.isAlive());
    verify(get, never()).getBlockEvent(1L);
    verify(load, never()).init();
  }
}
