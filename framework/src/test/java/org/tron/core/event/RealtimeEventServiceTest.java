package org.tron.core.event;

import static org.mockito.Mockito.mock;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.capsule.BlockLogTriggerCapsule;
import org.tron.common.logsfilter.capsule.TransactionLogTriggerCapsule;
import org.tron.common.logsfilter.capsule.TriggerCapsule;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;
import org.tron.core.services.event.BlockEventCache;
import org.tron.core.services.event.RealtimeEventService;
import org.tron.core.services.event.bo.BlockEvent;
import org.tron.core.services.event.bo.Event;
import org.tron.core.services.event.bo.SmartContractTrigger;

public class RealtimeEventServiceTest {

  RealtimeEventService realtimeEventService = new RealtimeEventService();

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

    BlockingQueue<TriggerCapsule> queue = new BlockingArrayQueue<>();
    Manager manager = mock(Manager.class);
    Mockito.when(manager.getTriggerCapsuleQueue()).thenReturn(queue);
    ReflectUtils.setFieldValue(realtimeEventService, "manager", manager);

    BlockCapsule blockCapsule = new BlockCapsule(0L, Sha256Hash.ZERO_HASH, 0L,
        ByteString.copyFrom(BlockEventCacheTest.getBlockId()));
    be2.setBlockLogTriggerCapsule(new BlockLogTriggerCapsule(blockCapsule));
    Mockito.when(instance.isBlockLogTriggerEnable()).thenReturn(true);
    Mockito.when(instance.isBlockLogTriggerSolidified()).thenReturn(false);

    realtimeEventService.add(event);
    realtimeEventService.work();

    Assert.assertEquals(0, queue.size());

    event = new Event(be2, false);
    realtimeEventService.add(event);
    realtimeEventService.work();

    Assert.assertEquals(1, queue.size());

    be2.setBlockLogTriggerCapsule(null);
    queue.poll();

    List<TransactionLogTriggerCapsule> list = new ArrayList<>();
    list.add(mock(TransactionLogTriggerCapsule.class));
    be2.setTransactionLogTriggerCapsules(list);

    Mockito.when(instance.isTransactionLogTriggerEnable()).thenReturn(true);
    Mockito.when(instance.isTransactionLogTriggerSolidified()).thenReturn(false);
    realtimeEventService.flush(be2, event.isRemove());
    Assert.assertEquals(1, queue.size());

    be2.setTransactionLogTriggerCapsules(null);

    SmartContractTrigger contractTrigger = new SmartContractTrigger();
    be2.setSmartContractTrigger(contractTrigger);

    contractTrigger.getContractEventTriggers().add(mock(ContractEventTrigger.class));
    Mockito.when(instance.isContractLogTriggerEnable()).thenReturn(true);
    try {
      realtimeEventService.flush(be2, event.isRemove());
    } catch (Exception e) {
      Assert.assertTrue(e instanceof NullPointerException);
    }

    contractTrigger.getContractEventTriggers().clear();

    realtimeEventService.flush(be2, event.isRemove());

    contractTrigger.getContractLogTriggers().add(mock(ContractLogTrigger.class));
    Mockito.when(instance.isContractEventTriggerEnable()).thenReturn(true);
    try {
      realtimeEventService.flush(be2, event.isRemove());
    } catch (Exception e) {
      Assert.assertTrue(e instanceof NullPointerException);
    }
  }
}
