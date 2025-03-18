package org.tron.core.config.args;

import static java.lang.System.exit;
import static org.tron.common.math.Maths.max;
import static org.tron.common.math.Maths.min;
import static org.tron.core.Constant.ADD_PRE_FIX_BYTE_MAINNET;
import static org.tron.core.Constant.DYNAMIC_ENERGY_INCREASE_FACTOR_RANGE;
import static org.tron.core.Constant.DYNAMIC_ENERGY_MAX_FACTOR_RANGE;
import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCE_TIMEOUT_PERCENT;
import static org.tron.core.config.Parameter.ChainConstant.MAX_ACTIVE_WITNESS_NUM;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyServerBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.arch.Arch;
import org.tron.common.args.Account;
import org.tron.common.args.GenesisBlock;
import org.tron.common.args.Witness;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.context.GlobalContext;
import org.tron.common.crypto.SignInterface;
import org.tron.common.logsfilter.EventPluginConfig;
import org.tron.common.logsfilter.FilterQuery;
import org.tron.common.logsfilter.TriggerConfig;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.parameter.RateLimiterInitialization;
import org.tron.common.setting.RocksDbSettings;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.LocalWitnesses;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.Configuration;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.exception.CipherException;
import org.tron.core.exception.TronError;
import org.tron.core.store.AccountStore;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.dns.update.DnsType;
import org.tron.p2p.dns.update.PublishConfig;
import org.tron.p2p.utils.NetUtil;
import org.tron.program.Version;

@Slf4j(topic = "app")
@NoArgsConstructor
@Component
public class Args extends CommonParameter {

  @Getter
  @Setter
  private static LocalWitnesses localWitnesses = new LocalWitnesses();

  @Autowired(required = false)
  @Getter
  private static final ConcurrentHashMap<Long, BlockingQueue<ContractLogTrigger>>
      solidityContractLogTriggerMap = new ConcurrentHashMap<>();

  @Autowired(required = false)
  @Getter
  private static final ConcurrentHashMap<Long, BlockingQueue<ContractEventTrigger>>
      solidityContractEventTriggerMap = new ConcurrentHashMap<>();


  public static void clearParam() {
    PARAMETER.shellConfFileName = "";
    PARAMETER.outputDirectory = "output-directory";
    PARAMETER.help = false;
    PARAMETER.witness = false;
    PARAMETER.seedNodes = new ArrayList<>();
    PARAMETER.privateKey = "";
    PARAMETER.witnessAddress = "";
    PARAMETER.storageDbDirectory = "";
    PARAMETER.storageIndexDirectory = "";
    PARAMETER.storageIndexSwitch = "";

    // FIXME: PARAMETER.storage maybe null ?
    if (PARAMETER.storage != null) {
      // WARNING: WILL DELETE DB STORAGE PATHS
      PARAMETER.storage.deleteAllStoragePaths();
      PARAMETER.storage = null;
    }

    PARAMETER.overlay = null;
    PARAMETER.seedNode = null;
    PARAMETER.genesisBlock = null;
    PARAMETER.chainId = null;
    localWitnesses = null;
    PARAMETER.needSyncCheck = false;
    PARAMETER.nodeDiscoveryEnable = false;
    PARAMETER.nodeDiscoveryPersist = false;
    PARAMETER.nodeEffectiveCheckEnable = false;
    PARAMETER.nodeConnectionTimeout = 2000;
    PARAMETER.activeNodes = new ArrayList<>();
    PARAMETER.passiveNodes = new ArrayList<>();
    PARAMETER.fastForwardNodes = new ArrayList<>();
    PARAMETER.maxFastForwardNum = 3;
    PARAMETER.nodeChannelReadTimeout = 0;
    PARAMETER.maxConnections = 30;
    PARAMETER.minConnections = 8;
    PARAMETER.minActiveConnections = 3;
    PARAMETER.maxConnectionsWithSameIp = 2;
    PARAMETER.maxTps = 1000;
    PARAMETER.minParticipationRate = 0;
    PARAMETER.nodeListenPort = 0;
    PARAMETER.nodeLanIp = "";
    PARAMETER.nodeExternalIp = "";
    PARAMETER.nodeP2pVersion = 0;
    PARAMETER.nodeEnableIpv6 = false;
    PARAMETER.dnsTreeUrls = new ArrayList<>();
    PARAMETER.dnsPublishConfig = null;
    PARAMETER.syncFetchBatchNum = 2000;
    PARAMETER.rpcPort = 0;
    PARAMETER.rpcOnSolidityPort = 0;
    PARAMETER.rpcOnPBFTPort = 0;
    PARAMETER.fullNodeHttpPort = 0;
    PARAMETER.solidityHttpPort = 0;
    PARAMETER.pBFTHttpPort = 0;
    PARAMETER.pBFTExpireNum = 20;
    PARAMETER.jsonRpcHttpFullNodePort = 0;
    PARAMETER.jsonRpcHttpSolidityPort = 0;
    PARAMETER.jsonRpcHttpPBFTPort = 0;
    PARAMETER.maintenanceTimeInterval = 0;
    PARAMETER.proposalExpireTime = 0;
    PARAMETER.checkFrozenTime = 1;
    PARAMETER.allowCreationOfContracts = 0;
    PARAMETER.allowAdaptiveEnergy = 0;
    PARAMETER.allowTvmTransferTrc10 = 0;
    PARAMETER.allowTvmConstantinople = 0;
    PARAMETER.allowDelegateResource = 0;
    PARAMETER.allowSameTokenName = 0;
    PARAMETER.allowTvmSolidity059 = 0;
    PARAMETER.forbidTransferToContract = 0;
    PARAMETER.tcpNettyWorkThreadNum = 0;
    PARAMETER.udpNettyWorkThreadNum = 0;
    PARAMETER.solidityNode = false;
    PARAMETER.trustNodeAddr = "";
    PARAMETER.walletExtensionApi = false;
    PARAMETER.estimateEnergy = false;
    PARAMETER.estimateEnergyMaxRetry = 3;
    PARAMETER.receiveTcpMinDataLength = 2048;
    PARAMETER.isOpenFullTcpDisconnect = false;
    PARAMETER.nodeDetectEnable = false;
    PARAMETER.inactiveThreshold = 600;
    PARAMETER.supportConstant = false;
    PARAMETER.debug = false;
    PARAMETER.minTimeRatio = 0.0;
    PARAMETER.maxTimeRatio = 5.0;
    PARAMETER.longRunningTime = 10;
    // PARAMETER.allowShieldedTransaction = 0;
    PARAMETER.maxHttpConnectNumber = 50;
    PARAMETER.allowMultiSign = 0;
    PARAMETER.trxExpirationTimeInMilliseconds = 0;
    PARAMETER.fullNodeAllowShieldedTransactionArgs = true;
    PARAMETER.zenTokenId = "000000";
    PARAMETER.allowProtoFilterNum = 0;
    PARAMETER.allowAccountStateRoot = 0;
    PARAMETER.validContractProtoThreadNum = 1;
    PARAMETER.shieldedTransInPendingMaxCounts = 10;
    PARAMETER.changedDelegation = 0;
    PARAMETER.rpcEnable = true;
    PARAMETER.rpcSolidityEnable = true;
    PARAMETER.rpcPBFTEnable = true;
    PARAMETER.fullNodeHttpEnable = true;
    PARAMETER.solidityNodeHttpEnable = true;
    PARAMETER.pBFTHttpEnable = true;
    PARAMETER.jsonRpcHttpFullNodeEnable = false;
    PARAMETER.jsonRpcHttpSolidityNodeEnable = false;
    PARAMETER.jsonRpcHttpPBFTNodeEnable = false;
    PARAMETER.nodeMetricsEnable = false;
    PARAMETER.metricsStorageEnable = false;
    PARAMETER.metricsPrometheusEnable = false;
    PARAMETER.agreeNodeCount = MAX_ACTIVE_WITNESS_NUM * 2 / 3 + 1;
    PARAMETER.allowPBFT = 0;
    PARAMETER.allowShieldedTRC20Transaction = 0;
    PARAMETER.allowMarketTransaction = 0;
    PARAMETER.allowTransactionFeePool = 0;
    PARAMETER.allowBlackHoleOptimization = 0;
    PARAMETER.allowNewResourceModel = 0;
    PARAMETER.allowTvmIstanbul = 0;
    PARAMETER.allowTvmFreeze = 0;
    PARAMETER.allowTvmVote = 0;
    PARAMETER.allowTvmLondon = 0;
    PARAMETER.allowTvmCompatibleEvm = 0;
    PARAMETER.historyBalanceLookup = false;
    PARAMETER.openPrintLog = true;
    PARAMETER.openTransactionSort = false;
    PARAMETER.allowAccountAssetOptimization = 0;
    PARAMETER.allowAssetOptimization = 0;
    PARAMETER.disabledApiList = Collections.emptyList();
    PARAMETER.shutdownBlockTime = null;
    PARAMETER.shutdownBlockHeight = -1;
    PARAMETER.shutdownBlockCount = -1;
    PARAMETER.blockCacheTimeout = 60;
    PARAMETER.allowNewRewardAlgorithm = 0;
    PARAMETER.allowNewReward = 0;
    PARAMETER.memoFee = 0;
    PARAMETER.rateLimiterGlobalQps = 50000;
    PARAMETER.rateLimiterGlobalIpQps = 10000;
    PARAMETER.rateLimiterGlobalApiQps = 1000;
    PARAMETER.p2pDisable = false;
    PARAMETER.dynamicConfigEnable = false;
    PARAMETER.dynamicConfigCheckInterval = 600;
    PARAMETER.allowTvmShangHai = 0;
    PARAMETER.unsolidifiedBlockCheck = false;
    PARAMETER.maxUnsolidifiedBlocks = 54;
    PARAMETER.allowOldRewardOpt = 0;
    PARAMETER.allowEnergyAdjustment = 0;
    PARAMETER.allowStrictMath = 0;
    PARAMETER.consensusLogicOptimization = 0;
    PARAMETER.allowTvmCancun = 0;
    PARAMETER.allowTvmBlob = 0;
    GlobalContext.removeHeader();
  }

  /**
   * print Version.
   */
  private static void printVersion() {
    Properties properties = new Properties();
    boolean noGitProperties = true;
    try {
      InputStream in = Thread.currentThread()
          .getContextClassLoader().getResourceAsStream("git.properties");
      if (in != null) {
        noGitProperties = false;
        properties.load(in);
      }
    } catch (IOException e) {
      logger.error(e.getMessage());
    }
    JCommander jCommander = new JCommander();
    jCommander.getConsole().println("OS : " + System.getProperty("os.name"));
    jCommander.getConsole().println("JVM : " + System.getProperty("java.vendor") + " "
        + System.getProperty("java.version") + " " + System.getProperty("os.arch"));
    if (!noGitProperties) {
      jCommander.getConsole().println("Git : " + properties.getProperty("git.commit.id"));
    }
    jCommander.getConsole().println("Version : " + Version.getVersion());
    jCommander.getConsole().println("Code : " + Version.VERSION_CODE);
  }

