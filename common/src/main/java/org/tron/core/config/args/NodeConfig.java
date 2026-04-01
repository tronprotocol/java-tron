package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Node configuration bean for the "node" section of config.conf.
 *
 * <p>This section is complex: it mixes flat scalars, dot-notation nested keys
 * (e.g. "listen.port"), sub-objects (http, rpc, jsonrpc, p2p, dynamicConfig, dns),
 * and list fields (active, passive, fastForward, disabledApi).
 *
 * <p>Strategy:
 * <ul>
 *   <li>ConfigBeanFactory handles simple scalar fields and clean sub-objects</li>
 *   <li>Dot-notation fields (listen.port, connection.timeout, fetchBlock.timeout,
 *       solidity.threads) are read manually — HOCON parses them as nested objects,
 *       not flat keys, so ConfigBeanFactory cannot bind them to flat fields</li>
 *   <li>PBFT-named fields in sub-beans have the same JavaBean naming issue as
 *       CommitteeConfig — handled manually after binding</li>
 *   <li>List fields are read manually since ConfigBeanFactory expects bean lists</li>
 * </ul>
 */
@Slf4j
@Getter
@Setter
@SuppressWarnings("unused") // setters used by ConfigBeanFactory via reflection
public class NodeConfig {

  // ---- Flat scalar fields (auto-bound by ConfigBeanFactory) ----
  private String trustNode = "127.0.0.1:50051";
  private boolean walletExtensionApi = true;
  private int syncFetchBatchNum = 2000;
  private int validateSignThreadNum = Runtime.getRuntime().availableProcessors();
  private int maxConnections = 30;
  private int minConnections = 8;
  private int minActiveConnections = 3;
  private int maxConnectionsWithSameIp = 2;
  private int maxHttpConnectNumber = 50;
  private int minParticipationRate = 15;
  private boolean openPrintLog = true;
  private boolean openTransactionSort = false;
  private int maxTps = 1000;
  // "isOpenFullTcpDisconnect" in config.conf: JavaBean convention converts
  // setOpenFullTcpDisconnect -> key "openFullTcpDisconnect", but config uses
  // "isOpenFullTcpDisconnect". Excluded from auto-binding, read manually.
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private boolean isOpenFullTcpDisconnect = false;

  public boolean isOpenFullTcpDisconnect() { return isOpenFullTcpDisconnect; }

  // node.discovery.* and node.channel.read.timeout — dot-notation, manually read
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private boolean discoveryEnable = false;
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private boolean discoveryPersist = false;

  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private String discoveryExternalIp = "";

  // node.shutdown.* — dot-notation, manually read
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private String shutdownBlockTime = "";
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private long shutdownBlockHeight = -1;
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private long shutdownBlockCount = -1;

  public boolean isDiscoveryEnable() { return discoveryEnable; }
  public boolean isDiscoveryPersist() { return discoveryPersist; }
  public String getDiscoveryExternalIp() { return discoveryExternalIp; }
  public String getShutdownBlockTime() { return shutdownBlockTime; }
  public long getShutdownBlockHeight() { return shutdownBlockHeight; }
  public long getShutdownBlockCount() { return shutdownBlockCount; }
  private int inactiveThreshold = 600;
  private boolean metricsEnable = false;
  private int blockProducedTimeOut = 75;
  private int netMaxTrxPerSecond = 700;
  private boolean nodeDetectEnable = false;
  private boolean enableIpv6 = false;
  private boolean effectiveCheckEnable = false;
  private int maxFastForwardNum = 4;
  private int tcpNettyWorkThreadNum = 0;
  private int udpNettyWorkThreadNum = 1;
  private int validContractProtoThreads = 2;
  private int shieldedTransInPendingMaxCounts = 10;
  private long blockCacheTimeout = 60;
  private long receiveTcpMinDataLength = 2048;
  private int channelReadTimeout = 60;
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
  private double disconnectNumberFactor = 0.4;
  private int maxActiveNodesWithSameIp = 2;

  // ---- Dot-notation fields (manually read — HOCON treats them as nested) ----
  // Excluded from ConfigBeanFactory auto-binding because HOCON parses dot-notation
  // keys (listen.port, connection.timeout, fetchBlock.timeout, solidity.threads) as
  // nested objects, not flat keys. ConfigBeanFactory expects flat key "listenPort" but
  // config has "listen { port }". Read manually in fromConfig().
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private int listenPort = 18888;
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private int connectionTimeout = 2;
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private int fetchBlockTimeout = 200;
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private int solidityThreads = Runtime.getRuntime().availableProcessors();

  public int getListenPort() { return listenPort; }
  public int getConnectionTimeout() { return connectionTimeout; }
  public int getFetchBlockTimeout() { return fetchBlockTimeout; }
  public int getSolidityThreads() { return solidityThreads; }

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

