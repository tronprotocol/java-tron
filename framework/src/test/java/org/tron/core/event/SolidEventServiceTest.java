package org.tron.core.event;

import static org.mockito.Mockito.mock;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.capsule.BlockLogTriggerCapsule;
import org.tron.common.logsfilter.capsule.SolidityTriggerCapsule;
import org.tron.common.logsfilter.capsule.TransactionLogTriggerCapsule;
import org.tron.common.logsfilter.capsule.TriggerCapsule;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.Trigger;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;
import org.tron.core.services.event.BlockEventCache;
import org.tron.core.services.event.SolidEventService;
import org.tron.core.services.event.bo.BlockEvent;
import org.tron.core.services.event.bo.SmartContractTrigger;

public class SolidEventServiceTest {

  SolidEventService solidEventService = new SolidEventService();

  @Test
  public void test() throws Exception {
    BlockEvent be0 = new BlockEvent();
    BlockCapsule.BlockId b0 = new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 1);
    be0.setBlockId(b0);
    be0.setParentId(b0);
    be0.setSolidId(new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 100));
    BlockEventCache.init(b0);
    BlockEventCache.add(be0);
    solidEventService.work();

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

    solidEventService.flush(be2);

    EventPluginLoader instance = mock(EventPluginLoader.class);
    ReflectUtils.setFieldValue(solidEventService, "instance", instance);

    BlockingQueue<TriggerCapsule> queue = new BlockingArrayQueue<>();
    Manager manager = mock(Manager.class);
    Mockito.when(manager.getTriggerCapsuleQueue()).thenReturn(queue);
    ReflectUtils.setFieldValue(solidEventService, "manager", manager);

    BlockCapsule blockCapsule = new BlockCapsule(0L, Sha256Hash.ZERO_HASH, 0L,
        ByteString.copyFrom(BlockEventCacheTest.getBlockId()));
    be2.setBlockLogTriggerCapsule(new BlockLogTriggerCapsule(blockCapsule));
    Mockito.when(instance.isBlockLogTriggerEnable()).thenReturn(true);
    Mockito.when(instance.isBlockLogTriggerSolidified()).thenReturn(true);

    solidEventService.flush(be2);

    Assert.assertEquals(1, queue.size());

    be2.setBlockLogTriggerCapsule(null);
    queue.poll();

    List<TransactionLogTriggerCapsule> list = new ArrayList<>();
    list.add(mock(TransactionLogTriggerCapsule.class));
    be2.setTransactionLogTriggerCapsules(list);

    Mockito.when(instance.isTransactionLogTriggerEnable()).thenReturn(true);
    Mockito.when(instance.isTransactionLogTriggerSolidified()).thenReturn(true);
    solidEventService.flush(be2);
    Assert.assertEquals(1, queue.size());

    be2.setTransactionLogTriggerCapsules(null);

    SmartContractTrigger contractTrigger = new SmartContractTrigger();
    be2.setSmartContractTrigger(contractTrigger);

    ContractEventTrigger eventTrigger = new ContractEventTrigger();
    eventTrigger.setRemoved(true);
    contractTrigger.getContractEventTriggers().add(eventTrigger);
    Mockito.when(instance.isSolidityEventTriggerEnable()).thenReturn(true);
    try (MockedStatic<EventPluginLoader> loader = Mockito.mockStatic(EventPluginLoader.class)) {
      loader.when(EventPluginLoader::getInstance).thenReturn(instance);
      solidEventService.flush(be2);
      Assert.assertEquals(Trigger.SOLIDITYEVENT_TRIGGER_NAME, eventTrigger.getTriggerName());
      Assert.assertFalse(eventTrigger.isRemoved());
      Mockito.verify(instance).postSolidityEventTrigger(eventTrigger);

      contractTrigger.getContractEventTriggers().clear();

      solidEventService.flush(be2);

      ContractLogTrigger logTrigger = new ContractLogTrigger();
      logTrigger.setRemoved(true);
      contractTrigger.getContractLogTriggers().add(logTrigger);
      Mockito.when(instance.isSolidityEventTriggerEnable()).thenReturn(false);
      Mockito.when(instance.isSolidityLogTriggerEnable()).thenReturn(true);
      solidEventService.flush(be2);
      Assert.assertEquals(Trigger.SOLIDITYLOG_TRIGGER_NAME, logTrigger.getTriggerName());
      Assert.assertFalse(logTrigger.isRemoved());
      Mockito.verify(instance).postSolidityLogTrigger(logTrigger);
    }

    be2.setSmartContractTrigger(null);

    Mockito.when(instance.isSolidityTriggerEnable()).thenReturn(true);
    be2.setSolidityTriggerCapsule(new SolidityTriggerCapsule(1));
    queue.clear();
    solidEventService.flush(be2);
    Assert.assertEquals(1, queue.size());
  }
}
