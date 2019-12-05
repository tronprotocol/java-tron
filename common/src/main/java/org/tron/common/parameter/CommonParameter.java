package org.tron.common.parameter;

import com.beust.jcommander.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.args.GenesisBlock;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.setting.RocksDbSettings;

public class CommonParameter {
  @Parameter(names = {"-c", "--config"}, description = "Config File")
  protected String shellConfFileName = "";

  @Parameter(names = {"-d", "--output-directory"}, description = "Directory")
  protected String outputDirectory = "output-directory";

  @Getter
  @Parameter(names = {"--log-config"})
  protected String logbackPath = "";

  @Getter
  @Parameter(names = {"-h", "--help"}, help = true, description = "HELP message")
  protected boolean help = false;

  @Getter
  @Setter
  @Parameter(names = {"-w", "--witness"})
  protected boolean witness = false;

  @Getter
  @Setter
  @Parameter(names = {"--support-constant"})
  protected boolean supportConstant = false;

  @Getter
  @Setter
  @Parameter(names = {"--debug"})
  protected boolean debug = false;

  @Getter
  @Setter
  @Parameter(names = {"--min-time-ratio"})
  protected double minTimeRatio = 0.0;

  @Getter
  @Setter
  @Parameter(names = {"--max-time-ratio"})
  protected double maxTimeRatio = calcMaxTimeRatio();

  @Getter
  @Setter
  @Parameter(names = {"--long-running-time"})
  protected int longRunningTime = 10;

  @Getter
  @Setter
  @Parameter(names = {"--max-connect-number"})
  protected int maxHttpConnectNumber = 50;

  @Getter
  @Parameter(description = "--seed-nodes")
  protected List<String> seedNodes = new ArrayList<>();

  @Parameter(names = {"-p", "--private-key"}, description = "private-key")
  protected String privateKey = "";

  @Parameter(names = {"--witness-address"}, description = "witness-address")
  protected String witnessAddress = "";

  @Parameter(names = {"--password"}, description = "password")
  protected String password;

  @Parameter(names = {"--storage-db-directory"}, description = "Storage db directory")
  protected String storageDbDirectory = "";

  @Parameter(names = {"--storage-db-version"}, description = "Storage db version.(1 or 2)")
  protected String storageDbVersion = "";

  @Parameter(names = {
      "--storage-db-engine"}, description = "Storage db engine.(leveldb or rocksdb)")
  protected String storageDbEngine = "";

  @Parameter(names = {
      "--storage-db-synchronous"},
      description = "Storage db is synchronous or not.(true or false)")
  protected String storageDbSynchronous = "";

  @Parameter(names = {"--contract-parse-enable"},
      description = "enable contract parses in java-tron or not.(true or false)")
  protected String contractParseEnable = "";

  @Parameter(names = {"--storage-index-directory"},
      description = "Storage index directory")
  protected String storageIndexDirectory = "";

  @Parameter(names = {"--storage-index-switch"}, description = "Storage index switch.(on or off)")
  protected String storageIndexSwitch = "";

  @Parameter(names = {"--storage-transactionHistory-switch"},
      description = "Storage transaction history switch.(on or off)")
  protected String storageTransactionHistoreSwitch = "";

  @Getter
  @Parameter(names = {"--fast-forward"})
  protected boolean fastForward = false;

  @Getter
  @Setter
  protected String chainId;

  @Getter
  @Setter
  protected boolean needSyncCheck;

  @Getter
  @Setter
  protected boolean nodeDiscoveryEnable;

  @Getter
  @Setter
  protected boolean nodeDiscoveryPersist;

  @Getter
  @Setter
  protected int nodeConnectionTimeout;

  @Getter
  @Setter
  protected int nodeChannelReadTimeout;

  @Getter
  @Setter
  protected int nodeMaxActiveNodes;

  @Getter
  @Setter
  protected int nodeMaxActiveNodesWithSameIp;

  @Getter
  @Setter
  protected int minParticipationRate;

  @Getter
  @Setter
  protected int nodeListenPort;

  @Getter
  @Setter
  protected String nodeDiscoveryBindIp;

  @Getter
  @Setter
  protected String nodeExternalIp;

  @Getter
  @Setter
  protected boolean nodeDiscoveryPublicHomeNode;

  @Getter
  @Setter
  protected long nodeP2pPingInterval;

  @Getter
  @Setter
  @Parameter(names = {"--save-internaltx"})
  protected boolean saveInternalTx;

  @Getter
  @Setter
  protected int nodeP2pVersion;

  @Getter
  @Setter
  protected String p2pNodeId;

  //If you are running a solidity node for java tron, this flag is set to true
  @Getter
  @Setter
  protected boolean solidityNode = false;

  @Getter
  @Setter
  protected int rpcPort;

  @Getter
  @Setter
  protected int rpcOnSolidityPort;

  @Getter
  @Setter
  protected int fullNodeHttpPort;

