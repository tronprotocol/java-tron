package org.tron.core.config.args;

import static org.tron.core.config.Parameter.ChainConstant.MAX_ACTIVE_WITNESS_NUM;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

// Node configuration bean for the "node" section of config.conf.
// ConfigBeanFactory auto-binds all fields including sub-beans, dot-notation keys,
// PBFT fields, and list fields. Only legacy key fallbacks and PascalCase shutdown
// keys are read manually.
@Slf4j
@Getter
@Setter
@SuppressWarnings("unused") // setters used by ConfigBeanFactory via reflection
public class NodeConfig {

  // ---- Flat scalar fields (auto-bound by ConfigBeanFactory) ----
  private String trustNode = "";
  private boolean walletExtensionApi = false;
  private int syncFetchBatchNum = 2000;
  private int validateSignThreadNum = 0; // 0 = auto (availableProcessors)
  private int maxConnections = 30;
  private int minConnections = 8;
  private int minActiveConnections = 3;
  private int maxConnectionsWithSameIp = 2;
  private int maxHttpConnectNumber = 50;
  private int minParticipationRate = 0;
  private boolean openPrintLog = true;
  private boolean openTransactionSort = false;
  private int maxTps = 1000;
  // Config key "isOpenFullTcpDisconnect" cannot auto-bind — read manually in fromConfig()
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private boolean isOpenFullTcpDisconnect = false;

  public boolean isOpenFullTcpDisconnect() { return isOpenFullTcpDisconnect; }

  // node.discovery.* — HOCON merges into node { discovery { ... } }, auto-bound
  private DiscoveryConfig discovery = new DiscoveryConfig();

  // node.shutdown.* uses PascalCase keys (BlockTime, BlockHeight, BlockCount)
  // that don't match JavaBean naming. Excluded, read manually.
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private String shutdownBlockTime = "";
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private long shutdownBlockHeight = -1;
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private long shutdownBlockCount = -1;

  public boolean isDiscoveryEnable() { return discovery.isEnable(); }
  public boolean isDiscoveryPersist() { return discovery.isPersist(); }
  public String getDiscoveryExternalIp() { return discovery.getExternal().getIp(); }
  public String getShutdownBlockTime() { return shutdownBlockTime; }
  public long getShutdownBlockHeight() { return shutdownBlockHeight; }
  public long getShutdownBlockCount() { return shutdownBlockCount; }
  private int inactiveThreshold = 600;
  private boolean metricsEnable = false;
  private int blockProducedTimeOut = 50;
  private int netMaxTrxPerSecond = 700;
  private boolean nodeDetectEnable = false;
  private boolean enableIpv6 = false;
  private boolean effectiveCheckEnable = false;
  private int maxFastForwardNum = 4;
  private int tcpNettyWorkThreadNum = 0;
  private int udpNettyWorkThreadNum = 1;
  private ValidContractProtoConfig validContractProto = new ValidContractProtoConfig();
  private int shieldedTransInPendingMaxCounts = 10;
  private long blockCacheTimeout = 60;
  private long receiveTcpMinDataLength = 2048;
  private ChannelConfig channel = new ChannelConfig();
  private int maxTransactionPendingSize = 2000;
  private long pendingTransactionTimeout = 60000;
  private int agreeNodeCount = 0;
  private boolean openHistoryQueryWhenLiteFN = false;
  private boolean unsolidifiedBlockCheck = false;
  private int maxUnsolidifiedBlocks = 54;
  private String zenTokenId = "000000";
  private boolean allowShieldedTransactionApi = true;
  private double activeConnectFactor = 0.1;
  private double connectFactor = 0.6;
  // Legacy alias `maxActiveNodesWithSameIp` has no bean field: we only peek at it via
  // section.hasPath() below. Keeping it field-less means reference.conf doesn't have to
  // ship a default that would otherwise mask the modern `maxConnectionsWithSameIp` key.

  // ---- Sub-beans matching config's dot-notation nested structure ----
  private ListenConfig listen = new ListenConfig();
  private ConnectionConfig connection = new ConnectionConfig();
  private FetchBlockConfig fetchBlock = new FetchBlockConfig();
  private SolidityConfig solidity = new SolidityConfig();

