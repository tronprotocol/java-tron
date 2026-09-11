package org.tron.common.logsfilter;

import com.beust.jcommander.internal.Sets;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.pf4j.CompoundPluginDescriptorFinder;
import org.pf4j.DefaultPluginManager;
import org.pf4j.DefaultVersionManager;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginManager;
import org.pf4j.VersionManager;
import org.springframework.util.StringUtils;
import org.tron.common.logsfilter.nativequeue.NativeMessageQueue;
import org.tron.common.logsfilter.trigger.BlockLogTrigger;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.logsfilter.trigger.SolidityTrigger;
import org.tron.common.logsfilter.trigger.TransactionLogTrigger;
import org.tron.common.logsfilter.trigger.Trigger;

@Slf4j
public class EventPluginLoader {

  /**
   * Minimum event-plugin Plugin-Version compatible with this node. Bumped to 3.0.0 to
   * reject pre-fastjson-removal builds whose worker threads would fail with
   * NoClassDefFoundError on com.alibaba.fastjson at runtime. The previous event-plugin
   * release is 2.2.0, so 3.0.0 is the first version that ships the Jackson replacement.
   */
  static final String MIN_PLUGIN_VERSION = "3.0.0";

  private static final VersionManager VERSION_MANAGER = new DefaultVersionManager();

  private static EventPluginLoader instance;

  private long MAX_PENDING_SIZE = 50000;

  private PluginManager pluginManager = null;

  private List<IPluginEventListener> eventListeners;

  private ObjectMapper objectMapper = new ObjectMapper();

  private String serverAddress;

  private String dbConfig;

  private List<TriggerConfig> triggerConfigList;

  private int version = 0;

  private long startSyncBlockNum = 0;

  private boolean blockLogTriggerEnable = false;

  private boolean blockLogTriggerSolidified = false;

  private boolean transactionLogTriggerEnable = false;

  private boolean transactionLogTriggerSolidified = false;

  private boolean transactionLogTriggerEthCompatible = false;

  private boolean contractEventTriggerEnable = false;

  private boolean contractLogTriggerEnable = false;

  private boolean contractLogTriggerRedundancy = false;

  private boolean solidityEventTriggerEnable = false;

  private boolean solidityLogTriggerEnable = false;

  private boolean solidityLogTriggerRedundancy = false;

  private boolean solidityTriggerEnable = false;

  private FilterQuery filterQuery;

  @Getter
  private boolean useNativeQueue = false;

  // Whether the event plugin is an active sink. May be turned off at runtime if the
  // plugin fails to load under the "ignore" failure policy.
  @Getter
  private boolean useEventPlugin = false;

  private static final String FAIL_POLICY = "fail";

  private static final String IGNORE_POLICY = "ignore";

  private String pluginLoadFailurePolicy = FAIL_POLICY;

  public static EventPluginLoader getInstance() {
    if (Objects.isNull(instance)) {
      synchronized (EventPluginLoader.class) {
        if (Objects.isNull(instance)) {
          instance = new EventPluginLoader();
        }
      }
    }
    return instance;
  }

  public static boolean matchFilter(ContractTrigger trigger) {
    long blockNumber = trigger.getBlockNumber();

    FilterQuery filterQuery = EventPluginLoader.getInstance().getFilterQuery();
    if (Objects.isNull(filterQuery)) {
      return true;
    }

    long fromBlockNumber = filterQuery.getFromBlock();
    long toBlockNumber = filterQuery.getToBlock();

    boolean matched = false;
    if (fromBlockNumber == FilterQuery.LATEST_BLOCK_NUM
        || toBlockNumber == FilterQuery.EARLIEST_BLOCK_NUM) {
      logger.error("invalid filter: fromBlockNumber: {}, toBlockNumber: {}",
          fromBlockNumber, toBlockNumber);
      return false;
    }

    if (toBlockNumber == FilterQuery.LATEST_BLOCK_NUM) {
      if (fromBlockNumber == FilterQuery.EARLIEST_BLOCK_NUM) {
        matched = true;
      } else {
        if (blockNumber >= fromBlockNumber) {
          matched = true;
        }
      }
    } else {
      if (fromBlockNumber == FilterQuery.EARLIEST_BLOCK_NUM) {
        if (blockNumber <= toBlockNumber) {
          matched = true;
        }
      } else {
        if (blockNumber >= fromBlockNumber && blockNumber <= toBlockNumber) {
          matched = true;
        }
      }
    }

    if (!matched) {
      return false;
    }

    return filterContractAddress(trigger, filterQuery.getContractAddressList())
        && filterContractTopicList(trigger, filterQuery.getContractTopicList());
  }

