package org.tron.core.config.args;

import static java.lang.Math.max;
import static java.lang.System.exit;
import static org.tron.consensus.base.Constant.BLOCK_PRODUCE_TIMEOUT_PERCENT;
import static org.tron.core.Constant.ADD_PRE_FIX_BYTE_MAINNET;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyServerBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.SignInterface;
import org.tron.common.logsfilter.EventPluginConfig;
import org.tron.common.logsfilter.FilterQuery;
import org.tron.common.logsfilter.TriggerConfig;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.storage.rocksdb.RocksDbSettings;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DBConfig;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.Configuration;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.db.backup.DbBackupConfig;
import org.tron.core.store.AccountStore;
import org.tron.keystore.CipherException;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;
import org.tron.program.Version;

@Slf4j(topic = "app")
@NoArgsConstructor
@Component
public class Args {

  private static final Args INSTANCE = new Args();

  @Parameter(names = {"-c", "--config"}, description = "Config File")
  private String shellConfFileName = "";

  @Parameter(names = {"-d", "--output-directory"}, description = "Directory")
  private String outputDirectory = "output-directory";

  @Getter
  @Parameter(names = {"--log-config"})
  private String logbackPath = "";

  @Getter
  @Parameter(names = {"-h", "--help"}, help = true, description = "HELP message")
  private boolean help = false;

  @Getter
  @Setter
  @Parameter(names = {"-w", "--witness"})
  private boolean witness = false;

  @Getter
  @Setter
  @Parameter(names = {"--support-constant"})
  private boolean supportConstant = false;

  @Getter
  @Setter
  @Parameter(names = {"--debug"})
  private boolean debug = false;

  @Getter
  @Setter
  @Parameter(names = {"--min-time-ratio"})
  private double minTimeRatio = 0.0;

  @Getter
  @Setter
  @Parameter(names = {"--max-time-ratio"})
  private double maxTimeRatio = calcMaxTimeRatio();

  @Getter
  @Setter
  @Parameter(names = {"--long-running-time"})
  private int longRunningTime = 10;

  @Getter
  @Setter
  @Parameter(names = {"--max-connect-number"})
  private int maxHttpConnectNumber = 50;

  @Getter
  @Parameter(description = "--seed-nodes")
  private List<String> seedNodes = new ArrayList<>();

  @Parameter(names = {"-p", "--private-key"}, description = "private-key")
  private String privateKey = "";

  @Parameter(names = {"--witness-address"}, description = "witness-address")
  private String witnessAddress = "";

  @Parameter(names = {"--password"}, description = "password")
  private String password;

  @Parameter(names = {"--storage-db-directory"}, description = "Storage db directory")
  private String storageDbDirectory = "";

  @Parameter(names = {"--storage-db-version"}, description = "Storage db version.(1 or 2)")
  private String storageDbVersion = "";

  @Parameter(names = {
      "--storage-db-engine"}, description = "Storage db engine.(leveldb or rocksdb)")
  private String storageDbEngine = "";

  @Parameter(names = {
      "--storage-db-synchronous"}, description = "Storage db is synchronous or not.(true or false)")
  private String storageDbSynchronous = "";

  @Parameter(names = {
      "--contract-parse-enable"}, description = "enable contract parses in java-tron or not.(true or false)")
  private String contractParseEnable = "";

  @Parameter(names = {"--storage-index-directory"}, description = "Storage index directory")
  private String storageIndexDirectory = "";

  @Parameter(names = {"--storage-index-switch"}, description = "Storage index switch.(on or off)")
  private String storageIndexSwitch = "";

  @Parameter(names = {
      "--storage-transactionHistory-switch"}, description = "Storage transaction history switch.(on or off)")
  private String storageTransactionHistoreSwitch = "";

  @Getter
  @Parameter(names = {"--fast-forward"})
  private boolean fastForward = false;

  @Getter
  private Storage storage;

  @Getter
  private Overlay overlay;

  @Getter
  private SeedNode seedNode;

  @Getter
  private GenesisBlock genesisBlock;

  @Getter
  @Setter
  private String chainId;

  @Getter
  @Setter
  private LocalWitnesses localWitnesses = new LocalWitnesses();

  @Getter
  @Setter
  private boolean needSyncCheck;

  @Getter
  @Setter
  private boolean nodeDiscoveryEnable;

  @Getter
  @Setter
  private boolean nodeDiscoveryPersist;

  @Getter
  @Setter
  private int nodeConnectionTimeout;

  @Getter
  @Setter
  private List<Node> activeNodes;

  @Getter
  @Setter
  private List<Node> passiveNodes;

  @Getter
  @Setter
  private List<Node> fastForwardNodes;

  @Getter
  @Setter
  private int nodeChannelReadTimeout;

  @Getter
  @Setter
  private int nodeMaxActiveNodes;

  @Getter
  @Setter
  private int nodeMaxActiveNodesWithSameIp;

  @Getter
  @Setter
  private int minParticipationRate;

  @Getter
  @Setter
  private int nodeListenPort;

  @Getter
  @Setter
  private String nodeDiscoveryBindIp;

  @Getter
  @Setter
  private String nodeExternalIp;

  @Getter
  @Setter
  private boolean nodeDiscoveryPublicHomeNode;

  @Getter
  @Setter
  private long nodeP2pPingInterval;

  @Getter
  @Setter
  @Parameter(names = {"--save-internaltx"})
  private boolean saveInternalTx;

  @Getter
  @Setter
  private int nodeP2pVersion;

  @Getter
  @Setter
  private String p2pNodeId;

  //If you are running a solidity node for java tron, this flag is set to true
  @Getter
  @Setter
  private boolean solidityNode = false;

  @Getter
  @Setter
  private int rpcPort;

  @Getter
  @Setter
  private int rpcOnSolidityPort;

  @Getter
  @Setter
  private int fullNodeHttpPort;

  @Getter
  @Setter
  private int solidityHttpPort;

  @Getter
  @Setter
  @Parameter(names = {"--rpc-thread"}, description = "Num of gRPC thread")
  private int rpcThreadNum;

  @Getter
  @Setter
  @Parameter(names = {"--solidity-thread"}, description = "Num of solidity thread")
  private int solidityThreads;

  @Getter
  @Setter
  private int maxConcurrentCallsPerConnection;

  @Getter
  @Setter
  private int flowControlWindow;

  @Getter
  @Setter
  private long maxConnectionIdleInMillis;

  @Getter
  @Setter
  private int blockProducedTimeOut;

  @Getter
  @Setter
  private long netMaxTrxPerSecond;

  @Getter
  @Setter
  private long maxConnectionAgeInMillis;

  @Getter
  @Setter
  private int maxMessageSize;

  @Getter
  @Setter
  private int maxHeaderListSize;

  @Getter
  @Setter
  @Parameter(names = {"--validate-sign-thread"}, description = "Num of validate thread")
  private int validateSignThreadNum;

  @Getter
  @Setter
  private long maintenanceTimeInterval; // (ms)

  @Getter
  @Setter
  private long proposalExpireTime; // (ms)

  @Getter
  @Setter
  private int checkFrozenTime; // for test only

  @Getter
  @Setter
  private long allowCreationOfContracts; //committee parameter

  @Getter
  @Setter
  private long allowAdaptiveEnergy; //committee parameter

  @Getter
  @Setter
  private long allowDelegateResource; //committee parameter

  @Getter
  @Setter
  private long allowSameTokenName; //committee parameter

  @Getter
  @Setter
  private long allowTvmTransferTrc10; //committee parameter

  @Getter
  @Setter
  private long allowTvmConstantinople; //committee parameter

  @Getter
  @Setter
  private long allowTvmSolidity059; //committee parameter

  @Getter
  @Setter
  private long forbidTransferToContract; //committee parameter

  @Getter
  @Setter
  private int tcpNettyWorkThreadNum;

  @Getter
  @Setter
  private int udpNettyWorkThreadNum;

  @Getter
  @Setter
  @Parameter(names = {"--trust-node"}, description = "Trust node addr")
  private String trustNodeAddr;

  @Getter
  @Setter
  private boolean walletExtensionApi;

  @Getter
  @Setter
  private int backupPriority;