  // Convenience getters for backward compatibility with applyNodeConfig
  public int getListenPort() { return listen.getPort(); }
  public int getConnectionTimeout() { return connection.getTimeout(); }
  public int getFetchBlockTimeout() { return fetchBlock.getTimeout(); }
  public int getSolidityThreads() { return solidity.getThreads(); }
  public int getChannelReadTimeout() { return channel.getRead().getTimeout(); }
  public int getValidContractProtoThreads() { return validContractProto.getThreads(); }

  // ---- List fields (manually read) ----
  private List<String> active = new ArrayList<>();
  private List<String> passive = new ArrayList<>();
  private List<String> fastForward = new ArrayList<>();
  private List<String> disabledApi = new ArrayList<>();

  // ---- Sub-object fields ----
  private P2pConfig p2p = new P2pConfig();
  private HttpConfig http = new HttpConfig();
  private RpcConfig rpc = new RpcConfig();
  private JsonRpcConfig jsonrpc = new JsonRpcConfig();
  private NodeBackupConfig backup = new NodeBackupConfig();
  private DynamicConfigSection dynamicConfig = new DynamicConfigSection();
  private DnsConfig dns = new DnsConfig();

  // ===========================================================================
  // Inner static classes for sub-beans
  // ===========================================================================

  // ---- Sub-beans for dot-notation config keys ----
  // HOCON merges dot-notation into nested objects, ConfigBeanFactory auto-binds

  @Getter
  @Setter
  public static class DiscoveryConfig {
    private boolean enable = false;
    private boolean persist = false;
    private ExternalConfig external = new ExternalConfig();

    @Getter
    @Setter
    public static class ExternalConfig {
      private String ip = "";
    }
  }

  @Getter
  @Setter
  public static class ListenConfig {
    private int port = 18888;
  }

  @Getter
  @Setter
  public static class ConnectionConfig {
    private int timeout = 2;
  }

  @Getter
  @Setter
  public static class FetchBlockConfig {
    private int timeout = 500;
  }

  @Getter
  @Setter
  public static class SolidityConfig {
    private int threads = 0; // 0 = auto (availableProcessors)
  }

  @Getter
  @Setter
  public static class ChannelConfig {
    private ReadConfig read = new ReadConfig();

    @Getter
    @Setter
    public static class ReadConfig {
      private int timeout = 0;
    }
  }

  @Getter
  @Setter
  public static class ValidContractProtoConfig {
    private int threads = 0; // 0 = auto (availableProcessors)
  }

  @Getter
  @Setter
  public static class P2pConfig {
    private int version = 11111;
  }

  @Getter
  @Setter
  public static class HttpConfig {
    private boolean fullNodeEnable = true;
    private int fullNodePort = 8090;
    private boolean solidityEnable = true;
    private int solidityPort = 8091;
    // PBFT fields — handled manually (same naming issue as CommitteeConfig)
    // Default must match CommonParameter.pBFTHttpEnable = true
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private boolean pBFTEnable = true;
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private int pBFTPort = 8092;

    public boolean isPBFTEnable() {
      return pBFTEnable;
    }

    public void setPBFTEnable(boolean v) {
      this.pBFTEnable = v;
    }

    public int getPBFTPort() {
      return pBFTPort;
    }

    public void setPBFTPort(int v) {
      this.pBFTPort = v;
    }
  }

  @Getter
  @Setter
  public static class RpcConfig {
    private boolean enable = true;
    private int port = 50051;
    private boolean solidityEnable = true;
    private int solidityPort = 50061;
    // PBFT fields — handled manually
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private boolean pBFTEnable = true;
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private int pBFTPort = 50071;

    public boolean isPBFTEnable() {
      return pBFTEnable;
    }

    public void setPBFTEnable(boolean v) {
      this.pBFTEnable = v;
    }

    public int getPBFTPort() {
      return pBFTPort;
    }

    public void setPBFTPort(int v) {
      this.pBFTPort = v;
    }

    private int thread = 0;
    private int maxConcurrentCallsPerConnection = 2147483647;
    private int flowControlWindow = 1048576;
    private long maxConnectionIdleInMillis = Long.MAX_VALUE;
    private long maxConnectionAgeInMillis = Long.MAX_VALUE;
    private int maxMessageSize = 4194304;
    private int maxHeaderListSize = 8192;
    private int maxRstStream = 0;
    private int secondsPerWindow = 0;
    private int minEffectiveConnection = 1;
    private boolean reflectionService = false;
    private boolean trxCacheEnable = false;
  }

