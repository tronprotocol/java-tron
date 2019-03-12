package org.tron.common.logsfilter;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class EventPluginConfig {

  public static final String BLOCK_TRIGGER_NAME = "block";
  public static final String TRANSACTION_TRIGGER_NAME = "transaction";
  public static final String CONTRACTEVENT_TRIGGER_NAME = "contractevent";
  public static final String CONTRACTLOG_TRIGGER_NAME = "contractlog";

  @Getter
  @Setter
  private String pluginPath;

  @Getter
  @Setter
  private String serverAddress;

  @Getter
  @Setter
  private String dbConfig;

  @Getter
  @Setter
  private boolean useNativeQueue;


  @Getter
  @Setter
  private List<TriggerConfig> triggerConfigList;

  public EventPluginConfig() {
    pluginPath = "";
    serverAddress = "";
    dbConfig = "";
    useNativeQueue = false;
    triggerConfigList = new ArrayList<>();
  }
}
