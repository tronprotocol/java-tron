package org.tron.core.config.args;

import static java.lang.System.exit;
import static org.tron.common.math.Maths.max;
import static org.tron.core.Constant.ADD_PRE_FIX_BYTE_MAINNET;
import static org.tron.core.Constant.ENERGY_LIMIT_IN_CONSTANT_TX;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.arch.Arch;
import org.tron.common.args.Account;
import org.tron.common.args.GenesisBlock;
import org.tron.common.args.Witness;
import org.tron.common.cron.CronExpression;
import org.tron.common.logsfilter.EventPluginConfig;
import org.tron.common.logsfilter.FilterQuery;
import org.tron.common.logsfilter.TriggerConfig;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.parameter.RateLimiterInitialization;
import org.tron.common.setting.RocksDbSettings;
import org.tron.common.utils.Commons;
import org.tron.common.utils.LocalWitnesses;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.Configuration;
import org.tron.core.exception.TronError;
import org.tron.core.store.AccountStore;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.dns.update.DnsType;
import org.tron.p2p.dns.update.PublishConfig;
import org.tron.p2p.utils.NetUtil;
import org.tron.program.Version;

@Slf4j(topic = "app")
@NoArgsConstructor
@Component
public class Args extends CommonParameter {

  /**
   * Maps deprecated CLI option names to their config-file equivalents.
   * Options not in this map have no config equivalent and are being removed entirely.
   */
  private static final Map<String, String> DEPRECATED_CLI_TO_CONFIG;

  static {
    Map<String, String> m = new HashMap<>();
    m.put("--storage-db-directory", "storage.db.directory");
    m.put("--storage-db-engine", "storage.db.engine");
    m.put("--storage-db-synchronous", "storage.db.sync");
    m.put("--storage-index-directory", "storage.index.directory");
    m.put("--storage-index-switch", "storage.index.switch");
    m.put("--storage-transactionHistory-switch", "storage.transHistory.switch");
    m.put("--contract-parse-enable", "event.subscribe.contractParse");
    m.put("--support-constant", "vm.supportConstant");
    m.put("--max-energy-limit-for-constant", "vm.maxEnergyLimitForConstant");
    m.put("--lru-cache-size", "vm.lruCacheSize");
    m.put("--min-time-ratio", "vm.minTimeRatio");
    m.put("--max-time-ratio", "vm.maxTimeRatio");
    m.put("--save-internaltx", "vm.saveInternalTx");
    m.put("--save-featured-internaltx", "vm.saveFeaturedInternalTx");
    m.put("--save-cancel-all-unfreeze-v2-details", "vm.saveCancelAllUnfreezeV2Details");
    m.put("--long-running-time", "vm.longRunningTime");
    m.put("--max-connect-number", "node.maxHttpConnectNumber");
    m.put("--rpc-thread", "node.rpc.thread");
    m.put("--solidity-thread", "node.solidity.threads");
    m.put("--validate-sign-thread", "node.validateSignThreadNum");
    m.put("--trust-node", "node.trustNode");
    m.put("--history-balance-lookup", "storage.balance.history.lookup");
    m.put("--es", "event.subscribe.enable");
    DEPRECATED_CLI_TO_CONFIG = Collections.unmodifiableMap(m);
  }

  @Getter
  private static String configFilePath = "";

  // Singleton config beans — populated at startup, read-only after init.
  // New code can read directly from these beans instead of CommonParameter.
  @Getter
  private static NodeConfig nodeConfig;
  @Getter
  private static VmConfig vmConfig;
  @Getter
  private static BlockConfig blockConfig;
  @Getter
  private static CommitteeConfig committeeConfig;
  @Getter
  private static StorageConfig storageConfig;
  @Getter
  private static GenesisConfig genesisConfig;
  @Getter
  private static MiscConfig miscConfig;
  @Getter
  private static RateLimiterConfig rateLimiterConfig;
  @Getter
  private static MetricsConfig metricsConfig;
  @Getter
  private static EventConfig eventConfig;

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


  /**
   * set parameters.
   */
  public static void setParam(final String[] args, final String confFileName) {
    // 1. Parse CLI args into a separate object
    CLIParameter cmd = new CLIParameter();
    JCommander jc = JCommander.newBuilder().addObject(cmd).build();
    jc.parse(args);

    if (cmd.version) {
      printVersion();
      exit(0);
    }
    if (cmd.help) {
      Args.printHelp(jc);
      exit(0);
    }

    // Resolve config file path
    configFilePath = StringUtils.isNoneBlank(cmd.shellConfFileName)
        ? cmd.shellConfFileName : confFileName;
    Config config = Configuration.getByFileName(configFilePath);

    // 2. Config overrides defaults
    applyConfigParams(config);

    // 3. CLI overrides Config (highest priority)
    applyCLIParams(cmd, jc);

    // 4. Apply platform constraints (e.g. ARM64 forces RocksDB)
    applyPlatformConstraints();

    // 5. Init witness (depends on CLI witness flag)
    initLocalWitnesses(config, cmd);
  }

  /**
   * Bridge VmConfig bean values to CommonParameter fields.
   * Temporary until Phase 2 moves fields into domain config objects.
   */
  private static void applyVmConfig(VmConfig vm) {
    PARAMETER.supportConstant = vm.isSupportConstant();
    PARAMETER.maxEnergyLimitForConstant = vm.getMaxEnergyLimitForConstant();
    PARAMETER.lruCacheSize = vm.getLruCacheSize();
    PARAMETER.minTimeRatio = vm.getMinTimeRatio();
    PARAMETER.maxTimeRatio = vm.getMaxTimeRatio();
    PARAMETER.longRunningTime = vm.getLongRunningTime();
    PARAMETER.estimateEnergy = vm.isEstimateEnergy();
    PARAMETER.estimateEnergyMaxRetry = vm.getEstimateEnergyMaxRetry();
    PARAMETER.vmTrace = vm.isVmTrace();
    PARAMETER.saveInternalTx = vm.isSaveInternalTx();
    PARAMETER.saveFeaturedInternalTx = vm.isSaveFeaturedInternalTx();
    PARAMETER.saveCancelAllUnfreezeV2Details = vm.isSaveCancelAllUnfreezeV2Details();
    PARAMETER.constantCallTimeoutMs = vm.getConstantCallTimeoutMs();
  }

  // Old applyStorageConfig removed — merged into applyStorageConfig()