  private static boolean filterContractAddress(ContractTrigger trigger, List<String> addressList) {
    addressList = addressList.stream().filter(item ->
            org.apache.commons.lang3.StringUtils.isNotEmpty(item))
        .collect(Collectors.toList());
    if (Objects.isNull(addressList) || addressList.isEmpty()) {
      return true;
    }

    String contractAddress = trigger.getContractAddress();
    if (Objects.isNull(contractAddress)) {
      return false;
    }

    for (String address : addressList) {
      if (contractAddress.equalsIgnoreCase(address)) {
        return true;
      }
    }
    return false;
  }

  private static boolean filterContractTopicList(ContractTrigger trigger, List<String> topList) {
    topList = topList.stream().filter(item -> org.apache.commons.lang3.StringUtils.isNotEmpty(item))
        .collect(Collectors.toList());
    if (Objects.isNull(topList) || topList.isEmpty()) {
      return true;
    }

    Set<String> hset = Sets.newHashSet();
    if (trigger instanceof ContractLogTrigger) {
      hset = ((ContractLogTrigger) trigger).getTopicList().stream().collect(Collectors.toSet());
    } else if (trigger instanceof ContractEventTrigger) {
      hset = new HashSet<>(((ContractEventTrigger) trigger).getTopicMap().values());
    } else if (trigger != null) {
      hset = trigger.getLogInfo().getClonedTopics()
          .stream().map(Hex::toHexString).collect(Collectors.toSet());
    }

    for (String top : topList) {
      if (hset.contains(top)) {
        return true;
      }
    }
    return false;
  }

  private boolean launchNativeQueue(EventPluginConfig config) {

    if (!NativeMessageQueue.getInstance()
        .start(config.getBindPort(), config.getSendQueueLength())) {
      return false;
    }

    if (Objects.isNull(triggerConfigList)) {
      logger.error("trigger config is null");
      return false;
    }

    triggerConfigList.forEach(triggerConfig -> {
      setSingleTriggerConfig(triggerConfig);
    });

    return true;
  }

  private boolean launchEventPlugin(EventPluginConfig config) {
    // parsing subscribe config from config.conf
    String pluginPath = config.getPluginPath();
    this.serverAddress = config.getServerAddress();
    this.dbConfig = config.getDbConfig();

    if (!startPlugin(pluginPath)) {
      logger.error("failed to load '{}'", pluginPath);
      return false;
    }

    setPluginConfig();

    if (Objects.nonNull(eventListeners)) {
      eventListeners.forEach(listener -> listener.start());
    }

    return true;
  }

  public boolean start(EventPluginConfig config) {

    if (Objects.isNull(config)) {
      return false;
    }

    this.version = config.getVersion();

    this.startSyncBlockNum = config.getStartSyncBlockNum();

    this.triggerConfigList = config.getTriggerConfigList();

    useNativeQueue = config.isUseNativeQueue();
    useEventPlugin = config.isUseEventPlugin();
    // Re-resolved every start() so no policy state leaks across runs; any unrecognized
    // value defaults to the safe "fail" policy rather than silently becoming "ignore".
    pluginLoadFailurePolicy = normalizeFailurePolicy(config.getPluginLoadFailurePolicy());

    if (!useNativeQueue && !useEventPlugin) {
      logger.error("No event sink is configured: enable the native queue "
          + "or set a plugin path.");
      return false;
    }

    // Start the plugin first so that, under the "fail" policy, a plugin problem is
    // surfaced before the native queue starts accepting subscribers.
    if (useEventPlugin && !launchEventPlugin(config)) {
      if (!IGNORE_POLICY.equals(pluginLoadFailurePolicy)) {
        return false;
      }
      // "ignore" policy: fall back to native-queue-only operation.
      logger.warn("Event plugin failed to load; continuing without it "
          + "(pluginLoadFailurePolicy = ignore).");
      useEventPlugin = false;
      if (!useNativeQueue) {
        return false;
      }
    }

    if (useNativeQueue && !launchNativeQueue(config)) {
      // Native queue failed after the plugin already started; stop the plugin so a
      // failed startup does not leak its threads/resources.
      if (useEventPlugin) {
        stopPlugin();
      }
      return false;
    }

    return true;
  }

