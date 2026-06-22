package org.tron.core.event;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.IPluginEventListener;
import org.tron.common.utils.ReflectUtils;

public class EventPluginLoaderTest {

  @Test
  public void testIsBusy() {

    EventPluginLoader eventPluginLoader = EventPluginLoader.getInstance();

    // When the event plugin is not active, the node is never "busy" regardless of the
    // native queue: back-pressure only throttles the plugin's async queue.
    ReflectUtils.setFieldValue(eventPluginLoader, "useEventPlugin", false);
    ReflectUtils.setFieldValue(eventPluginLoader, "useNativeQueue", true);
    boolean flag = eventPluginLoader.isBusy();
    Assert.assertFalse(flag);

    // When the plugin is active (with or without the native queue), busy-ness is driven
    // by the plugin queue size.
    ReflectUtils.setFieldValue(eventPluginLoader, "useEventPlugin", true);

    IPluginEventListener p1 = mock(IPluginEventListener.class);
    List<IPluginEventListener> list = new ArrayList<>();
    list.add(p1);
    ReflectUtils.setFieldValue(eventPluginLoader, "eventListeners", list);

    Mockito.when(p1.getPendingSize()).thenReturn(100);
    flag = eventPluginLoader.isBusy();
    Assert.assertFalse(flag);

    Mockito.when(p1.getPendingSize()).thenReturn(60000);
    flag = eventPluginLoader.isBusy();
    Assert.assertTrue(flag);

    Mockito.when(p1.getPendingSize()).thenThrow(new AbstractMethodError());
    flag = eventPluginLoader.isBusy();
    Assert.assertFalse(flag);
  }
}
