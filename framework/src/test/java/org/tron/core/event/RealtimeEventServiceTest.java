package org.tron.core.event;

import static org.mockito.Mockito.mock;

import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.capsule.BlockLogTriggerCapsule;
import org.tron.common.logsfilter.capsule.TransactionLogTriggerCapsule;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.services.event.BlockEventCache;
import org.tron.core.services.event.RealtimeEventService;
import org.tron.core.services.event.bo.BlockEvent;
import org.tron.core.services.event.bo.Event;
import org.tron.core.services.event.bo.SmartContractTrigger;

public class RealtimeEventServiceTest {

  RealtimeEventService realtimeEventService = new RealtimeEventService();

  @Test
  public void shouldBecomeBusyAt500EventsAndRetainLaterEvents() throws Exception {
    Field queueField = RealtimeEventService.class.getDeclaredField("queue");
    queueField.setAccessible(true);
    BlockingQueue<Event> queue = (BlockingQueue<Event>) queueField.get(null);
    queue.clear();

    try {
      Event event = new Event(new BlockEvent(), false);
      for (int i = 0; i < 499; i++) {
        realtimeEventService.add(event);
      }

      Assert.assertFalse(realtimeEventService.isBusy());

      realtimeEventService.add(event);
      Assert.assertTrue(realtimeEventService.isBusy());

      Event laterEvent = new Event(new BlockEvent(), true);
      realtimeEventService.add(laterEvent);
      Assert.assertEquals(501, queue.size());
      for (int i = 0; i < 500; i++) {
        Assert.assertSame(event, queue.remove());
      }
      Assert.assertSame(laterEvent, queue.remove());
    } finally {
      queue.clear();
    }

    Assert.assertFalse(realtimeEventService.isBusy());
  }

  @Test
  public void test() throws Exception {
    BlockEvent be1 = new BlockEvent();
    BlockCapsule.BlockId b1 = new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 1);
    be1.setBlockId(b1);
    be1.setParentId(b1);
    be1.setSolidId(b1);
    BlockEventCache.init(b1);

