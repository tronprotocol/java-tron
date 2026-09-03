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
  public static final String SOLIDITY_TRIGGER_NAME = "solidity";
  public static final String SOLIDITY_EVENT_NAME = "solidityevent";
  public static final String SOLIDITY_LOG_NAME = "soliditylog";

  @Getter
  @Setter
  private int version;

  @Getter
  @Setter
  private long startSyncBlockNum;

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

  // Whether the event plugin should be activated. Resolved in Args from the plugin path
  // and the runPluginWithNativeQueue opt-in, so the loader never has to infer activation
  // from a possibly-stale path on its own.
  @Getter
  @Setter
  private boolean useEventPlugin;

  // See EventConfig#pluginLoadFailurePolicy ("fail" | "ignore").
  @Getter
  @Setter
  private String pluginLoadFailurePolicy = "fail";

  @Getter
  @Setter
  private int bindPort;

  @Getter
  @Setter
  private int sendQueueLength;


  @Getter
  @Setter
  private List<TriggerConfig> triggerConfigList;

  /**
   * Decide whether the event plugin should be an active sink.
   *
   * <p>Backward-compatible by design:
   * <ul>
   *   <li>native queue OFF: the plugin is the only sink, so it is active whenever a
   *       plugin path is configured (unchanged legacy behavior);</li>
   *   <li>native queue ON: the plugin runs alongside the queue only when the operator
   *       opts in via {@code runPluginWithNativeQueue}. A leftover path alone never
   *       activates it, so upgrading a native-queue node cannot suddenly load a plugin.
   *       </li>
   * </ul>
   */
  public static boolean resolveUseEventPlugin(boolean hasPluginPath, boolean useNativeQueue,
      boolean runPluginWithNativeQueue) {
    return hasPluginPath && (!useNativeQueue || runPluginWithNativeQueue);
  }

  public EventPluginConfig() {
    pluginPath = "";
    serverAddress = "";
    dbConfig = "";
    useNativeQueue = false;
    bindPort = 0;
    sendQueueLength = 0;
    triggerConfigList = new ArrayList<>();
  }
}