    private int thread = 16;
    private int maxConcurrentCallsPerConnection = 100;
    private int flowControlWindow = 0;
    private long maxConnectionIdleInMillis = Long.MAX_VALUE;
    private long maxConnectionAgeInMillis = Long.MAX_VALUE;
    private int maxMessageSize = 0;
    private int maxHeaderListSize = 0;
    private int maxRstStream = 0;
    private int secondsPerWindow = 0;
    private int minEffectiveConnection = 0;
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
    private int maxBlockFilterNum = 0;
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

  // ===========================================================================
  // DEFAULTS
  // ===========================================================================

  private static final Config DEFAULTS;

  static {
    int cpus = Runtime.getRuntime().availableProcessors();
    StringBuilder sb = new StringBuilder();
    // flat scalars
    sb.append("trustNode = \"127.0.0.1:50051\"\n");
    sb.append("walletExtensionApi = true\n");
    sb.append("syncFetchBatchNum = 2000\n");
    sb.append("validateSignThreadNum = ").append(cpus).append("\n");
    sb.append("maxConnections = 30\n");
    sb.append("minConnections = 8\n");
    sb.append("minActiveConnections = 3\n");
    sb.append("maxConnectionsWithSameIp = 2\n");
    sb.append("maxHttpConnectNumber = 50\n");
    sb.append("minParticipationRate = 15\n");
    sb.append("openPrintLog = true\n");
    sb.append("openTransactionSort = false\n");
    sb.append("maxTps = 1000\n");
    // isOpenFullTcpDisconnect excluded from auto-binding — read manually in fromConfig()
    sb.append("inactiveThreshold = 600\n");
    sb.append("metricsEnable = false\n");
    sb.append("blockProducedTimeOut = 75\n");
    sb.append("netMaxTrxPerSecond = 700\n");
    sb.append("nodeDetectEnable = false\n");
    sb.append("enableIpv6 = false\n");
    sb.append("effectiveCheckEnable = false\n");
    sb.append("maxFastForwardNum = 4\n");
    sb.append("tcpNettyWorkThreadNum = 0\n");
    sb.append("udpNettyWorkThreadNum = 1\n");
    sb.append("validContractProtoThreads = 2\n");
    sb.append("shieldedTransInPendingMaxCounts = 10\n");
    sb.append("blockCacheTimeout = 60\n");
    sb.append("receiveTcpMinDataLength = 2048\n");
    sb.append("channelReadTimeout = 60\n");
    sb.append("maxTransactionPendingSize = 2000\n");
    sb.append("pendingTransactionTimeout = 60000\n");
    sb.append("agreeNodeCount = 0\n");
    sb.append("openHistoryQueryWhenLiteFN = false\n");
    sb.append("unsolidifiedBlockCheck = false\n");
    sb.append("maxUnsolidifiedBlocks = 54\n");
    sb.append("zenTokenId = \"000000\"\n");
    sb.append("allowShieldedTransactionApi = true\n");
    sb.append("activeConnectFactor = 0.1\n");
    sb.append("connectFactor = 0.6\n");
    sb.append("disconnectNumberFactor = 0.4\n");
    sb.append("maxActiveNodesWithSameIp = 2\n");
    // dot-notation fields (HOCON nests them automatically)
    sb.append("listen { port = 18888 }\n");
    sb.append("connection { timeout = 2 }\n");
    sb.append("fetchBlock { timeout = 200 }\n");
    sb.append("solidity { threads = ").append(cpus).append(" }\n");
    // sub-objects
    sb.append("p2p { version = 11111 }\n");
    sb.append("http { fullNodeEnable = true, fullNodePort = 8090,");
    sb.append(" solidityEnable = true, solidityPort = 8091,");
    sb.append(" PBFTEnable = true, PBFTPort = 8092 }\n");
    sb.append("rpc { enable = true, port = 50051,");
    sb.append(" solidityEnable = true, solidityPort = 50061,");
    sb.append(" PBFTEnable = true, PBFTPort = 50071,");
    sb.append(" thread = 16, maxConcurrentCallsPerConnection = 100,");
    sb.append(" flowControlWindow = 0, maxConnectionIdleInMillis = ");
    sb.append(Long.MAX_VALUE).append(",");
    sb.append(" maxConnectionAgeInMillis = ").append(Long.MAX_VALUE).append(",");
    sb.append(" maxMessageSize = 0, maxHeaderListSize = 0,");
    sb.append(" maxRstStream = 0, secondsPerWindow = 0,");
    sb.append(" minEffectiveConnection = 0, reflectionService = false,");
    sb.append(" trxCacheEnable = false }\n");
    sb.append("jsonrpc { httpFullNodeEnable = false, httpFullNodePort = 8545,");
    sb.append(" httpSolidityEnable = false, httpSolidityPort = 8555,");
    sb.append(" httpPBFTEnable = false, httpPBFTPort = 8565,");
    sb.append(" maxBlockRange = 5000, maxSubTopics = 1000, maxBlockFilterNum = 50000 }\n");
    sb.append("dynamicConfig { enable = false, checkInterval = 600 }\n");
    sb.append("backup { priority = 0, port = 10001, keepAliveInterval = 3000, members = [] }\n");
    sb.append("dns { treeUrls = [], publish = false, dnsDomain = \"\",");
    sb.append(" dnsPrivate = \"\", knownUrls = [], staticNodes = [],");
    sb.append(" maxMergeSize = 0, changeThreshold = 0.0, serverType = \"\",");
    sb.append(" accessKeyId = \"\", accessKeySecret = \"\",");
    sb.append(" aliyunDnsEndpoint = \"\", awsRegion = \"\", awsHostZoneId = \"\" }\n");
    // list fields
    sb.append("active = []\n");
    sb.append("passive = []\n");
    sb.append("fastForward = []\n");
    sb.append("disabledApi = []\n");
    DEFAULTS = ConfigFactory.parseString(sb.toString());
  }

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
    Config section = config.hasPath("node")
        ? config.getConfig("node").withFallback(DEFAULTS)
        : DEFAULTS;

