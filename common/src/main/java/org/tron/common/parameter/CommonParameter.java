package org.tron.common.parameter;

import com.beust.jcommander.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.args.GenesisBlock;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.logsfilter.EventPluginConfig;
import org.tron.common.logsfilter.FilterQuery;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.setting.RocksDbSettings;
import org.tron.core.Constant;
import org.tron.core.config.args.Overlay;
import org.tron.core.config.args.SeedNode;
import org.tron.core.config.args.Storage;

public class CommonParameter {

  public static final String IGNORE_WRONG_WITNESS_ADDRESS_FORMAT =
      "The localWitnessAccountAddress format is incorrect, ignored";
  public static CommonParameter PARAMETER = new CommonParameter();
  @Setter
  public static boolean ENERGY_LIMIT_HARD_FORK = false;
  @Parameter(names = {"-c", "--config"}, description = "Config File")
  public String shellConfFileName = "";
  @Getter
  @Parameter(names = {"-d", "--output-directory"}, description = "Directory")
  public String outputDirectory = "output-directory";
  @Getter
  @Parameter(names = {"--log-config"})
  public String logbackPath = "";
  @Getter
  @Parameter(names = {"-h", "--help"}, help = true, description = "HELP message")
  public boolean help = false;
  @Getter
  @Setter
  @Parameter(names = {"-w", "--witness"})
  public boolean witness = false;
  @Getter
  @Setter
  @Parameter(names = {"--support-constant"})
  public boolean supportConstant = false;
  @Getter
  @Setter
  @Parameter(names = {"--debug"})
  public boolean debug = false;
  @Getter
  @Setter
  @Parameter(names = {"--min-time-ratio"})
  public double minTimeRatio = 0.0;
  @Getter
  @Setter
  @Parameter(names = {"--max-time-ratio"})
  public double maxTimeRatio = calcMaxTimeRatio();
  @Getter
  @Setter
  @Parameter(names = {"--long-running-time"})
  public int longRunningTime = 10;
  @Getter
  @Setter
  @Parameter(names = {"--max-connect-number"})
  public int maxHttpConnectNumber = 50;
  @Getter
  @Parameter(description = "--seed-nodes")
  public List<String> seedNodes = new ArrayList<>();
  @Parameter(names = {"-p", "--private-key"}, description = "private-key")
  public String privateKey = "";
  @Parameter(names = {"--witness-address"}, description = "witness-address")
  public String witnessAddress = "";
  @Parameter(names = {"--password"}, description = "password")
  public String password;
  @Parameter(names = {"--storage-db-directory"}, description = "Storage db directory")
  public String storageDbDirectory = "";
  @Parameter(names = {"--storage-db-version"}, description = "Storage db version.(1 or 2)")
  public String storageDbVersion = "";
  @Parameter(names = {
      "--storage-db-engine"}, description = "Storage db engine.(leveldb or rocksdb)")
  public String storageDbEngine = "";
  @Parameter(names = {
      "--storage-db-synchronous"},
      description = "Storage db is synchronous or not.(true or false)")
  public String storageDbSynchronous = "";
  @Parameter(names = {"--contract-parse-enable"},
      description = "enable contract parses in java-tron or not.(true or false)")
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
  public int nodeConnectionTimeout;
  @Getter
  @Setter
  public int nodeChannelReadTimeout;
  @Getter
  @Setter
  public int nodeMaxActiveNodes;
  @Getter
  @Setter
  public int nodeMaxActiveNodesWithSameIp;
  @Getter
  @Setter
  public int minParticipationRate;
  @Getter
  @Setter
  public int nodeListenPort;
  @Getter
  @Setter
  public String nodeDiscoveryBindIp;
  @Getter
  @Setter
  public String nodeExternalIp;
  @Getter
  @Setter
  public boolean nodeDiscoveryPublicHomeNode;
  @Getter
  @Setter
  public long nodeDiscoveryPingTimeout;
  @Getter
  @Setter
  public long nodeP2pPingInterval;
  @Getter
  @Setter
  @Parameter(names = {"--save-internaltx"})
  public boolean saveInternalTx;
  @Getter
  @Setter
  public int nodeP2pVersion;
  @Getter
  @Setter
  public String p2pNodeId;
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
  public double connectFactor;
  @Getter
  @Setter
  public double activeConnectFactor;
  @Getter
  @Setter
  public double disconnectNumberFactor;
  @Getter
  @Setter
  public double maxConnectNumberFactor;
  @Getter
  @Setter
  public long receiveTcpMinDataLength;
  @Getter
  @Setter
  public boolean isOpenFullTcpDisconnect;
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
  public long allowMarketTransaction; //committee parameter

  @Getter
  @Setter
  public long allowTransactionFeePool;

  @Getter
  @Setter
  public long allowBlackHoleOptimization;

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
  @Parameter(names = {"--es"})
  public boolean eventSubscribe = false;
  @Getter
  @Setter
  public long trxExpirationTimeInMilliseconds; // (ms)
  @Parameter(names = {"-v", "--version"}, description = "output code version", help = true)
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
  public int validContractProtoThreadNum;
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
  public DbBackupConfig dbBackupConfig;
  @Getter
  public RocksDbSettings rocksDBCustomSettings;
  @Getter
  public GenesisBlock genesisBlock;
  @Getter
  @Setter
  public List<Node> activeNodes;
  @Getter
  @Setter
  public List<Node> passiveNodes;
  @Getter
  public List<Node> fastForwardNodes;
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
  public long oldSolidityBlockNum = -1;

  @Getter/**/
  @Setter
  public long allowShieldedTRC20Transaction;

  @Getter/**/
  @Setter
  public long allowTvmIstanbul;

  @Getter
  @Setter
  public long allowTvmStake;

  @Getter
  @Setter
  public long allowTvmAssetIssue;

  @Getter
  @Setter
  public boolean openHistoryQueryWhenLiteFN = false;

  @Getter
  @Setter
  public boolean isLiteFullNode = false;

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
}