  @Getter
  @Setter
  private int backupPort;

  @Getter
  @Setter
  private List<String> backupMembers;

  @Getter
  @Setter
  private double connectFactor;

  @Getter
  @Setter
  private double activeConnectFactor;

  @Getter
  @Setter
  private double disconnectNumberFactor;

  @Getter
  @Setter
  private double maxConnectNumberFactor;

  @Getter
  @Setter
  private long receiveTcpMinDataLength;

  @Getter
  @Setter
  private boolean isOpenFullTcpDisconnect;

  @Getter
  @Setter
  private int allowMultiSign;

  @Getter
  @Setter
  private boolean vmTrace;

  @Getter
  @Setter
  private boolean needToUpdateAsset;

  @Getter
  @Setter
  private String trxReferenceBlock;

  @Getter
  @Setter
  private int minEffectiveConnection;

//  @Getter
//  @Setter
//  private long allowShieldedTransaction; //committee parameter

  // full node used this parameter to close shielded transaction
  @Getter
  @Setter
  private boolean fullNodeAllowShieldedTransactionArgs;

  @Getter
  @Setter
  private long blockNumForEneryLimit;

  @Getter
  @Setter
  @Parameter(names = {"--es"})
  private boolean eventSubscribe = false;

  @Getter
  private EventPluginConfig eventPluginConfig;

  @Getter
  private FilterQuery eventFilter;

  @Getter
  @Setter
  private String cryptoEngine = Constant.ECKey_ENGINE;

  @Getter
  @Setter
  private long trxExpirationTimeInMilliseconds; // (ms)

  @Getter
  private DbBackupConfig dbBackupConfig;

  @Getter
  private RocksDbSettings rocksDBCustomSettings;

  @Parameter(names = {"-v", "--version"}, description = "output code version", help = true)
  private boolean version;


  @Getter
  @Setter
  private String zenTokenId;

  @Getter
  @Setter
  private long allowProtoFilterNum;

  @Getter
  @Setter
  private long allowAccountStateRoot;

  @Getter
  @Setter
  private int validContractProtoThreadNum;

  @Getter
  @Setter
  private int shieldedTransInPendingMaxCounts;

  @Getter
  @Setter
  private RateLimiterInitialization rateLimiterInitialization;

  @Getter
  @Setter
  private long changedDelegation;

  @Getter
  @Setter
  private Set<String> actuatorSet;

  @Getter
  @Setter
  public boolean fullNodeHttpEnable = true;

  @Getter
  @Setter
  public boolean solidityNodeHttpEnable = true;

  @Getter
  @Setter
  public boolean isEckey=true;

  public static void clearParam() {
    INSTANCE.outputDirectory = "output-directory";
    INSTANCE.help = false;
    INSTANCE.witness = false;
    INSTANCE.seedNodes = new ArrayList<>();
    INSTANCE.privateKey = "";
    INSTANCE.witnessAddress = "";
    INSTANCE.storageDbDirectory = "";
    INSTANCE.storageIndexDirectory = "";
    INSTANCE.storageIndexSwitch = "";

    // FIXME: INSTANCE.storage maybe null ?
    if (INSTANCE.storage != null) {
      // WARNING: WILL DELETE DB STORAGE PATHS
      INSTANCE.storage.deleteAllStoragePaths();
      INSTANCE.storage = null;
    }

    INSTANCE.overlay = null;
    INSTANCE.seedNode = null;
    INSTANCE.genesisBlock = null;
    INSTANCE.chainId = null;
    INSTANCE.localWitnesses = null;
    INSTANCE.needSyncCheck = false;
    INSTANCE.nodeDiscoveryEnable = false;
    INSTANCE.nodeDiscoveryPersist = false;
    INSTANCE.nodeConnectionTimeout = 0;
    INSTANCE.activeNodes = Collections.emptyList();
    INSTANCE.passiveNodes = Collections.emptyList();
    INSTANCE.fastForwardNodes = Collections.emptyList();
    INSTANCE.nodeChannelReadTimeout = 0;
    INSTANCE.nodeMaxActiveNodes = 30;
    INSTANCE.nodeMaxActiveNodesWithSameIp = 2;
    INSTANCE.minParticipationRate = 0;
    INSTANCE.nodeListenPort = 0;
    INSTANCE.nodeDiscoveryBindIp = "";
    INSTANCE.nodeExternalIp = "";
    INSTANCE.nodeDiscoveryPublicHomeNode = false;
    INSTANCE.nodeP2pPingInterval = 0L;
    INSTANCE.nodeP2pVersion = 0;
    INSTANCE.rpcPort = 0;
    INSTANCE.rpcOnSolidityPort = 0;
    INSTANCE.fullNodeHttpPort = 0;
    INSTANCE.solidityHttpPort = 0;
    INSTANCE.maintenanceTimeInterval = 0;
    INSTANCE.proposalExpireTime = 0;
    INSTANCE.checkFrozenTime = 1;
    INSTANCE.allowCreationOfContracts = 0;
    INSTANCE.allowAdaptiveEnergy = 0;
    INSTANCE.allowTvmTransferTrc10 = 0;
    INSTANCE.allowTvmConstantinople = 0;
    INSTANCE.allowDelegateResource = 0;
    INSTANCE.allowSameTokenName = 0;
    INSTANCE.allowTvmSolidity059 = 0;
    INSTANCE.forbidTransferToContract = 0;
    INSTANCE.tcpNettyWorkThreadNum = 0;
    INSTANCE.udpNettyWorkThreadNum = 0;
    INSTANCE.p2pNodeId = "";
    INSTANCE.solidityNode = false;
    INSTANCE.trustNodeAddr = "";
    INSTANCE.walletExtensionApi = false;
    INSTANCE.connectFactor = 0.3;
    INSTANCE.activeConnectFactor = 0.1;
    INSTANCE.disconnectNumberFactor = 0.4;
    INSTANCE.maxConnectNumberFactor = 0.8;
    INSTANCE.receiveTcpMinDataLength = 2048;
    INSTANCE.isOpenFullTcpDisconnect = false;
    INSTANCE.supportConstant = false;
    INSTANCE.debug = false;
    INSTANCE.minTimeRatio = 0.0;
    INSTANCE.maxTimeRatio = 5.0;
    INSTANCE.longRunningTime = 10;
//    INSTANCE.allowShieldedTransaction = 0;
    INSTANCE.maxHttpConnectNumber = 50;
    INSTANCE.allowMultiSign = 0;
    INSTANCE.trxExpirationTimeInMilliseconds = 0;
    INSTANCE.fullNodeAllowShieldedTransactionArgs = true;
    INSTANCE.zenTokenId = "000000";
    INSTANCE.allowProtoFilterNum = 0;
    INSTANCE.allowAccountStateRoot = 0;
    INSTANCE.validContractProtoThreadNum = 1;
    INSTANCE.shieldedTransInPendingMaxCounts = 10;
    INSTANCE.changedDelegation = 0;
    INSTANCE.fullNodeHttpEnable = true;
    INSTANCE.solidityNodeHttpEnable = true;
    INSTANCE.isEckey = true;
  }

