package org.tron.common.parameter;

import com.google.common.annotations.VisibleForTesting;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.tron.common.args.GenesisBlock;
import org.tron.common.cron.CronExpression;
import org.tron.common.logsfilter.EventPluginConfig;
import org.tron.common.logsfilter.FilterQuery;
import org.tron.common.setting.RocksDbSettings;
import org.tron.core.Constant;
import org.tron.core.config.args.Overlay;
import org.tron.core.config.args.SeedNode;
import org.tron.core.config.args.Storage;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.dns.update.PublishConfig;

public class CommonParameter {

  // Install the JUL->SLF4J bridge early so that JUL log records emitted during
  // static init of grpc classes (or from unit tests that don't invoke
  // LogService.load()) still reach Logback.
  // removeHandlersForRootLogger() strips JUL's default ConsoleHandler so the
  // same record is not emitted twice (once by JUL's own console output and
  // once via the bridge to Logback).
  static {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    if (!SLF4JBridgeHandler.isInstalled()) {
      SLF4JBridgeHandler.install();
    }
  }

  protected static CommonParameter PARAMETER = new CommonParameter();

  // Runtime chain state: set by VMConfig.initVmHardFork()
  // when the energy-limit governance proposal is activated.
  // Legacy: should belong to VMConfig, not here.
  @Setter
  public static boolean ENERGY_LIMIT_HARD_FORK = false;

  // -- Startup parameters --
  @Getter
  public String outputDirectory = "output-directory";
  @Getter
  public String logbackPath = "";
  // -- Flags (CLI + Config) --
  @Getter
  @Setter
  public boolean witness = false;
  @Getter
  @Setter
  public boolean supportConstant = false;
  @Getter
  @Setter
  public long maxEnergyLimitForConstant = 100_000_000L;
  @Getter
  @Setter
  public int lruCacheSize = 500;
  @Getter
  @Setter
  public boolean debug = false;
  @Getter
  @Setter
  public double minTimeRatio = 0.0;
  @Getter
  @Setter
  public double maxTimeRatio = calcMaxTimeRatio();
  /**
   * Max TVM execution time (ms) for constant calls — covers
   * triggerconstantcontract, triggersmartcontract dispatched to view/pure
   * functions, estimateenergy, eth_call, eth_estimateGas, and any other
   * RPC routed through Wallet#callConstantContract. 0 = use the same
   * deadline as block processing (current behaviour). When operators set
   * this in config the value must be positive and fit VM deadline conversion;
   * validated at config-load in VmConfig.
   */
  @Getter
  @Setter
  public long constantCallTimeoutMs = 0L;
  @Getter
  @Setter
  public boolean saveInternalTx;
  @Getter
  @Setter
  public boolean saveFeaturedInternalTx;
  @Getter
  @Setter
  public boolean saveCancelAllUnfreezeV2Details;
  @Getter
  @Setter
  public int longRunningTime = 10;
  @Getter
  @Setter
  public int maxHttpConnectNumber = 50;
  @Getter
  public List<String> seedNodes = new ArrayList<>();
  @Getter
  public boolean fastForward = false;
  // -- Network / P2P --
  @Getter
  @Setter
  public String chainId;
  @Getter
  @Setter
  public boolean needSyncCheck;
  @Getter
  @Setter
  public boolean nodeDiscoveryEnable;
  @Getter
  @Setter
  public boolean nodeDiscoveryPersist;
  @Getter
  @Setter
  public boolean nodeEffectiveCheckEnable;
  @Getter
  @Setter
  public int fetchBlockTimeout;
  @Getter
  @Setter
  public int maxConnections = 30; // from clearParam(), consistent with mainnet.conf
  @Getter
  @Setter
  public int minConnections = 8; // from clearParam(), consistent with mainnet.conf
  @Getter
  @Setter
  public int minActiveConnections = 3; // from clearParam(), consistent with mainnet.conf
  @Getter
  @Setter
  public int maxConnectionsWithSameIp = 2; // from clearParam(), consistent with mainnet.conf
  @Getter
  @Setter
  public int maxTps; // clearParam: 1000
  @Getter
  @Setter
  public int maxBlockInvPerSecond = 10; // default: 10 block inv hashes/s per peer
  @Getter
  @Setter
  public int minParticipationRate;
  @Getter
  public P2pConfig p2pConfig;
  @Getter
  @Setter
  public int nodeListenPort;
  @Getter
  @Setter
  public String nodeLanIp;
  @Getter
  @Setter
  public String nodeExternalIp;
  @Getter
  @Setter
  public int nodeP2pVersion;
  @Getter
  @Setter
  public boolean nodeEnableIpv6 = false;
  @Getter
  @Setter
  public List<String> dnsTreeUrls; // clearParam: new ArrayList<>()
  @Getter
  @Setter
  public PublishConfig dnsPublishConfig;
  @Getter
  @Setter
  public long syncFetchBatchNum; // clearParam: 2000
  @Getter
  @Setter
  public int maxPendingBlockSize;