  @Getter
  @Setter
  public static class JsonRpcConfig {
    private boolean httpFullNodeEnable = false;
    private int httpFullNodePort = 8545;
    private boolean httpSolidityEnable = false;
    private int httpSolidityPort = 8555;
    // PBFT fields — handled manually
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private boolean httpPBFTEnable = false;
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private int httpPBFTPort = 8565;

    public boolean isHttpPBFTEnable() {
      return httpPBFTEnable;
    }

    public void setHttpPBFTEnable(boolean v) {
      this.httpPBFTEnable = v;
    }

    public int getHttpPBFTPort() {
      return httpPBFTPort;
    }

    public void setHttpPBFTPort(int v) {
      this.httpPBFTPort = v;
    }

    private int maxBlockRange = 5000;
    private int maxSubTopics = 1000;
    private int maxBlockFilterNum = 50000;
  }

  @Getter
  @Setter
  public static class NodeBackupConfig {
    private int priority = 0;
    private int port = 10001;
    private int keepAliveInterval = 3000;
    private List<String> members = new ArrayList<>();
  }

  @Getter
  @Setter
  public static class DynamicConfigSection {
    private boolean enable = false;
    private long checkInterval = 600;
  }

  @Getter
  @Setter
  public static class DnsConfig {
    private List<String> treeUrls = new ArrayList<>();
    private boolean publish = false;
    private String dnsDomain = "";
    private String dnsPrivate = "";
    private List<String> knownUrls = new ArrayList<>();
    private List<String> staticNodes = new ArrayList<>();
    private int maxMergeSize = 0;
    private double changeThreshold = 0.0;
    private String serverType = "";
    private String accessKeyId = "";
    private String accessKeySecret = "";
    private String aliyunDnsEndpoint = "";
    private String awsRegion = "";
    private String awsHostZoneId = "";
  }

  // Defaults come from reference.conf (loaded globally via Configuration.java)

  // ===========================================================================
  // Factory method
  // ===========================================================================

  /**
   * Create NodeConfig from the "node" section of the application config.
   *
   * <p>Dot-notation keys (listen.port, connection.timeout, fetchBlock.timeout,
   * solidity.threads) become nested HOCON objects and cannot be auto-bound to flat
   * Java fields. They are read manually after ConfigBeanFactory binding.
   *
   * <p>PBFT-named fields in http, rpc, and jsonrpc sub-beans have the same JavaBean
   * naming issue as CommitteeConfig and are patched manually.
   *
   * <p>List fields (active, passive, fastForward, disabledApi) are read manually
   * since ConfigBeanFactory expects typed bean lists, not string lists.
   */
  public static NodeConfig fromConfig(Config config) {
    Config section = config.getConfig("node");

    // Auto-bind all fields and sub-beans. ConfigBeanFactory fails fast with a
    // descriptive path on any `= null` value — external configs that use the
    // HOCON null keyword should fix their config rather than rely on silent coercion.
    NodeConfig nc = ConfigBeanFactory.create(section, NodeConfig.class);

    // isOpenFullTcpDisconnect: boolean "is" prefix breaks JavaBean pairing
    nc.isOpenFullTcpDisconnect = getBool(section, "isOpenFullTcpDisconnect", false);

    // --- Legacy key fallbacks (backward compatibility) ---
    // node.maxActiveNodes (old) -> maxConnections (new)
    if (section.hasPath("maxActiveNodes")) {
      nc.maxConnections = section.getInt("maxActiveNodes");
      if (section.hasPath("connectFactor")) {
        nc.minConnections = (int) (nc.maxConnections * section.getDouble("connectFactor"));
      }
      if (section.hasPath("activeConnectFactor")) {
        nc.minActiveConnections = (int) (nc.maxConnections
            * section.getDouble("activeConnectFactor"));
      }
    }
    if (section.hasPath("maxActiveNodesWithSameIp")) {
      nc.maxConnectionsWithSameIp = section.getInt("maxActiveNodesWithSameIp");
    }

    // Legacy key fallback: node.fullNodeAllowShieldedTransaction -> allowShieldedTransactionApi.
    // reference.conf does not ship the legacy key, so hasPath here reliably means the user
    // set it in their config. When present, it overrides the modern key.
    if (section.hasPath("fullNodeAllowShieldedTransaction")) {
      nc.allowShieldedTransactionApi = section.getBoolean("fullNodeAllowShieldedTransaction");
      logger.warn("Configuring [node.fullNodeAllowShieldedTransaction] will be deprecated. "
          + "Please use [node.allowShieldedTransactionApi] instead.");
    }
    // node.shutdown.* — PascalCase keys (BlockTime, BlockHeight), cannot auto-bind
    nc.shutdownBlockTime = config.hasPath("node.shutdown.BlockTime")
        ? config.getString("node.shutdown.BlockTime") : "";
    nc.shutdownBlockHeight = config.hasPath("node.shutdown.BlockHeight")
        ? config.getLong("node.shutdown.BlockHeight") : -1;
    nc.shutdownBlockCount = config.hasPath("node.shutdown.BlockCount")
        ? config.getLong("node.shutdown.BlockCount") : -1;


    nc.postProcess();
    return nc;
  }

