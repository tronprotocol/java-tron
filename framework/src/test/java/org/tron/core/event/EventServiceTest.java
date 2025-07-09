package org.tron.core.event;

import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;
import org.tron.core.services.event.BlockEventCache;
import org.tron.core.services.event.BlockEventLoad;
import org.tron.core.services.event.EventService;
import org.tron.core.services.event.HistoryEventService;
import org.tron.core.services.event.RealtimeEventService;
import org.tron.core.services.event.SolidEventService;

public class EventServiceTest {

  @Test
  public void test() {
    BlockCapsule.BlockId b1 = new BlockCapsule.BlockId(BlockEventCacheTest.getBlockId(), 1);
    BlockEventCache.init(b1);

    EventService eventService = new EventService();
    HistoryEventService historyEventService = new HistoryEventService();
    RealtimeEventService realtimeEventService = new RealtimeEventService();
    SolidEventService solidEventService = new SolidEventService();
    BlockEventLoad blockEventLoad = new BlockEventLoad();

    ReflectUtils.setFieldValue(eventService, "historyEventService", historyEventService);
    ReflectUtils.setFieldValue(eventService, "solidEventService", solidEventService);
    ReflectUtils.setFieldValue(eventService, "realtimeEventService", realtimeEventService);
    ReflectUtils.setFieldValue(eventService, "blockEventLoad", blockEventLoad);

    Manager manager = mock(Manager.class);
    ReflectUtils.setFieldValue(eventService, "manager", manager);
    Mockito.when(manager.isEventPluginLoaded()).thenReturn(true);

    eventService.init();
    eventService.close();

    EventPluginLoader instance = mock(EventPluginLoader.class);
    Mockito.when(instance.getVersion()).thenReturn(1);
    ReflectUtils.setFieldValue(eventService, "instance", instance);
    eventService.close();
  }
}