  /**
   * set parameters.
   */
  public static void setParam(final String[] args, final String confFileName) {
    JCommander.newBuilder().addObject(INSTANCE).build().parse(args);
    if (INSTANCE.version) {
      JCommander.getConsole()
          .println(Version.getVersion() + "\n" + Version.versionName + "\n" + Version.versionCode);
      exit(0);
    }

    Config config = Configuration.getByFileName(INSTANCE.shellConfFileName, confFileName);

    if (config.hasPath(Constant.CRYPTO_ENGINE)) {
      INSTANCE.isEckey = "eckey".equalsIgnoreCase(config.getString(Constant.CRYPTO_ENGINE));
    }
    if (config.hasPath(Constant.CRYPTO_ENGINE)) {
      INSTANCE.cryptoEngine =config.getString(Constant.CRYPTO_ENGINE);
    }
    initEncryptoEngine(INSTANCE);

    if (config.hasPath(Constant.NET_TYPE) && Constant.TESTNET.equalsIgnoreCase(config.getString(Constant.NET_TYPE))) {
      Wallet.setAddressPreFixByte(Constant.ADD_PRE_FIX_BYTE_TESTNET);
      Wallet.setAddressPreFixString(Constant.ADD_PRE_FIX_STRING_TESTNET);
    } else {
      Wallet.setAddressPreFixByte(ADD_PRE_FIX_BYTE_MAINNET);
      Wallet.setAddressPreFixString(Constant.ADD_PRE_FIX_STRING_MAINNET);
    }

    if (StringUtils.isNoneBlank(INSTANCE.privateKey)) {
      INSTANCE.setLocalWitnesses(new LocalWitnesses(INSTANCE.privateKey));
      if (StringUtils.isNoneBlank(INSTANCE.witnessAddress)) {
        byte[] bytes = Commons.decodeFromBase58Check(INSTANCE.witnessAddress);
        if (bytes != null) {
          INSTANCE.localWitnesses.setWitnessAccountAddress(bytes);
          logger.debug("Got localWitnessAccountAddress from cmd");
        } else {
          INSTANCE.witnessAddress = "";
          logger.warn("The localWitnessAccountAddress format is incorrect, ignored");
        }
      }
      INSTANCE.localWitnesses.initWitnessAccountAddress(DBConfig.isECKeyCryptoEngine());
      logger.debug("Got privateKey from cmd");
    } else if (config.hasPath(Constant.LOCAL_WITENSS)) {
      INSTANCE.localWitnesses = new LocalWitnesses();
      List<String> localwitness = config.getStringList(Constant.LOCAL_WITENSS);
//      if (localwitness.size() > 1) {
//        logger.warn("localwitness size must be one, get the first one");
//        localwitness = localwitness.subList(0, 1);
//      }
      INSTANCE.localWitnesses.setPrivateKeys(localwitness);

      if (config.hasPath(Constant.LOCAL_WITNESS_ACCOUNT_ADDRESS)) {
        byte[] bytes = Commons
            .decodeFromBase58Check(config.getString(Constant.LOCAL_WITNESS_ACCOUNT_ADDRESS));
        if (bytes != null) {
          INSTANCE.localWitnesses.setWitnessAccountAddress(bytes);
          logger.debug("Got localWitnessAccountAddress from config.conf");
        } else {
          logger.warn("The localWitnessAccountAddress format is incorrect, ignored");
        }
      }
      INSTANCE.localWitnesses.initWitnessAccountAddress(DBConfig.isECKeyCryptoEngine());

      logger.debug("Got privateKey from config.conf");
    } else if (config.hasPath(Constant.LOCAL_WITNESS_KEYSTORE)) {
      INSTANCE.localWitnesses = new LocalWitnesses();
      List<String> privateKeys = new ArrayList<String>();
      if (INSTANCE.isWitness()) {
        List<String> localwitness = config.getStringList(Constant.LOCAL_WITNESS_KEYSTORE);
        if (localwitness.size() > 0) {
          String fileName = System.getProperty("user.dir") + "/" + localwitness.get(0);
          String password;
          if (StringUtils.isEmpty(INSTANCE.password)) {
            System.out.println("Please input your password.");
            password = WalletUtils.inputPassword();
          } else {
            password = INSTANCE.password;
            INSTANCE.password = null;
          }

          try {
            Credentials credentials = WalletUtils
                .loadCredentials(password, new File(fileName));
            SignInterface sign = credentials.getSignInterface();
            String prikey = ByteArray.toHexString(sign.getPrivateKey());
            privateKeys.add(prikey);
          } catch (IOException e) {
            logger.error(e.getMessage());
            logger.error("Witness node start faild!");
            exit(-1);
          } catch (CipherException e) {
            logger.error(e.getMessage());
            logger.error("Witness node start faild!");
            exit(-1);
          }
        }
      }
      INSTANCE.localWitnesses.setPrivateKeys(privateKeys);

      if (config.hasPath(Constant.LOCAL_WITNESS_ACCOUNT_ADDRESS)) {
        byte[] bytes = Commons
            .decodeFromBase58Check(config.getString(Constant.LOCAL_WITNESS_ACCOUNT_ADDRESS));
        if (bytes != null) {
          INSTANCE.localWitnesses.setWitnessAccountAddress(bytes);
          logger.debug("Got localWitnessAccountAddress from config.conf");
        } else {
          logger.warn("The localWitnessAccountAddress format is incorrect, ignored");
        }
      }
      INSTANCE.localWitnesses.initWitnessAccountAddress(DBConfig.isECKeyCryptoEngine());
      logger.debug("Got privateKey from keystore");
    }

    if (INSTANCE.isWitness() && CollectionUtils.isEmpty(INSTANCE.localWitnesses.getPrivateKeys())) {
      logger.warn("This is a witness node,but localWitnesses is null");
    }

    if (config.hasPath(Constant.VM_SUPPORT_CONSTANT)) {
      INSTANCE.supportConstant = config.getBoolean(Constant.VM_SUPPORT_CONSTANT);
    }

    if (config.hasPath(Constant.NODE_HTTP_FULLNODE_ENABLE)) {
      INSTANCE.fullNodeHttpEnable = config.getBoolean(Constant.NODE_HTTP_FULLNODE_ENABLE);
    }

    if (config.hasPath(Constant.NODE_HTTP_SOLIDITY_ENABLE)) {
      INSTANCE.solidityNodeHttpEnable = config.getBoolean(Constant.NODE_HTTP_SOLIDITY_ENABLE);
    }

    if (config.hasPath(Constant.VM_MIN_TIME_RATIO)) {
      INSTANCE.minTimeRatio = config.getDouble(Constant.VM_MIN_TIME_RATIO);
    }

    if (config.hasPath(Constant.VM_MAX_TIME_RATIO)) {
      INSTANCE.maxTimeRatio = config.getDouble(Constant.VM_MAX_TIME_RATIO);
    }

    if (config.hasPath(Constant.VM_LONG_RUNNING_TIME)) {
      INSTANCE.longRunningTime = config.getInt(Constant.VM_LONG_RUNNING_TIME);
    }

    INSTANCE.storage = new Storage();
    INSTANCE.storage.setDbVersion(Optional.ofNullable(INSTANCE.storageDbVersion)
        .filter(StringUtils::isNotEmpty)
        .map(Integer::valueOf)
        .orElse(Storage.getDbVersionFromConfig(config)));

    INSTANCE.storage.setDbEngine(Optional.ofNullable(INSTANCE.storageDbEngine)
        .filter(StringUtils::isNotEmpty)
        .orElse(Storage.getDbEngineFromConfig(config)));

    if (Constant.ROCKSDB.equals(INSTANCE.storage.getDbEngine().toUpperCase())
        && INSTANCE.storage.getDbVersion() == 1) {
      throw new RuntimeException("db.version = 1 is not supported by ROCKSDB engine.");
    }

    INSTANCE.storage.setDbSync(Optional.ofNullable(INSTANCE.storageDbSynchronous)
        .filter(StringUtils::isNotEmpty)
        .map(Boolean::valueOf)
        .orElse(Storage.getDbVersionSyncFromConfig(config)));

    INSTANCE.storage.setContractParseSwitch(Optional.ofNullable(INSTANCE.contractParseEnable)
        .filter(StringUtils::isNotEmpty)
        .map(Boolean::valueOf)
        .orElse(Storage.getContractParseSwitchFromConfig(config)));

    INSTANCE.storage.setDbDirectory(Optional.ofNullable(INSTANCE.storageDbDirectory)
        .filter(StringUtils::isNotEmpty)
        .orElse(Storage.getDbDirectoryFromConfig(config)));

    INSTANCE.storage.setIndexDirectory(Optional.ofNullable(INSTANCE.storageIndexDirectory)
        .filter(StringUtils::isNotEmpty)
        .orElse(Storage.getIndexDirectoryFromConfig(config)));

    INSTANCE.storage.setIndexSwitch(Optional.ofNullable(INSTANCE.storageIndexSwitch)
        .filter(StringUtils::isNotEmpty)
        .orElse(Storage.getIndexSwitchFromConfig(config)));

    INSTANCE.storage
        .setTransactionHistoreSwitch(Optional.ofNullable(INSTANCE.storageTransactionHistoreSwitch)
            .filter(StringUtils::isNotEmpty)
            .orElse(Storage.getTransactionHistoreSwitchFromConfig(config)));

    INSTANCE.storage.setPropertyMapFromConfig(config);

    INSTANCE.seedNode = new SeedNode();
    INSTANCE.seedNode.setIpList(Optional.ofNullable(INSTANCE.seedNodes)
        .filter(seedNode -> 0 != seedNode.size())
        .orElse(config.getStringList("seed.node.ip.list")));
    if (config.hasPath(Constant.GENESIS_BLOCK)) {
      INSTANCE.genesisBlock = new GenesisBlock();

      INSTANCE.genesisBlock.setTimestamp(config.getString(Constant.GENESIS_BLOCK_TIMESTAMP));
      INSTANCE.genesisBlock.setParentHash(config.getString(Constant.GENESIS_BLOCK_PARENTHASH));

      if (config.hasPath(Constant.GENESIS_BLOCK_ASSETS)) {
        INSTANCE.genesisBlock.setAssets(getAccountsFromConfig(config));
        AccountStore.setAccount(config);
      }
      if (config.hasPath(Constant.GENESIS_BLOCK_WITNESSES)) {
        INSTANCE.genesisBlock.setWitnesses(getWitnessesFromConfig(config));
      }
    } else {
      INSTANCE.genesisBlock = GenesisBlock.getDefault();
    }

    INSTANCE.needSyncCheck =
        config.hasPath(Constant.BLOCK_NEED_SYNC_CHECK) && config.getBoolean(Constant.BLOCK_NEED_SYNC_CHECK);

    INSTANCE.nodeDiscoveryEnable =
        config.hasPath(Constant.NODE_DISCOVERY_ENABLE) && config.getBoolean(Constant.NODE_DISCOVERY_ENABLE);

    INSTANCE.nodeDiscoveryPersist =
        config.hasPath(Constant.NODE_DISCOVERY_PERSIST) && config.getBoolean(Constant.NODE_DISCOVERY_PERSIST);

    INSTANCE.nodeConnectionTimeout =
        config.hasPath(Constant.NODE_CONNECTION_TIMEOUT) ? config.getInt(Constant.NODE_CONNECTION_TIMEOUT) * 1000
            : 0;

    INSTANCE.nodeChannelReadTimeout =
        config.hasPath(Constant.NODE_CHANNEL_READ_TIMEOUT) ? config.getInt(Constant.NODE_CHANNEL_READ_TIMEOUT)
            : 0;

    INSTANCE.nodeMaxActiveNodes =
        config.hasPath(Constant.NODE_MAX_ACTIVE_NODES) ? config.getInt(Constant.NODE_MAX_ACTIVE_NODES) : 30;

    INSTANCE.nodeMaxActiveNodesWithSameIp =
        config.hasPath(Constant.NODE_MAX_ACTIVE_NODES_WITH_SAMEIP) ? config
            .getInt(Constant.NODE_MAX_ACTIVE_NODES_WITH_SAMEIP) : 2;

    INSTANCE.minParticipationRate =
        config.hasPath(Constant.NODE_MIN_PARTICIPATION_RATE) ? config.getInt(Constant.NODE_MIN_PARTICIPATION_RATE)
            : 0;

    INSTANCE.nodeListenPort =
        config.hasPath(Constant.NODE_LISTEN_PORT) ? config.getInt(Constant.NODE_LISTEN_PORT) : 0;

    bindIp(config);
    externalIp(config);

    INSTANCE.nodeDiscoveryPublicHomeNode =
        config.hasPath(Constant.NODE_DISCOVERY_PUBLIC_HOME_NODE) && config
            .getBoolean(Constant.NODE_DISCOVERY_PUBLIC_HOME_NODE);

    INSTANCE.nodeP2pPingInterval =
        config.hasPath(Constant.NODE_P2P_PING_INTERVAL) ? config.getLong(Constant.NODE_P2P_PING_INTERVAL) : 0;

    INSTANCE.nodeP2pVersion =
        config.hasPath(Constant.NODE_P2P_VERSION) ? config.getInt(Constant.NODE_P2P_VERSION) : 0;

    INSTANCE.rpcPort =
        config.hasPath(Constant.NODE_RPC_PORT) ? config.getInt(Constant.NODE_RPC_PORT) : 50051;

    INSTANCE.rpcOnSolidityPort =
        config.hasPath(Constant.NODE_RPC_SOLIDITY_PORT) ? config.getInt(Constant.NODE_RPC_SOLIDITY_PORT) : 50061;

    INSTANCE.fullNodeHttpPort =
        config.hasPath(Constant.NODE_HTTP_FULLNODE_PORT) ? config.getInt(Constant.NODE_HTTP_FULLNODE_PORT) : 8090;

    INSTANCE.solidityHttpPort =
        config.hasPath(Constant.NODE_HTTP_SOLIDITY_PORT) ? config.getInt(Constant.NODE_HTTP_SOLIDITY_PORT) : 8091;

    INSTANCE.rpcThreadNum =
        config.hasPath(Constant.NODE_RPC_THREAD) ? config.getInt(Constant.NODE_RPC_THREAD)
            : Runtime.getRuntime().availableProcessors() / 2;

    INSTANCE.solidityThreads =
        config.hasPath(Constant.NODE_SOLIDITY_THREADS) ? config.getInt(Constant.NODE_SOLIDITY_THREADS)
            : Runtime.getRuntime().availableProcessors();

    INSTANCE.maxConcurrentCallsPerConnection =
        config.hasPath(Constant.NODE_RPC_MAX_CONCURRENT_CALLS_PER_CONNECTION) ?
            config.getInt(Constant.NODE_RPC_MAX_CONCURRENT_CALLS_PER_CONNECTION) : Integer.MAX_VALUE;

    INSTANCE.flowControlWindow = config.hasPath(Constant.NODE_RPC_FLOW_CONTROL_WINDOW) ?
        config.getInt(Constant.NODE_RPC_FLOW_CONTROL_WINDOW)
        : NettyServerBuilder.DEFAULT_FLOW_CONTROL_WINDOW;

    INSTANCE.maxConnectionIdleInMillis = config.hasPath(Constant.NODE_RPC_MAX_CONNECTION_IDLE_IN_MILLIS) ?
        config.getLong(Constant.NODE_RPC_MAX_CONNECTION_IDLE_IN_MILLIS) : Long.MAX_VALUE;

    INSTANCE.blockProducedTimeOut = config.hasPath(Constant.NODE_PRODUCED_TIMEOUT) ?
        config.getInt(Constant.NODE_PRODUCED_TIMEOUT) : BLOCK_PRODUCE_TIMEOUT_PERCENT;

    INSTANCE.maxHttpConnectNumber = config.hasPath(Constant.NODE_MAX_HTTP_CONNECT_NUMBER) ?
        config.getInt(Constant.NODE_MAX_HTTP_CONNECT_NUMBER) : NodeConstant.MAX_HTTP_CONNECT_NUMBER;

    if (INSTANCE.blockProducedTimeOut < 30) {
      INSTANCE.blockProducedTimeOut = 30;
    }
    if (INSTANCE.blockProducedTimeOut > 100) {
      INSTANCE.blockProducedTimeOut = 100;
    }

    INSTANCE.netMaxTrxPerSecond = config.hasPath(Constant.NODE_NET_MAX_TRX_PER_SECOND) ?
        config.getInt(Constant.NODE_NET_MAX_TRX_PER_SECOND) : NetConstants.NET_MAX_TRX_PER_SECOND;

    INSTANCE.maxConnectionAgeInMillis = config.hasPath(Constant.NODE_RPC_MAX_CONNECTION_AGE_IN_MILLIS) ?
        config.getLong(Constant.NODE_RPC_MAX_CONNECTION_AGE_IN_MILLIS) : Long.MAX_VALUE;

    INSTANCE.maxMessageSize = config.hasPath(Constant.NODE_RPC_MAX_MESSAGE_SIZE) ?
        config.getInt(Constant.NODE_RPC_MAX_MESSAGE_SIZE) : GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

    INSTANCE.maxHeaderListSize = config.hasPath(Constant.NODE_RPC_MAX_HEADER_LIST_ISZE) ?
        config.getInt(Constant.NODE_RPC_MAX_HEADER_LIST_ISZE) : GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE;

    INSTANCE.maintenanceTimeInterval =
        config.hasPath(Constant.BLOCK_MAINTENANCE_TIME_INTERVAL) ? config
            .getInt(Constant.BLOCK_MAINTENANCE_TIME_INTERVAL) : 21600000L;

    INSTANCE.proposalExpireTime =
        config.hasPath(Constant.BLOCK_PROPOSAL_EXPIRE_TIME) ? config
            .getInt(Constant.BLOCK_PROPOSAL_EXPIRE_TIME) : 259200000L;

    INSTANCE.checkFrozenTime =
        config.hasPath(Constant.BLOCK_CHECK_FROZEN_TIME) ? config
            .getInt(Constant.BLOCK_CHECK_FROZEN_TIME) : 1;

    INSTANCE.allowCreationOfContracts =
        config.hasPath(Constant.COMMITTEE_ALLOW_CREATION_OF_CONTRACTS) ? config
            .getInt(Constant.COMMITTEE_ALLOW_CREATION_OF_CONTRACTS) : 0;

    INSTANCE.allowMultiSign =
        config.hasPath(Constant.COMMITTEE_ALLOW_MULTI_SIGN) ? config
            .getInt(Constant.COMMITTEE_ALLOW_MULTI_SIGN) : 0;

    INSTANCE.allowAdaptiveEnergy =
        config.hasPath(Constant.COMMITTEE_ALLOW_ADAPTIVE_ENERGY) ? config
            .getInt(Constant.COMMITTEE_ALLOW_ADAPTIVE_ENERGY) : 0;

    INSTANCE.allowDelegateResource =
        config.hasPath(Constant.COMMITTEE_ALLOW_DELEGATE_RESOURCE) ? config
            .getInt(Constant.COMMITTEE_ALLOW_DELEGATE_RESOURCE) : 0;

    INSTANCE.allowSameTokenName =
        config.hasPath(Constant.COMMITTEE_ALLOW_SAME_TOKEN_NAME) ? config
            .getInt(Constant.COMMITTEE_ALLOW_SAME_TOKEN_NAME) : 0;

    INSTANCE.allowTvmTransferTrc10 =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_TRANSFER_TRC10) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_TRANSFER_TRC10) : 0;

    INSTANCE.allowTvmConstantinople =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_CONSTANTINOPLE) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_CONSTANTINOPLE) : 0;

    INSTANCE.allowTvmSolidity059 =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_SOLIDITY059) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_SOLIDITY059) : 0;

    INSTANCE.forbidTransferToContract =
        config.hasPath("committee.forbidTransferToContract") ? config
            .getInt("committee.forbidTransferToContract") : 0;

    INSTANCE.tcpNettyWorkThreadNum = config.hasPath(Constant.NODE_TCP_NETTY_WORK_THREAD_NUM) ? config
        .getInt(Constant.NODE_TCP_NETTY_WORK_THREAD_NUM) : 0;

    INSTANCE.udpNettyWorkThreadNum = config.hasPath(Constant.NODE_UDP_NETTY_WORK_THREAD_NUM) ? config
        .getInt(Constant.NODE_UDP_NETTY_WORK_THREAD_NUM) : 1;

    if (StringUtils.isEmpty(INSTANCE.trustNodeAddr)) {
      INSTANCE.trustNodeAddr =
          config.hasPath(Constant.NODE_TRUST_NODE) ? config.getString(Constant.NODE_TRUST_NODE) : null;
    }

    INSTANCE.validateSignThreadNum = config.hasPath(Constant.NODE_VALIDATE_SIGN_THREAD_NUM) ? config
        .getInt(Constant.NODE_VALIDATE_SIGN_THREAD_NUM) : Runtime.getRuntime().availableProcessors() / 2;

    INSTANCE.walletExtensionApi =
        config.hasPath(Constant.NODE_WALLET_EXTENSION_API) && config.getBoolean(Constant.NODE_WALLET_EXTENSION_API);

    INSTANCE.connectFactor =
        config.hasPath(Constant.NODE_CONNECT_FACTOR) ? config.getDouble(Constant.NODE_CONNECT_FACTOR) : 0.3;

    INSTANCE.activeConnectFactor = config.hasPath(Constant.NODE_ACTIVE_CONNECT_FACTOR) ?
        config.getDouble(Constant.NODE_ACTIVE_CONNECT_FACTOR) : 0.1;

    INSTANCE.disconnectNumberFactor = config.hasPath(Constant.NODE_DISCONNECT_NUMBER_FACTOR) ?
        config.getDouble(Constant.NODE_DISCONNECT_NUMBER_FACTOR) : 0.4;
    INSTANCE.maxConnectNumberFactor = config.hasPath(Constant.NODE_MAX_CONNECT_NUMBER_FACTOR) ?
        config.getDouble(Constant.NODE_MAX_CONNECT_NUMBER_FACTOR) : 0.8;
    INSTANCE.receiveTcpMinDataLength = config.hasPath(Constant.NODE_RECEIVE_TCP_MIN_DATA_LENGTH) ?
        config.getLong(Constant.NODE_RECEIVE_TCP_MIN_DATA_LENGTH) : 2048;
    INSTANCE.isOpenFullTcpDisconnect = config.hasPath(Constant.NODE_IS_OPEN_FULL_TCP_DISCONNECT) && config
        .getBoolean(Constant.NODE_IS_OPEN_FULL_TCP_DISCONNECT);
    INSTANCE.needToUpdateAsset =
        config.hasPath(Constant.STORAGE_NEEDTO_UPDATE_ASSET) ? config
            .getBoolean(Constant.STORAGE_NEEDTO_UPDATE_ASSET)
            : true;
    INSTANCE.trxReferenceBlock = config.hasPath(Constant.TRX_REFERENCE_BLOCK) ?
        config.getString(Constant.TRX_REFERENCE_BLOCK) : "head";

    INSTANCE.trxExpirationTimeInMilliseconds =
        config.hasPath(Constant.TRX_EXPIRATION_TIME_IN_MILLIS_SECONDS)
            && config.getLong(Constant.TRX_EXPIRATION_TIME_IN_MILLIS_SECONDS) > 0 ?
            config.getLong(Constant.TRX_EXPIRATION_TIME_IN_MILLIS_SECONDS)
            : Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME;

    INSTANCE.minEffectiveConnection = config.hasPath(Constant.NODE_RPC_MIN_EFFECTIVE_CONNECTION) ?
        config.getInt(Constant.NODE_RPC_MIN_EFFECTIVE_CONNECTION) : 1;

    INSTANCE.blockNumForEneryLimit = config.hasPath(Constant.ENERGY_LIMIT_BLOCK_NUM) ?
        config.getInt(Constant.ENERGY_LIMIT_BLOCK_NUM) : 4727890L;

    INSTANCE.vmTrace =
        config.hasPath(Constant.VM_TRACE) && config.getBoolean(Constant.VM_TRACE);

    INSTANCE.saveInternalTx =
        config.hasPath(Constant.VM_SAVE_INTERNAL_TX) && config.getBoolean(Constant.VM_SAVE_INTERNAL_TX);