  // If you are running a solidity node for java tron,
  // this flag is set to true
  @Getter
  @Setter
  public boolean solidityNode = false;

  // If you are running KeystoreFactory,
  // this flag is set to true
  @Getter
  @Setter
  public boolean keystoreFactory = false;

  // -- RPC / HTTP --
  @Getter
  @Setter
  public int rpcPort;
  @Getter
  @Setter
  public int rpcOnSolidityPort;
  @Getter
  @Setter
  public int fullNodeHttpPort;
  @Getter
  @Setter
  public int solidityHttpPort;
  @Getter
  @Setter
  public int jsonRpcHttpFullNodePort;
  @Getter
  @Setter
  public int jsonRpcHttpSolidityPort;
  @Getter
  @Setter
  public int jsonRpcHttpPBFTPort;
  @Getter
  @Setter
  public int rpcThreadNum;
  @Getter
  @Setter
  public int solidityThreads;
  @Getter
  @Setter
  public int maxConcurrentCallsPerConnection;
  @Getter
  @Setter
  public int flowControlWindow;
  @Getter
  @Setter
  public int rpcMaxRstStream;
  @Getter
  @Setter
  public int rpcSecondsPerWindow;
  @Getter
  @Setter
  public long maxConnectionIdleInMillis;
  @Getter
  @Setter
  public int blockProducedTimeOut;
  @Getter
  @Setter
  public long netMaxTrxPerSecond;
  @Getter
  @Setter
  public long maxConnectionAgeInMillis;
  // Refers to RPC (gRPC) max message size; see httpMaxMessageSize / jsonRpcMaxMessageSize
  // below for the HTTP / JSON-RPC counterparts.
  @Getter
  @Setter
  public int maxMessageSize;
  @Getter
  @Setter
  public long httpMaxMessageSize;
  @Getter
  @Setter
  public long jsonRpcMaxMessageSize;
  @Getter
  @Setter
  public int maxHeaderListSize;
  @Getter
  @Setter
  public boolean isRpcReflectionServiceEnable;
  @Getter
  @Setter
  public int validateSignThreadNum;
  @Getter
  @Setter
  public long maintenanceTimeInterval;
  @Getter
  @Setter
  public long proposalExpireTime;
  @Getter
  @Setter
  public int checkFrozenTime; // clearParam: 1

  // -- Committee parameters --
  @Getter
  @Setter
  public long allowCreationOfContracts;
  @Getter
  @Setter
  public long allowAdaptiveEnergy;
  @Getter
  @Setter
  public long allowDelegateResource;
  @Getter
  @Setter
  public long allowSameTokenName;
  @Getter
  @Setter
  public long allowTvmTransferTrc10;
  @Getter
  @Setter
  public long allowTvmConstantinople;
  @Getter
  @Setter
  public long allowTvmSolidity059;
  @Getter
  @Setter
  public long forbidTransferToContract;

  @Getter
  @Setter
  public String trustNodeAddr; // clearParam: ""
  @Getter
  @Setter
  public boolean walletExtensionApi;
  @Getter
  @Setter
  public boolean estimateEnergy;
  @Getter
  @Setter
  public int estimateEnergyMaxRetry = 3; // from clearParam(), consistent with mainnet.conf
  @Getter
  @Setter
  public int backupPriority;
  @Getter
  @Setter
  public int backupPort;
  @Getter
  @Setter
  public int keepAliveInterval;
  @Getter
  @Setter
  public List<String> backupMembers;
  @Getter
  @Setter
  public long receiveTcpMinDataLength; // clearParam: 2048
  @Getter
  @Setter
  public boolean isOpenFullTcpDisconnect;
  @Getter
  @Setter
  public int inactiveThreshold = 600; // from clearParam(), consistent with mainnet.conf
  @Getter
  @Setter
  public boolean nodeDetectEnable;
  @Getter
  @Setter
  public int allowMultiSign;
  @Getter
  @Setter
  public boolean vmTrace;
  @Getter
  @Setter
  public boolean needToUpdateAsset;
  @Getter
  @Setter
  public String trxReferenceBlock;
  @Getter
  @Setter
  public int minEffectiveConnection;
  @Getter
  @Setter
  public boolean trxCacheEnable;
  @Getter
  @Setter
  public long allowMarketTransaction;
  @Getter
  @Setter
  public long allowTransactionFeePool;
  @Getter
  @Setter
  public long allowBlackHoleOptimization;
  @Getter
  @Setter
  public long allowNewResourceModel;