  private String normalizeFailurePolicy(String policy) {
    String p = Objects.isNull(policy) ? "" : policy.trim();
    if (IGNORE_POLICY.equalsIgnoreCase(p)) {
      return IGNORE_POLICY;
    }
    if (!p.isEmpty() && !FAIL_POLICY.equalsIgnoreCase(p)) {
      logger.warn("Unknown pluginLoadFailurePolicy '{}', falling back to '{}'.",
          p, FAIL_POLICY);
    }
    return FAIL_POLICY;
  }

  private void setPluginConfig() {

    if (Objects.isNull(eventListeners)) {
      return;
    }

    // set server address to plugin
    eventListeners.forEach(listener -> listener.setServerAddress(this.serverAddress));

    // set db config to plugin
    eventListeners.forEach(listener -> listener.setDBConfig(this.dbConfig));

    triggerConfigList.forEach(triggerConfig -> {
      setSingleTriggerConfig(triggerConfig);
    });
  }

  private void setSingleTriggerConfig(TriggerConfig triggerConfig) {
    if (EventPluginConfig.BLOCK_TRIGGER_NAME.equalsIgnoreCase(triggerConfig.getTriggerName())) {
      if (triggerConfig.isEnabled()) {
        blockLogTriggerEnable = true;
        if (triggerConfig.isSolidified()) {
          blockLogTriggerSolidified = true;
        }
      } else {
        blockLogTriggerEnable = false;
        blockLogTriggerSolidified = false;
      }

      if (useEventPlugin) {
        setPluginTopic(Trigger.BLOCK_TRIGGER, triggerConfig.getTopic());
      }

    } else if (EventPluginConfig.TRANSACTION_TRIGGER_NAME
        .equalsIgnoreCase(triggerConfig.getTriggerName())) {
      if (triggerConfig.isEnabled()) {
        transactionLogTriggerEnable = true;
        if (triggerConfig.isEthCompatible()) {
          transactionLogTriggerEthCompatible = true;
        }
        if (triggerConfig.isSolidified()) {
          transactionLogTriggerSolidified = true;
        }
      } else {
        transactionLogTriggerEnable = false;
        transactionLogTriggerEthCompatible = false;
        transactionLogTriggerSolidified = false;
      }

      if (useEventPlugin) {
        setPluginTopic(Trigger.TRANSACTION_TRIGGER, triggerConfig.getTopic());
      }

    } else if (EventPluginConfig.CONTRACTEVENT_TRIGGER_NAME
        .equalsIgnoreCase(triggerConfig.getTriggerName())) {
      if (triggerConfig.isEnabled()) {
        contractEventTriggerEnable = true;
      } else {
        contractEventTriggerEnable = false;
      }

      if (useEventPlugin) {
        setPluginTopic(Trigger.CONTRACTEVENT_TRIGGER, triggerConfig.getTopic());
      }

    } else if (EventPluginConfig.CONTRACTLOG_TRIGGER_NAME
        .equalsIgnoreCase(triggerConfig.getTriggerName())) {
      if (triggerConfig.isEnabled()) {
        contractLogTriggerEnable = true;
        if (triggerConfig.isRedundancy()) {
          contractLogTriggerRedundancy = true;
        }
      } else {
        contractLogTriggerEnable = false;
        contractLogTriggerRedundancy = false;
      }

      if (useEventPlugin) {
        setPluginTopic(Trigger.CONTRACTLOG_TRIGGER, triggerConfig.getTopic());
      }
    } else if (EventPluginConfig.SOLIDITY_TRIGGER_NAME
        .equalsIgnoreCase(triggerConfig.getTriggerName())) {
      if (triggerConfig.isEnabled()) {
        solidityTriggerEnable = true;
      } else {
        solidityTriggerEnable = false;
      }
      if (useEventPlugin) {
        setPluginTopic(Trigger.SOLIDITY_TRIGGER, triggerConfig.getTopic());
      }
    } else if (EventPluginConfig.SOLIDITY_EVENT_NAME
        .equalsIgnoreCase(triggerConfig.getTriggerName())) {
      if (triggerConfig.isEnabled()) {
        solidityEventTriggerEnable = true;
      } else {
        solidityEventTriggerEnable = false;
      }

      if (useEventPlugin) {
        setPluginTopic(Trigger.SOLIDITY_EVENT_TRIGGER, triggerConfig.getTopic());
      }
    } else if (EventPluginConfig.SOLIDITY_LOG_NAME
        .equalsIgnoreCase(triggerConfig.getTriggerName())) {
      if (triggerConfig.isEnabled()) {
        solidityLogTriggerEnable = true;
        if (triggerConfig.isRedundancy()) {
          solidityLogTriggerRedundancy = true;
        }
      } else {
        solidityLogTriggerEnable = false;
        solidityLogTriggerRedundancy = false;
      }
      if (useEventPlugin) {
        setPluginTopic(Trigger.SOLIDITY_LOG_TRIGGER, triggerConfig.getTopic());
      }
    }
  }