  @Getter
  @Setter
  protected int solidityHttpPort;

  @Getter
  @Setter
  @Parameter(names = {"--rpc-thread"}, description = "Num of gRPC thread")
  protected int rpcThreadNum;

  @Getter
  @Setter
  @Parameter(names = {"--solidity-thread"}, description = "Num of solidity thread")
  protected int solidityThreads;

  @Getter
  @Setter
  protected int maxConcurrentCallsPerConnection;

  @Getter
  @Setter
  protected int flowControlWindow;

  @Getter
  @Setter
  protected long maxConnectionIdleInMillis;

  @Getter
  @Setter
  protected int blockProducedTimeOut;

  @Getter
  @Setter
  protected long netMaxTrxPerSecond;

  @Getter
  @Setter
  protected long maxConnectionAgeInMillis;

  @Getter
  @Setter
  protected int maxMessageSize;

  @Getter
  @Setter
  protected int maxHeaderListSize;

  @Getter
  @Setter
  @Parameter(names = {"--validate-sign-thread"}, description = "Num of validate thread")
  protected int validateSignThreadNum;

  @Getter
  @Setter
  protected long maintenanceTimeInterval; // (ms)

  @Getter
  @Setter
  protected long proposalExpireTime; // (ms)

  @Getter
  @Setter
  protected int checkFrozenTime; // for test only

  @Getter
  @Setter
  protected long allowCreationOfContracts; //committee parameter

  @Getter
  @Setter
  protected long allowAdaptiveEnergy; //committee parameter

  @Getter
  @Setter
  protected long allowDelegateResource; //committee parameter

  @Getter
  @Setter
  protected long allowSameTokenName; //committee parameter

  @Getter
  @Setter
  protected long allowTvmTransferTrc10; //committee parameter

  @Getter
  @Setter
  protected long allowTvmConstantinople; //committee parameter

  @Getter
  @Setter
  protected long allowTvmSolidity059; //committee parameter

  @Getter
  @Setter
  protected int tcpNettyWorkThreadNum;

  @Getter
  @Setter
  protected int udpNettyWorkThreadNum;

  @Getter
  @Setter
  @Parameter(names = {"--trust-node"}, description = "Trust node addr")
  protected String trustNodeAddr;

  @Getter
  @Setter
  protected boolean walletExtensionApi;

  @Getter
  @Setter
  protected int backupPriority;

  @Getter
  @Setter
  protected int backupPort;

  @Getter
  @Setter
  protected int keepAliveInterval;

  @Getter
  @Setter
  protected List<String> backupMembers;

  @Getter
  @Setter
  protected double connectFactor;

  @Getter
  @Setter
  protected double activeConnectFactor;

  @Getter
  @Setter
  protected double disconnectNumberFactor;

  @Getter
  @Setter
  protected double maxConnectNumberFactor;

  @Getter
  @Setter
  protected long receiveTcpMinDataLength;

  @Getter
  @Setter
  protected boolean isOpenFullTcpDisconnect;

  @Getter
  @Setter
  protected int allowMultiSign;

  @Getter
  @Setter
  protected boolean vmTrace;

  @Getter
  @Setter
  protected boolean needToUpdateAsset;

  @Getter
  @Setter
  protected String trxReferenceBlock;

  @Getter
  @Setter
  protected int minEffectiveConnection;

  @Getter
  @Setter
  protected long allowShieldedTransaction; //committee parameter

  // full node used this parameter to close shielded transaction
  @Getter
  @Setter
  protected boolean fullNodeAllowShieldedTransactionArgs;

  @Getter
  @Setter
  protected long blockNumForEneryLimit;

  @Getter
  @Setter
  @Parameter(names = {"--es"})
  protected boolean eventSubscribe = false;

  @Getter
  @Setter
  protected long trxExpirationTimeInMilliseconds; // (ms)

  @Parameter(names = {"-v", "--version"}, description = "output code version", help = true)
  protected boolean version;


  @Getter
  @Setter
  protected String zenTokenId;

  @Getter
  @Setter
  protected long allowProtoFilterNum;

  @Getter
  @Setter
  protected long allowAccountStateRoot;

  @Getter
  @Setter
  protected int validContractProtoThreadNum;

  @Getter
  @Setter
  protected int shieldedTransInPendingMaxCounts;

  @Getter
  @Setter
  protected long changedDelegation;

  @Getter
  @Setter
  protected Set<String> actuatorSet;

  @Getter
  @Setter
  protected RateLimiterInitialization rateLimiterInitialization;


  @Getter
  protected DbBackupConfig dbBackupConfig;

  @Getter
  protected RocksDbSettings rocksDBCustomSettings;

  @Getter
  protected GenesisBlock genesisBlock;

  private static double calcMaxTimeRatio() {
    //return max(2.0, min(5.0, 5 * 4.0 / max(Runtime.getRuntime().availableProcessors(), 1)));
    return 5.0;
  }
}