  @Getter
  @Setter
  public boolean allowShieldedTransactionApi; // clearParam: false
  @Getter
  @Setter
  public long blockNumForEnergyLimit;
  @Getter
  @Setter
  public boolean eventSubscribe = false;
  @Getter
  @Setter
  public long trxExpirationTimeInMilliseconds;

  // -- Shielded / ZK --
  @Getter
  @Setter
  public String zenTokenId; // clearParam: "000000"
  @Getter
  @Setter
  public long allowProtoFilterNum;
  @Getter
  @Setter
  public long allowAccountStateRoot;
  @Getter
  @Setter
  public int validContractProtoThreadNum = 1;
  @Getter
  @Setter
  public int shieldedTransInPendingMaxCounts; // clearParam: 10
  @Getter
  @Setter
  public long changedDelegation;
  @Getter
  @Setter
  public RateLimiterInitialization rateLimiterInitialization;
  @Getter
  @Setter
  public int rateLimiterGlobalQps = 50000; // from clearParam(), consistent with mainnet.conf
  @Getter
  @Setter
  public int rateLimiterGlobalIpQps = 10000; // from clearParam(), consistent with mainnet.conf
  @Getter
  public int rateLimiterGlobalApiQps = 1000; // from clearParam(), consistent with mainnet.conf
  @Getter
  @Setter
  public double rateLimiterSyncBlockChain; // clearParam: 3.0
  @Getter
  @Setter
  public double rateLimiterFetchInvData; // clearParam: 3.0
  @Getter
  @Setter
  public double rateLimiterDisconnect; // clearParam: 1.0
  @Getter
  @Setter
  public boolean rateLimiterApiNonBlocking = false;
  @Getter
  public RocksDbSettings rocksDBCustomSettings;
  @Getter
  public GenesisBlock genesisBlock;
  @Getter
  @Setter
  public boolean p2pDisable = false;
  @Getter
  @Setter
  // from clearParam(), consistent with mainnet.conf
  public List<InetSocketAddress> activeNodes = new ArrayList<>();
  @Getter
  @Setter
  // from clearParam(), consistent with mainnet.conf
  public List<InetAddress> passiveNodes = new ArrayList<>();
  @Getter
  public List<InetSocketAddress> fastForwardNodes; // clearParam: new ArrayList<>()
  @Getter
  public int maxFastForwardNum; // clearParam: 4
  @Getter
  public Storage storage;
  @Getter
  public Overlay overlay;
  @Getter
  public SeedNode seedNode;
  @Getter
  public EventPluginConfig eventPluginConfig;
  @Getter
  public FilterQuery eventFilter;
  @Getter
  @Setter
  public String cryptoEngine = Constant.ECKey_ENGINE;

  @Getter
  @Setter
  public boolean rpcEnable = true;
  @Getter
  @Setter
  public boolean rpcSolidityEnable = true;
  @Getter
  @Setter
  public boolean rpcPBFTEnable = true;
  @Getter
  @Setter
  public boolean fullNodeHttpEnable = true;
  @Getter
  @Setter
  public boolean solidityNodeHttpEnable = true;
  @Getter
  @Setter
  public boolean pBFTHttpEnable = true;
  @Getter
  @Setter
  public boolean jsonRpcHttpFullNodeEnable = false;
  @Getter
  @Setter
  public boolean jsonRpcHttpSolidityNodeEnable = false;
  @Getter
  @Setter
  public boolean jsonRpcHttpPBFTNodeEnable = false;
  @Getter
  @Setter
  public int jsonRpcMaxBlockRange = 5000;
  @Getter
  @Setter
  public int jsonRpcMaxSubTopics = 1000;
  @Getter
  @Setter
  public int jsonRpcMaxBlockFilterNum = 50000;
  @Getter
  @Setter
  public int jsonRpcMaxBatchSize = 100;
  @Getter
  @Setter
  public int jsonRpcMaxResponseSize = 25 * 1024 * 1024;
  @Getter
  @Setter
  public int jsonRpcMaxAddressSize = 1000;
  @Getter
  @Setter
  public int jsonRpcMaxLogFilterNum = 20000;
  @Getter
  @Setter
  public int maxTransactionPendingSize;
  @Getter
  @Setter
  public long pendingTransactionTimeout;
  @Getter
  @Setter
  public int maxTrxCacheSize;
  @Getter
  @Setter
  public boolean nodeMetricsEnable = false;
  @Getter
  @Setter
  public boolean metricsPrometheusEnable = false;
  @Getter
  @Setter
  public int metricsPrometheusPort;
  @Getter
  @Setter
  public int agreeNodeCount;
  @Getter
  @Setter
  public long allowPBFT;
  @Getter
  @Setter
  public int rpcOnPBFTPort;
  @Getter
  @Setter
  public int pBFTHttpPort;
  @Getter
  @Setter
  public int maxNestingDepth = 100;
  @Getter
  @Setter
  public int maxTokenCount = 100_000;
  @Getter
  @Setter
  public long pBFTExpireNum; // clearParam: 20
  @Getter
  @Setter
  public long oldSolidityBlockNum = -1;