  // Route a trigger to the plugin listeners, keeping a plugin fault from tearing down
  // the node's event thread. Serialization is done once by the caller and shared with
  // the native queue, so dual-sink mode adds no extra serialization on the hot path.
  private void postToPlugin(Consumer<IPluginEventListener> handler) {
    if (Objects.isNull(eventListeners)) {
      return;
    }
    // Isolate per listener so one misbehaving plugin can neither block delivery to the
    // others nor tear down the node's event thread. Errors (e.g. OutOfMemoryError) are
    // intentionally not caught and are allowed to propagate.
    for (IPluginEventListener listener : eventListeners) {
      try {
        handler.accept(listener);
      } catch (Exception e) {
        logger.error("event plugin listener failed to handle trigger", e);
      }
    }
  }

  public void postSolidityTrigger(SolidityTrigger trigger) {
    String json = toJsonString(trigger);
    if (useNativeQueue) {
      NativeMessageQueue.getInstance().publishTrigger(json, trigger.getTriggerName());
    }
    if (useEventPlugin) {
      postToPlugin(listener -> listener.handleSolidityTrigger(json));
    }
  }

  public synchronized int getVersion() {
    return version;
  }

  public synchronized long getStartSyncBlockNum() {
    return startSyncBlockNum;
  }

  public synchronized boolean isBlockLogTriggerEnable() {
    return blockLogTriggerEnable;
  }

  public synchronized boolean isBlockLogTriggerSolidified() {
    return blockLogTriggerSolidified;
  }

  public synchronized boolean isSolidityTriggerEnable() {
    return solidityTriggerEnable;
  }

  public synchronized boolean isSolidityEventTriggerEnable() {
    return solidityEventTriggerEnable;
  }

  public synchronized boolean isSolidityLogTriggerEnable() {
    return solidityLogTriggerEnable;
  }

  public synchronized boolean isSolidityLogTriggerRedundancy() {
    return solidityLogTriggerRedundancy;
  }

  public synchronized boolean isTransactionLogTriggerEnable() {
    return transactionLogTriggerEnable;
  }

  public synchronized boolean isTransactionLogTriggerEthCompatible() {
    return transactionLogTriggerEthCompatible;
  }

  public synchronized boolean isTransactionLogTriggerSolidified() {
    return transactionLogTriggerSolidified;
  }

  public synchronized boolean isContractEventTriggerEnable() {
    return contractEventTriggerEnable;
  }

  public synchronized boolean isContractLogTriggerEnable() {
    return contractLogTriggerEnable;
  }

  public synchronized boolean isContractLogTriggerRedundancy() {
    return contractLogTriggerRedundancy;
  }

  private void setPluginTopic(int eventType, String topic) {
    if (Objects.isNull(eventListeners)) {
      return;
    }
    eventListeners.forEach(listener -> listener.setTopic(eventType, topic));
  }

  private boolean startPlugin(String path) {

    logger.info("start loading '{}'", path);

    File pluginPath = new File(path);
    if (!pluginPath.exists()) {
      logger.error("'{}' doesn't exist", path);
      return false;
    }

    if (Objects.isNull(pluginManager)) {

      pluginManager = new DefaultPluginManager(pluginPath.toPath()) {
        @Override
        protected CompoundPluginDescriptorFinder createPluginDescriptorFinder() {
          return new CompoundPluginDescriptorFinder()
              .add(new ManifestPluginDescriptorFinder());
        }
      };
    }

    String pluginId = pluginManager.loadPlugin(pluginPath.toPath());
    if (StringUtils.isEmpty(pluginId)) {
      logger.error("invalid pluginID");
      return false;
    }

    if (!isPluginVersionSupported(pluginManager, pluginId)) {
      return false;
    }

    pluginManager.startPlugins();

    eventListeners = pluginManager.getExtensions(IPluginEventListener.class);

    if (Objects.isNull(eventListeners) || eventListeners.isEmpty()) {
      logger.error("No eventListener is registered");
      return false;
    }

    logger.info("'{}' loaded", path);

    return true;
  }