  /**
   * Post-processing: clamping, dynamic defaults, and cross-field validation.
   * Runs after ConfigBeanFactory binding and manual field reads.
   */
  private void postProcess() {
    // rpcThreadNum: 0 = auto-detect
    if (rpc.thread == 0) {
      rpc.thread = (Runtime.getRuntime().availableProcessors() + 1) / 2;
    }

    // validateSignThreadNum: 0 = auto-detect
    if (validateSignThreadNum == 0) {
      validateSignThreadNum = Runtime.getRuntime().availableProcessors();
    }

    // solidityThreads: 0 = auto-detect
    if (solidity.threads == 0) {
      solidity.threads = Runtime.getRuntime().availableProcessors();
    }

    // validContractProto.threads: 0 = auto-detect (matches develop Args.java:743-746)
    if (validContractProto.threads == 0) {
      validContractProto.threads = Runtime.getRuntime().availableProcessors();
    }

    // syncFetchBatchNum: clamp to [100, 2000]
    if (syncFetchBatchNum > 2000) {
      syncFetchBatchNum = 2000;
    }
    if (syncFetchBatchNum < 100) {
      syncFetchBatchNum = 100;
    }

    // blockProducedTimeOut: clamp to [30, 100]
    if (blockProducedTimeOut < 30) {
      blockProducedTimeOut = 30;
    }
    if (blockProducedTimeOut > 100) {
      blockProducedTimeOut = 100;
    }

    // inactiveThreshold: minimum 1
    if (inactiveThreshold < 1) {
      inactiveThreshold = 1;
    }

    // maxFastForwardNum: clamp to [1, MAX_ACTIVE_WITNESS_NUM]
    if (maxFastForwardNum > MAX_ACTIVE_WITNESS_NUM) {
      maxFastForwardNum = MAX_ACTIVE_WITNESS_NUM;
    }
    if (maxFastForwardNum < 1) {
      maxFastForwardNum = 1;
    }

    // agreeNodeCount: 0 = auto (2/3 + 1 of witnesses), clamp to max
    if (agreeNodeCount == 0) {
      agreeNodeCount = MAX_ACTIVE_WITNESS_NUM * 2 / 3 + 1;
    }
    if (agreeNodeCount > MAX_ACTIVE_WITNESS_NUM) {
      agreeNodeCount = MAX_ACTIVE_WITNESS_NUM;
    }

    // dynamicConfigCheckInterval: minimum 600
    if (dynamicConfig.checkInterval <= 0) {
      dynamicConfig.checkInterval = 600;
    }
  }

  // ===========================================================================
  // Helper methods for safe config reads
  // ===========================================================================

  private static int getInt(Config config, String path, int defaultValue) {
    return config.hasPath(path) ? config.getInt(path) : defaultValue;
  }

  private static long getLong(Config config, String path, long defaultValue) {
    return config.hasPath(path) ? config.getLong(path) : defaultValue;
  }

  private static boolean getBool(Config config, String path, boolean defaultValue) {
    return config.hasPath(path) ? config.getBoolean(path) : defaultValue;
  }

  private static String getString(Config config, String path, String defaultValue) {
    return config.hasPath(path) ? config.getString(path) : defaultValue;
  }

}
