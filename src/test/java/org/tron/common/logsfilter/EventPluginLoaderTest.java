package org.tron.common.logsfilter;

import org.junit.Assert;
import org.junit.Test;

public class EventPluginLoaderTest {
  @Test
  public synchronized void testStartFailed() {
    EventPluginConfig config = new EventPluginConfig();
    config.setServerAddress("127.0.0.1:9092");
    config.setPluginPath("/Users/tron/sourcecode/eventplugin/build/plugins/plugin-kafka-1.0.0.zip");
    TriggerConfig triggerConfig = new TriggerConfig();
    triggerConfig.setTopic("block");
    triggerConfig.setEnabled(true);
    triggerConfig.setTriggerName("block");
    config.getTriggerConfigList().add(triggerConfig);
    Assert.assertEquals(false, EventPluginLoader.getInstance().start(config));
  }
}