  static boolean isPluginVersionSupported(PluginManager pm, String pluginId) {
    String pluginVersion = pm.getPlugin(pluginId).getDescriptor().getVersion();
    if (Strings.isNullOrEmpty(pluginVersion)) {
      return false;
    }
    boolean isSupported = VERSION_MANAGER.compareVersions(pluginVersion, MIN_PLUGIN_VERSION) >= 0;

    if (!isSupported) {
      logger.error(
          "event-plugin '{}' version {} is older than required {}, please upgrade event-plugin",
          pluginId, pluginVersion, MIN_PLUGIN_VERSION);
    }
    return isSupported;
  }

  public void stopPlugin() {
    if (Objects.nonNull(pluginManager)) {
      pluginManager.stopPlugins();
    }

    NativeMessageQueue.getInstance().stop();

    logger.info("eventPlugin stopped");
  }

  public void postBlockTrigger(BlockLogTrigger trigger) {
    String json = toJsonString(trigger);
    if (useNativeQueue) {
      NativeMessageQueue.getInstance().publishTrigger(json, trigger.getTriggerName());
    }
    if (useEventPlugin) {
      postToPlugin(listener -> listener.handleBlockEvent(json));
    }
  }

  public void postSolidityLogTrigger(ContractLogTrigger trigger) {
    String json = toJsonString(trigger);
    if (useNativeQueue) {
      NativeMessageQueue.getInstance().publishTrigger(json, trigger.getTriggerName());
    }
    if (useEventPlugin) {
      postToPlugin(listener -> listener.handleSolidityLogTrigger(json));
    }
  }

  public void postSolidityEventTrigger(ContractEventTrigger trigger) {
    String json = toJsonString(trigger);
    if (useNativeQueue) {
      NativeMessageQueue.getInstance().publishTrigger(json, trigger.getTriggerName());
    }
    if (useEventPlugin) {
      postToPlugin(listener -> listener.handleSolidityEventTrigger(json));
    }
  }

  public void postTransactionTrigger(TransactionLogTrigger trigger) {
    String json = toJsonString(trigger);
    if (useNativeQueue) {
      NativeMessageQueue.getInstance().publishTrigger(json, trigger.getTriggerName());
    }
    if (useEventPlugin) {
      postToPlugin(listener -> listener.handleTransactionTrigger(json));
    }
  }

  public void postContractLogTrigger(ContractLogTrigger trigger) {
    String json = toJsonString(trigger);
    if (useNativeQueue) {
      NativeMessageQueue.getInstance().publishTrigger(json, trigger.getTriggerName());
    }
    if (useEventPlugin) {
      postToPlugin(listener -> listener.handleContractLogTrigger(json));
    }
  }

  public void postContractEventTrigger(ContractEventTrigger trigger) {
    String json = toJsonString(trigger);
    if (useNativeQueue) {
      NativeMessageQueue.getInstance().publishTrigger(json, trigger.getTriggerName());
    }
    if (useEventPlugin) {
      postToPlugin(listener -> listener.handleContractEventTrigger(json));
    }
  }

  public boolean isBusy() {
    // Back-pressure guards the plugin's async pending queue, so it applies whenever the
    // plugin is an active sink — including dual-sink mode. The native queue has no such
    // queue of its own and never reports busy.
    if (!useEventPlugin) {
      return false;
    }
    int queueSize = 0;
    if (eventListeners == null || eventListeners.isEmpty()) {
      // only occurs in mock test. TODO fix test
      return false;
    }
    for (IPluginEventListener listener : eventListeners) {
      try {
        queueSize += listener.getPendingSize();
      } catch (AbstractMethodError error) {
        break;
      }
    }
    return queueSize >= MAX_PENDING_SIZE;
  }

  private String toJsonString(Object data) {
    String jsonData = "";

    try {
      jsonData = objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      logger.error("'{}'", e);
    }

    return jsonData;
  }

  public synchronized FilterQuery getFilterQuery() {
    return filterQuery;
  }

  public synchronized void setFilterQuery(FilterQuery filterQuery) {
    this.filterQuery = filterQuery;
  }
}