  @Getter
  @Setter
  public long allowShieldedTRC20Transaction;
  @Getter
  @Setter
  public long allowTvmIstanbul;
  @Getter
  @Setter
  public long allowTvmFreeze;
  @Getter
  @Setter
  public long allowTvmVote;
  @Getter
  @Setter
  public long allowTvmLondon;
  @Getter
  @Setter
  public long allowTvmCompatibleEvm;
  @Getter
  @Setter
  public long allowHigherLimitForMaxCpuTimeOfOneTx;
  @Getter
  @Setter
  public boolean openHistoryQueryWhenLiteFN = false;
  @Getter
  @Setter
  public boolean historyBalanceLookup = false;
  @Getter
  @Setter
  public boolean openPrintLog = true;
  @Getter
  @Setter
  public boolean openTransactionSort = false;
  @Getter
  @Setter
  public long allowAccountAssetOptimization;
  @Getter
  @Setter
  public long allowAssetOptimization;
  @Getter
  @Setter
  public List<String> disabledApiList; // clearParam: Collections.emptyList()
  @Getter
  @Setter
  public CronExpression shutdownBlockTime = null;
  @Getter
  @Setter
  public long shutdownBlockHeight = -1;
  @Getter
  @Setter
  public long shutdownBlockCount = -1;
  @Getter
  @Setter
  public long blockCacheTimeout = 60;
  @Getter
  @Setter
  public long allowNewRewardAlgorithm;
  @Getter
  @Setter
  public long allowNewReward = 0L;
  @Getter
  @Setter
  public long memoFee = 0L;
  @Getter
  @Setter
  public long allowDelegateOptimization = 0L;
  @Getter
  @Setter
  public long unfreezeDelayDays = 0L;
  @Getter
  @Setter
  public long allowOptimizedReturnValueOfChainId = 0L;
  @Getter
  @Setter
  public long allowDynamicEnergy = 0L;
  @Getter
  @Setter
  public long dynamicEnergyThreshold = 0L;
  @Getter
  @Setter
  public long dynamicEnergyIncreaseFactor = 0L;
  @Getter
  @Setter
  public long dynamicEnergyMaxFactor = 0L;
  @Getter
  @Setter
  public boolean dynamicConfigEnable;
  @Getter
  @Setter
  public long dynamicConfigCheckInterval; // clearParam: 600
  @Getter
  @Setter
  public long allowTvmShangHai;
  @Getter
  @Setter
  public long allowCancelAllUnfreezeV2;
  @Getter
  @Setter
  public boolean unsolidifiedBlockCheck;
  @Getter
  @Setter
  public int maxUnsolidifiedBlocks; // clearParam: 54
  @Getter
  @Setter
  public long allowOldRewardOpt;
  @Getter
  @Setter
  public long allowEnergyAdjustment;
  @Getter
  @Setter
  public long maxCreateAccountTxSize = 1000L;
  @Getter
  @Setter
  public long allowStrictMath;
  @Getter
  @Setter
  public long consensusLogicOptimization;
  @Getter
  @Setter
  public long allowTvmCancun;
  @Getter
  @Setter
  public long allowTvmBlob;

  private static double calcMaxTimeRatio() {
    return 5.0;
  }

  public static CommonParameter getInstance() {
    return PARAMETER;
  }

  /**
   * Reset to a fresh instance. Test-only.
   */
  @VisibleForTesting
  public static void reset() {
    if (PARAMETER.storage != null) {
      PARAMETER.storage.deleteAllStoragePaths();
    }
    PARAMETER = new CommonParameter();
  }

  public boolean isECKeyCryptoEngine() {
    return cryptoEngine.equalsIgnoreCase(Constant.ECKey_ENGINE);
  }

  public boolean isJsonRpcFilterEnabled() {
    return jsonRpcHttpFullNodeEnable
        || jsonRpcHttpSolidityNodeEnable;
  }

  public int getSafeLruCacheSize() {
    return lruCacheSize < 1 ? 500 : lruCacheSize;
  }
}