  public static void printHelp(JCommander jCommander) {
    List<ParameterDescription> parameterDescriptionList = jCommander.getParameters();
    Map<String, ParameterDescription> stringParameterDescriptionMap = new HashMap<>();
    for (ParameterDescription parameterDescription : parameterDescriptionList) {
      String parameterName = parameterDescription.getParameterized().getName();
      stringParameterDescriptionMap.put(parameterName, parameterDescription);
    }

    StringBuilder helpStr = new StringBuilder();
    helpStr.append("Name:\n\tFullNode - the java-tron command line interface\n");
    String programName = Strings.isNullOrEmpty(jCommander.getProgramName()) ? "FullNode.jar" :
        jCommander.getProgramName();
    helpStr.append(String.format("%nUsage: java -jar %s [options] [seedNode <seedNode> ...]%n",
        programName));
    helpStr.append(String.format("%nVERSION: %n%s-%s%n", Version.getVersion(),
        getCommitIdAbbrev()));

    Map<String, String[]> groupOptionListMap = Args.getOptionGroup();
    for (Map.Entry<String, String[]> entry : groupOptionListMap.entrySet()) {
      String group = entry.getKey();
      helpStr.append(String.format("%n%s OPTIONS:%n", group.toUpperCase()));
      int optionMaxLength = Arrays.stream(entry.getValue()).mapToInt(p -> {
        ParameterDescription tmpParameterDescription = stringParameterDescriptionMap.get(p);
        if (tmpParameterDescription == null) {
          return 1;
        }
        return tmpParameterDescription.getNames().length();
      }).max().orElse(1);

      for (String option : groupOptionListMap.get(group)) {
        ParameterDescription parameterDescription = stringParameterDescriptionMap.get(option);
        if (parameterDescription == null) {
          logger.warn("Miss option:{}", option);
          continue;
        }
        String tmpOptionDesc = String.format("%s\t\t\t%s%n",
            Strings.padEnd(parameterDescription.getNames(), optionMaxLength, ' '),
            upperFirst(parameterDescription.getDescription()));
        helpStr.append(tmpOptionDesc);
      }
    }
    jCommander.getConsole().println(helpStr.toString());
  }

  public static String upperFirst(String name) {
    if (name.length() <= 1) {
      return name;
    }
    name = name.substring(0, 1).toUpperCase() + name.substring(1);
    return name;
  }

  private static String getCommitIdAbbrev() {
    Properties properties = new Properties();
    try {
      InputStream in = Thread.currentThread()
          .getContextClassLoader().getResourceAsStream("git.properties");
      properties.load(in);
    } catch (IOException e) {
      logger.warn("Load resource failed,git.properties {}", e.getMessage());
    }
    return properties.getProperty("git.commit.id.abbrev");
  }

  private static Map<String, String[]> getOptionGroup() {
    String[] tronOption = new String[] {"version", "help", "shellConfFileName", "logbackPath",
        "eventSubscribe"};
    String[] dbOption = new String[] {"outputDirectory"};
    String[] witnessOption = new String[] {"witness", "privateKey"};
    String[] vmOption = new String[] {"debug"};

    Map<String, String[]> optionGroupMap = new LinkedHashMap<>();
    optionGroupMap.put("tron", tronOption);
    optionGroupMap.put("db", dbOption);
    optionGroupMap.put("witness", witnessOption);
    optionGroupMap.put("virtual machine", vmOption);

    for (String[] optionList : optionGroupMap.values()) {
      for (String option : optionList) {
        try {
          CommonParameter.class.getField(option);
        } catch (NoSuchFieldException e) {
          logger.warn("NoSuchFieldException:{},{}", option, e.getMessage());
        }
      }
    }
    return optionGroupMap;
  }

  /**
   * set parameters.
   */
  public static void setParam(final String[] args, final String confFileName) {
    JCommander.newBuilder().addObject(PARAMETER).build().parse(args);
    if (PARAMETER.version) {
      printVersion();
      exit(0);
    }

    Config config = Configuration.getByFileName(PARAMETER.shellConfFileName, confFileName);
    setParam(config);
  }