    // --- Phase 1: Auto-bind flat scalars and sub-objects ---
    // ConfigBeanFactory will bind all simple fields and nested sub-beans.
    // It will skip dot-notation fields (they are nested objects, not scalar keys)
    // and may mis-bind PBFT fields due to JavaBean naming.
    NodeConfig nc = ConfigBeanFactory.create(section, NodeConfig.class);

    // --- Phase 2: Dot-notation and naming-mismatch fields (manually read) ---
    nc.listenPort = getInt(section, "listen.port", 18888);
    nc.connectionTimeout = getInt(section, "connection.timeout", 2);
    nc.fetchBlockTimeout = getInt(section, "fetchBlock.timeout", 200);
    nc.solidityThreads = getInt(section, "solidity.threads",
        Runtime.getRuntime().availableProcessors());
    nc.isOpenFullTcpDisconnect = getBool(section, "isOpenFullTcpDisconnect", false);

    // Legacy key fallback: node.maxActiveNodes (old) -> maxConnections (new)
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

    // Legacy key fallback: node.fullNodeAllowShieldedTransaction -> allowShieldedTransactionApi
    if (section.hasPath("allowShieldedTransactionApi")) {
      nc.allowShieldedTransactionApi = section.getBoolean("allowShieldedTransactionApi");
    } else if (section.hasPath("fullNodeAllowShieldedTransaction")) {
      nc.allowShieldedTransactionApi = section.getBoolean("fullNodeAllowShieldedTransaction");
    }
    nc.discoveryExternalIp = config.hasPath("node.discovery.external.ip")
        ? config.getString("node.discovery.external.ip").trim() : "";

    // node.discovery.* — dot-notation creates nested HOCON objects
    Config discoverySection = config.hasPath("node.discovery")
        ? config.getConfig("node.discovery") : ConfigFactory.empty();
    nc.discoveryEnable = getBool(discoverySection, "enable", false);
    nc.discoveryPersist = getBool(discoverySection, "persist", false);

    // node.shutdown.* — dot-notation
    nc.shutdownBlockTime = config.hasPath("node.shutdown.BlockTime")
        ? config.getString("node.shutdown.BlockTime") : "";
    nc.shutdownBlockHeight = config.hasPath("node.shutdown.BlockHeight")
        ? config.getLong("node.shutdown.BlockHeight") : -1;
    nc.shutdownBlockCount = config.hasPath("node.shutdown.BlockCount")
        ? config.getLong("node.shutdown.BlockCount") : -1;

    // node.channel.read.timeout — triple-dot-notation
    nc.channelReadTimeout = config.hasPath("node.channel.read.timeout")
        ? config.getInt("node.channel.read.timeout") : 0;

    // --- Phase 3: PBFT fields in sub-beans (manually patch) ---
    // http
    Config httpSection = section.hasPath("http")
        ? section.getConfig("http") : ConfigFactory.empty();
    nc.http.pBFTEnable = getBool(httpSection, "PBFTEnable", true);
    nc.http.pBFTPort = getInt(httpSection, "PBFTPort", 8092);

    // rpc
    Config rpcSection = section.hasPath("rpc")
        ? section.getConfig("rpc") : ConfigFactory.empty();
    nc.rpc.pBFTEnable = getBool(rpcSection, "PBFTEnable", true);
    nc.rpc.pBFTPort = getInt(rpcSection, "PBFTPort", 50071);

    // jsonrpc
    Config jsonrpcSection = section.hasPath("jsonrpc")
        ? section.getConfig("jsonrpc") : ConfigFactory.empty();
    nc.jsonrpc.httpPBFTEnable = getBool(jsonrpcSection, "httpPBFTEnable", false);
    nc.jsonrpc.httpPBFTPort = getInt(jsonrpcSection, "httpPBFTPort", 8565);

    // --- Phase 4: List fields (manually read) ---
    nc.active = getStringList(section, "active");
    nc.passive = getStringList(section, "passive");
    nc.fastForward = getStringList(section, "fastForward");
    nc.disabledApi = getStringList(section, "disabledApi");

    return nc;
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

  private static List<String> getStringList(Config config, String path) {
    if (config.hasPath(path)) {
      return config.getStringList(path);
    }
    return Collections.emptyList();
  }
}
