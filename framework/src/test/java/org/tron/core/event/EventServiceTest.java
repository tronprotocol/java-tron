package org.tron.core.event;

import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.db.Manager;
import org.tron.core.services.event.BlockEventLoad;
import org.tron.core.services.event.EventService;
import org.tron.core.services.event.HistoryEventService;
import org.tron.core.services.event.RealtimeEventService;

public class EventServiceTest {

  @Test
  public void test() {
    EventService eventService = new EventService();
    HistoryEventService historyEventService = new HistoryEventService();
    RealtimeEventService realtimeEventService = new RealtimeEventService();
    BlockEventLoad blockEventLoad = new BlockEventLoad();

    ReflectUtils.setFieldValue(eventService, "historyEventService", historyEventService);
    ReflectUtils.setFieldValue(eventService, "realtimeEventService", realtimeEventService);
    ReflectUtils.setFieldValue(eventService, "blockEventLoad", blockEventLoad);

    Manager manager = mock(Manager.class);
    ReflectUtils.setFieldValue(eventService, "manager", manager);
    Mockito.when(manager.isEventPluginLoaded()).thenReturn(true);

    eventService.init();
    eventService.close();
  }
}
