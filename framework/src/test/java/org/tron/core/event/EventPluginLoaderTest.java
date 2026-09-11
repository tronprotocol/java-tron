package org.tron.core.event;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.logsfilter.EventPluginConfig;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.IPluginEventListener;
import org.tron.common.logsfilter.trigger.BlockLogTrigger;
import org.tron.common.utils.ReflectUtils;

public class EventPluginLoaderTest {

  // EventPluginLoader is a process-wide singleton and these tests mutate its private
  // fields via reflection. Reset them after each test so state cannot leak into other
  // test classes that also use EventPluginLoader.getInstance().
  @After
  public void resetLoader() {
    EventPluginLoader loader = EventPluginLoader.getInstance();
    ReflectUtils.setFieldValue(loader, "useNativeQueue", false);
    ReflectUtils.setFieldValue(loader, "useEventPlugin", false);
    ReflectUtils.setFieldValue(loader, "eventListeners", null);
    ReflectUtils.setFieldValue(loader, "pluginLoadFailurePolicy", "fail");
  }

  private static IPluginEventListener mockListener(EventPluginLoader loader) {
    IPluginEventListener listener = mock(IPluginEventListener.class);
    List<IPluginEventListener> list = new ArrayList<>();
    list.add(listener);
    ReflectUtils.setFieldValue(loader, "eventListeners", list);
    return listener;
  }

  @Test
  public void testIsBusy() {

    EventPluginLoader eventPluginLoader = EventPluginLoader.getInstance();

    // Back-pressure is keyed on the plugin being active, not on the native queue.
    // Native-queue-only (plugin off) never reports busy.
    ReflectUtils.setFieldValue(eventPluginLoader, "useNativeQueue", true);
    ReflectUtils.setFieldValue(eventPluginLoader, "useEventPlugin", false);
    Assert.assertFalse(eventPluginLoader.isBusy());

    // Plugin active (whether standalone or dual-sink) does apply back-pressure.
    ReflectUtils.setFieldValue(eventPluginLoader, "useEventPlugin", true);
    IPluginEventListener p1 = mockListener(eventPluginLoader);

    Mockito.when(p1.getPendingSize()).thenReturn(100);
    Assert.assertFalse(eventPluginLoader.isBusy());

    Mockito.when(p1.getPendingSize()).thenReturn(60000);
    Assert.assertTrue(eventPluginLoader.isBusy());

    // Dual-sink mode: native queue on AND plugin on -> back-pressure still honored.
    ReflectUtils.setFieldValue(eventPluginLoader, "useNativeQueue", true);
    Assert.assertTrue(eventPluginLoader.isBusy());

    Mockito.when(p1.getPendingSize()).thenThrow(new AbstractMethodError());
    Assert.assertFalse(eventPluginLoader.isBusy());
  }

  /**
   * Regression for the upgrade scenario raised in review: a native-queue node that still
   * carries a stale plugin "path" must NOT start loading the plugin after an upgrade.
   * The plugin only joins when the operator explicitly opts in.
   */
  @Test
  public void testResolveUseEventPlugin() {
    // native queue OFF -> plugin is the only sink, active whenever a path is present.
    Assert.assertTrue(EventPluginConfig.resolveUseEventPlugin(true, false, false));
    Assert.assertFalse(EventPluginConfig.resolveUseEventPlugin(false, false, false));

    // native queue ON + stale path + no opt-in -> plugin stays OFF (the key regression).
    Assert.assertFalse(EventPluginConfig.resolveUseEventPlugin(true, true, false));

    // native queue ON + path + explicit opt-in -> dual-sink mode.
    Assert.assertTrue(EventPluginConfig.resolveUseEventPlugin(true, true, true));

    // opt-in without a path is meaningless -> plugin stays OFF.
    Assert.assertFalse(EventPluginConfig.resolveUseEventPlugin(false, true, true));
  }

  @Test
  public void testStartRequiresASink() {
    EventPluginLoader loader = EventPluginLoader.getInstance();
    EventPluginConfig config = new EventPluginConfig();
    config.setUseNativeQueue(false);
    config.setUseEventPlugin(false);
    Assert.assertFalse(loader.start(config));
  }

  @Test
  public void testPostRoutesToPluginOnlyWhenActive() {
    EventPluginLoader loader = EventPluginLoader.getInstance();
    IPluginEventListener listener = mockListener(loader);

    // Plugin inactive -> listener is never called.
    ReflectUtils.setFieldValue(loader, "useEventPlugin", false);
    ReflectUtils.setFieldValue(loader, "useNativeQueue", true);
    loader.postBlockTrigger(new BlockLogTrigger());
    Mockito.verify(listener, Mockito.never()).handleBlockEvent(Mockito.anyString());

    // Dual-sink: plugin active alongside native queue -> listener receives the trigger.
    ReflectUtils.setFieldValue(loader, "useEventPlugin", true);
    loader.postBlockTrigger(new BlockLogTrigger());
    Mockito.verify(listener, Mockito.times(1)).handleBlockEvent(Mockito.anyString());
  }

  @Test
  public void testPluginFailureIsIsolated() {
    EventPluginLoader loader = EventPluginLoader.getInstance();
    IPluginEventListener bad = mock(IPluginEventListener.class);
    IPluginEventListener good = mock(IPluginEventListener.class);
    List<IPluginEventListener> listeners = new ArrayList<>();
    listeners.add(bad);
    listeners.add(good);
    ReflectUtils.setFieldValue(loader, "eventListeners", listeners);
    ReflectUtils.setFieldValue(loader, "useEventPlugin", true);
    ReflectUtils.setFieldValue(loader, "useNativeQueue", false);

    Mockito.doThrow(new RuntimeException("boom"))
        .when(bad).handleBlockEvent(Mockito.anyString());

    // A misbehaving plugin must not propagate out of the node's event thread, and it
    // must not prevent delivery to the other listeners.
    loader.postBlockTrigger(new BlockLogTrigger());
    Mockito.verify(good, Mockito.times(1)).handleBlockEvent(Mockito.anyString());
  }

  /**
   * With the "ignore" failure policy, a plugin that cannot load must not abort startup;
   * the node falls back to native-queue-only and clears the plugin flag.
   */
  @Test
  public void testLoadFailurePolicyIgnoreDisablesPlugin() {
    EventPluginLoader loader = EventPluginLoader.getInstance();
    ReflectUtils.setFieldValue(loader, "eventListeners", null);

    EventPluginConfig config = new EventPluginConfig();
    config.setUseNativeQueue(false);
    config.setUseEventPlugin(true);
    config.setPluginPath("/non/existent/plugin/path.zip");
    config.setPluginLoadFailurePolicy("ignore");
    // No native queue left as a fallback -> start still fails (nothing to run).
    Assert.assertFalse(loader.start(config));
    Assert.assertFalse(loader.isUseEventPlugin());

    // "fail" policy on a bad path -> startup aborts.
    config.setPluginLoadFailurePolicy("fail");
    config.setUseEventPlugin(true);
    Assert.assertFalse(loader.start(config));

    // An unrecognized policy value must not silently behave as "ignore": it is
    // normalized to the safe "fail" default, and re-resolved on every start().
    config.setPluginLoadFailurePolicy("typo");
    config.setUseEventPlugin(true);
    Assert.assertFalse(loader.start(config));
    Assert.assertEquals("fail",
        ReflectUtils.getFieldValue(loader, "pluginLoadFailurePolicy"));
  }
}
