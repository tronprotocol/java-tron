package org.tron.common.logsfilter;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class EventLoaderTest {
  @Test
  public void launchNativeQueue(){
    EventPluginConfig config = new EventPluginConfig();
    config.setSendQueueLength(1000);
    config.setBindPort(5555);
    config.setUseNativeQueue(true);

    List<TriggerConfig> triggerConfigList = new ArrayList<>();

    TriggerConfig blockTriggerConfig = new TriggerConfig();
    blockTriggerConfig.setTriggerName("block");
    blockTriggerConfig.setEnabled(true);
    blockTriggerConfig.setTopic("block");
    triggerConfigList.add(blockTriggerConfig);

    config.setTriggerConfigList(triggerConfigList);

    Assert.assertEquals(true,EventPluginLoader.getInstance().start(config));

    EventPluginLoader.getInstance().stopPlugin();
  }
}
