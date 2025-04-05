package org.tron.common.parameter;

import com.beust.jcommander.Parameter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.quartz.CronExpression;
import org.tron.common.args.GenesisBlock;
import org.tron.common.config.DbBackupConfig;
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

  public static final String IGNORE_WRONG_WITNESS_ADDRESS_FORMAT =
      "The localWitnessAccountAddress format is incorrect, ignored";
  public static CommonParameter PARAMETER = new CommonParameter();
  @Setter
  public static boolean ENERGY_LIMIT_HARD_FORK = false;
  @Getter
  @Parameter(names = {"-c", "--config"}, description = "Config file (default:config.conf)")
  public String shellConfFileName = "";
  @Getter
  @Parameter(names = {"-d", "--output-directory"},
      description = "Data directory for the databases (default:output-directory)")
  public String outputDirectory = "output-directory";
  @Getter
  @Parameter(names = {"--log-config"}, description = "Logback config file")
  public String logbackPath = "";
  @Getter
  @Parameter(names = {"-h", "--help"}, help = true, description = "Show help message")
  public boolean help = false;
  @Getter
  @Setter
  @Parameter(names = {"-w", "--witness"}, description = "Is witness node")
  public boolean witness = false;
  @Getter
  @Setter
  @Parameter(names = {"--support-constant"}, description = "Support constant calling for TVM. "
      + "(defalut: false)")
  public boolean supportConstant = false;
  @Getter
  @Setter
  @Parameter(names = {"--max-energy-limit-for-constant"}, description = "Max energy limit for "
      + "constant calling. (default: 100,000,000)")
  public long maxEnergyLimitForConstant = 100_000_000L;
  @Getter
  @Setter
  @Parameter(names = {"--lru-cache-size"}, description = "Max LRU size for caching bytecode and "
      + "result of JUMPDEST analysis. (default: 500)")
  public int lruCacheSize = 500;
  @Getter
  @Setter
  @Parameter(names = {"--debug"}, description = "Switch for TVM debug mode. In debug model, TVM "
      + "will not check for timeout. (default: false)")
  public boolean debug = false;
  @Getter
  @Setter
  @Parameter(names = {"--min-time-ratio"}, description = "Maximum CPU tolerance when executing "
      + "timeout transactions while synchronizing blocks. (default: 0.0)")
  public double minTimeRatio = 0.0;
  @Getter
  @Setter
  @Parameter(names = {"--max-time-ratio"}, description = "Maximum CPU tolerance when executing "
      + "non-timeout transactions while synchronizing blocks. (default: 5.0)")
  public double maxTimeRatio = calcMaxTimeRatio();
  @Getter
  @Setter
  @Parameter(names = {"--save-internaltx"}, description = "Save internal transactions generated "
      + "during TVM execution, such as create, call and suicide. (default: false)")
  public boolean saveInternalTx;
  @Getter
  @Setter
  @Parameter(names = {"--save-featured-internaltx"}, description = "Save featured internal "
      + "transactions generated during TVM execution, such as freeze, vote and so on. "
      + "(default: false)")
  public boolean saveFeaturedInternalTx;
  @Getter
  @Setter
  @Parameter(names = {"--long-running-time"})
  public int longRunningTime = 10;
  @Getter
  @Setter
  @Parameter(names = {"--max-connect-number"}, description = "Http server max connect number "
      + "(default:50)")
  public int maxHttpConnectNumber = 50;
  @Getter
  @Parameter(description = "--seed-nodes")
  public List<String> seedNodes = new ArrayList<>();
  @Parameter(names = {"-p", "--private-key"}, description = "Witness private key")
  public String privateKey = "";
  @Parameter(names = {"--witness-address"}, description = "witness-address")
  public String witnessAddress = "";
  @Parameter(names = {"--password"}, description = "password")
  public String password;
  @Parameter(names = {"--storage-db-directory"}, description = "Storage db directory")
  public String storageDbDirectory = "";
  @Parameter(names = {
      "--storage-db-engine"}, description = "Storage db engine.(leveldb or rocksdb)")
  public String storageDbEngine = "";
  @Parameter(names = {
      "--storage-db-synchronous"},
      description = "Storage db is synchronous or not.(true or false)")
  public String storageDbSynchronous = "";
  @Parameter(names = {"--contract-parse-enable"}, description = "Switch for contract parses in " +
      "java-tron. (default: true)")
  public String contractParseEnable = "";
  @Parameter(names = {"--storage-index-directory"},
      description = "Storage index directory")
  public String storageIndexDirectory = "";
  @Parameter(names = {"--storage-index-switch"}, description = "Storage index switch.(on or off)")
  public String storageIndexSwitch = "";
  @Parameter(names = {"--storage-transactionHistory-switch"},
      description = "Storage transaction history switch.(on or off)")
  public String storageTransactionHistorySwitch = "";
  @Getter
  @Parameter(names = {"--fast-forward"})
  public boolean fastForward = false;
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
  public int nodeConnectionTimeout;
  @Getter
  @Setter
  public int fetchBlockTimeout;
  @Getter
  @Setter
  public int nodeChannelReadTimeout;
  @Getter
  @Setter
  public int maxConnections;
  @Getter
  @Setter
  public int minConnections;
  @Getter
  @Setter
  public int minActiveConnections;
  @Getter
  @Setter
  public int maxConnectionsWithSameIp;
  @Getter
  @Setter
  public int maxTps;
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
  public List<String> dnsTreeUrls;
  @Getter
  @Setter
  public PublishConfig dnsPublishConfig;
  @Getter
  @Setter
  public long syncFetchBatchNum;

  //If you are running a solidity node for java tron, this flag is set to true
  @Getter
  @Setter
  public boolean solidityNode = false;
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
  @Parameter(names = {"--rpc-thread"}, description = "Num of gRPC thread")
  public int rpcThreadNum;
  @Getter
  @Setter
  @Parameter(names = {"--solidity-thread"}, description = "Num of solidity thread")
  public int solidityThreads;
  @Getter
  @Setter
  public int maxConcurrentCallsPerConnection;
  @Getter
  @Setter
  public int flowControlWindow;
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
  @Getter
  @Setter
  public int maxMessageSize;
  @Getter
  @Setter
  public int maxHeaderListSize;
  @Getter
  @Setter
  public boolean isRpcReflectionServiceEnable;
  @Getter
  @Setter
  @Parameter(names = {"--validate-sign-thread"}, description = "Num of validate thread")
  public int validateSignThreadNum;
  @Getter
  @Setter
  public long maintenanceTimeInterval; // (ms)
  @Getter
  @Setter
  public long proposalExpireTime; // (ms)
  @Getter
  @Setter
  public int checkFrozenTime; // for test only
  @Getter
  @Setter
  public long allowCreationOfContracts; //committee parameter
  @Getter
  @Setter
  public long allowAdaptiveEnergy; //committee parameter
  @Getter
  @Setter
  public long allowDelegateResource; //committee parameter
  @Getter
  @Setter
  public long allowSameTokenName; //committee parameter
  @Getter
  @Setter
  public long allowTvmTransferTrc10; //committee parameter
  @Getter
  @Setter
  public long allowTvmConstantinople; //committee parameter
  @Getter
  @Setter
  public long allowTvmSolidity059; //committee parameter
  @Getter
  @Setter
  public long forbidTransferToContract; //committee parameter

  @Getter
  @Setter
  public int tcpNettyWorkThreadNum;
  @Getter
  @Setter
  public int udpNettyWorkThreadNum;
  @Getter
  @Setter
  @Parameter(names = {"--trust-node"}, description = "Trust node addr")
  public String trustNodeAddr;
  @Getter
  @Setter
  public boolean walletExtensionApi;
  @Getter
  @Setter
  public boolean estimateEnergy;
  @Getter
  @Setter
  public int estimateEnergyMaxRetry;
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
  public long receiveTcpMinDataLength;
  @Getter
  @Setter
  public boolean isOpenFullTcpDisconnect;
  @Getter
  @Setter
  public int inactiveThreshold;
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
  public long allowMarketTransaction; //committee parameter

  @Getter
  @Setter
  public long allowTransactionFeePool;

  @Getter
  @Setter
  public long allowBlackHoleOptimization;

  @Getter
  @Setter
  public long allowNewResourceModel;

  // @Getter
  // @Setter
  // public long allowShieldedTransaction; //committee parameter
  // full node used this parameter to close shielded transaction
  @Getter
  @Setter
  public boolean fullNodeAllowShieldedTransactionArgs;
  @Getter
  @Setter
  public long blockNumForEnergyLimit;
  @Getter
  @Setter
  @Parameter(names = {"--es"}, description = "Start event subscribe server")
  public boolean eventSubscribe = false;
  @Getter
  @Setter
  public long trxExpirationTimeInMilliseconds; // (ms)
  @Parameter(names = {"-v", "--version"}, description = "Output code version", help = true)
  public boolean version;
  @Getter
  @Setter
  public String zenTokenId;
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
  public int shieldedTransInPendingMaxCounts;
  @Getter
  @Setter
  public long changedDelegation;
  @Getter
  @Setter
  public Set<String> actuatorSet;
  @Getter
  @Setter
  public RateLimiterInitialization rateLimiterInitialization;
  @Getter
  @Setter
  public int rateLimiterGlobalQps;
  @Getter
  @Setter
  public int rateLimiterGlobalIpQps;
  @Getter
  public int rateLimiterGlobalApiQps;
  @Getter
  public DbBackupConfig dbBackupConfig;
  @Getter
  public RocksDbSettings rocksDBCustomSettings;
  @Getter
  public GenesisBlock genesisBlock;
  @Getter
  @Setter
  @Parameter(names = {"--p2p-disable"}, description = "Switch for p2p module initialization. "
      + "(defalut: false)", arity = 1)
  public boolean p2pDisable = false;
  @Getter
  @Setter
  public List<InetSocketAddress> activeNodes;
  @Getter
  @Setter
  public List<InetAddress> passiveNodes;
  @Getter
  public List<InetSocketAddress> fastForwardNodes;
  @Getter
  public int maxFastForwardNum;
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
  public boolean fullNodeHttpEnable = true;
  @Getter
  @Setter
  public boolean solidityNodeHttpEnable = true;
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
  public int maxTransactionPendingSize;
  @Getter
  @Setter
  public long pendingTransactionTimeout;
  @Getter
  @Setter
  public boolean nodeMetricsEnable = false;

  @Getter
  @Setter
  public boolean metricsStorageEnable = false;

  @Getter
  @Setter
  public String influxDbIp;

  @Getter
  @Setter
  public int influxDbPort;

  @Getter
  @Setter
  public String influxDbDatabase;

  @Getter
  @Setter
  public int metricsReportInterval = 10;

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
  public long pBFTExpireNum;
  @Getter
  @Setter
  public long oldSolidityBlockNum = -1;

  @Getter/**/
  @Setter
  public long allowShieldedTRC20Transaction;

  @Getter/**/
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
  @Parameter(names = {"--history-balance-lookup"})
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
  public List<String> disabledApiList;

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
  public long dynamicConfigCheckInterval;

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
  public int maxUnsolidifiedBlocks;

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
  public long  consensusLogicOptimization;

  private static double calcMaxTimeRatio() {
    //return max(2.0, min(5.0, 5 * 4.0 / max(Runtime.getRuntime().availableProcessors(), 1)));
    return 5.0;
  }

  public static CommonParameter getInstance() {
    return PARAMETER;
  }

  public boolean isECKeyCryptoEngine() {

    return cryptoEngine.equalsIgnoreCase(Constant.ECKey_ENGINE);
  }

  public boolean isJsonRpcFilterEnabled() {
    return jsonRpcHttpFullNodeEnable || jsonRpcHttpSolidityNodeEnable;
  }

  public int getSafeLruCacheSize() {
    return lruCacheSize < 1 ? 500 : lruCacheSize;
  }
}