  /**
   * set parameters.
   */
  public static void setParam(final Config config) {

    if (config.hasPath(Constant.NET_TYPE)
        && Constant.TESTNET.equalsIgnoreCase(config.getString(Constant.NET_TYPE))) {
      Wallet.setAddressPreFixByte(Constant.ADD_PRE_FIX_BYTE_TESTNET);
      Wallet.setAddressPreFixString(Constant.ADD_PRE_FIX_STRING_TESTNET);
    } else {
      Wallet.setAddressPreFixByte(ADD_PRE_FIX_BYTE_MAINNET);
      Wallet.setAddressPreFixString(Constant.ADD_PRE_FIX_STRING_MAINNET);
    }

    PARAMETER.cryptoEngine = config.hasPath(Constant.CRYPTO_ENGINE) ? config
        .getString(Constant.CRYPTO_ENGINE) : Constant.ECKey_ENGINE;

    if (StringUtils.isNoneBlank(PARAMETER.privateKey)) {
      localWitnesses = (new LocalWitnesses(PARAMETER.privateKey));
      if (StringUtils.isNoneBlank(PARAMETER.witnessAddress)) {
        byte[] bytes = Commons.decodeFromBase58Check(PARAMETER.witnessAddress);
        if (bytes != null) {
          localWitnesses.setWitnessAccountAddress(bytes);
          logger.debug("Got localWitnessAccountAddress from cmd");
        } else {
          PARAMETER.witnessAddress = "";
          logger.warn(IGNORE_WRONG_WITNESS_ADDRESS_FORMAT);
        }
      }
      localWitnesses.initWitnessAccountAddress(PARAMETER.isECKeyCryptoEngine());
      logger.debug("Got privateKey from cmd");
    } else if (config.hasPath(Constant.LOCAL_WITNESS)) {
      localWitnesses = new LocalWitnesses();
      List<String> localwitness = config.getStringList(Constant.LOCAL_WITNESS);
      localWitnesses.setPrivateKeys(localwitness);
      witnessAddressCheck(config);
      localWitnesses.initWitnessAccountAddress(PARAMETER.isECKeyCryptoEngine());
      logger.debug("Got privateKey from config.conf");
    } else if (config.hasPath(Constant.LOCAL_WITNESS_KEYSTORE)) {
      localWitnesses = new LocalWitnesses();
      List<String> privateKeys = new ArrayList<String>();
      if (PARAMETER.isWitness()) {
        List<String> localwitness = config.getStringList(Constant.LOCAL_WITNESS_KEYSTORE);
        if (localwitness.size() > 0) {
          String fileName = System.getProperty("user.dir") + "/" + localwitness.get(0);
          String password;
          if (StringUtils.isEmpty(PARAMETER.password)) {
            System.out.println("Please input your password.");
            password = WalletUtils.inputPassword();
          } else {
            password = PARAMETER.password;
            PARAMETER.password = null;
          }

          try {
            Credentials credentials = WalletUtils
                .loadCredentials(password, new File(fileName));
            SignInterface sign = credentials.getSignInterface();
            String prikey = ByteArray.toHexString(sign.getPrivateKey());
            privateKeys.add(prikey);
          } catch (IOException | CipherException e) {
            logger.error("Witness node start failed!");
            throw new TronError(e, TronError.ErrCode.WITNESS_KEYSTORE_LOAD);
          }
        }
      }
      localWitnesses.setPrivateKeys(privateKeys);
      witnessAddressCheck(config);
      localWitnesses.initWitnessAccountAddress(PARAMETER.isECKeyCryptoEngine());
      logger.debug("Got privateKey from keystore");
    }

    if (PARAMETER.isWitness()
        && CollectionUtils.isEmpty(localWitnesses.getPrivateKeys())) {
      throw new TronError("This is a witness node, but localWitnesses is null",
          TronError.ErrCode.WITNESS_INIT);
    }

    if (config.hasPath(Constant.VM_SUPPORT_CONSTANT)) {
      PARAMETER.supportConstant = config.getBoolean(Constant.VM_SUPPORT_CONSTANT);
    }

    if (config.hasPath(Constant.VM_MAX_ENERGY_LIMIT_FOR_CONSTANT)) {
      long configLimit = config.getLong(Constant.VM_MAX_ENERGY_LIMIT_FOR_CONSTANT);
      PARAMETER.maxEnergyLimitForConstant = max(3_000_000L, configLimit, true);
    }

    if (config.hasPath(Constant.VM_LRU_CACHE_SIZE)) {
      PARAMETER.lruCacheSize = config.getInt(Constant.VM_LRU_CACHE_SIZE);
    }

    if (config.hasPath(Constant.NODE_RPC_ENABLE)) {
      PARAMETER.rpcEnable = config.getBoolean(Constant.NODE_RPC_ENABLE);
    }

    if (config.hasPath(Constant.NODE_RPC_SOLIDITY_ENABLE)) {
      PARAMETER.rpcSolidityEnable = config.getBoolean(Constant.NODE_RPC_SOLIDITY_ENABLE);
    }

    if (config.hasPath(Constant.NODE_RPC_PBFT_ENABLE)) {
      PARAMETER.rpcPBFTEnable = config.getBoolean(Constant.NODE_RPC_PBFT_ENABLE);
    }

    if (config.hasPath(Constant.NODE_HTTP_FULLNODE_ENABLE)) {
      PARAMETER.fullNodeHttpEnable = config.getBoolean(Constant.NODE_HTTP_FULLNODE_ENABLE);
    }

    if (config.hasPath(Constant.NODE_HTTP_SOLIDITY_ENABLE)) {
      PARAMETER.solidityNodeHttpEnable = config.getBoolean(Constant.NODE_HTTP_SOLIDITY_ENABLE);
    }

    if (config.hasPath(Constant.NODE_HTTP_PBFT_ENABLE)) {
      PARAMETER.pBFTHttpEnable = config.getBoolean(Constant.NODE_HTTP_PBFT_ENABLE);
    }

    if (config.hasPath(Constant.NODE_JSONRPC_HTTP_FULLNODE_ENABLE)) {
      PARAMETER.jsonRpcHttpFullNodeEnable =
          config.getBoolean(Constant.NODE_JSONRPC_HTTP_FULLNODE_ENABLE);
    }

    if (config.hasPath(Constant.NODE_JSONRPC_HTTP_SOLIDITY_ENABLE)) {
      PARAMETER.jsonRpcHttpSolidityNodeEnable =
          config.getBoolean(Constant.NODE_JSONRPC_HTTP_SOLIDITY_ENABLE);
    }

    if (config.hasPath(Constant.NODE_JSONRPC_HTTP_PBFT_ENABLE)) {
      PARAMETER.jsonRpcHttpPBFTNodeEnable =
          config.getBoolean(Constant.NODE_JSONRPC_HTTP_PBFT_ENABLE);
    }

    if (config.hasPath(Constant.VM_MIN_TIME_RATIO)) {
      PARAMETER.minTimeRatio = config.getDouble(Constant.VM_MIN_TIME_RATIO);
    }

    if (config.hasPath(Constant.VM_MAX_TIME_RATIO)) {
      PARAMETER.maxTimeRatio = config.getDouble(Constant.VM_MAX_TIME_RATIO);
    }

    if (config.hasPath(Constant.VM_LONG_RUNNING_TIME)) {
      PARAMETER.longRunningTime = config.getInt(Constant.VM_LONG_RUNNING_TIME);
    }

    PARAMETER.storage = new Storage();

    PARAMETER.storage.setDbEngine(Optional.ofNullable(PARAMETER.storageDbEngine)
        .filter(StringUtils::isNotEmpty)
        .orElse(Storage.getDbEngineFromConfig(config)));

    PARAMETER.storage.setDbSync(Optional.ofNullable(PARAMETER.storageDbSynchronous)
        .filter(StringUtils::isNotEmpty)
        .map(Boolean::valueOf)
        .orElse(Storage.getDbVersionSyncFromConfig(config)));

    PARAMETER.storage.setContractParseSwitch(Optional.ofNullable(PARAMETER.contractParseEnable)
        .filter(StringUtils::isNotEmpty)
        .map(Boolean::valueOf)
        .orElse(Storage.getContractParseSwitchFromConfig(config)));

    PARAMETER.storage.setDbDirectory(Optional.ofNullable(PARAMETER.storageDbDirectory)
        .filter(StringUtils::isNotEmpty)
        .orElse(Storage.getDbDirectoryFromConfig(config)));

    PARAMETER.storage.setIndexDirectory(Optional.ofNullable(PARAMETER.storageIndexDirectory)
        .filter(StringUtils::isNotEmpty)
        .orElse(Storage.getIndexDirectoryFromConfig(config)));

    PARAMETER.storage.setIndexSwitch(Optional.ofNullable(PARAMETER.storageIndexSwitch)
        .filter(StringUtils::isNotEmpty)
        .orElse(Storage.getIndexSwitchFromConfig(config)));

    PARAMETER.storage
        .setTransactionHistorySwitch(
            Optional.ofNullable(PARAMETER.storageTransactionHistorySwitch)
                .filter(StringUtils::isNotEmpty)
                .orElse(Storage.getTransactionHistorySwitchFromConfig(config)));

    PARAMETER.storage
        .setCheckpointVersion(Storage.getCheckpointVersionFromConfig(config));
    PARAMETER.storage
        .setCheckpointSync(Storage.getCheckpointSyncFromConfig(config));

    PARAMETER.storage.setEstimatedBlockTransactions(
        Storage.getEstimatedTransactionsFromConfig(config));
    PARAMETER.storage.setTxCacheInitOptimization(
        Storage.getTxCacheInitOptimizationFromConfig(config));
    PARAMETER.storage.setMaxFlushCount(Storage.getSnapshotMaxFlushCountFromConfig(config));

    PARAMETER.storage.setDefaultDbOptions(config);
    PARAMETER.storage.setPropertyMapFromConfig(config);
    PARAMETER.storage.setCacheStrategies(config);
    PARAMETER.storage.setDbRoots(config);

    PARAMETER.seedNode = new SeedNode();
    PARAMETER.seedNode.setAddressList(loadSeeds(config));

    if (config.hasPath(Constant.GENESIS_BLOCK)) {
      PARAMETER.genesisBlock = new GenesisBlock();

      PARAMETER.genesisBlock.setTimestamp(config.getString(Constant.GENESIS_BLOCK_TIMESTAMP));
      PARAMETER.genesisBlock.setParentHash(config.getString(Constant.GENESIS_BLOCK_PARENTHASH));

      if (config.hasPath(Constant.GENESIS_BLOCK_ASSETS)) {
        PARAMETER.genesisBlock.setAssets(getAccountsFromConfig(config));
        AccountStore.setAccount(config);
      }
      if (config.hasPath(Constant.GENESIS_BLOCK_WITNESSES)) {
        PARAMETER.genesisBlock.setWitnesses(getWitnessesFromConfig(config));
      }
    } else {
      PARAMETER.genesisBlock = GenesisBlock.getDefault();
    }

    PARAMETER.needSyncCheck =
        config.hasPath(Constant.BLOCK_NEED_SYNC_CHECK)
            && config.getBoolean(Constant.BLOCK_NEED_SYNC_CHECK);

    PARAMETER.nodeDiscoveryEnable =
        config.hasPath(Constant.NODE_DISCOVERY_ENABLE)
            && config.getBoolean(Constant.NODE_DISCOVERY_ENABLE);

    PARAMETER.nodeDiscoveryPersist =
        config.hasPath(Constant.NODE_DISCOVERY_PERSIST)
            && config.getBoolean(Constant.NODE_DISCOVERY_PERSIST);

    PARAMETER.nodeEffectiveCheckEnable =
        config.hasPath(Constant.NODE_EFFECTIVE_CHECK_ENABLE)
            && config.getBoolean(Constant.NODE_EFFECTIVE_CHECK_ENABLE);

    PARAMETER.nodeConnectionTimeout =
        config.hasPath(Constant.NODE_CONNECTION_TIMEOUT)
            ? config.getInt(Constant.NODE_CONNECTION_TIMEOUT) * 1000
            : 2000;

    if (!config.hasPath(Constant.NODE_FETCH_BLOCK_TIMEOUT)) {
      PARAMETER.fetchBlockTimeout = 500;
    } else if (config.getInt(Constant.NODE_FETCH_BLOCK_TIMEOUT) > 1000) {
      PARAMETER.fetchBlockTimeout = 1000;
    } else if (config.getInt(Constant.NODE_FETCH_BLOCK_TIMEOUT) < 100) {
      PARAMETER.fetchBlockTimeout = 100;
    } else {
      PARAMETER.fetchBlockTimeout = config.getInt(Constant.NODE_FETCH_BLOCK_TIMEOUT);
    }

    PARAMETER.nodeChannelReadTimeout =
        config.hasPath(Constant.NODE_CHANNEL_READ_TIMEOUT)
            ? config.getInt(Constant.NODE_CHANNEL_READ_TIMEOUT)
            : 0;

    if (config.hasPath(Constant.NODE_MAX_ACTIVE_NODES)) {
      PARAMETER.maxConnections = config.getInt(Constant.NODE_MAX_ACTIVE_NODES);
    } else {
      PARAMETER.maxConnections =
              config.hasPath(Constant.NODE_MAX_CONNECTIONS)
                      ? config.getInt(Constant.NODE_MAX_CONNECTIONS) : 30;
    }

    if (config.hasPath(Constant.NODE_MAX_ACTIVE_NODES)
            && config.hasPath(Constant.NODE_CONNECT_FACTOR)) {
      PARAMETER.minConnections = (int) (PARAMETER.maxConnections
              * config.getDouble(Constant.NODE_CONNECT_FACTOR));
    } else {
      PARAMETER.minConnections =
              config.hasPath(Constant.NODE_MIN_CONNECTIONS)
                      ? config.getInt(Constant.NODE_MIN_CONNECTIONS) : 8;
    }

    if (config.hasPath(Constant.NODE_MAX_ACTIVE_NODES)
            && config.hasPath(Constant.NODE_ACTIVE_CONNECT_FACTOR)) {
      PARAMETER.minActiveConnections = (int) (PARAMETER.maxConnections
              * config.getDouble(Constant.NODE_ACTIVE_CONNECT_FACTOR));
    } else {
      PARAMETER.minActiveConnections =
              config.hasPath(Constant.NODE_MIN_ACTIVE_CONNECTIONS)
                      ? config.getInt(Constant.NODE_MIN_ACTIVE_CONNECTIONS) : 3;
    }

    if (config.hasPath(Constant.NODE_MAX_ACTIVE_NODES_WITH_SAME_IP)) {
      PARAMETER.maxConnectionsWithSameIp =
              config.getInt(Constant.NODE_MAX_ACTIVE_NODES_WITH_SAME_IP);
    } else {
      PARAMETER.maxConnectionsWithSameIp =
              config.hasPath(Constant.NODE_MAX_CONNECTIONS_WITH_SAME_IP) ? config
                      .getInt(Constant.NODE_MAX_CONNECTIONS_WITH_SAME_IP) : 2;
    }

    PARAMETER.maxTps = config.hasPath(Constant.NODE_MAX_TPS)
            ? config.getInt(Constant.NODE_MAX_TPS) : 1000;

    PARAMETER.minParticipationRate =
        config.hasPath(Constant.NODE_MIN_PARTICIPATION_RATE)
            ? config.getInt(Constant.NODE_MIN_PARTICIPATION_RATE)
            : 0;

    PARAMETER.p2pConfig = new P2pConfig();
    PARAMETER.nodeListenPort =
        config.hasPath(Constant.NODE_LISTEN_PORT)
            ? config.getInt(Constant.NODE_LISTEN_PORT) : 0;

    PARAMETER.nodeLanIp = PARAMETER.p2pConfig.getLanIp();
    externalIp(config);

    PARAMETER.nodeP2pVersion =
        config.hasPath(Constant.NODE_P2P_VERSION)
            ? config.getInt(Constant.NODE_P2P_VERSION) : 0;

    PARAMETER.nodeEnableIpv6 =
        config.hasPath(Constant.NODE_ENABLE_IPV6) && config.getBoolean(Constant.NODE_ENABLE_IPV6);

    PARAMETER.dnsTreeUrls = config.hasPath(Constant.NODE_DNS_TREE_URLS) ? config.getStringList(
        Constant.NODE_DNS_TREE_URLS) : new ArrayList<>();

    PARAMETER.dnsPublishConfig = loadDnsPublishConfig(config);

    PARAMETER.syncFetchBatchNum = config.hasPath(Constant.NODE_SYNC_FETCH_BATCH_NUM) ? config
        .getInt(Constant.NODE_SYNC_FETCH_BATCH_NUM) : 2000;
    if (PARAMETER.syncFetchBatchNum > 2000) {
      PARAMETER.syncFetchBatchNum = 2000;
    }
    if (PARAMETER.syncFetchBatchNum < 100) {
      PARAMETER.syncFetchBatchNum = 100;
    }

    PARAMETER.rpcPort =
        config.hasPath(Constant.NODE_RPC_PORT)
            ? config.getInt(Constant.NODE_RPC_PORT) : 50051;

    PARAMETER.rpcOnSolidityPort =
        config.hasPath(Constant.NODE_RPC_SOLIDITY_PORT)
            ? config.getInt(Constant.NODE_RPC_SOLIDITY_PORT) : 50061;

    PARAMETER.rpcOnPBFTPort =
        config.hasPath(Constant.NODE_RPC_PBFT_PORT)
            ? config.getInt(Constant.NODE_RPC_PBFT_PORT) : 50071;

    PARAMETER.fullNodeHttpPort =
        config.hasPath(Constant.NODE_HTTP_FULLNODE_PORT)
            ? config.getInt(Constant.NODE_HTTP_FULLNODE_PORT) : 8090;

    PARAMETER.solidityHttpPort =
        config.hasPath(Constant.NODE_HTTP_SOLIDITY_PORT)
            ? config.getInt(Constant.NODE_HTTP_SOLIDITY_PORT) : 8091;

    PARAMETER.pBFTHttpPort =
        config.hasPath(Constant.NODE_HTTP_PBFT_PORT)
            ? config.getInt(Constant.NODE_HTTP_PBFT_PORT) : 8092;

    PARAMETER.jsonRpcHttpFullNodePort =
        config.hasPath(Constant.NODE_JSONRPC_HTTP_FULLNODE_PORT)
            ? config.getInt(Constant.NODE_JSONRPC_HTTP_FULLNODE_PORT) : 8545;

    PARAMETER.jsonRpcHttpSolidityPort =
        config.hasPath(Constant.NODE_JSONRPC_HTTP_SOLIDITY_PORT)
            ? config.getInt(Constant.NODE_JSONRPC_HTTP_SOLIDITY_PORT) : 8555;

    PARAMETER.jsonRpcHttpPBFTPort =
        config.hasPath(Constant.NODE_JSONRPC_HTTP_PBFT_PORT)
            ? config.getInt(Constant.NODE_JSONRPC_HTTP_PBFT_PORT) : 8565;

    PARAMETER.rpcThreadNum =
        config.hasPath(Constant.NODE_RPC_THREAD) ? config.getInt(Constant.NODE_RPC_THREAD)
            : (Runtime.getRuntime().availableProcessors() + 1) / 2;

    PARAMETER.solidityThreads =
        config.hasPath(Constant.NODE_SOLIDITY_THREADS)
            ? config.getInt(Constant.NODE_SOLIDITY_THREADS)
            : Runtime.getRuntime().availableProcessors();

    PARAMETER.maxConcurrentCallsPerConnection =
        config.hasPath(Constant.NODE_RPC_MAX_CONCURRENT_CALLS_PER_CONNECTION)
            ? config.getInt(Constant.NODE_RPC_MAX_CONCURRENT_CALLS_PER_CONNECTION)
            : Integer.MAX_VALUE;

    PARAMETER.flowControlWindow = config.hasPath(Constant.NODE_RPC_FLOW_CONTROL_WINDOW)
        ? config.getInt(Constant.NODE_RPC_FLOW_CONTROL_WINDOW)
        : NettyServerBuilder.DEFAULT_FLOW_CONTROL_WINDOW;

    PARAMETER.maxConnectionIdleInMillis =
        config.hasPath(Constant.NODE_RPC_MAX_CONNECTION_IDLE_IN_MILLIS)
            ? config.getLong(Constant.NODE_RPC_MAX_CONNECTION_IDLE_IN_MILLIS)
            : Long.MAX_VALUE;

    PARAMETER.blockProducedTimeOut = config.hasPath(Constant.NODE_PRODUCED_TIMEOUT)
        ? config.getInt(Constant.NODE_PRODUCED_TIMEOUT) : BLOCK_PRODUCE_TIMEOUT_PERCENT;

    PARAMETER.maxHttpConnectNumber = config.hasPath(Constant.NODE_MAX_HTTP_CONNECT_NUMBER)
        ? config.getInt(Constant.NODE_MAX_HTTP_CONNECT_NUMBER)
        : NodeConstant.MAX_HTTP_CONNECT_NUMBER;

    if (PARAMETER.blockProducedTimeOut < 30) {
      PARAMETER.blockProducedTimeOut = 30;
    }
    if (PARAMETER.blockProducedTimeOut > 100) {
      PARAMETER.blockProducedTimeOut = 100;
    }

    PARAMETER.netMaxTrxPerSecond = config.hasPath(Constant.NODE_NET_MAX_TRX_PER_SECOND)
        ? config.getInt(Constant.NODE_NET_MAX_TRX_PER_SECOND)
        : NetConstants.NET_MAX_TRX_PER_SECOND;

    PARAMETER.maxConnectionAgeInMillis =
        config.hasPath(Constant.NODE_RPC_MAX_CONNECTION_AGE_IN_MILLIS)
            ? config.getLong(Constant.NODE_RPC_MAX_CONNECTION_AGE_IN_MILLIS)
            : Long.MAX_VALUE;

    PARAMETER.maxMessageSize = config.hasPath(Constant.NODE_RPC_MAX_MESSAGE_SIZE)
        ? config.getInt(Constant.NODE_RPC_MAX_MESSAGE_SIZE) : GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

    PARAMETER.maxHeaderListSize = config.hasPath(Constant.NODE_RPC_MAX_HEADER_LIST_SIZE)
        ? config.getInt(Constant.NODE_RPC_MAX_HEADER_LIST_SIZE)
        : GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE;

    PARAMETER.isRpcReflectionServiceEnable =
        config.hasPath(Constant.NODE_RPC_REFLECTION_SERVICE)
            && config.getBoolean(Constant.NODE_RPC_REFLECTION_SERVICE);

    PARAMETER.maintenanceTimeInterval =
        config.hasPath(Constant.BLOCK_MAINTENANCE_TIME_INTERVAL) ? config
            .getInt(Constant.BLOCK_MAINTENANCE_TIME_INTERVAL) : 21600000L;

    PARAMETER.proposalExpireTime =
        config.hasPath(Constant.BLOCK_PROPOSAL_EXPIRE_TIME) ? config
            .getInt(Constant.BLOCK_PROPOSAL_EXPIRE_TIME) : 259200000L;

    PARAMETER.checkFrozenTime =
        config.hasPath(Constant.BLOCK_CHECK_FROZEN_TIME) ? config
            .getInt(Constant.BLOCK_CHECK_FROZEN_TIME) : 1;

    PARAMETER.allowCreationOfContracts =
        config.hasPath(Constant.COMMITTEE_ALLOW_CREATION_OF_CONTRACTS) ? config
            .getInt(Constant.COMMITTEE_ALLOW_CREATION_OF_CONTRACTS) : 0;

    PARAMETER.allowMultiSign =
        config.hasPath(Constant.COMMITTEE_ALLOW_MULTI_SIGN) ? config
            .getInt(Constant.COMMITTEE_ALLOW_MULTI_SIGN) : 0;

    PARAMETER.allowAdaptiveEnergy =
        config.hasPath(Constant.COMMITTEE_ALLOW_ADAPTIVE_ENERGY) ? config
            .getInt(Constant.COMMITTEE_ALLOW_ADAPTIVE_ENERGY) : 0;

    PARAMETER.allowDelegateResource =
        config.hasPath(Constant.COMMITTEE_ALLOW_DELEGATE_RESOURCE) ? config
            .getInt(Constant.COMMITTEE_ALLOW_DELEGATE_RESOURCE) : 0;

    PARAMETER.allowSameTokenName =
        config.hasPath(Constant.COMMITTEE_ALLOW_SAME_TOKEN_NAME) ? config
            .getInt(Constant.COMMITTEE_ALLOW_SAME_TOKEN_NAME) : 0;

    PARAMETER.allowTvmTransferTrc10 =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_TRANSFER_TRC10) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_TRANSFER_TRC10) : 0;

    PARAMETER.allowTvmConstantinople =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_CONSTANTINOPLE) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_CONSTANTINOPLE) : 0;

    PARAMETER.allowTvmSolidity059 =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_SOLIDITY059) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_SOLIDITY059) : 0;

    PARAMETER.forbidTransferToContract =
        config.hasPath(Constant.COMMITTEE_FORBID_TRANSFER_TO_CONTRACT) ? config
            .getInt(Constant.COMMITTEE_FORBID_TRANSFER_TO_CONTRACT) : 0;

    PARAMETER.tcpNettyWorkThreadNum = config.hasPath(Constant.NODE_TCP_NETTY_WORK_THREAD_NUM)
        ? config.getInt(Constant.NODE_TCP_NETTY_WORK_THREAD_NUM) : 0;

    PARAMETER.udpNettyWorkThreadNum = config.hasPath(Constant.NODE_UDP_NETTY_WORK_THREAD_NUM)
        ? config.getInt(Constant.NODE_UDP_NETTY_WORK_THREAD_NUM) : 1;

    if (StringUtils.isEmpty(PARAMETER.trustNodeAddr)) {
      PARAMETER.trustNodeAddr =
          config.hasPath(Constant.NODE_TRUST_NODE)
              ? config.getString(Constant.NODE_TRUST_NODE) : null;
    }

    PARAMETER.validateSignThreadNum =
        config.hasPath(Constant.NODE_VALIDATE_SIGN_THREAD_NUM) ? config
            .getInt(Constant.NODE_VALIDATE_SIGN_THREAD_NUM)
            : Runtime.getRuntime().availableProcessors();

    PARAMETER.walletExtensionApi =
        config.hasPath(Constant.NODE_WALLET_EXTENSION_API)
            && config.getBoolean(Constant.NODE_WALLET_EXTENSION_API);
    PARAMETER.estimateEnergy =
        config.hasPath(Constant.VM_ESTIMATE_ENERGY)
            && config.getBoolean(Constant.VM_ESTIMATE_ENERGY);
    PARAMETER.estimateEnergyMaxRetry = config.hasPath(Constant.VM_ESTIMATE_ENERGY_MAX_RETRY)
        ? config.getInt(Constant.VM_ESTIMATE_ENERGY_MAX_RETRY) : 3;
    if (PARAMETER.estimateEnergyMaxRetry < 0) {
      PARAMETER.estimateEnergyMaxRetry = 0;
    }
    if (PARAMETER.estimateEnergyMaxRetry > 10) {
      PARAMETER.estimateEnergyMaxRetry = 10;
    }

    PARAMETER.receiveTcpMinDataLength = config.hasPath(Constant.NODE_RECEIVE_TCP_MIN_DATA_LENGTH)
        ? config.getLong(Constant.NODE_RECEIVE_TCP_MIN_DATA_LENGTH) : 2048;

    PARAMETER.isOpenFullTcpDisconnect = config.hasPath(Constant.NODE_IS_OPEN_FULL_TCP_DISCONNECT)
        && config.getBoolean(Constant.NODE_IS_OPEN_FULL_TCP_DISCONNECT);

    PARAMETER.nodeDetectEnable = config.hasPath(Constant.NODE_DETECT_ENABLE)
          && config.getBoolean(Constant.NODE_DETECT_ENABLE);

    PARAMETER.inactiveThreshold = config.hasPath(Constant.NODE_INACTIVE_THRESHOLD)
        ? config.getInt(Constant.NODE_INACTIVE_THRESHOLD) : 600;
    if (PARAMETER.inactiveThreshold < 1) {
      PARAMETER.inactiveThreshold = 1;
    }

    PARAMETER.maxTransactionPendingSize = config.hasPath(Constant.NODE_MAX_TRANSACTION_PENDING_SIZE)
        ? config.getInt(Constant.NODE_MAX_TRANSACTION_PENDING_SIZE) : 2000;

    PARAMETER.pendingTransactionTimeout = config.hasPath(Constant.NODE_PENDING_TRANSACTION_TIMEOUT)
        ? config.getLong(Constant.NODE_PENDING_TRANSACTION_TIMEOUT) : 60_000;

    PARAMETER.needToUpdateAsset =
        !config.hasPath(Constant.STORAGE_NEEDTO_UPDATE_ASSET) || config
            .getBoolean(Constant.STORAGE_NEEDTO_UPDATE_ASSET);
    PARAMETER.trxReferenceBlock = config.hasPath(Constant.TRX_REFERENCE_BLOCK)
        ? config.getString(Constant.TRX_REFERENCE_BLOCK) : "solid";

    PARAMETER.trxExpirationTimeInMilliseconds =
        config.hasPath(Constant.TRX_EXPIRATION_TIME_IN_MILLIS_SECONDS)
            && config.getLong(Constant.TRX_EXPIRATION_TIME_IN_MILLIS_SECONDS) > 0
            ? config.getLong(Constant.TRX_EXPIRATION_TIME_IN_MILLIS_SECONDS)
            : Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME;

    PARAMETER.minEffectiveConnection = config.hasPath(Constant.NODE_RPC_MIN_EFFECTIVE_CONNECTION)
        ? config.getInt(Constant.NODE_RPC_MIN_EFFECTIVE_CONNECTION) : 1;

    PARAMETER.trxCacheEnable = config.hasPath(Constant.NODE_RPC_TRX_CACHE_ENABLE)
        && config.getBoolean(Constant.NODE_RPC_TRX_CACHE_ENABLE);

    PARAMETER.blockNumForEnergyLimit = config.hasPath(Constant.ENERGY_LIMIT_BLOCK_NUM)
        ? config.getInt(Constant.ENERGY_LIMIT_BLOCK_NUM) : 4727890L;

    PARAMETER.vmTrace =
        config.hasPath(Constant.VM_TRACE) && config.getBoolean(Constant.VM_TRACE);

    if (!PARAMETER.saveInternalTx
        && config.hasPath(Constant.VM_SAVE_INTERNAL_TX)) {
      PARAMETER.saveInternalTx = config.getBoolean(Constant.VM_SAVE_INTERNAL_TX);
    }

    if (!PARAMETER.saveFeaturedInternalTx
        && config.hasPath(Constant.VM_SAVE_FEATURED_INTERNAL_TX)) {
      PARAMETER.saveFeaturedInternalTx = config.getBoolean(Constant.VM_SAVE_FEATURED_INTERNAL_TX);
    }

    if (!PARAMETER.saveCancelAllUnfreezeV2Details
        && config.hasPath(Constant.VM_SAVE_CANCEL_ALL_UNFREEZE_V2_DETAILS)) {
      PARAMETER.saveCancelAllUnfreezeV2Details =
          config.getBoolean(Constant.VM_SAVE_CANCEL_ALL_UNFREEZE_V2_DETAILS);
    }

    if (PARAMETER.saveCancelAllUnfreezeV2Details
        && (!PARAMETER.saveInternalTx || !PARAMETER.saveFeaturedInternalTx)) {
      logger.warn("Configuring [vm.saveCancelAllUnfreezeV2Details] won't work as "
          + "vm.saveInternalTx or vm.saveFeaturedInternalTx is off.");
    }

    // PARAMETER.allowShieldedTransaction =
    //     config.hasPath(Constant.COMMITTEE_ALLOW_SHIELDED_TRANSACTION) ? config
    //         .getInt(Constant.COMMITTEE_ALLOW_SHIELDED_TRANSACTION) : 0;

    PARAMETER.allowShieldedTRC20Transaction =
        config.hasPath(Constant.COMMITTEE_ALLOW_SHIELDED_TRC20_TRANSACTION) ? config
            .getInt(Constant.COMMITTEE_ALLOW_SHIELDED_TRC20_TRANSACTION) : 0;

    PARAMETER.allowMarketTransaction =
        config.hasPath(Constant.COMMITTEE_ALLOW_MARKET_TRANSACTION) ? config
            .getInt(Constant.COMMITTEE_ALLOW_MARKET_TRANSACTION) : 0;

    PARAMETER.allowTransactionFeePool =
        config.hasPath(Constant.COMMITTEE_ALLOW_TRANSACTION_FEE_POOL) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TRANSACTION_FEE_POOL) : 0;

    PARAMETER.allowBlackHoleOptimization =
        config.hasPath(Constant.COMMITTEE_ALLOW_BLACK_HOLE_OPTIMIZATION) ? config
            .getInt(Constant.COMMITTEE_ALLOW_BLACK_HOLE_OPTIMIZATION) : 0;

    PARAMETER.allowNewResourceModel =
        config.hasPath(Constant.COMMITTEE_ALLOW_NEW_RESOURCE_MODEL) ? config
            .getInt(Constant.COMMITTEE_ALLOW_NEW_RESOURCE_MODEL) : 0;

    PARAMETER.allowTvmIstanbul =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_ISTANBUL) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_ISTANBUL) : 0;

    PARAMETER.eventPluginConfig =
        config.hasPath(Constant.EVENT_SUBSCRIBE)
            ? getEventPluginConfig(config) : null;

    PARAMETER.eventFilter =
        config.hasPath(Constant.EVENT_SUBSCRIBE_FILTER) ? getEventFilter(config) : null;

    PARAMETER.fullNodeAllowShieldedTransactionArgs =
        !config.hasPath(Constant.NODE_FULLNODE_ALLOW_SHIELDED_TRANSACTION)
            || config.getBoolean(Constant.NODE_FULLNODE_ALLOW_SHIELDED_TRANSACTION);

    PARAMETER.zenTokenId = config.hasPath(Constant.NODE_ZEN_TOKENID)
        ? config.getString(Constant.NODE_ZEN_TOKENID) : "000000";

    PARAMETER.allowProtoFilterNum =
        config.hasPath(Constant.COMMITTEE_ALLOW_PROTO_FILTER_NUM) ? config
            .getInt(Constant.COMMITTEE_ALLOW_PROTO_FILTER_NUM) : 0;

    PARAMETER.allowAccountStateRoot =
        config.hasPath(Constant.COMMITTEE_ALLOW_ACCOUNT_STATE_ROOT) ? config
            .getInt(Constant.COMMITTEE_ALLOW_ACCOUNT_STATE_ROOT) : 0;

    PARAMETER.validContractProtoThreadNum =
        config.hasPath(Constant.NODE_VALID_CONTRACT_PROTO_THREADS) ? config
            .getInt(Constant.NODE_VALID_CONTRACT_PROTO_THREADS)
            : Runtime.getRuntime().availableProcessors();

    PARAMETER.activeNodes = getInetSocketAddress(config, Constant.NODE_ACTIVE, true);

    PARAMETER.passiveNodes = getInetAddress(config, Constant.NODE_PASSIVE);

    PARAMETER.fastForwardNodes = getInetSocketAddress(config, Constant.NODE_FAST_FORWARD, true);

    PARAMETER.maxFastForwardNum = config.hasPath(Constant.NODE_MAX_FAST_FORWARD_NUM) ? config
            .getInt(Constant.NODE_MAX_FAST_FORWARD_NUM) : 4;
    if (PARAMETER.maxFastForwardNum > MAX_ACTIVE_WITNESS_NUM) {
      PARAMETER.maxFastForwardNum = MAX_ACTIVE_WITNESS_NUM;
    }
    if (PARAMETER.maxFastForwardNum < 1) {
      PARAMETER.maxFastForwardNum = 1;
    }

    PARAMETER.shieldedTransInPendingMaxCounts =
        config.hasPath(Constant.NODE_SHIELDED_TRANS_IN_PENDING_MAX_COUNTS) ? config
            .getInt(Constant.NODE_SHIELDED_TRANS_IN_PENDING_MAX_COUNTS) : 10;

    if (PARAMETER.isWitness()) {
      PARAMETER.fullNodeAllowShieldedTransactionArgs = true;
    }

    PARAMETER.rateLimiterGlobalQps =
        config.hasPath(Constant.RATE_LIMITER_GLOBAL_QPS) ? config
            .getInt(Constant.RATE_LIMITER_GLOBAL_QPS) : 50000;

    PARAMETER.rateLimiterGlobalIpQps =
        config.hasPath(Constant.RATE_LIMITER_GLOBAL_IP_QPS) ? config
            .getInt(Constant.RATE_LIMITER_GLOBAL_IP_QPS) : 10000;

    PARAMETER.rateLimiterGlobalApiQps =
      config.hasPath(Constant.RATE_LIMITER_GLOBAL_API_QPS) ? config
        .getInt(Constant.RATE_LIMITER_GLOBAL_API_QPS) : 1000;

    PARAMETER.rateLimiterInitialization = getRateLimiterFromConfig(config);

    PARAMETER.changedDelegation =
        config.hasPath(Constant.COMMITTEE_CHANGED_DELEGATION) ? config
            .getInt(Constant.COMMITTEE_CHANGED_DELEGATION) : 0;

    PARAMETER.allowPBFT =
        config.hasPath(Constant.COMMITTEE_ALLOW_PBFT) ? config
            .getLong(Constant.COMMITTEE_ALLOW_PBFT) : 0;

    PARAMETER.pBFTExpireNum =
        config.hasPath(Constant.COMMITTEE_PBFT_EXPIRE_NUM) ? config
            .getLong(Constant.COMMITTEE_PBFT_EXPIRE_NUM) : 20;

    PARAMETER.agreeNodeCount = config.hasPath(Constant.NODE_AGREE_NODE_COUNT) ? config
        .getInt(Constant.NODE_AGREE_NODE_COUNT) : MAX_ACTIVE_WITNESS_NUM * 2 / 3 + 1;
    PARAMETER.agreeNodeCount = PARAMETER.agreeNodeCount > MAX_ACTIVE_WITNESS_NUM
        ? MAX_ACTIVE_WITNESS_NUM : PARAMETER.agreeNodeCount;
    if (PARAMETER.isWitness()) {
      //  INSTANCE.agreeNodeCount = MAX_ACTIVE_WITNESS_NUM * 2 / 3 + 1;
    }

    PARAMETER.allowTvmFreeze =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_FREEZE) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_FREEZE) : 0;

    PARAMETER.allowTvmVote =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_VOTE) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_VOTE) : 0;

    PARAMETER.allowTvmLondon =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_LONDON) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_LONDON) : 0;

    PARAMETER.allowTvmCompatibleEvm =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_COMPATIBLE_EVM) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_COMPATIBLE_EVM) : 0;

    PARAMETER.allowHigherLimitForMaxCpuTimeOfOneTx =
        config.hasPath(Constant.COMMITTEE_ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX) ? config
            .getInt(Constant.COMMITTEE_ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX) : 0;

    PARAMETER.allowNewRewardAlgorithm =
        config.hasPath(Constant.COMMITTEE_ALLOW_NEW_REWARD_ALGORITHM) ? config
            .getInt(Constant.COMMITTEE_ALLOW_NEW_REWARD_ALGORITHM) : 0;

    PARAMETER.allowOptimizedReturnValueOfChainId =
        config.hasPath(Constant.COMMITTEE_ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID) ? config
            .getInt(Constant.COMMITTEE_ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID) : 0;

    initBackupProperty(config);
    if (Constant.ROCKSDB.equalsIgnoreCase(CommonParameter
        .getInstance().getStorage().getDbEngine())) {
      initRocksDbBackupProperty(config);
      initRocksDbSettings(config);
    }

    PARAMETER.actuatorSet =
        config.hasPath(Constant.ACTUATOR_WHITELIST)
            ? new HashSet<>(config.getStringList(Constant.ACTUATOR_WHITELIST))
            : Collections.emptySet();

    if (config.hasPath(Constant.NODE_METRICS_ENABLE)) {
      PARAMETER.nodeMetricsEnable = config.getBoolean(Constant.NODE_METRICS_ENABLE);
    }

    PARAMETER.metricsStorageEnable = config.hasPath(Constant.METRICS_STORAGE_ENABLE) && config
        .getBoolean(Constant.METRICS_STORAGE_ENABLE);
    PARAMETER.influxDbIp = config.hasPath(Constant.METRICS_INFLUXDB_IP) ? config
        .getString(Constant.METRICS_INFLUXDB_IP) : Constant.LOCAL_HOST;
    PARAMETER.influxDbPort = config.hasPath(Constant.METRICS_INFLUXDB_PORT) ? config
        .getInt(Constant.METRICS_INFLUXDB_PORT) : 8086;
    PARAMETER.influxDbDatabase = config.hasPath(Constant.METRICS_INFLUXDB_DATABASE) ? config
        .getString(Constant.METRICS_INFLUXDB_DATABASE) : "metrics";
    PARAMETER.metricsReportInterval = config.hasPath(Constant.METRICS_REPORT_INTERVAL) ? config
        .getInt(Constant.METRICS_REPORT_INTERVAL) : 10;

    PARAMETER.metricsPrometheusEnable = config.hasPath(Constant.METRICS_PROMETHEUS_ENABLE) && config
        .getBoolean(Constant.METRICS_PROMETHEUS_ENABLE);
    PARAMETER.metricsPrometheusPort = config.hasPath(Constant.METRICS_PROMETHEUS_PORT) ? config
        .getInt(Constant.METRICS_PROMETHEUS_PORT) : 9527;
    PARAMETER.setOpenHistoryQueryWhenLiteFN(
        config.hasPath(Constant.NODE_OPEN_HISTORY_QUERY_WHEN_LITEFN)
            && config.getBoolean(Constant.NODE_OPEN_HISTORY_QUERY_WHEN_LITEFN));

    PARAMETER.historyBalanceLookup = config.hasPath(Constant.HISTORY_BALANCE_LOOKUP) && config
        .getBoolean(Constant.HISTORY_BALANCE_LOOKUP);

    if (config.hasPath(Constant.OPEN_PRINT_LOG)) {
      PARAMETER.openPrintLog = config.getBoolean(Constant.OPEN_PRINT_LOG);
    }

    PARAMETER.openTransactionSort = config.hasPath(Constant.OPEN_TRANSACTION_SORT) && config
        .getBoolean(Constant.OPEN_TRANSACTION_SORT);

    PARAMETER.allowAccountAssetOptimization = config
        .hasPath(Constant.ALLOW_ACCOUNT_ASSET_OPTIMIZATION) ? config
        .getInt(Constant.ALLOW_ACCOUNT_ASSET_OPTIMIZATION) : 0;

    PARAMETER.allowAssetOptimization = config
        .hasPath(Constant.ALLOW_ASSET_OPTIMIZATION) ? config
        .getInt(Constant.ALLOW_ASSET_OPTIMIZATION) : 0;

    PARAMETER.disabledApiList =
        config.hasPath(Constant.NODE_DISABLED_API_LIST)
            ? config.getStringList(Constant.NODE_DISABLED_API_LIST)
            .stream().map(String::toLowerCase).collect(Collectors.toList())
            : Collections.emptyList();

    if (config.hasPath(Constant.NODE_SHUTDOWN_BLOCK_TIME)) {
      try {
        PARAMETER.shutdownBlockTime = new CronExpression(config.getString(
            Constant.NODE_SHUTDOWN_BLOCK_TIME));
      } catch (ParseException e) {
        throw new TronError(e, TronError.ErrCode.AUTO_STOP_PARAMS);
      }
    }

    if (config.hasPath(Constant.NODE_SHUTDOWN_BLOCK_HEIGHT)) {
      PARAMETER.shutdownBlockHeight = config.getLong(Constant.NODE_SHUTDOWN_BLOCK_HEIGHT);
    }

    if (config.hasPath(Constant.NODE_SHUTDOWN_BLOCK_COUNT)) {
      PARAMETER.shutdownBlockCount = config.getLong(Constant.NODE_SHUTDOWN_BLOCK_COUNT);
    }

    if (config.hasPath(Constant.BLOCK_CACHE_TIMEOUT)) {
      PARAMETER.blockCacheTimeout = config.getLong(Constant.BLOCK_CACHE_TIMEOUT);
    }

    if (config.hasPath(Constant.ALLOW_NEW_REWARD)) {
      PARAMETER.allowNewReward = config.getLong(Constant.ALLOW_NEW_REWARD);
      if (PARAMETER.allowNewReward > 1) {
        PARAMETER.allowNewReward = 1;
      }
      if (PARAMETER.allowNewReward < 0) {
        PARAMETER.allowNewReward = 0;
      }
    }

    if (config.hasPath(Constant.MEMO_FEE)) {
      PARAMETER.memoFee = config.getLong(Constant.MEMO_FEE);
      if (PARAMETER.memoFee > 1_000_000_000) {
        PARAMETER.memoFee = 1_000_000_000;
      }
      if (PARAMETER.memoFee < 0) {
        PARAMETER.memoFee = 0;
      }
    }

    if (config.hasPath(Constant.ALLOW_DELEGATE_OPTIMIZATION)) {
      PARAMETER.allowDelegateOptimization = config.getLong(Constant.ALLOW_DELEGATE_OPTIMIZATION);
      PARAMETER.allowDelegateOptimization = min(PARAMETER.allowDelegateOptimization, 1, true);
      PARAMETER.allowDelegateOptimization = max(PARAMETER.allowDelegateOptimization, 0, true);
    }

    if (config.hasPath(Constant.COMMITTEE_UNFREEZE_DELAY_DAYS)) {
      PARAMETER.unfreezeDelayDays = config.getLong(Constant.COMMITTEE_UNFREEZE_DELAY_DAYS);
      if (PARAMETER.unfreezeDelayDays > 365) {
        PARAMETER.unfreezeDelayDays = 365;
      }
      if (PARAMETER.unfreezeDelayDays < 0) {
        PARAMETER.unfreezeDelayDays = 0;
      }
    }

    if (config.hasPath(Constant.ALLOW_DYNAMIC_ENERGY)) {
      PARAMETER.allowDynamicEnergy = config.getLong(Constant.ALLOW_DYNAMIC_ENERGY);
      PARAMETER.allowDynamicEnergy = min(PARAMETER.allowDynamicEnergy, 1, true);
      PARAMETER.allowDynamicEnergy = max(PARAMETER.allowDynamicEnergy, 0, true);
    }

    if (config.hasPath(Constant.DYNAMIC_ENERGY_THRESHOLD)) {
      PARAMETER.dynamicEnergyThreshold = config.getLong(Constant.DYNAMIC_ENERGY_THRESHOLD);
      PARAMETER.dynamicEnergyThreshold
          = min(PARAMETER.dynamicEnergyThreshold, 100_000_000_000_000_000L, true);
      PARAMETER.dynamicEnergyThreshold = max(PARAMETER.dynamicEnergyThreshold, 0, true);
    }

    if (config.hasPath(Constant.DYNAMIC_ENERGY_INCREASE_FACTOR)) {
      PARAMETER.dynamicEnergyIncreaseFactor
          = config.getLong(Constant.DYNAMIC_ENERGY_INCREASE_FACTOR);
      PARAMETER.dynamicEnergyIncreaseFactor =
          min(PARAMETER.dynamicEnergyIncreaseFactor, DYNAMIC_ENERGY_INCREASE_FACTOR_RANGE, true);
      PARAMETER.dynamicEnergyIncreaseFactor = max(PARAMETER.dynamicEnergyIncreaseFactor, 0, true);
    }

    if (config.hasPath(Constant.DYNAMIC_ENERGY_MAX_FACTOR)) {
      PARAMETER.dynamicEnergyMaxFactor
          = config.getLong(Constant.DYNAMIC_ENERGY_MAX_FACTOR);
      PARAMETER.dynamicEnergyMaxFactor =
          min(PARAMETER.dynamicEnergyMaxFactor, DYNAMIC_ENERGY_MAX_FACTOR_RANGE, true);
      PARAMETER.dynamicEnergyMaxFactor = max(PARAMETER.dynamicEnergyMaxFactor, 0, true);
    }

    PARAMETER.dynamicConfigEnable = config.hasPath(Constant.DYNAMIC_CONFIG_ENABLE)
        && config.getBoolean(Constant.DYNAMIC_CONFIG_ENABLE);
    if (config.hasPath(Constant.DYNAMIC_CONFIG_CHECK_INTERVAL)) {
      PARAMETER.dynamicConfigCheckInterval
          = config.getLong(Constant.DYNAMIC_CONFIG_CHECK_INTERVAL);
      if (PARAMETER.dynamicConfigCheckInterval <= 0) {
        PARAMETER.dynamicConfigCheckInterval = 600;
      }
    } else {
      PARAMETER.dynamicConfigCheckInterval = 600;
    }

    PARAMETER.allowTvmShangHai =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_SHANGHAI) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_SHANGHAI) : 0;

    PARAMETER.unsolidifiedBlockCheck =
      config.hasPath(Constant.UNSOLIDIFIED_BLOCK_CHECK)
      && config.getBoolean(Constant.UNSOLIDIFIED_BLOCK_CHECK);

    PARAMETER.maxUnsolidifiedBlocks =
      config.hasPath(Constant.MAX_UNSOLIDIFIED_BLOCKS) ? config
        .getInt(Constant.MAX_UNSOLIDIFIED_BLOCKS) : 54;

    long allowOldRewardOpt = config.hasPath(Constant.COMMITTEE_ALLOW_OLD_REWARD_OPT) ? config
        .getInt(Constant.COMMITTEE_ALLOW_OLD_REWARD_OPT) : 0;
    if (allowOldRewardOpt == 1 && PARAMETER.allowNewRewardAlgorithm != 1
        && PARAMETER.allowNewReward != 1 && PARAMETER.allowTvmVote != 1) {
      throw new IllegalArgumentException(
          "At least one of the following proposals is required to be opened first: "
          + "committee.allowNewRewardAlgorithm = 1"
          + " or committee.allowNewReward = 1"
          + " or committee.allowTvmVote = 1.");
    }
    PARAMETER.allowOldRewardOpt = allowOldRewardOpt;

    PARAMETER.allowEnergyAdjustment =
            config.hasPath(Constant.COMMITTEE_ALLOW_ENERGY_ADJUSTMENT) ? config
                    .getInt(Constant.COMMITTEE_ALLOW_ENERGY_ADJUSTMENT) : 0;

    PARAMETER.allowStrictMath =
        config.hasPath(Constant.COMMITTEE_ALLOW_STRICT_MATH) ? config
            .getInt(Constant.COMMITTEE_ALLOW_STRICT_MATH) : 0;

    PARAMETER.consensusLogicOptimization =
        config.hasPath(Constant.COMMITTEE_CONSENSUS_LOGIC_OPTIMIZATION) ? config
            .getInt(Constant.COMMITTEE_CONSENSUS_LOGIC_OPTIMIZATION) : 0;

    PARAMETER.allowTvmCancun =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_CANCUN) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_CANCUN) : 0;

    PARAMETER.allowTvmBlob =
        config.hasPath(Constant.COMMITTEE_ALLOW_TVM_BLOB) ? config
            .getInt(Constant.COMMITTEE_ALLOW_TVM_BLOB) : 0;

    logConfig();
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
    if (config.hasPath(Constant.RATE_LIMITER_HTTP)) {
      ArrayList<RateLimiterInitialization.HttpRateLimiterItem> list1 = config
              .getObjectList(Constant.RATE_LIMITER_HTTP).stream()
              .map(RateLimiterInitialization::createHttpItem)
              .collect(Collectors.toCollection(ArrayList::new));
      initialization.setHttpMap(list1);
    }
    if (config.hasPath(Constant.RATE_LIMITER_RPC)) {
      ArrayList<RateLimiterInitialization.RpcRateLimiterItem> list2 = config
              .getObjectList(Constant.RATE_LIMITER_RPC).stream()
              .map(RateLimiterInitialization::createRpcItem)
              .collect(Collectors.toCollection(ArrayList::new));
      initialization.setRpcMap(list2);
    }
    return initialization;
  }

  public static List<InetSocketAddress> getInetSocketAddress(
      final com.typesafe.config.Config config, String path, boolean filter) {
    List<InetSocketAddress> ret = new ArrayList<>();
    if (!config.hasPath(path)) {
      return ret;
    }
    List<String> list = config.getStringList(path);
    for (String configString : list) {
      InetSocketAddress inetSocketAddress = NetUtil.parseInetSocketAddress(configString);
      if (filter) {
        String ip = inetSocketAddress.getAddress().getHostAddress();
        int port = inetSocketAddress.getPort();
        if (!(PARAMETER.nodeLanIp.equals(ip)
            || PARAMETER.nodeExternalIp.equals(ip)
            || Constant.LOCAL_HOST.equals(ip))
            || PARAMETER.nodeListenPort != port) {
          ret.add(inetSocketAddress);
        }
      } else {
        ret.add(inetSocketAddress);
      }
    }
    return ret;
  }

  public static List<InetAddress> getInetAddress(
      final com.typesafe.config.Config config, String path) {
    List<InetAddress> ret = new ArrayList<>();
    if (!config.hasPath(path)) {
      return ret;
    }
    List<String> list = config.getStringList(path);
    for (String configString : list) {
      InetSocketAddress inetSocketAddress = NetUtil.parseInetSocketAddress(configString);
      ret.add(inetSocketAddress.getAddress());
    }
    return ret;
  }

  private static EventPluginConfig getEventPluginConfig(
          final com.typesafe.config.Config config) {
    EventPluginConfig eventPluginConfig = new EventPluginConfig();

    if (config.hasPath(Constant.EVENT_SUBSCRIBE_VERSION)) {
      eventPluginConfig.setVersion(config.getInt(Constant.EVENT_SUBSCRIBE_VERSION));
    }

    if (config.hasPath(Constant.EVENT_SUBSCRIBE_START_SYNC_BLOCK_NUM)) {
      eventPluginConfig.setStartSyncBlockNum(config
          .getLong(Constant.EVENT_SUBSCRIBE_START_SYNC_BLOCK_NUM));
    }

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

      if (config.hasPath(Constant.EVENT_SUBSCRIBE_DB_CONFIG)) {
        String dbConfig = config.getString(Constant.EVENT_SUBSCRIBE_DB_CONFIG);
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

  private static List<InetSocketAddress> loadSeeds(final com.typesafe.config.Config config) {
    List<InetSocketAddress> inetSocketAddressList = new ArrayList<>();
    if (PARAMETER.seedNodes != null && !PARAMETER.seedNodes.isEmpty()) {
      for (String s : PARAMETER.seedNodes) {
        InetSocketAddress inetSocketAddress = NetUtil.parseInetSocketAddress(s);
        inetSocketAddressList.add(inetSocketAddress);
      }
    } else {
      inetSocketAddressList = getInetSocketAddress(config, Constant.SEED_NODE_IP_LIST, false);
    }

    return inetSocketAddressList;
  }

  public static PublishConfig loadDnsPublishConfig(final com.typesafe.config.Config config) {
    PublishConfig publishConfig = new PublishConfig();
    if (config.hasPath(Constant.NODE_DNS_PUBLISH)) {
      publishConfig.setDnsPublishEnable(config.getBoolean(Constant.NODE_DNS_PUBLISH));
    }
    loadDnsPublishParameters(config, publishConfig);
    return publishConfig;
  }

  public static void loadDnsPublishParameters(final com.typesafe.config.Config config,
      PublishConfig publishConfig) {
    if (publishConfig.isDnsPublishEnable()) {
      if (config.hasPath(Constant.NODE_DNS_DOMAIN) && StringUtils.isNotEmpty(
          config.getString(Constant.NODE_DNS_DOMAIN))) {
        publishConfig.setDnsDomain(config.getString(Constant.NODE_DNS_DOMAIN));
      } else {
        logEmptyError(Constant.NODE_DNS_DOMAIN);
      }

      if (config.hasPath(Constant.NODE_DNS_CHANGE_THRESHOLD)) {
        double changeThreshold = config.getDouble(Constant.NODE_DNS_CHANGE_THRESHOLD);
        if (changeThreshold > 0) {
          publishConfig.setChangeThreshold(changeThreshold);
        } else {
          logger.error("Check {}, should be bigger than 0, default 0.1",
              Constant.NODE_DNS_CHANGE_THRESHOLD);
        }
      }

      if (config.hasPath(Constant.NODE_DNS_MAX_MERGE_SIZE)) {
        int maxMergeSize = config.getInt(Constant.NODE_DNS_MAX_MERGE_SIZE);
        if (maxMergeSize >= 1 && maxMergeSize <= 5) {
          publishConfig.setMaxMergeSize(maxMergeSize);
        } else {
          logger.error("Check {}, should be [1~5], default 5", Constant.NODE_DNS_MAX_MERGE_SIZE);
        }
      }

      if (config.hasPath(Constant.NODE_DNS_PRIVATE) && StringUtils.isNotEmpty(
          config.getString(Constant.NODE_DNS_PRIVATE))) {
        publishConfig.setDnsPrivate(config.getString(Constant.NODE_DNS_PRIVATE));
      } else {
        logEmptyError(Constant.NODE_DNS_PRIVATE);
      }

      if (config.hasPath(Constant.NODE_DNS_KNOWN_URLS)) {
        publishConfig.setKnownTreeUrls(config.getStringList(Constant.NODE_DNS_KNOWN_URLS));
      }

      if (config.hasPath(Constant.NODE_DNS_STATIC_NODES)) {
        publishConfig.setStaticNodes(
            getInetSocketAddress(config, Constant.NODE_DNS_STATIC_NODES, false));
      }

      if (config.hasPath(Constant.NODE_DNS_SERVER_TYPE) && StringUtils.isNotEmpty(
          config.getString(Constant.NODE_DNS_SERVER_TYPE))) {
        String serverType = config.getString(Constant.NODE_DNS_SERVER_TYPE);
        if (!"aws".equalsIgnoreCase(serverType) && !"aliyun".equalsIgnoreCase(serverType)) {
          throw new IllegalArgumentException(
              String.format("Check %s, must be aws or aliyun", Constant.NODE_DNS_SERVER_TYPE));
        }
        if ("aws".equalsIgnoreCase(serverType)) {
          publishConfig.setDnsType(DnsType.AwsRoute53);
        } else {
          publishConfig.setDnsType(DnsType.AliYun);
        }
      } else {
        logEmptyError(Constant.NODE_DNS_SERVER_TYPE);
      }

      if (config.hasPath(Constant.NODE_DNS_ACCESS_KEY_ID) && StringUtils.isNotEmpty(
          config.getString(Constant.NODE_DNS_ACCESS_KEY_ID))) {
        publishConfig.setAccessKeyId(config.getString(Constant.NODE_DNS_ACCESS_KEY_ID));
      } else {
        logEmptyError(Constant.NODE_DNS_ACCESS_KEY_ID);
      }
      if (config.hasPath(Constant.NODE_DNS_ACCESS_KEY_SECRET) && StringUtils.isNotEmpty(
          config.getString(Constant.NODE_DNS_ACCESS_KEY_SECRET))) {
        publishConfig.setAccessKeySecret(config.getString(Constant.NODE_DNS_ACCESS_KEY_SECRET));
      } else {
        logEmptyError(Constant.NODE_DNS_ACCESS_KEY_SECRET);
      }

      if (publishConfig.getDnsType() == DnsType.AwsRoute53) {
        if (config.hasPath(Constant.NODE_DNS_AWS_REGION) && StringUtils.isNotEmpty(
            config.getString(Constant.NODE_DNS_AWS_REGION))) {
          publishConfig.setAwsRegion(config.getString(Constant.NODE_DNS_AWS_REGION));
        } else {
          logEmptyError(Constant.NODE_DNS_AWS_REGION);
        }
        if (config.hasPath(Constant.NODE_DNS_AWS_HOST_ZONE_ID)) {
          publishConfig.setAwsHostZoneId(config.getString(Constant.NODE_DNS_AWS_HOST_ZONE_ID));
        }
      } else {
        if (config.hasPath(Constant.NODE_DNS_ALIYUN_ENDPOINT) && StringUtils.isNotEmpty(
            config.getString(Constant.NODE_DNS_ALIYUN_ENDPOINT))) {
          publishConfig.setAliDnsEndpoint(config.getString(Constant.NODE_DNS_ALIYUN_ENDPOINT));
        } else {
          logEmptyError(Constant.NODE_DNS_ALIYUN_ENDPOINT);
        }
      }
    }
  }

  private static void logEmptyError(String arg) {
    throw new IllegalArgumentException(String.format("Check %s, must not be null or empty", arg));
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

    if (triggerObject.containsKey("redundancy")) {
      String redundancy = triggerObject.get("redundancy").unwrapped().toString();
      triggerConfig.setRedundancy("true".equalsIgnoreCase(redundancy));
    }

    if (triggerObject.containsKey("ethCompatible")) {
      String ethCompatible = triggerObject.get("ethCompatible").unwrapped().toString();
      triggerConfig.setEthCompatible("true".equalsIgnoreCase(ethCompatible));
    }

    if (triggerObject.containsKey("solidified")) {
      String solidified = triggerObject.get("solidified").unwrapped().toString();
      triggerConfig.setSolidified("true".equalsIgnoreCase(solidified));
    }

    return triggerConfig;
  }

  private static FilterQuery getEventFilter(final com.typesafe.config.Config config) {
    FilterQuery filter = new FilterQuery();
    long fromBlockLong = 0;
    long toBlockLong = 0;

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

  private static void externalIp(final com.typesafe.config.Config config) {
    if (!config.hasPath(Constant.NODE_DISCOVERY_EXTERNAL_IP) || config
        .getString(Constant.NODE_DISCOVERY_EXTERNAL_IP).trim().isEmpty()) {
      if (PARAMETER.nodeExternalIp == null) {
        logger.info("External IP wasn't set, using ipv4 from libp2p");
        PARAMETER.nodeExternalIp = PARAMETER.p2pConfig.getIp();
        if (StringUtils.isEmpty(PARAMETER.nodeExternalIp)) {
          PARAMETER.nodeExternalIp = PARAMETER.nodeLanIp;
        }
      }
    } else {
      PARAMETER.nodeExternalIp = config.getString(Constant.NODE_DISCOVERY_EXTERNAL_IP).trim();
    }
  }

  private static void initRocksDbSettings(Config config) {
    String prefix = Constant.STORAGE_DB_SETTING;
    int levelNumber = config.hasPath(prefix + "levelNumber")
        ? config.getInt(prefix + "levelNumber") : 7;
    int compactThreads = config.hasPath(prefix + "compactThreads")
        ? config.getInt(prefix + "compactThreads")
        : max(Runtime.getRuntime().availableProcessors(), 1, true);
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
    int maxOpenFiles = config.hasPath(prefix + "maxOpenFiles")
        ? config.getInt(prefix + "maxOpenFiles") : 5000;

    PARAMETER.rocksDBCustomSettings = RocksDbSettings
        .initCustomSettings(levelNumber, compactThreads, blocksize, maxBytesForLevelBase,
            maxBytesForLevelMultiplier, level0FileNumCompactionTrigger,
            targetFileSizeBase, targetFileSizeMultiplier, maxOpenFiles);
    RocksDbSettings.loggingSettings();
  }

  private static void initRocksDbBackupProperty(Config config) {
    boolean enable =
        config.hasPath(Constant.STORAGE_BACKUP_ENABLE)
            && config.getBoolean(Constant.STORAGE_BACKUP_ENABLE);
    String propPath = config.hasPath(Constant.STORAGE_BACKUP_PROP_PATH)
        ? config.getString(Constant.STORAGE_BACKUP_PROP_PATH) : "prop.properties";
    String bak1path = config.hasPath(Constant.STORAGE_BACKUP_BAK1PATH)
        ? config.getString(Constant.STORAGE_BACKUP_BAK1PATH) : "bak1/database/";
    String bak2path = config.hasPath(Constant.STORAGE_BACKUP_BAK2PATH)
        ? config.getString(Constant.STORAGE_BACKUP_BAK2PATH) : "bak2/database/";
    int frequency = config.hasPath(Constant.STORAGE_BACKUP_FREQUENCY)
        ? config.getInt(Constant.STORAGE_BACKUP_FREQUENCY) : 10000;
    PARAMETER.dbBackupConfig = DbBackupConfig.getInstance()
        .initArgs(enable, propPath, bak1path, bak2path, frequency);
  }

  private static void initBackupProperty(Config config) {
    PARAMETER.backupPriority = config.hasPath(Constant.NODE_BACKUP_PRIORITY)
        ? config.getInt(Constant.NODE_BACKUP_PRIORITY) : 0;

    PARAMETER.backupPort = config.hasPath(Constant.NODE_BACKUP_PORT)
        ? config.getInt(Constant.NODE_BACKUP_PORT) : 10001;

    PARAMETER.keepAliveInterval = config.hasPath(Constant.NODE_BACKUP_KEEPALIVEINTERVAL)
        ? config.getInt(Constant.NODE_BACKUP_KEEPALIVEINTERVAL) : 3000;

    PARAMETER.backupMembers = config.hasPath(Constant.NODE_BACKUP_MEMBERS)
        ? config.getStringList(Constant.NODE_BACKUP_MEMBERS) : new ArrayList<>();
  }

  public static void logConfig() {
    CommonParameter parameter = CommonParameter.getInstance();
    logger.info("\n");
    logger.info("************************ System info ************************");
    logger.info("{}", Arch.withAll());
    logger.info("************************ Net config ************************");
    logger.info("P2P version: {}", parameter.getNodeP2pVersion());
    logger.info("LAN IP: {}", parameter.getNodeLanIp());
    logger.info("External IP: {}", parameter.getNodeExternalIp());
    logger.info("Listen port: {}", parameter.getNodeListenPort());
    logger.info("Node ipv6 enable: {}", parameter.isNodeEnableIpv6());
    logger.info("Discover enable: {}", parameter.isNodeDiscoveryEnable());
    logger.info("Active node size: {}", parameter.getActiveNodes().size());
    logger.info("Passive node size: {}", parameter.getPassiveNodes().size());
    logger.info("FastForward node size: {}", parameter.getFastForwardNodes().size());
    logger.info("FastForward node number: {}", parameter.getMaxFastForwardNum());
    logger.info("Seed node size: {}", parameter.getSeedNode().getAddressList().size());
    logger.info("Max connection: {}", parameter.getMaxConnections());
    logger.info("Min connection: {}", parameter.getMinConnections());
    logger.info("Min active connection: {}", parameter.getMinActiveConnections());
    logger.info("Max connection with same IP: {}", parameter.getMaxConnectionsWithSameIp());
    logger.info("Solidity threads: {}", parameter.getSolidityThreads());
    logger.info("Trx reference block: {}", parameter.getTrxReferenceBlock());
    logger.info("Open full tcp disconnect: {}", parameter.isOpenFullTcpDisconnect());
    logger.info("Node detect enable: {}", parameter.isNodeDetectEnable());
    logger.info("Node effective check enable: {}", parameter.isNodeEffectiveCheckEnable());
    logger.info("Rate limiter global qps: {}", parameter.getRateLimiterGlobalQps());
    logger.info("Rate limiter global ip qps: {}", parameter.getRateLimiterGlobalIpQps());
    logger.info("Rate limiter global api qps: {}", parameter.getRateLimiterGlobalApiQps());
    logger.info("************************ Backup config ************************");
    logger.info("Backup priority: {}", parameter.getBackupPriority());
    logger.info("Backup listen port: {}", parameter.getBackupPort());
    logger.info("Backup listen keepAliveInterval: {}", parameter.getKeepAliveInterval());
    logger.info("Backup member size: {}", parameter.getBackupMembers().size());
    logger.info("************************ Code version *************************");
    logger.info("Code version : {}", Version.getVersion());
    logger.info("Version code: {}", Version.VERSION_CODE);
    logger.info("************************ DB config *************************");
    logger.info("DB engine : {}", parameter.getStorage().getDbEngine());
    logger.info("Snapshot max flush count: {}", parameter.getStorage().getMaxFlushCount());
    logger.info("***************************************************************");
    logger.info("************************ shutDown config *************************");
    logger.info("ShutDown blockTime  : {}", parameter.getShutdownBlockTime());
    logger.info("ShutDown blockHeight : {}", parameter.getShutdownBlockHeight());
    logger.info("ShutDown blockCount : {}", parameter.getShutdownBlockCount());
    logger.info("***************************************************************");
    logger.info("\n");
  }

  public static void setFullNodeAllowShieldedTransaction(boolean fullNodeAllowShieldedTransaction) {
    PARAMETER.fullNodeAllowShieldedTransactionArgs = fullNodeAllowShieldedTransaction;
  }

  private static void witnessAddressCheck(Config config) {
    if (config.hasPath(Constant.LOCAL_WITNESS_ACCOUNT_ADDRESS)) {
      byte[] bytes = Commons
          .decodeFromBase58Check(config.getString(Constant.LOCAL_WITNESS_ACCOUNT_ADDRESS));
      if (bytes != null) {
        localWitnesses.setWitnessAccountAddress(bytes);
        logger.debug("Got localWitnessAccountAddress from config.conf");
      } else {
        logger.warn(IGNORE_WRONG_WITNESS_ADDRESS_FORMAT);
      }
    }
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
}