  /**
   * Bridge StorageConfig bean to PARAMETER.storage fields.
   * Reads all storage config from one StorageConfig bean instance.
   * Config param is still needed for setDefaultDbOptions/setCacheStrategies/setDbRoots
   * which use raw Config for dynamic nested objects.
   */
  private static void applyStorageConfig(StorageConfig sc) {
    PARAMETER.storage.setDbEngine(sc.getDb().getEngine());
    PARAMETER.storage.setDbSync(sc.getDb().isSync());
    PARAMETER.storage.setDbDirectory(sc.getDb().getDirectory());
    PARAMETER.storage.setIndexDirectory(sc.getIndex().getDirectory());
    String indexSwitch = sc.getIndex().getSwitch();
    PARAMETER.storage.setIndexSwitch(
        org.apache.commons.lang3.StringUtils.isNotEmpty(indexSwitch) ? indexSwitch : "on");
    PARAMETER.storage.setTransactionHistorySwitch(sc.getTransHistory().getSwitch());
    // contractParse is set in applyEventConfig — it belongs to event.subscribe domain
    PARAMETER.storage.setCheckpointVersion(sc.getCheckpoint().getVersion());
    PARAMETER.storage.setCheckpointSync(sc.getCheckpoint().isSync());

    // estimatedTransactions / maxFlushCount clamping & validation run inside
    // TxCacheConfig.postProcess / SnapshotConfig.postProcess during bean load.
    PARAMETER.storage.setEstimatedBlockTransactions(sc.getTxCache().getEstimatedTransactions());
    PARAMETER.storage.setTxCacheInitOptimization(sc.getTxCache().isInitOptimization());
    PARAMETER.storage.setMaxFlushCount(sc.getSnapshot().getMaxFlushCount());

    // RocksDB settings
    StorageConfig.DbSettingsConfig dbs = sc.getDbSettings();
    PARAMETER.rocksDBCustomSettings = RocksDbSettings
        .initCustomSettings(dbs.getLevelNumber(), dbs.getCompactThreads(),
            dbs.getBlocksize(), dbs.getMaxBytesForLevelBase(),
            dbs.getMaxBytesForLevelMultiplier(), dbs.getLevel0FileNumCompactionTrigger(),
            dbs.getTargetFileSizeBase(), dbs.getTargetFileSizeMultiplier(),
            dbs.getMaxOpenFiles());
    RocksDbSettings.loggingSettings();

    // Dynamic nested objects use StorageConfig's raw storage sub-tree
    // setDefaultDbOptions must be called before setPropertyMapFromBean because
    // createPropertyFromBean calls newDefaultDbOptions which needs defaultDbOptions initialized
    PARAMETER.storage.setDefaultDbOptions(sc);
    PARAMETER.storage.setPropertyMapFromBean(sc.getProperties());
    PARAMETER.storage.setCacheStrategies(sc.getRawStorageConfig());
    PARAMETER.storage.setDbRoots(sc.getRawStorageConfig());
  }

  /**
   * Bridge NodeConfig backup sub-bean to PARAMETER fields.
   */
  private static void applyNodeBackupConfig(NodeConfig nc) {
    NodeConfig.NodeBackupConfig b = nc.getBackup();
    PARAMETER.backupPriority = b.getPriority();
    PARAMETER.backupPort = b.getPort();
    PARAMETER.keepAliveInterval = b.getKeepAliveInterval();
    PARAMETER.backupMembers = b.getMembers();
  }

  /**
   * Bridge GenesisConfig bean to GenesisBlock business object.
   * Converts raw address strings via Base58Check decoding.
   */
  private static void applyGenesisConfig(GenesisConfig gc, Config config) {
    if (gc.getTimestamp().isEmpty() && gc.getAssets().isEmpty()) {
      PARAMETER.genesisBlock = GenesisBlock.getDefault();
      return;
    }
    PARAMETER.genesisBlock = new GenesisBlock();
    PARAMETER.genesisBlock.setTimestamp(gc.getTimestamp());
    PARAMETER.genesisBlock.setParentHash(gc.getParentHash());

    if (!gc.getAssets().isEmpty()) {
      List<Account> accounts = new ArrayList<>();
      for (GenesisConfig.AssetConfig ac : gc.getAssets()) {
        Account account = new Account();
        account.setAccountName(ac.getAccountName());
        account.setAccountType(ac.getAccountType());
        account.setAddress(Commons.decodeFromBase58Check(ac.getAddress()));
        account.setBalance(ac.getBalance());
        accounts.add(account);
      }
      PARAMETER.genesisBlock.setAssets(accounts);
      AccountStore.setAccount(config);
    }
    {
      List<Witness> witnesses = new ArrayList<>();
      for (GenesisConfig.WitnessConfig wc : gc.getWitnesses()) {
        Witness witness = new Witness();
        witness.setAddress(Commons.decodeFromBase58Check(wc.getAddress()));
        witness.setUrl(wc.getUrl());
        witness.setVoteCount(wc.getVoteCount());
        witnesses.add(witness);
      }
      PARAMETER.genesisBlock.setWitnesses(witnesses);
    }
  }

  /**
   * Bridge MiscConfig bean values to CommonParameter fields.
   */
  private static void applyMiscConfig(MiscConfig mc) {
    PARAMETER.cryptoEngine = mc.getCryptoEngine();
    PARAMETER.needToUpdateAsset = mc.isNeedToUpdateAsset();
    PARAMETER.historyBalanceLookup = mc.isHistoryBalanceLookup();
    PARAMETER.trxReferenceBlock = mc.getTrxReferenceBlock();
    PARAMETER.trxExpirationTimeInMilliseconds = mc.getTrxExpirationTimeInMilliseconds();
    PARAMETER.blockNumForEnergyLimit = mc.getBlockNumForEnergyLimit();
    // seed.node — top-level config section, not under "node"
    // Config structure is arguably misplaced but preserved for backward compatibility
    PARAMETER.seedNode = new SeedNode();
    PARAMETER.seedNode.setAddressList(
        mc.getSeedNodeIpList().stream()
            .map(s -> org.tron.p2p.utils.NetUtil.parseInetSocketAddress(s))
            .collect(Collectors.toList()));
  }

  /**
   * Bridge RateLimiterConfig bean values to CommonParameter fields.
   * HTTP/RPC rate limiter lists still use getRateLimiterFromConfig() for
   * conversion to RateLimiterInitialization business objects.
   */
  private static void applyRateLimiterConfig(RateLimiterConfig rl) {
    PARAMETER.rateLimiterGlobalQps = rl.getGlobal().getQps();
    PARAMETER.rateLimiterGlobalIpQps = rl.getGlobal().getIp().getQps();
    PARAMETER.rateLimiterGlobalApiQps = rl.getGlobal().getApi().getQps();
    PARAMETER.rateLimiterSyncBlockChain = rl.getP2p().getSyncBlockChain();
    PARAMETER.rateLimiterFetchInvData = rl.getP2p().getFetchInvData();
    PARAMETER.rateLimiterDisconnect = rl.getP2p().getDisconnect();

    // HTTP/RPC rate limiter items: convert bean lists to business objects
    RateLimiterInitialization initialization = new RateLimiterInitialization();
    ArrayList<RateLimiterInitialization.HttpRateLimiterItem> httpItems = new ArrayList<>();
    for (RateLimiterConfig.HttpRateLimitItem item : rl.getHttp()) {
      httpItems.add(new RateLimiterInitialization.HttpRateLimiterItem(
          item.getComponent(), item.getStrategy(), item.getParamString()));
    }
    initialization.setHttpMap(httpItems);
    ArrayList<RateLimiterInitialization.RpcRateLimiterItem> rpcItems = new ArrayList<>();
    for (RateLimiterConfig.RpcRateLimitItem item : rl.getRpc()) {
      rpcItems.add(new RateLimiterInitialization.RpcRateLimiterItem(
          item.getComponent(), item.getStrategy(), item.getParamString()));
    }
    initialization.setRpcMap(rpcItems);
    PARAMETER.rateLimiterInitialization = initialization;
  }