//    INSTANCE.allowShieldedTransaction =
//        config.hasPath(Constant.COMMITTEE_ALLOW_SHIELDED_TRANSACTION) ? config
//            .getInt(Constant.COMMITTEE_ALLOW_SHIELDED_TRANSACTION) : 0;

    INSTANCE.eventPluginConfig =
        config.hasPath(Constant.EVENT_SUBSCRIBE) ?
            getEventPluginConfig(config) : null;

    INSTANCE.eventFilter =
        config.hasPath(Constant.EVENT_SUBSCRIBE_FILTER) ? getEventFilter(config) : null;

    INSTANCE.fullNodeAllowShieldedTransactionArgs =
        !config.hasPath(Constant.NODE_FULLNODE_ALLOW_SHIELDED_TRANSACTION)
            || config.getBoolean(Constant.NODE_FULLNODE_ALLOW_SHIELDED_TRANSACTION);

    INSTANCE.zenTokenId = config.hasPath(Constant.NODE_ZEN_TOKENID) ?
        config.getString(Constant.NODE_ZEN_TOKENID) : "000000";

    INSTANCE.allowProtoFilterNum =
        config.hasPath(Constant.COMMITTEE_ALLOW_PROTO_FILTER_NUM) ? config
            .getInt(Constant.COMMITTEE_ALLOW_PROTO_FILTER_NUM) : 0;

    INSTANCE.allowAccountStateRoot =
        config.hasPath(Constant.COMMITTEE_ALLOW_ACCOUNT_STATE_ROOT) ? config
            .getInt(Constant.COMMITTEE_ALLOW_ACCOUNT_STATE_ROOT) : 0;

    INSTANCE.validContractProtoThreadNum =
        config.hasPath(Constant.NODE_VALID_CONTRACT_PROTO_THREADS) ? config
            .getInt(Constant.NODE_VALID_CONTRACT_PROTO_THREADS)
            : Runtime.getRuntime().availableProcessors();

    INSTANCE.activeNodes = getNodes(config, Constant.NODE_ACTIVE);

    INSTANCE.passiveNodes = getNodes(config, Constant.NODE_PASSIVE);

    INSTANCE.fastForwardNodes = getNodes(config, Constant.NODE_FAST_FORWARD);
    INSTANCE.shieldedTransInPendingMaxCounts =
        config.hasPath(Constant.NODE_SHIELDED_TRANS_IN_PENDING_MAX_COUNTS) ? config
            .getInt(Constant.NODE_SHIELDED_TRANS_IN_PENDING_MAX_COUNTS) : 10;

    if (INSTANCE.isWitness()) {
      INSTANCE.fullNodeAllowShieldedTransactionArgs = true;
    }

    INSTANCE.rateLimiterInitialization =
        config.hasPath(Constant.RATE_LIMITER) ? getRateLimiterFromConfig(config)
            : new RateLimiterInitialization();

    INSTANCE.changedDelegation =
        config.hasPath(Constant.COMMITTEE_CHANGED_DELEGATION) ? config
            .getInt(Constant.COMMITTEE_CHANGED_DELEGATION) : 0;

    initBackupProperty(config);
    if (Constant.ROCKSDB.equals(Args.getInstance().getStorage().getDbEngine().toUpperCase())) {
      initRocksDbBackupProperty(config);
      initRocksDbSettings(config);
    }

    INSTANCE.actuatorSet =
            config.hasPath(Constant.ACTUATOR_WHITELIST) ?
                    new HashSet<>(config.getStringList(Constant.ACTUATOR_WHITELIST))
                    : Collections.emptySet();


    logConfig();
    initDBConfig(INSTANCE);
  }

  private static List<Witness> getWitnessesFromConfig(final com.typesafe.config.Config config) {
    return config.getObjectList(Constant.GENESIS_BLOCK_WITNESSES).stream()
        .map(Args::createWitness)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static Witness createWitness(final ConfigObject witnessAccount) {
    final Witness witness = new Witness();
    witness.setAddress(
        Commons.decodeFromBase58Check(witnessAccount.get("address").unwrapped().toString()));
    witness.setUrl(witnessAccount.get("url").unwrapped().toString());
    witness.setVoteCount(witnessAccount.toConfig().getLong("voteCount"));
    return witness;
  }

  private static List<Account> getAccountsFromConfig(final com.typesafe.config.Config config) {
    return config.getObjectList(Constant.GENESIS_BLOCK_ASSETS).stream()
        .map(Args::createAccount)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static Account createAccount(final ConfigObject asset) {
    final Account account = new Account();
    account.setAccountName(asset.get("accountName").unwrapped().toString());
    account.setAccountType(asset.get("accountType").unwrapped().toString());
    account.setAddress(Commons.decodeFromBase58Check(asset.get("address").unwrapped().toString()));
    account.setBalance(asset.get("balance").unwrapped().toString());
    return account;
  }

  private static RateLimiterInitialization getRateLimiterFromConfig(
      final com.typesafe.config.Config config) {

    RateLimiterInitialization initialization = new RateLimiterInitialization();
    ArrayList<RateLimiterInitialization.HttpRateLimiterItem> list1 = config
        .getObjectList("rate.limiter.http").stream()
        .map(RateLimiterInitialization::createHttpItem)
        .collect(Collectors.toCollection(ArrayList::new));
    initialization.setHttpMap(list1);

    ArrayList<RateLimiterInitialization.RpcRateLimiterItem> list2 = config
        .getObjectList("rate.limiter.rpc").stream()
        .map(RateLimiterInitialization::createRpcItem)
        .collect(Collectors.toCollection(ArrayList::new));

    initialization.setRpcMap(list2);
    return initialization;
  }

  public static Args getInstance() {
    return INSTANCE;
  }

  private static List<Node> getNodes(final com.typesafe.config.Config config, String path) {
    if (!config.hasPath(path)) {
      return Collections.emptyList();
    }
    List<Node> ret = new ArrayList<>();
    List<String> list = config.getStringList(path);
    for (String configString : list) {
      Node n = Node.instanceOf(configString);
      if (!(INSTANCE.nodeDiscoveryBindIp.equals(n.getHost()) ||
          INSTANCE.nodeExternalIp.equals(n.getHost()) ||
          "127.0.0.1".equals(n.getHost())) ||
          INSTANCE.nodeListenPort != n.getPort()) {
        ret.add(n);
      }
    }
    return ret;
  }

  private static EventPluginConfig getEventPluginConfig(final com.typesafe.config.Config config) {
    EventPluginConfig eventPluginConfig = new EventPluginConfig();

    boolean useNativeQueue = false;
    int bindPort = 0;
    int sendQueueLength = 0;
    if (config.hasPath(Constant.USE_NATIVE_QUEUE)) {
      useNativeQueue = config.getBoolean(Constant.USE_NATIVE_QUEUE);

      if (config.hasPath(Constant.NATIVE_QUEUE_BIND_PORT)) {
        bindPort = config.getInt(Constant.NATIVE_QUEUE_BIND_PORT);
      }

      if (config.hasPath(Constant.NATIVE_QUEUE_SEND_LENGTH)) {
        sendQueueLength = config.getInt(Constant.NATIVE_QUEUE_SEND_LENGTH);
      }

      eventPluginConfig.setUseNativeQueue(useNativeQueue);
      eventPluginConfig.setBindPort(bindPort);
      eventPluginConfig.setSendQueueLength(sendQueueLength);
    }

    // use event plugin
    if (!useNativeQueue) {
      if (config.hasPath(Constant.EVENT_SUBSCRIBE_PATH)) {
        String pluginPath = config.getString(Constant.EVENT_SUBSCRIBE_PATH);
        if (StringUtils.isNotEmpty(pluginPath)) {
          eventPluginConfig.setPluginPath(pluginPath.trim());
        }
      }

      if (config.hasPath(Constant.EVENT_SUBSCRIBE_SERVER)) {
        String serverAddress = config.getString(Constant.EVENT_SUBSCRIBE_SERVER);
        if (StringUtils.isNotEmpty(serverAddress)) {
          eventPluginConfig.setServerAddress(serverAddress.trim());
        }
      }

      if (config.hasPath(Constant.EVENT_SUBSCIBE_DB_CONFIG)) {
        String dbConfig = config.getString(Constant.EVENT_SUBSCIBE_DB_CONFIG);
        if (StringUtils.isNotEmpty(dbConfig)) {
          eventPluginConfig.setDbConfig(dbConfig.trim());
        }
      }
    }

    if (config.hasPath(Constant.EVENT_SUBSCRIBE_TOPICS)) {
      List<TriggerConfig> triggerConfigList = config.getObjectList(Constant.EVENT_SUBSCRIBE_TOPICS)
          .stream()
          .map(Args::createTriggerConfig)
          .collect(Collectors.toCollection(ArrayList::new));

      eventPluginConfig.setTriggerConfigList(triggerConfigList);
    }

    return eventPluginConfig;
  }

  private static TriggerConfig createTriggerConfig(ConfigObject triggerObject) {
    if (Objects.isNull(triggerObject)) {
      return null;
    }

    TriggerConfig triggerConfig = new TriggerConfig();

    String triggerName = triggerObject.get("triggerName").unwrapped().toString();
    triggerConfig.setTriggerName(triggerName);

    String enabled = triggerObject.get("enable").unwrapped().toString();
    triggerConfig.setEnabled("true".equalsIgnoreCase(enabled));

    String topic = triggerObject.get("topic").unwrapped().toString();
    triggerConfig.setTopic(topic);

    return triggerConfig;
  }

  private static FilterQuery getEventFilter(final com.typesafe.config.Config config) {
    FilterQuery filter = new FilterQuery();
    long fromBlockLong = 0, toBlockLong = 0;

    String fromBlock = config.getString(Constant.EVENT_SUBSCRIBE_FROM_BLOCK).trim();
    try {
      fromBlockLong = FilterQuery.parseFromBlockNumber(fromBlock);
    } catch (Exception e) {
      logger.error("{}", e);
      return null;
    }
    filter.setFromBlock(fromBlockLong);

    String toBlock = config.getString(Constant.EVENT_SUBSCRIBE_TO_BLOCK).trim();
    try {
      toBlockLong = FilterQuery.parseToBlockNumber(toBlock);
    } catch (Exception e) {
      logger.error("{}", e);
      return null;
    }
    filter.setToBlock(toBlockLong);

    List<String> addressList = config.getStringList(Constant.EVENT_SUBSCRIBE_CONTRACT_ADDRESS);
    addressList = addressList.stream().filter(address -> StringUtils.isNotEmpty(address)).collect(
        Collectors.toList());
    filter.setContractAddressList(addressList);

    List<String> topicList = config.getStringList(Constant.EVENT_SUBSCRIBE_CONTRACT_TOPIC);
    topicList = topicList.stream().filter(top -> StringUtils.isNotEmpty(top)).collect(
        Collectors.toList());
    filter.setContractTopicList(topicList);

    return filter;
  }

  private static String getGeneratedNodePrivateKey() {
    String nodeId;
    try {
      File file = new File(
          INSTANCE.outputDirectory + File.separator + INSTANCE.storage.getDbDirectory(),
          "nodeId.properties");
      Properties props = new Properties();
      if (file.canRead()) {
        try (Reader r = new FileReader(file)) {
          props.load(r);
        }
      } else {
        ECKey key = new ECKey();
        props.setProperty("nodeIdPrivateKey", Hex.toHexString(key.getPrivKeyBytes()));
        props.setProperty("nodeId", Hex.toHexString(key.getNodeId()));
        file.getParentFile().mkdirs();
        try (Writer w = new FileWriter(file)) {
          props.store(w,
              "Generated NodeID. To use your own nodeId please refer to 'peer.privateKey' config option.");
        }
        logger.info("New nodeID generated: " + props.getProperty("nodeId"));
        logger.info("Generated nodeID and its private key stored in " + file);
      }
      nodeId = props.getProperty("nodeIdPrivateKey");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return nodeId;
  }

  private static void bindIp(final com.typesafe.config.Config config) {
    if (!config.hasPath(Constant.NODE_DISCOVERY_BIND_IP) || config.getString(Constant.NODE_DISCOVERY_BIND_IP)
            .trim().isEmpty()) {
      if (INSTANCE.nodeDiscoveryBindIp == null) {
        logger.info("Bind address wasn't set, Punching to identify it...");
        try (Socket s = new Socket("www.baidu.com", 80)) {
          INSTANCE.nodeDiscoveryBindIp = s.getLocalAddress().getHostAddress();
          logger.info("UDP local bound to: {}", INSTANCE.nodeDiscoveryBindIp);
        } catch (IOException e) {
          logger.warn("Can't get bind IP. Fall back to 0.0.0.0: " + e);
          INSTANCE.nodeDiscoveryBindIp = "0.0.0.0";
        }
      }
    } else {
      INSTANCE.nodeDiscoveryBindIp = config.getString(Constant.NODE_DISCOVERY_BIND_IP).trim();
    }
  }

  private static void externalIp(final com.typesafe.config.Config config) {
    if (!config.hasPath(Constant.NODE_DISCOVERY_EXTENNAL_IP) || config
            .getString(Constant.NODE_DISCOVERY_EXTENNAL_IP).trim().isEmpty()) {
      if (INSTANCE.nodeExternalIp == null) {
        logger.info("External IP wasn't set, using checkip.amazonaws.com to identify it...");
        BufferedReader in = null;
        try {
          in = new BufferedReader(new InputStreamReader(
                  new URL(Constant.AMAZONAWS_URL).openStream()));
          INSTANCE.nodeExternalIp = in.readLine();
          if (INSTANCE.nodeExternalIp == null || INSTANCE.nodeExternalIp.trim().isEmpty()) {
            throw new IOException("Invalid address: '" + INSTANCE.nodeExternalIp + "'");
          }
          try {
            InetAddress.getByName(INSTANCE.nodeExternalIp);
          } catch (Exception e) {
            throw new IOException("Invalid address: '" + INSTANCE.nodeExternalIp + "'");
          }
          logger.info("External address identified: {}", INSTANCE.nodeExternalIp);
        } catch (IOException e) {
          INSTANCE.nodeExternalIp = INSTANCE.nodeDiscoveryBindIp;
          logger.warn(
                  "Can't get external IP. Fall back to peer.bind.ip: " + INSTANCE.nodeExternalIp + " :"
                          + e);
        } finally {
          if (in != null) {
            try {
              in.close();
            } catch (IOException e) {
              //ignore
            }
          }

        }
      }
    } else {
      INSTANCE.nodeExternalIp = config.getString(Constant.NODE_DISCOVERY_EXTENNAL_IP).trim();
    }
  }

  private static double calcMaxTimeRatio() {
    //return max(2.0, min(5.0, 5 * 4.0 / max(Runtime.getRuntime().availableProcessors(), 1)));
    return 5.0;
  }

  private static void initRocksDbSettings(Config config) {
    String prefix = "storage.dbSettings.";
    int levelNumber = config.hasPath(prefix + "levelNumber")
        ? config.getInt(prefix + "levelNumber") : 7;
    int compactThreads = config.hasPath(prefix + "compactThreads")
        ? config.getInt(prefix + "compactThreads")
        : max(Runtime.getRuntime().availableProcessors(), 1);
    int blocksize = config.hasPath(prefix + "blocksize")
        ? config.getInt(prefix + "blocksize") : 16;
    long maxBytesForLevelBase = config.hasPath(prefix + "maxBytesForLevelBase")
        ? config.getInt(prefix + "maxBytesForLevelBase") : 256;
    double maxBytesForLevelMultiplier = config.hasPath(prefix + "maxBytesForLevelMultiplier")
        ? config.getDouble(prefix + "maxBytesForLevelMultiplier") : 10;
    int level0FileNumCompactionTrigger =
        config.hasPath(prefix + "level0FileNumCompactionTrigger") ? config
            .getInt(prefix + "level0FileNumCompactionTrigger") : 2;
    long targetFileSizeBase = config.hasPath(prefix + "targetFileSizeBase") ? config
        .getLong(prefix + "targetFileSizeBase") : 64;
    int targetFileSizeMultiplier = config.hasPath(prefix + "targetFileSizeMultiplier") ? config
        .getInt(prefix + "targetFileSizeMultiplier") : 1;

    INSTANCE.rocksDBCustomSettings = RocksDbSettings
        .initCustomSettings(levelNumber, compactThreads, blocksize, maxBytesForLevelBase,
            maxBytesForLevelMultiplier, level0FileNumCompactionTrigger,
            targetFileSizeBase, targetFileSizeMultiplier);
    RocksDbSettings.loggingSettings();
  }

  private static void initRocksDbBackupProperty(Config config) {
    boolean enable =
        config.hasPath(Constant.STORAGE_BACKUP_ENABLE) && config.getBoolean(Constant.STORAGE_BACKUP_ENABLE);
    String propPath = config.hasPath(Constant.STORAGE_BACKUP_PROP_PATH)
        ? config.getString(Constant.STORAGE_BACKUP_PROP_PATH) : "prop.properties";
    String bak1path = config.hasPath("storage.backup.bak1path")
        ? config.getString("storage.backup.bak1path") : "bak1/database/";
    String bak2path = config.hasPath("storage.backup.bak2path")
        ? config.getString("storage.backup.bak2path") : "bak2/database/";
    int frequency = config.hasPath("storage.backup.frequency")
        ? config.getInt("storage.backup.frequency") : 10000;
    INSTANCE.dbBackupConfig = DbBackupConfig.getInstance()
        .initArgs(enable, propPath, bak1path, bak2path, frequency);
  }

  private static void initBackupProperty(Config config) {
    INSTANCE.backupPriority = config.hasPath(Constant.NODE_BACKUP_PRIORITY)
        ? config.getInt(Constant.NODE_BACKUP_PRIORITY) : 0;
    INSTANCE.backupPort = config.hasPath(Constant.NODE_BACKUP_PORT)
        ? config.getInt(Constant.NODE_BACKUP_PORT) : 10001;
    INSTANCE.backupMembers = config.hasPath(Constant.NODE_BACKUP_MEMBERS)
        ? config.getStringList(Constant.NODE_BACKUP_MEMBERS) : new ArrayList<>();
  }

  private static void logConfig() {
    Args args = getInstance();
    logger.info("\n");
    logger.info("************************ Net config ************************");
    logger.info("P2P version: {}", args.getNodeP2pVersion());
    logger.info("Bind IP: {}", args.getNodeDiscoveryBindIp());
    logger.info("External IP: {}", args.getNodeExternalIp());
    logger.info("Listen port: {}", args.getNodeListenPort());
    logger.info("Discover enable: {}", args.isNodeDiscoveryEnable());
    logger.info("Active node size: {}", args.getActiveNodes().size());
    logger.info("Passive node size: {}", args.getPassiveNodes().size());
    logger.info("FastForward node size: {}", args.getFastForwardNodes().size());
    logger.info("Seed node size: {}", args.getSeedNode().getIpList().size());
    logger.info("Max connection: {}", args.getNodeMaxActiveNodes());
    logger.info("Max connection with same IP: {}", args.getNodeMaxActiveNodesWithSameIp());
    logger.info("Solidity threads: {}", args.getSolidityThreads());
    logger.info("************************ Backup config ************************");
    logger.info("Backup listen port: {}", args.getBackupPort());
    logger.info("Backup member size: {}", args.getBackupMembers().size());
    logger.info("Backup priority: {}", args.getBackupPriority());
    logger.info("************************ Code version *************************");
    logger.info("Code version : {}", Version.getVersion());
    logger.info("Version name: {}", Version.versionName);
    logger.info("Version code: {}", Version.versionCode);
    logger.info("************************ DB config *************************");
    logger.info("DB version : {}", args.getStorage().getDbVersion());
    logger.info("DB engine : {}", args.getStorage().getDbEngine());
    logger.info("***************************************************************");
    logger.info("\n");
  }

  public static void initEncryptoEngine(Args cfgArgs) {
    DBConfig.setECKeyCryptoEngine(cfgArgs.isEckey());
  }
  public static void initDBConfig(Args cfgArgs) {
    if (Objects.nonNull(cfgArgs.getStorage())) {
      DBConfig.setDbVersion(cfgArgs.getStorage().getDbVersion());
      DBConfig.setDbEngine(cfgArgs.getStorage().getDbEngine());
      DBConfig.setPropertyMap(cfgArgs.getStorage().getPropertyMap());
      DBConfig.setDbSync(cfgArgs.getStorage().isDbSync());
      DBConfig.setDbDirectory(cfgArgs.getStorage().getDbDirectory());
    }

    if (Objects.nonNull(cfgArgs.getGenesisBlock())) {
      DBConfig.setBlocktimestamp(cfgArgs.getGenesisBlock().getTimestamp());
      DBConfig.setGenesisBlock(cfgArgs.getGenesisBlock());
    }

    DBConfig.setOutputDirectoryConfig(cfgArgs.getOutputDirectory());
    DBConfig.setRocksDbSettings(cfgArgs.getRocksDBCustomSettings());
    DBConfig.setAllowMultiSign(cfgArgs.getAllowMultiSign());
    DBConfig.setMaintenanceTimeInterval(cfgArgs.getMaintenanceTimeInterval());
    DBConfig.setAllowAdaptiveEnergy(cfgArgs.getAllowAdaptiveEnergy());
    DBConfig.setAllowDelegateResource(cfgArgs.getAllowDelegateResource());
    DBConfig.setAllowTvmTransferTrc10(cfgArgs.getAllowTvmTransferTrc10());
    DBConfig.setAllowTvmConstantinople(cfgArgs.getAllowTvmConstantinople());
    DBConfig.setAllowTvmSolidity059(cfgArgs.getAllowTvmSolidity059());
    DBConfig.setForbidTransferToContract(cfgArgs.getForbidTransferToContract());
    DBConfig.setAllowSameTokenName(cfgArgs.getAllowSameTokenName());
    DBConfig.setAllowCreationOfContracts(cfgArgs.getAllowCreationOfContracts());
//    DBConfig.setAllowShieldedTransaction(cfgArgs.getAllowShieldedTransaction());
    DBConfig.setAllowAccountStateRoot(cfgArgs.getAllowAccountStateRoot());
    DBConfig.setAllowProtoFilterNum(cfgArgs.getAllowProtoFilterNum());
    DBConfig.setProposalExpireTime(cfgArgs.getProposalExpireTime());
    DBConfig.setBlockNumForEneryLimit(cfgArgs.getBlockNumForEneryLimit());
    DBConfig.setFullNodeAllowShieldedTransaction(cfgArgs.isFullNodeAllowShieldedTransactionArgs());
    DBConfig.setZenTokenId(cfgArgs.getZenTokenId());
    DBConfig.setCheckFrozenTime(cfgArgs.getCheckFrozenTime());
    DBConfig.setValidContractProtoThreadNum(cfgArgs.getValidContractProtoThreadNum());
    DBConfig.setVmTrace(cfgArgs.isVmTrace());
    DBConfig.setDebug(cfgArgs.isDebug());
    DBConfig.setMinTimeRatio(cfgArgs.getMinTimeRatio());
    DBConfig.setMaxTimeRatio(cfgArgs.getMaxTimeRatio());
    DBConfig.setSolidityNode(cfgArgs.isSolidityNode());
    DBConfig.setSupportConstant(cfgArgs.isSupportConstant());
    DBConfig.setLongRunningTime(cfgArgs.getLongRunningTime());
    DBConfig.setChangedDelegation(cfgArgs.getChangedDelegation());
    DBConfig.setActuatorSet(cfgArgs.getActuatorSet());
//    DBConfig.setECKeyCryptoEngine(cfgArgs.isECKeyCryptoEngine());
    DBConfig.setECKeyCryptoEngine(cfgArgs.isEckey());
  }

  public void setFullNodeAllowShieldedTransaction(boolean fullNodeAllowShieldedTransaction) {
    this.fullNodeAllowShieldedTransactionArgs = fullNodeAllowShieldedTransaction;
    DBConfig.setFullNodeAllowShieldedTransaction(fullNodeAllowShieldedTransaction);
  }

  /**
   * Get storage path by name of database
   *
   * @param dbName name of database
   * @return path of that database
   */
  public String getOutputDirectoryByDbName(String dbName) {
    String path = storage.getPathByDbName(dbName);
    if (!StringUtils.isBlank(path)) {
      return path;
    }
    return getOutputDirectory();
  }

  /**
   * get output directory.
   */
  public String getOutputDirectory() {
    if (!this.outputDirectory.equals("") && !this.outputDirectory.endsWith(File.separator)) {
      return this.outputDirectory + File.separator;
    }
    return this.outputDirectory;
  }

  public ECKey getMyKey() {
    if (StringUtils.isEmpty(INSTANCE.p2pNodeId)) {
      INSTANCE.p2pNodeId = getGeneratedNodePrivateKey();
    }

    return ECKey.fromPrivate(Hex.decode(INSTANCE.p2pNodeId));
  }

  public boolean isECKeyCryptoEngine() {
//    return cryptoEngine.equalsIgnoreCase(Constant.ECKey_ENGINE);
    return isEckey;
  }
}