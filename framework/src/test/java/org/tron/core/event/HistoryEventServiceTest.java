package org.tron.core.event;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;
import org.tron.core.services.event.BlockEventGet;
import org.tron.core.services.event.BlockEventLoad;
import org.tron.core.services.event.HistoryEventService;
import org.tron.core.services.event.RealtimeEventService;
import org.tron.core.services.event.SolidEventService;
import org.tron.core.services.event.bo.BlockEvent;
import org.tron.core.store.DynamicPropertiesStore;

public class HistoryEventServiceTest {

  HistoryEventService historyEventService = new HistoryEventService();

  @Test
  public void test() throws Exception {
    EventPluginLoader instance = mock(EventPluginLoader.class);
    ReflectUtils.setFieldValue(historyEventService, "instance", instance);

    DynamicPropertiesStore dynamicPropertiesStore = mock(DynamicPropertiesStore.class);
    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    Manager manager = mock(Manager.class);
    ReflectUtils.setFieldValue(historyEventService, "manager", manager);
    Mockito.when(manager.getChainBaseManager()).thenReturn(chainBaseManager);
    Mockito.when(manager.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStore);
    Mockito.when(chainBaseManager.getHeadBlockId()).thenReturn(new BlockCapsule.BlockId());

    SolidEventService solidEventService = new SolidEventService();
    RealtimeEventService realtimeEventService = new RealtimeEventService();
    BlockEventLoad blockEventLoad = new BlockEventLoad();

    ReflectUtils.setFieldValue(historyEventService, "solidEventService", solidEventService);
    ReflectUtils.setFieldValue(historyEventService, "realtimeEventService", realtimeEventService);
    ReflectUtils.setFieldValue(historyEventService, "blockEventLoad", blockEventLoad);
    historyEventService.init();
    historyEventService.close();
    solidEventService.close();
    realtimeEventService.close();
    blockEventLoad.close();

    solidEventService = mock(SolidEventService.class);
    ReflectUtils.setFieldValue(historyEventService, "solidEventService", solidEventService);
    realtimeEventService = mock(RealtimeEventService.class);
    ReflectUtils.setFieldValue(historyEventService, "realtimeEventService", realtimeEventService);
    blockEventLoad = mock(BlockEventLoad.class);
    ReflectUtils.setFieldValue(historyEventService, "blockEventLoad", blockEventLoad);

    Mockito.when(instance.getStartSyncBlockNum()).thenReturn(0L);

    Mockito.when(dynamicPropertiesStore.getLatestSolidifiedBlockNum()).thenReturn(0L);
    Mockito.when(chainBaseManager.getBlockIdByNum(0L))
        .thenReturn(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 0));
    historyEventService.init();

    BlockEvent be2 = new BlockEvent();
    BlockCapsule.BlockId b2 = new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 2);
    be2.setBlockId(b2);

    BlockEventGet blockEventGet = mock(BlockEventGet.class);
    ReflectUtils.setFieldValue(historyEventService, "blockEventGet", blockEventGet);
    Mockito.when(blockEventGet.getBlockEvent(1)).thenReturn(be2);

    Mockito.when(instance.getStartSyncBlockNum()).thenReturn(1L);
    Mockito.when(dynamicPropertiesStore.getLatestSolidifiedBlockNum()).thenReturn(1L);

    Mockito.when(chainBaseManager.getBlockIdByNum(1L))
        .thenReturn(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 1));

    Method method1 = historyEventService.getClass().getDeclaredMethod("syncEvent");
    method1.setAccessible(true);
    method1.invoke(historyEventService);

    historyEventService.init();
    historyEventService.close();
  }
}