  /**
   * Bridge EventConfig bean values to CommonParameter fields.
   * Converts EventConfig (raw bean) into EventPluginConfig and FilterQuery (business objects).
   */
  private static void applyEventConfig(EventConfig ec) {
    PARAMETER.eventSubscribe = ec.isEnable();
    // contractParse belongs to event.subscribe but Storage object holds it
    PARAMETER.storage.setContractParseSwitch(ec.isContractParse());

    // PARAMETER.eventPluginConfig and PARAMETER.eventFilter are only consumed by
    // Manager.startEventSubscribing(), which itself is gated by isEventSubscribe()
    // (= ec.isEnable()) at Manager.java:564. When subscribe is disabled, building
    // these objects has no observable effect — skip both early so PARAMETER stays
    // consistent with the runtime intent.
    if (!ec.isEnable()) {
      return;
    }

    // Build EventPluginConfig from EventConfig bean
    EventPluginConfig epc = new EventPluginConfig();
    epc.setVersion(ec.getVersion());
    epc.setStartSyncBlockNum(ec.getStartSyncBlockNum());

    // native queue
    EventConfig.NativeConfig nq = ec.getNativeQueue();
    epc.setUseNativeQueue(nq.isUseNativeQueue());
    epc.setBindPort(nq.getBindport());
    epc.setSendQueueLength(nq.getSendqueuelength());

    if (!nq.isUseNativeQueue()) {
      if (StringUtils.isNotEmpty(ec.getPath())) {
        epc.setPluginPath(ec.getPath().trim());
      }
      if (StringUtils.isNotEmpty(ec.getServer())) {
        epc.setServerAddress(ec.getServer().trim());
      }
      if (StringUtils.isNotEmpty(ec.getDbconfig())) {
        epc.setDbConfig(ec.getDbconfig().trim());
      }
    }

    // topics
    List<TriggerConfig> triggerConfigs = new ArrayList<>();
    for (EventConfig.TopicConfig tc : ec.getTopics()) {
      TriggerConfig trig = new TriggerConfig();
      trig.setTriggerName(tc.getTriggerName());
      trig.setEnabled(tc.isEnable());
      trig.setTopic(tc.getTopic());
      trig.setSolidified(tc.isSolidified());
      trig.setEthCompatible(tc.isEthCompatible());
      trig.setRedundancy(tc.isRedundancy());
      triggerConfigs.add(trig);
    }
    epc.setTriggerConfigList(triggerConfigs);

    PARAMETER.eventPluginConfig = epc;

    // Build FilterQuery from EventConfig.FilterConfig bean
    EventConfig.FilterConfig fc = ec.getFilter();
    FilterQuery filter = new FilterQuery();

    try {
      filter.setFromBlock(FilterQuery.parseFromBlockNumber(fc.getFromblock().trim()));
    } catch (Exception e) {
      logger.error("invalid filter: fromBlockNumber: {}", fc.getFromblock(), e);
      PARAMETER.eventFilter = null;
      return;
    }

    try {
      filter.setToBlock(FilterQuery.parseToBlockNumber(fc.getToblock().trim()));
    } catch (Exception e) {
      logger.error("invalid filter: toBlockNumber: {}", fc.getToblock(), e);
      PARAMETER.eventFilter = null;
      return;
    }

    filter.setContractAddressList(
        fc.getContractAddress().stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList()));
    filter.setContractTopicList(
        fc.getContractTopic().stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList()));

    PARAMETER.eventFilter = filter;
  }

  /**
   * Bridge MetricsConfig bean values to CommonParameter fields.
   * Note: node.metricsEnable is handled in applyNodeConfig (it's a node-level field).
   */
  private static void applyMetricsConfig(MetricsConfig mc) {
    PARAMETER.metricsPrometheusEnable = mc.getPrometheus().isEnable();
    PARAMETER.metricsPrometheusPort = mc.getPrometheus().getPort();
  }

  /**
   * Bridge CommitteeConfig bean values to CommonParameter fields.
   */
  private static void applyCommitteeConfig(CommitteeConfig cc) {
    PARAMETER.allowCreationOfContracts = cc.getAllowCreationOfContracts();
    PARAMETER.allowMultiSign = (int) cc.getAllowMultiSign();
    PARAMETER.allowAdaptiveEnergy = cc.getAllowAdaptiveEnergy();
    PARAMETER.allowDelegateResource = cc.getAllowDelegateResource();
    PARAMETER.allowSameTokenName = cc.getAllowSameTokenName();
    PARAMETER.allowTvmTransferTrc10 = cc.getAllowTvmTransferTrc10();
    PARAMETER.allowTvmConstantinople = cc.getAllowTvmConstantinople();
    PARAMETER.allowTvmSolidity059 = cc.getAllowTvmSolidity059();
    PARAMETER.forbidTransferToContract = cc.getForbidTransferToContract();
    PARAMETER.allowShieldedTRC20Transaction = cc.getAllowShieldedTRC20Transaction();
    PARAMETER.allowMarketTransaction = cc.getAllowMarketTransaction();
    PARAMETER.allowTransactionFeePool = cc.getAllowTransactionFeePool();
    PARAMETER.allowBlackHoleOptimization = cc.getAllowBlackHoleOptimization();
    PARAMETER.allowNewResourceModel = cc.getAllowNewResourceModel();
    PARAMETER.allowTvmIstanbul = cc.getAllowTvmIstanbul();
    PARAMETER.allowProtoFilterNum = cc.getAllowProtoFilterNum();
    PARAMETER.allowAccountStateRoot = cc.getAllowAccountStateRoot();
    PARAMETER.changedDelegation = cc.getChangedDelegation();
    PARAMETER.allowPBFT = cc.getAllowPBFT();
    PARAMETER.pBFTExpireNum = cc.getPBFTExpireNum();
    PARAMETER.allowTvmFreeze = cc.getAllowTvmFreeze();
    PARAMETER.allowTvmVote = cc.getAllowTvmVote();
    PARAMETER.allowTvmLondon = cc.getAllowTvmLondon();
    PARAMETER.allowTvmCompatibleEvm = cc.getAllowTvmCompatibleEvm();
    PARAMETER.allowHigherLimitForMaxCpuTimeOfOneTx =
        cc.getAllowHigherLimitForMaxCpuTimeOfOneTx();
    PARAMETER.allowNewRewardAlgorithm = cc.getAllowNewRewardAlgorithm();
    PARAMETER.allowOptimizedReturnValueOfChainId =
        cc.getAllowOptimizedReturnValueOfChainId();
    PARAMETER.allowTvmShangHai = cc.getAllowTvmShangHai();
    PARAMETER.allowOldRewardOpt = cc.getAllowOldRewardOpt();
    PARAMETER.allowEnergyAdjustment = cc.getAllowEnergyAdjustment();
    PARAMETER.allowStrictMath = cc.getAllowStrictMath();
    PARAMETER.consensusLogicOptimization = cc.getConsensusLogicOptimization();
    PARAMETER.allowTvmCancun = cc.getAllowTvmCancun();
    PARAMETER.allowTvmBlob = cc.getAllowTvmBlob();
    PARAMETER.unfreezeDelayDays = cc.getUnfreezeDelayDays();
    // allowReceiptsMerkleRoot not in CommonParameter — skip for now
    PARAMETER.allowAccountAssetOptimization = cc.getAllowAccountAssetOptimization();
    PARAMETER.allowAssetOptimization = cc.getAllowAssetOptimization();
    PARAMETER.allowNewReward = cc.getAllowNewReward();
    PARAMETER.memoFee = cc.getMemoFee();
    PARAMETER.allowDelegateOptimization = cc.getAllowDelegateOptimization();
    PARAMETER.allowDynamicEnergy = cc.getAllowDynamicEnergy();
    PARAMETER.dynamicEnergyThreshold = cc.getDynamicEnergyThreshold();
    PARAMETER.dynamicEnergyIncreaseFactor = cc.getDynamicEnergyIncreaseFactor();
    PARAMETER.dynamicEnergyMaxFactor = cc.getDynamicEnergyMaxFactor();
  }

  /**
   * Bridge BlockConfig bean values to CommonParameter fields.
   */
  private static void applyBlockConfig(BlockConfig block) {
    PARAMETER.needSyncCheck = block.isNeedSyncCheck();
    PARAMETER.maintenanceTimeInterval = block.getMaintenanceTimeInterval();
    PARAMETER.proposalExpireTime = block.getProposalExpireTime();
    PARAMETER.checkFrozenTime = block.getCheckFrozenTime();
  }

  /**
   * Bridge NodeConfig bean values to CommonParameter fields.
   * Some fields require post-binding range checks or dynamic defaults (e.g. CPUs/2),
   * which are applied here after copying the bean value.
   *
   * @param nc the NodeConfig bean populated from config.conf "node" section
   *               node.discovery / node.channel.read.timeout (dot-notation paths
   *               not part of the NodeConfig bean)
   */
  @SuppressWarnings("checkstyle:MethodLength")
  private static void applyNodeConfig(NodeConfig nc) {
    // ---- RPC sub-bean ----
    NodeConfig.RpcConfig rpc = nc.getRpc();
    PARAMETER.rpcEnable = rpc.isEnable();
    PARAMETER.rpcSolidityEnable = rpc.isSolidityEnable();
    PARAMETER.rpcPBFTEnable = rpc.isPBFTEnable();
    PARAMETER.rpcPort = rpc.getPort();
    PARAMETER.rpcOnSolidityPort = rpc.getSolidityPort();
    PARAMETER.rpcOnPBFTPort = rpc.getPBFTPort();
    PARAMETER.rpcThreadNum = rpc.getThread();
    PARAMETER.maxConcurrentCallsPerConnection = rpc.getMaxConcurrentCallsPerConnection();
    PARAMETER.flowControlWindow = rpc.getFlowControlWindow();
    PARAMETER.rpcMaxRstStream = rpc.getMaxRstStream();
    PARAMETER.rpcSecondsPerWindow = rpc.getSecondsPerWindow();
    PARAMETER.maxConnectionIdleInMillis = rpc.getMaxConnectionIdleInMillis();
    PARAMETER.maxConnectionAgeInMillis = rpc.getMaxConnectionAgeInMillis();
    PARAMETER.maxMessageSize = rpc.getMaxMessageSize();
    PARAMETER.maxHeaderListSize = rpc.getMaxHeaderListSize();
    PARAMETER.isRpcReflectionServiceEnable = rpc.isReflectionService();
    PARAMETER.minEffectiveConnection = rpc.getMinEffectiveConnection();
    PARAMETER.trxCacheEnable = rpc.isTrxCacheEnable();

    // ---- HTTP sub-bean ----
    NodeConfig.HttpConfig http = nc.getHttp();
    PARAMETER.fullNodeHttpEnable = http.isFullNodeEnable();
    PARAMETER.solidityNodeHttpEnable = http.isSolidityEnable();
    PARAMETER.pBFTHttpEnable = http.isPBFTEnable();
    PARAMETER.fullNodeHttpPort = http.getFullNodePort();
    PARAMETER.solidityHttpPort = http.getSolidityPort();
    PARAMETER.pBFTHttpPort = http.getPBFTPort();
    PARAMETER.httpMaxMessageSize = http.getMaxMessageSize();
    PARAMETER.maxNestingDepth = http.getMaxNestingDepth();
    PARAMETER.maxTokenCount = http.getMaxTokenCount();

    // ---- JSON-RPC sub-bean ----
    NodeConfig.JsonRpcConfig jsonrpc = nc.getJsonrpc();
    PARAMETER.jsonRpcHttpFullNodeEnable = jsonrpc.isHttpFullNodeEnable();
    PARAMETER.jsonRpcHttpSolidityNodeEnable = jsonrpc.isHttpSolidityEnable();
    PARAMETER.jsonRpcHttpPBFTNodeEnable = jsonrpc.isHttpPBFTEnable();
    PARAMETER.jsonRpcHttpFullNodePort = jsonrpc.getHttpFullNodePort();
    PARAMETER.jsonRpcHttpSolidityPort = jsonrpc.getHttpSolidityPort();
    PARAMETER.jsonRpcHttpPBFTPort = jsonrpc.getHttpPBFTPort();
    PARAMETER.jsonRpcMaxBlockRange = jsonrpc.getMaxBlockRange();
    PARAMETER.jsonRpcMaxSubTopics = jsonrpc.getMaxSubTopics();
    PARAMETER.jsonRpcMaxBlockFilterNum = jsonrpc.getMaxBlockFilterNum();
    PARAMETER.jsonRpcMaxMessageSize = jsonrpc.getMaxMessageSize();

    // ---- P2P sub-bean ----
    PARAMETER.nodeP2pVersion = nc.getP2p().getVersion();

    // ---- DNS sub-bean (tree URLs only — publish config uses complex validation) ----
    PARAMETER.dnsTreeUrls = nc.getDns().getTreeUrls().isEmpty()
        ? new ArrayList<>() : new ArrayList<>(nc.getDns().getTreeUrls());

    // ---- Dynamic config sub-bean ----
    PARAMETER.dynamicConfigEnable = nc.getDynamicConfig().isEnable();
    PARAMETER.dynamicConfigCheckInterval = nc.getDynamicConfig().getCheckInterval();

    // ---- Flat scalar fields ----
    PARAMETER.nodeEffectiveCheckEnable = nc.isEffectiveCheckEnable();
    PARAMETER.nodeConnectionTimeout = nc.getConnectionTimeout() * 1000;

    // fetchBlock.timeout — range check [100, 1000], default 500
    int fetchTimeout = nc.getFetchBlockTimeout();
    if (fetchTimeout > 1000) {
      fetchTimeout = 1000;
    } else if (fetchTimeout < 100) {
      fetchTimeout = 100;
    }
    PARAMETER.fetchBlockTimeout = fetchTimeout;

    PARAMETER.maxConnections = nc.getMaxConnections();
    PARAMETER.minConnections = nc.getMinConnections();
    PARAMETER.minActiveConnections = nc.getMinActiveConnections();
    PARAMETER.maxConnectionsWithSameIp = nc.getMaxConnectionsWithSameIp();
    PARAMETER.maxTps = nc.getMaxTps();
    PARAMETER.maxBlockInvPerSecond = nc.getMaxBlockInvPerSecond();
    PARAMETER.minParticipationRate = nc.getMinParticipationRate();
    PARAMETER.nodeListenPort = nc.getListenPort();
    PARAMETER.nodeEnableIpv6 = nc.isEnableIpv6();

    PARAMETER.syncFetchBatchNum = nc.getSyncFetchBatchNum();
    PARAMETER.maxPendingBlockSize = nc.getMaxPendingBlockSize();
    PARAMETER.solidityThreads = nc.getSolidityThreads();
    PARAMETER.blockProducedTimeOut = nc.getBlockProducedTimeOut();

    PARAMETER.maxHttpConnectNumber = nc.getMaxHttpConnectNumber();
    PARAMETER.netMaxTrxPerSecond = nc.getNetMaxTrxPerSecond();
    PARAMETER.tcpNettyWorkThreadNum = nc.getTcpNettyWorkThreadNum();
    PARAMETER.udpNettyWorkThreadNum = nc.getUdpNettyWorkThreadNum();

    if (StringUtils.isEmpty(PARAMETER.trustNodeAddr)) {
      String trustNode = nc.getTrustNode();
      PARAMETER.trustNodeAddr = StringUtils.isEmpty(trustNode) ? null : trustNode;
    }

    PARAMETER.validateSignThreadNum = nc.getValidateSignThreadNum();
    PARAMETER.walletExtensionApi = nc.isWalletExtensionApi();
    PARAMETER.receiveTcpMinDataLength = nc.getReceiveTcpMinDataLength();
    PARAMETER.isOpenFullTcpDisconnect = nc.isOpenFullTcpDisconnect();
    PARAMETER.nodeDetectEnable = nc.isNodeDetectEnable();

    PARAMETER.inactiveThreshold = nc.getInactiveThreshold();

    PARAMETER.maxTransactionPendingSize = nc.getMaxTransactionPendingSize();
    PARAMETER.pendingTransactionTimeout = nc.getPendingTransactionTimeout();
    PARAMETER.maxTrxCacheSize = nc.getMaxTrxCacheSize();

    PARAMETER.validContractProtoThreadNum = nc.getValidContractProtoThreads();

    PARAMETER.maxFastForwardNum = nc.getMaxFastForwardNum();
    PARAMETER.shieldedTransInPendingMaxCounts = nc.getShieldedTransInPendingMaxCounts();
    PARAMETER.agreeNodeCount = nc.getAgreeNodeCount();

    PARAMETER.setOpenHistoryQueryWhenLiteFN(nc.isOpenHistoryQueryWhenLiteFN());
    PARAMETER.nodeMetricsEnable = nc.isMetricsEnable();
    PARAMETER.openPrintLog = nc.isOpenPrintLog();
    PARAMETER.openTransactionSort = nc.isOpenTransactionSort();
    PARAMETER.blockCacheTimeout = nc.getBlockCacheTimeout();
    PARAMETER.zenTokenId = nc.getZenTokenId();
    PARAMETER.allowShieldedTransactionApi = nc.isAllowShieldedTransactionApi();

    PARAMETER.unsolidifiedBlockCheck = nc.isUnsolidifiedBlockCheck();
    PARAMETER.maxUnsolidifiedBlocks = nc.getMaxUnsolidifiedBlocks();

    // disabledApi list — lowercase normalization
    PARAMETER.disabledApiList = nc.getDisabledApi().isEmpty()
        ? Collections.emptyList()
        : nc.getDisabledApi().stream().map(s -> s.toLowerCase(Locale.ROOT))
            .collect(Collectors.toList());

    // ---- Fields previously scattered in applyConfigParams ----

    // discovery (dot-notation, read in NodeConfig.fromConfig)
    PARAMETER.nodeDiscoveryEnable = nc.isDiscoveryEnable();
    PARAMETER.nodeDiscoveryPersist = nc.isDiscoveryPersist();
    PARAMETER.nodeChannelReadTimeout = nc.getChannelReadTimeout();

    // Legacy maxActiveNodes fallback handled in NodeConfig.fromConfig()

    // p2p config and external IP
    PARAMETER.p2pConfig = new P2pConfig();
    PARAMETER.nodeLanIp = PARAMETER.p2pConfig.getLanIp();
    externalIp(nc);

    // DNS publish config
    PARAMETER.dnsPublishConfig = loadDnsPublishConfig(nc);

    // Shielded transaction API — legacy fallback handled in NodeConfig.fromConfig()
    PARAMETER.allowShieldedTransactionApi = nc.isAllowShieldedTransactionApi();

    // Active/passive/fastForward node lists from bean with filtering
    PARAMETER.activeNodes = filterInetSocketAddress(nc.getActive(), true);
    PARAMETER.passiveNodes = new ArrayList<>();
    for (InetSocketAddress sa : filterInetSocketAddress(nc.getPassive(), false)) {
      PARAMETER.passiveNodes.add(sa.getAddress());
    }
    PARAMETER.fastForwardNodes = filterInetSocketAddress(nc.getFastForward(), true);

    // node.shutdown from bean (dot-notation, read in NodeConfig.fromConfig)
    String shutdownBlockTime = nc.getShutdownBlockTime();
    if (!shutdownBlockTime.isEmpty()) {
      try {
        PARAMETER.shutdownBlockTime = new CronExpression(shutdownBlockTime);
      } catch (ParseException e) {
        throw new TronError(e, TronError.ErrCode.AUTO_STOP_PARAMS);
      }
    }
    if (nc.getShutdownBlockHeight() >= 0) {
      PARAMETER.shutdownBlockHeight = nc.getShutdownBlockHeight();
    }
    if (nc.getShutdownBlockCount() >= 0) {
      PARAMETER.shutdownBlockCount = nc.getShutdownBlockCount();
    }
  }

  /**
   * Apply platform-specific constraints after all config sources are resolved.
   * ARM64 does not support LevelDB (native JNI library unavailable),
   * so db.engine is forced to RocksDB regardless of config or CLI settings.
   */
  private static void applyPlatformConstraints() {
    if (Arch.isArm64()
        && !Constant.ROCKSDB.equalsIgnoreCase(PARAMETER.storage.getDbEngine())) {
      logger.warn("ARM64 only supports RocksDB, ignoring db.engine='{}'",
          PARAMETER.storage.getDbEngine());
      PARAMETER.storage.setDbEngine(Constant.ROCKSDB);
    }
  }

  /**
   * Apply parameters from config file.
   */
  public static void applyConfigParams(
      final Config config) {

    Wallet.setAddressPreFixByte(ADD_PRE_FIX_BYTE_MAINNET);
    Wallet.setAddressPreFixString(Constant.ADD_PRE_FIX_STRING_MAINNET);

    // crypto.engine handled by MiscConfig

    // VM config: bind from config.conf "vm" section
    vmConfig = VmConfig.fromConfig(config);
    applyVmConfig(vmConfig);

    // Node config: bind from config.conf "node" section
    nodeConfig = NodeConfig.fromConfig(config);
    applyNodeConfig(nodeConfig);

    // vm.minTimeRatio, vm.maxTimeRatio, vm.longRunningTime already handled by VmConfig above

    // Storage config: bind from config.conf "storage" section
    PARAMETER.storage = new Storage();
    storageConfig = StorageConfig.fromConfig(config);
    applyStorageConfig(storageConfig);

    // seed.node is a top-level config section (not under "node") — config structure
    // is arguably misplaced, but preserved for backward compatibility

    // Genesis config: bind from config.conf "genesis.block" section
    genesisConfig = GenesisConfig.fromConfig(config);
    applyGenesisConfig(genesisConfig, config);

    // Block config: bind from config.conf "block" section
    blockConfig = BlockConfig.fromConfig(config);
    applyBlockConfig(blockConfig);

    // node discovery, legacy fallback, p2p, dns — all handled in applyNodeConfig

    // Misc config: storage, trx, energy — small domains, read via beans
    miscConfig = MiscConfig.fromConfig(config);
    applyMiscConfig(miscConfig);

    // vm, committee already handled above

    // Committee config: bind from config.conf "committee" section
    committeeConfig = CommitteeConfig.fromConfig(config);
    applyCommitteeConfig(committeeConfig);

    // shielded transaction API, active/passive/fastForward — handled in applyNodeConfig

    // Rate limiter config: bind from config.conf "rate.limiter" section
    rateLimiterConfig = RateLimiterConfig.fromConfig(config);
    applyRateLimiterConfig(rateLimiterConfig);

    // Node backup: from NodeConfig bean
    applyNodeBackupConfig(nodeConfig);

    // Metrics config: bind from config.conf "node.metrics" section
    metricsConfig = MetricsConfig.fromConfig(config);
    applyMetricsConfig(metricsConfig);

    // historyBalanceLookup already handled by MiscConfig above

    // node.shutdown — handled in applyNodeConfig

    // Event config: bind from config.conf "event.subscribe" section
    eventConfig = EventConfig.fromConfig(config);
    applyEventConfig(eventConfig);

    logConfig();
  }

  /**
   * Apply CLI parameters that were explicitly passed.
   * Only assigned parameters override Config values.
   */
  private static void applyCLIParams(CLIParameter cmd, JCommander jc) {
    Set<String> assigned = jc.getParameters().stream()
        .filter(ParameterDescription::isAssigned)
        .map(ParameterDescription::getLongestName)
        .collect(Collectors.toSet());

    jc.getParameters().stream()
        .filter(ParameterDescription::isAssigned)
        .filter(pd -> {
          try {
            return CLIParameter.class.getDeclaredField(pd.getParameterized().getName())
                .isAnnotationPresent(Deprecated.class);
          } catch (NoSuchFieldException e) {
            return false;
          }
        })
        .forEach(pd -> {
          String cliOption = pd.getLongestName();
          String configKey = DEPRECATED_CLI_TO_CONFIG.get(cliOption);
          if (configKey != null) {
            logger.warn("CLI option '{}' is deprecated and will be removed in a future release."
                + " Please use config key '{}' instead.", cliOption, configKey);
          } else {
            logger.warn("CLI option '{}' is deprecated and will be removed in a future release.",
                cliOption);
          }
        });

    if (assigned.contains("--output-directory")) {
      PARAMETER.outputDirectory = cmd.outputDirectory;
    }
    if (assigned.contains("--witness")) {
      PARAMETER.witness = cmd.witness;
    }
    if (assigned.contains("--support-constant")) {
      PARAMETER.supportConstant = cmd.supportConstant;
    }
    if (assigned.contains("--max-energy-limit-for-constant")) {
      PARAMETER.maxEnergyLimitForConstant = max(ENERGY_LIMIT_IN_CONSTANT_TX,
          cmd.maxEnergyLimitForConstant, true);
    }
    if (assigned.contains("--lru-cache-size")) {
      PARAMETER.lruCacheSize = cmd.lruCacheSize;
    }
    if (assigned.contains("--debug")) {
      PARAMETER.debug = cmd.debug;
    }
    if (assigned.contains("--min-time-ratio")) {
      PARAMETER.minTimeRatio = cmd.minTimeRatio;
    }
    if (assigned.contains("--max-time-ratio")) {
      PARAMETER.maxTimeRatio = cmd.maxTimeRatio;
    }
    if (assigned.contains("--save-internaltx")) {
      PARAMETER.saveInternalTx = cmd.saveInternalTx;
    }
    if (assigned.contains("--save-featured-internaltx")) {
      PARAMETER.saveFeaturedInternalTx = cmd.saveFeaturedInternalTx;
    }
    if (assigned.contains("--save-cancel-all-unfreeze-v2-details")) {
      PARAMETER.saveCancelAllUnfreezeV2Details = cmd.saveCancelAllUnfreezeV2Details;
    }
    if (assigned.contains("--long-running-time")) {
      PARAMETER.longRunningTime = cmd.longRunningTime;
    }
    if (assigned.contains("--max-connect-number")) {
      PARAMETER.maxHttpConnectNumber = cmd.maxHttpConnectNumber;
    }
    if (assigned.contains("--storage-db-directory")) {
      PARAMETER.storage.setDbDirectory(cmd.storageDbDirectory);
    }
    if (assigned.contains("--storage-db-engine")) {
      PARAMETER.storage.setDbEngine(cmd.storageDbEngine);
    }
    if (assigned.contains("--storage-db-synchronous")) {
      PARAMETER.storage.setDbSync(Boolean.valueOf(cmd.storageDbSynchronous));
    }
    if (assigned.contains("--contract-parse-enable")) {
      PARAMETER.storage.setContractParseSwitch(Boolean.valueOf(cmd.contractParseEnable));
    }
    if (assigned.contains("--storage-index-directory")) {
      PARAMETER.storage.setIndexDirectory(cmd.storageIndexDirectory);
    }
    if (assigned.contains("--storage-index-switch")) {
      PARAMETER.storage.setIndexSwitch(cmd.storageIndexSwitch);
    }
    if (assigned.contains("--storage-transactionHistory-switch")) {
      PARAMETER.storage.setTransactionHistorySwitch(cmd.storageTransactionHistorySwitch);
    }
    if (assigned.contains("--fast-forward")) {
      PARAMETER.fastForward = cmd.fastForward;
    }
    if (assigned.contains("--solidity")) {
      PARAMETER.solidityNode = cmd.solidityNode;
    }
    if (assigned.contains("--keystore-factory")) {
      PARAMETER.keystoreFactory = cmd.keystoreFactory;
    }
    if (assigned.contains("--rpc-thread")) {
      PARAMETER.rpcThreadNum = cmd.rpcThreadNum;
    }
    if (assigned.contains("--solidity-thread")) {
      PARAMETER.solidityThreads = cmd.solidityThreads;
    }
    if (assigned.contains("--validate-sign-thread")) {
      PARAMETER.validateSignThreadNum = cmd.validateSignThreadNum;
    }
    if (assigned.contains("--trust-node")) {
      PARAMETER.trustNodeAddr = cmd.trustNodeAddr;
    }
    if (assigned.contains("--es")) {
      PARAMETER.eventSubscribe = cmd.eventSubscribe;
    }
    if (assigned.contains("--p2p-disable")) {
      PARAMETER.p2pDisable = cmd.p2pDisable;
    }
    if (assigned.contains("--history-balance-lookup")) {
      PARAMETER.historyBalanceLookup = cmd.historyBalanceLookup;
    }
    if (assigned.contains("--log-config")) {
      PARAMETER.logbackPath = cmd.logbackPath;
    }
    // seedNodes is a JCommander positional (main) parameter, which does not support
    // isAssigned(). An empty-check is used instead — this is safe because the default
    // is an empty list, so non-empty means the user explicitly passed values on CLI.
    if (!cmd.seedNodes.isEmpty()) {
      logger.warn("Positional seed-node arguments are deprecated. "
          + "Please use seed.node.ip.list in the config file instead.");
      List<InetSocketAddress> seeds = new ArrayList<>();
      for (String s : cmd.seedNodes) {
        seeds.add(NetUtil.parseInetSocketAddress(s));
      }
      PARAMETER.seedNode.setAddressList(seeds);
    }
  }

  private static void initLocalWitnesses(Config config, CLIParameter cmd) {
    // not a witness node, skip
    if (!PARAMETER.isWitness()) {
      localWitnesses = new LocalWitnesses();
      return;
    }

    // path 1: CLI --private-key
    if (StringUtils.isNotBlank(cmd.privateKey)) {
      localWitnesses = WitnessInitializer.initFromCLIPrivateKey(
          cmd.privateKey, cmd.witnessAddress);
      return;
    }

    LocalWitnessConfig lwConfig = LocalWitnessConfig.fromConfig(config);

    // path 2: config localwitness (private key list)
    if (!lwConfig.getPrivateKeys().isEmpty()) {
      localWitnesses = WitnessInitializer.initFromCFGPrivateKey(
          lwConfig.getPrivateKeys(), lwConfig.getAccountAddress());
      return;
    }

    // path 3: config localwitnesskeystore + password
    if (!lwConfig.getKeystores().isEmpty()) {
      localWitnesses = WitnessInitializer.initFromKeystore(
          lwConfig.getKeystores(), cmd.password, lwConfig.getAccountAddress());
      return;
    }

    // no private key source configured
    throw new TronError("This is a witness node, but localWitnesses is null",
        TronError.ErrCode.WITNESS_INIT);
  }

  @VisibleForTesting
  public static void clearParam() {
    CommonParameter.reset();
    configFilePath = "";
    localWitnesses = null;
    nodeConfig = null;
    vmConfig = null;
    blockConfig = null;
    committeeConfig = null;
    storageConfig = null;
    genesisConfig = null;
    miscConfig = null;
    rateLimiterConfig = null;
    metricsConfig = null;
    eventConfig = null;
  }

  // getProposalExpirationTime removed — logic moved to BlockConfig.fromConfig()

  // getWitnessesFromConfig, createWitness, getAccountsFromConfig, createAccount
  // removed — logic moved to applyGenesisConfig()

  // getRateLimiterFromConfig removed — logic moved to applyRateLimiterConfig()

  // getInetSocketAddress removed — use filterInetSocketAddress

  /**
   * Parse and optionally filter a list of address strings.
   * Overload that accepts a pre-read list from a bean instead of a config path.
   */
  public static List<InetSocketAddress> filterInetSocketAddress(
      List<String> addressList, boolean filter) {
    List<InetSocketAddress> ret = new ArrayList<>();
    for (String configString : addressList) {
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

  // getInetAddress removed — use filterInetSocketAddress

  // getEventPluginConfig removed — logic moved to applyEventConfig()


  public static PublishConfig loadDnsPublishConfig(NodeConfig nodeConfig) {
    PublishConfig publishConfig = new PublishConfig();
    NodeConfig.DnsConfig dns = nodeConfig.getDns();
    publishConfig.setDnsPublishEnable(dns.isPublish());
    loadDnsPublishParameters(dns, publishConfig);
    return publishConfig;
  }

  /**
   * Load DNS publish parameters from bean into PublishConfig.
   * Public method — called by tests and external code.
   */
  public static void loadDnsPublishParameters(final com.typesafe.config.Config config,
      PublishConfig publishConfig) {
    NodeConfig nodeConfig = NodeConfig.fromConfig(config);
    loadDnsPublishParameters(nodeConfig.getDns(), publishConfig);
  }

  private static void loadDnsPublishParameters(NodeConfig.DnsConfig dns,
      PublishConfig publishConfig) {

    if (publishConfig.isDnsPublishEnable()) {
      if (StringUtils.isNotEmpty(dns.getDnsDomain())) {
        publishConfig.setDnsDomain(dns.getDnsDomain());
      } else {
        logEmptyError("node.dns.dnsDomain");
      }

      if (dns.getChangeThreshold() > 0) {
        publishConfig.setChangeThreshold(dns.getChangeThreshold());
      } else if (Double.compare(dns.getChangeThreshold(), 0.0) != 0) {
        logger.error("Check node.dns.changeThreshold, should be bigger than 0, default 0.1");
      }

      int maxMergeSize = dns.getMaxMergeSize();
      if (maxMergeSize >= 1 && maxMergeSize <= 5) {
        publishConfig.setMaxMergeSize(maxMergeSize);
      } else if (maxMergeSize != 0) {
        logger.error("Check node.dns.maxMergeSize, should be [1~5], default 5");
      }

      if (StringUtils.isNotEmpty(dns.getDnsPrivate())) {
        publishConfig.setDnsPrivate(dns.getDnsPrivate());
      } else {
        logEmptyError("node.dns.dnsPrivate");
      }

      if (!dns.getKnownUrls().isEmpty()) {
        publishConfig.setKnownTreeUrls(dns.getKnownUrls());
      }

      if (!dns.getStaticNodes().isEmpty()) {
        publishConfig.setStaticNodes(
            filterInetSocketAddress(dns.getStaticNodes(), false));
      }

      String serverType = dns.getServerType();
      if (StringUtils.isNotEmpty(serverType)) {
        if (!"aws".equalsIgnoreCase(serverType) && !"aliyun".equalsIgnoreCase(serverType)) {
          throw new IllegalArgumentException(
              "Check node.dns.serverType, must be aws or aliyun");
        }
        if ("aws".equalsIgnoreCase(serverType)) {
          publishConfig.setDnsType(DnsType.AwsRoute53);
        } else {
          publishConfig.setDnsType(DnsType.AliYun);
        }
      } else {
        logEmptyError("node.dns.serverType");
      }

      if (StringUtils.isNotEmpty(dns.getAccessKeyId())) {
        publishConfig.setAccessKeyId(dns.getAccessKeyId());
      } else {
        logEmptyError("node.dns.accessKeyId");
      }
      if (StringUtils.isNotEmpty(dns.getAccessKeySecret())) {
        publishConfig.setAccessKeySecret(dns.getAccessKeySecret());
      } else {
        logEmptyError("node.dns.accessKeySecret");
      }

      if (publishConfig.getDnsType() == DnsType.AwsRoute53) {
        if (StringUtils.isNotEmpty(dns.getAwsRegion())) {
          publishConfig.setAwsRegion(dns.getAwsRegion());
        } else {
          logEmptyError("node.dns.awsRegion");
        }
        if (StringUtils.isNotEmpty(dns.getAwsHostZoneId())) {
          publishConfig.setAwsHostZoneId(dns.getAwsHostZoneId());
        }
      } else {
        if (StringUtils.isNotEmpty(dns.getAliyunDnsEndpoint())) {
          publishConfig.setAliDnsEndpoint(dns.getAliyunDnsEndpoint());
        } else {
          logEmptyError("node.dns.aliyunDnsEndpoint");
        }
      }
    }
  }

  private static void logEmptyError(String arg) {
    throw new IllegalArgumentException(String.format("Check %s, must not be null or empty", arg));
  }

  // createTriggerConfig removed — logic moved to applyEventConfig()
  // getEventFilter removed — logic moved to applyEventConfig()

  private static void externalIp(NodeConfig nodeConfig) {
    String externalIp = nodeConfig.getDiscoveryExternalIp();
    if (StringUtils.isEmpty(externalIp)) {
      if (StringUtils.isEmpty(PARAMETER.nodeExternalIp)) {
        logger.info("External IP wasn't set, using ipv4 from libp2p");
        PARAMETER.nodeExternalIp = PARAMETER.p2pConfig.getIp();
        if (StringUtils.isEmpty(PARAMETER.nodeExternalIp)) {
          PARAMETER.nodeExternalIp = PARAMETER.nodeLanIp;
        }
      }
    } else {
      PARAMETER.nodeExternalIp = externalIp;
    }
  }

  // initRocksDbSettings, initRocksDbBackupProperty, initBackupProperty
  // removed — logic moved to applyStorageConfig() and applyNodeBackupConfig()

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

  /**
   * get output directory.
   */
  public String getOutputDirectory() {
    if (!this.outputDirectory.equals("") && !this.outputDirectory.endsWith(File.separator)) {
      return this.outputDirectory + File.separator;
    }
    return this.outputDirectory;
  }

  // ── CLI help / version utilities ─────────────────

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
    helpStr.append(String.format(
        "%nNote: Positional seedNode arguments are deprecated."
            + " Use seed.node.ip.list in the config file instead.%n"));
    helpStr.append(String.format("%nVERSION: %n%s-%s%n", Version.getVersion(),
        getCommitIdAbbrev()));

    Map<String, String[]> groupOptionListMap = Args.getOptionGroup();
    for (Map.Entry<String, String[]> entry : groupOptionListMap.entrySet()) {
      String group = entry.getKey();
      helpStr.append(String.format("%n%s OPTIONS:%n", group.toUpperCase(Locale.ROOT)));
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
        boolean isDeprecated;
        try {
          isDeprecated = CLIParameter.class.getDeclaredField(
              parameterDescription.getParameterized().getName())
              .isAnnotationPresent(Deprecated.class);
        } catch (NoSuchFieldException e) {
          isDeprecated = false;
        }
        String desc = upperFirst(parameterDescription.getDescription());
        if (isDeprecated) {
          desc += " (deprecated)";
        }
        String tmpOptionDesc = String.format("%s\t\t\t%s%n",
            Strings.padEnd(parameterDescription.getNames(), optionMaxLength, ' '),
            desc);
        helpStr.append(tmpOptionDesc);
      }
    }
    jCommander.getConsole().println(helpStr.toString());
  }

  public static String upperFirst(String name) {
    if (name.length() <= 1) {
      return name;
    }
    name = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    return name;
  }

  private static String getCommitIdAbbrev() {
    Properties properties = new Properties();
    try {
      InputStream in = Thread.currentThread()
          .getContextClassLoader().getResourceAsStream("git.properties");
      if (in == null) {
        logger.warn("git.properties not found on classpath");
        return "";
      }
      properties.load(in);
    } catch (IOException e) {
      logger.warn("Load resource failed,git.properties {}", e.getMessage());
    }
    return properties.getProperty("git.commit.id.abbrev");
  }

  private static Map<String, String[]> getOptionGroup() {
    String[] tronOption = new String[] {"version", "help", "shellConfFileName", "logbackPath",
        "eventSubscribe", "solidityNode", "keystoreFactory"};
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
          CLIParameter.class.getField(option);
        } catch (NoSuchFieldException e) {
          logger.warn("NoSuchFieldException:{},{}", option, e.getMessage());
        }
      }
    }
    return optionGroupMap;
  }
}