    BlockEvent be2 = new BlockEvent();
    BlockCapsule.BlockId b2 = new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 2);
    be2.setBlockId(b2);
    be2.setParentId(b1);
    be2.setSolidId(b1);
    BlockEventCache.add(be2);
    Assert.assertEquals(be2, BlockEventCache.getHead());
    Assert.assertEquals(be2, BlockEventCache.getBlockEvent(b2));

    Event event = new Event(be2, true);

    realtimeEventService.add(event);
    realtimeEventService.work();

    EventPluginLoader instance = mock(EventPluginLoader.class);
    ReflectUtils.setFieldValue(realtimeEventService, "instance", instance);

    BlockCapsule blockCapsule = new BlockCapsule(0L, Sha256Hash.ZERO_HASH, 0L,
        ByteString.copyFrom(BlockEventCacheTest.getBlockId()));
    // Capture the removed flag at delivery time, before the cached capsule is reused.
    BlockLogTriggerCapsule blockCap = Mockito.spy(new BlockLogTriggerCapsule(blockCapsule));
    List<Boolean> deliveredRemovedFlags = new ArrayList<>();
    Mockito.doAnswer(invocation -> {
      deliveredRemovedFlags.add(blockCap.getBlockLogTrigger().isRemoved());
      return null;
    }).when(blockCap).processTrigger();
    be2.setBlockLogTriggerCapsule(blockCap);
    Mockito.when(instance.isBlockLogTriggerEnable()).thenReturn(true);
    Mockito.when(instance.isBlockLogTriggerSolidified()).thenReturn(false);

    // reorg rollback: block trigger re-emitted (posted synchronously) with removed=true
    realtimeEventService.flush(be2, true);
    Assert.assertTrue(blockCap.getBlockLogTrigger().isRemoved());

    // forward: block trigger posted with removed=false
    realtimeEventService.flush(be2, false);
    Assert.assertFalse(blockCap.getBlockLogTrigger().isRemoved());
    // posted directly to the plugin both times, never via the async queue
    Assert.assertEquals(Arrays.asList(true, false), deliveredRemovedFlags);

    be2.setBlockLogTriggerCapsule(null);

    TransactionLogTriggerCapsule txCap = mock(TransactionLogTriggerCapsule.class);
    List<TransactionLogTriggerCapsule> list = new ArrayList<>();
    list.add(txCap);
    be2.setTransactionLogTriggerCapsules(list);
    Mockito.when(instance.isTransactionLogTriggerEnable()).thenReturn(true);
    Mockito.when(instance.isTransactionLogTriggerSolidified()).thenReturn(false);

    // rollback: tx trigger posted synchronously with removed=true
    realtimeEventService.flush(be2, true);
    realtimeEventService.flush(be2, false);
    InOrder delivery = Mockito.inOrder(txCap);
    delivery.verify(txCap).setRemoved(true);
    delivery.verify(txCap).processTrigger();
    delivery.verify(txCap).setRemoved(false);
    delivery.verify(txCap).processTrigger();
    delivery.verifyNoMoreInteractions();

  }

  @Test
  public void shouldDeliverContractEventsOnlyWhenEnabledWithCurrentRemovedFlag() {
    EventPluginLoader plugin = mock(EventPluginLoader.class);
    ReflectUtils.setFieldValue(realtimeEventService, "instance", plugin);
    BlockEvent block = new BlockEvent();
    block.setBlockId(new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 1));
    SmartContractTrigger triggers = new SmartContractTrigger();
    ContractEventTrigger event = new ContractEventTrigger();
    event.setTriggerName("staleName");
    triggers.getContractEventTriggers().add(event);
    block.setSmartContractTrigger(triggers);
    List<Boolean> deliveredFlags = new ArrayList<>();
    Mockito.doAnswer(invocation -> {
      Assert.assertEquals("contractEventTrigger", event.getTriggerName());
      deliveredFlags.add(event.isRemoved());
      return null;
    }).when(plugin).postContractEventTrigger(event);

    try (MockedStatic<EventPluginLoader> loader = Mockito.mockStatic(EventPluginLoader.class)) {
      loader.when(EventPluginLoader::getInstance).thenReturn(plugin);
      realtimeEventService.flush(block, true);
      Mockito.verify(plugin, Mockito.never()).postContractEventTrigger(Mockito.any());

      Mockito.when(plugin.isContractEventTriggerEnable()).thenReturn(true);
      realtimeEventService.flush(block, true);
      realtimeEventService.flush(block, false);

      Assert.assertEquals(Arrays.asList(true, false), deliveredFlags);
      Mockito.verify(plugin, Mockito.times(2)).postContractEventTrigger(event);
      Mockito.verify(plugin, Mockito.never()).postContractLogTrigger(Mockito.any());
    }
  }

  @Test
  public void shouldDeliverContractLogsOnlyWhenEnabledWithCurrentRemovedFlag() {
    EventPluginLoader plugin = mock(EventPluginLoader.class);
    ReflectUtils.setFieldValue(realtimeEventService, "instance", plugin);
    BlockEvent block = new BlockEvent();
    block.setBlockId(new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 1));
    SmartContractTrigger triggers = new SmartContractTrigger();
    ContractLogTrigger log = new ContractLogTrigger();
    log.setTriggerName("staleName");
    triggers.getContractLogTriggers().add(log);
    block.setSmartContractTrigger(triggers);
    List<Boolean> deliveredFlags = new ArrayList<>();
    Mockito.doAnswer(invocation -> {
      Assert.assertEquals("contractLogTrigger", log.getTriggerName());
      deliveredFlags.add(log.isRemoved());
      return null;
    }).when(plugin).postContractLogTrigger(log);

    try (MockedStatic<EventPluginLoader> loader = Mockito.mockStatic(EventPluginLoader.class)) {
      loader.when(EventPluginLoader::getInstance).thenReturn(plugin);
      realtimeEventService.flush(block, true);
      Mockito.verify(plugin, Mockito.never()).postContractLogTrigger(Mockito.any());

      Mockito.when(plugin.isContractLogTriggerEnable()).thenReturn(true);
      realtimeEventService.flush(block, true);
      realtimeEventService.flush(block, false);

      Assert.assertEquals(Arrays.asList(true, false), deliveredFlags);
      Mockito.verify(plugin, Mockito.times(2)).postContractLogTrigger(log);
      Mockito.verify(plugin, Mockito.never()).postContractEventTrigger(Mockito.any());
    }
  }
}
