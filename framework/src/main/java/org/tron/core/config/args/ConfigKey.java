package org.tron.core.config.args;

/**
 * HOCON configuration key constants.
 * These map to paths in config files (e.g. config.conf) and are read by Args.setParam().
 */
final class ConfigKey {

  private ConfigKey() {
  }

  // local witness
  public static final String LOCAL_WITNESS = "localwitness"; // private key
  public static final String LOCAL_WITNESS_ACCOUNT_ADDRESS = "localWitnessAccountAddress";
  public static final String LOCAL_WITNESS_KEYSTORE = "localwitnesskeystore";

  // crypto
  public static final String CRYPTO_ENGINE = "crypto.engine";

  // vm
  public static final String VM_SUPPORT_CONSTANT = "vm.supportConstant";
  public static final String VM_MAX_ENERGY_LIMIT_FOR_CONSTANT = "vm.maxEnergyLimitForConstant";
  public static final String VM_LRU_CACHE_SIZE = "vm.lruCacheSize";
  public static final String VM_MIN_TIME_RATIO = "vm.minTimeRatio";
  public static final String VM_MAX_TIME_RATIO = "vm.maxTimeRatio";
  public static final String VM_LONG_RUNNING_TIME = "vm.longRunningTime";
  public static final String VM_ESTIMATE_ENERGY = "vm.estimateEnergy";
  public static final String VM_ESTIMATE_ENERGY_MAX_RETRY = "vm.estimateEnergyMaxRetry";
  public static final String VM_TRACE = "vm.vmTrace";
  public static final String VM_SAVE_INTERNAL_TX = "vm.saveInternalTx";
  public static final String VM_SAVE_FEATURED_INTERNAL_TX = "vm.saveFeaturedInternalTx";
  public static final String VM_SAVE_CANCEL_ALL_UNFREEZE_V2_DETAILS =
      "vm.saveCancelAllUnfreezeV2Details";

  // genesis
  public static final String GENESIS_BLOCK = "genesis.block";
  public static final String GENESIS_BLOCK_TIMESTAMP = "genesis.block.timestamp";
  public static final String GENESIS_BLOCK_PARENTHASH = "genesis.block.parentHash";
  public static final String GENESIS_BLOCK_ASSETS = "genesis.block.assets";
  public static final String GENESIS_BLOCK_WITNESSES = "genesis.block.witnesses";

  // block
  public static final String BLOCK_NEED_SYNC_CHECK = "block.needSyncCheck";
  public static final String BLOCK_MAINTENANCE_TIME_INTERVAL = "block.maintenanceTimeInterval";
  public static final String BLOCK_PROPOSAL_EXPIRE_TIME = "block.proposalExpireTime";
  public static final String BLOCK_CHECK_FROZEN_TIME = "block.checkFrozenTime";
  public static final String BLOCK_CACHE_TIMEOUT = "node.blockCacheTimeout";

  // node - discovery
  public static final String NODE_DISCOVERY_ENABLE = "node.discovery.enable";
  public static final String NODE_DISCOVERY_PERSIST = "node.discovery.persist";
  public static final String NODE_DISCOVERY_EXTERNAL_IP = "node.discovery.external.ip";

  // node - connection
  public static final String NODE_EFFECTIVE_CHECK_ENABLE = "node.effectiveCheckEnable";
  public static final String NODE_CONNECTION_TIMEOUT = "node.connection.timeout";
  public static final String NODE_FETCH_BLOCK_TIMEOUT = "node.fetchBlock.timeout";
  public static final String NODE_CHANNEL_READ_TIMEOUT = "node.channel.read.timeout";
  public static final String NODE_MAX_CONNECTIONS = "node.maxConnections";
  public static final String NODE_MIN_CONNECTIONS = "node.minConnections";
  public static final String NODE_MIN_ACTIVE_CONNECTIONS = "node.minActiveConnections";
  public static final String NODE_MAX_CONNECTIONS_WITH_SAME_IP = "node.maxConnectionsWithSameIp";
  public static final String NODE_MIN_PARTICIPATION_RATE = "node.minParticipationRate";
  public static final String NODE_MAX_ACTIVE_NODES = "node.maxActiveNodes";
  public static final String NODE_MAX_ACTIVE_NODES_WITH_SAME_IP = "node.maxActiveNodesWithSameIp";
  public static final String NODE_CONNECT_FACTOR = "node.connectFactor";
  public static final String NODE_ACTIVE_CONNECT_FACTOR = "node.activeConnectFactor";
  public static final String NODE_IS_OPEN_FULL_TCP_DISCONNECT = "node.isOpenFullTcpDisconnect";
  public static final String NODE_INACTIVE_THRESHOLD = "node.inactiveThreshold";
  public static final String NODE_DETECT_ENABLE = "node.nodeDetectEnable";
  public static final String NODE_MAX_HTTP_CONNECT_NUMBER = "node.maxHttpConnectNumber";

  // node - p2p
  public static final String NODE_LISTEN_PORT = "node.listen.port";
  public static final String NODE_P2P_VERSION = "node.p2p.version";
  public static final String NODE_ENABLE_IPV6 = "node.enableIpv6";
  public static final String NODE_SYNC_FETCH_BATCH_NUM = "node.syncFetchBatchNum";
  public static final String NODE_MAX_TPS = "node.maxTps";
  public static final String NODE_NET_MAX_TRX_PER_SECOND = "node.netMaxTrxPerSecond";
  public static final String NODE_TCP_NETTY_WORK_THREAD_NUM = "node.tcpNettyWorkThreadNum";
  public static final String NODE_UDP_NETTY_WORK_THREAD_NUM = "node.udpNettyWorkThreadNum";
  public static final String NODE_VALIDATE_SIGN_THREAD_NUM = "node.validateSignThreadNum";
  public static final String NODE_RECEIVE_TCP_MIN_DATA_LENGTH = "node.receiveTcpMinDataLength";
  public static final String NODE_PRODUCED_TIMEOUT = "node.blockProducedTimeOut";
  public static final String NODE_MAX_TRANSACTION_PENDING_SIZE = "node.maxTransactionPendingSize";
  public static final String NODE_PENDING_TRANSACTION_TIMEOUT = "node.pendingTransactionTimeout";
  public static final String NODE_ACTIVE = "node.active";
  public static final String NODE_PASSIVE = "node.passive";
  public static final String NODE_FAST_FORWARD = "node.fastForward";
  public static final String NODE_MAX_FAST_FORWARD_NUM = "node.maxFastForwardNum";
  public static final String NODE_AGREE_NODE_COUNT = "node.agreeNodeCount";
  public static final String NODE_SOLIDITY_THREADS = "node.solidity.threads";
  public static final String NODE_TRUST_NODE = "node.trustNode";
  public static final String NODE_WALLET_EXTENSION_API = "node.walletExtensionApi";
  public static final String NODE_VALID_CONTRACT_PROTO_THREADS = "node.validContractProto.threads";
  public static final String NODE_SHIELDED_TRANS_IN_PENDING_MAX_COUNTS =
      "node.shieldedTransInPendingMaxCounts";
  public static final String NODE_FULLNODE_ALLOW_SHIELDED_TRANSACTION =
      "node.fullNodeAllowShieldedTransaction";
  public static final String ALLOW_SHIELDED_TRANSACTION_API =
      "node.allowShieldedTransactionApi";
  public static final String NODE_ZEN_TOKENID = "node.zenTokenId";
  public static final String NODE_OPEN_HISTORY_QUERY_WHEN_LITEFN =
      "node.openHistoryQueryWhenLiteFN";
  public static final String NODE_METRICS_ENABLE = "node.metricsEnable";
  public static final String NODE_DISABLED_API_LIST = "node.disabledApi";

  // node - rpc
  public static final String NODE_RPC_PORT = "node.rpc.port";
  public static final String NODE_RPC_SOLIDITY_PORT = "node.rpc.solidityPort";
  public static final String NODE_RPC_PBFT_PORT = "node.rpc.PBFTPort";
  public static final String NODE_RPC_ENABLE = "node.rpc.enable";
  public static final String NODE_RPC_SOLIDITY_ENABLE = "node.rpc.solidityEnable";
  public static final String NODE_RPC_PBFT_ENABLE = "node.rpc.PBFTEnable";
  public static final String NODE_RPC_THREAD = "node.rpc.thread";
  public static final String NODE_RPC_MAX_CONCURRENT_CALLS_PER_CONNECTION =
      "node.rpc.maxConcurrentCallsPerConnection";
  public static final String NODE_RPC_FLOW_CONTROL_WINDOW = "node.rpc.flowControlWindow";
  public static final String NODE_RPC_MAX_CONNECTION_IDLE_IN_MILLIS =
      "node.rpc.maxConnectionIdleInMillis";
  public static final String NODE_RPC_MAX_RST_STREAM = "node.rpc.maxRstStream";
  public static final String NODE_RPC_SECONDS_PER_WINDOW = "node.rpc.secondsPerWindow";
  public static final String NODE_RPC_MAX_CONNECTION_AGE_IN_MILLIS =
      "node.rpc.maxConnectionAgeInMillis";
  public static final String NODE_RPC_MAX_MESSAGE_SIZE = "node.rpc.maxMessageSize";
  public static final String NODE_RPC_MAX_HEADER_LIST_SIZE = "node.rpc.maxHeaderListSize";
  public static final String NODE_RPC_REFLECTION_SERVICE = "node.rpc.reflectionService";
  public static final String NODE_RPC_MIN_EFFECTIVE_CONNECTION =
      "node.rpc.minEffectiveConnection";
  public static final String NODE_RPC_TRX_CACHE_ENABLE = "node.rpc.trxCacheEnable";

  // node - http
  public static final String NODE_HTTP_FULLNODE_PORT = "node.http.fullNodePort";
  public static final String NODE_HTTP_SOLIDITY_PORT = "node.http.solidityPort";
  public static final String NODE_HTTP_FULLNODE_ENABLE = "node.http.fullNodeEnable";
  public static final String NODE_HTTP_SOLIDITY_ENABLE = "node.http.solidityEnable";
  public static final String NODE_HTTP_PBFT_ENABLE = "node.http.PBFTEnable";
  public static final String NODE_HTTP_PBFT_PORT = "node.http.PBFTPort";

  // node - jsonrpc
  public static final String NODE_JSONRPC_HTTP_FULLNODE_ENABLE =
      "node.jsonrpc.httpFullNodeEnable";
  public static final String NODE_JSONRPC_HTTP_FULLNODE_PORT = "node.jsonrpc.httpFullNodePort";
  public static final String NODE_JSONRPC_HTTP_SOLIDITY_ENABLE =
      "node.jsonrpc.httpSolidityEnable";
  public static final String NODE_JSONRPC_HTTP_SOLIDITY_PORT = "node.jsonrpc.httpSolidityPort";
  public static final String NODE_JSONRPC_HTTP_PBFT_ENABLE = "node.jsonrpc.httpPBFTEnable";
  public static final String NODE_JSONRPC_HTTP_PBFT_PORT = "node.jsonrpc.httpPBFTPort";
  public static final String NODE_JSONRPC_MAX_BLOCK_RANGE = "node.jsonrpc.maxBlockRange";
  public static final String NODE_JSONRPC_MAX_SUB_TOPICS = "node.jsonrpc.maxSubTopics";
  public static final String NODE_JSONRPC_MAX_BLOCK_FILTER_NUM =
      "node.jsonrpc.maxBlockFilterNum";

  // node - dns
  public static final String NODE_DNS_TREE_URLS = "node.dns.treeUrls";
  public static final String NODE_DNS_PUBLISH = "node.dns.publish";
  public static final String NODE_DNS_DOMAIN = "node.dns.dnsDomain";
  public static final String NODE_DNS_CHANGE_THRESHOLD = "node.dns.changeThreshold";
  public static final String NODE_DNS_MAX_MERGE_SIZE = "node.dns.maxMergeSize";
  public static final String NODE_DNS_PRIVATE = "node.dns.dnsPrivate";
  public static final String NODE_DNS_KNOWN_URLS = "node.dns.knownUrls";
  public static final String NODE_DNS_STATIC_NODES = "node.dns.staticNodes";
  public static final String NODE_DNS_SERVER_TYPE = "node.dns.serverType";
  public static final String NODE_DNS_ACCESS_KEY_ID = "node.dns.accessKeyId";
  public static final String NODE_DNS_ACCESS_KEY_SECRET = "node.dns.accessKeySecret";
  public static final String NODE_DNS_ALIYUN_ENDPOINT = "node.dns.aliyunDnsEndpoint";
  public static final String NODE_DNS_AWS_REGION = "node.dns.awsRegion";
  public static final String NODE_DNS_AWS_HOST_ZONE_ID = "node.dns.awsHostZoneId";

  // node - backup
  public static final String NODE_BACKUP_PRIORITY = "node.backup.priority";
  public static final String NODE_BACKUP_PORT = "node.backup.port";
  public static final String NODE_BACKUP_KEEPALIVEINTERVAL = "node.backup.keepAliveInterval";
  public static final String NODE_BACKUP_MEMBERS = "node.backup.members";

  // node - shutdown
  public static final String NODE_SHUTDOWN_BLOCK_TIME = "node.shutdown.BlockTime";
  public static final String NODE_SHUTDOWN_BLOCK_HEIGHT = "node.shutdown.BlockHeight";
  public static final String NODE_SHUTDOWN_BLOCK_COUNT = "node.shutdown.BlockCount";

  // node - dynamic config
  public static final String DYNAMIC_CONFIG_ENABLE = "node.dynamicConfig.enable";
  public static final String DYNAMIC_CONFIG_CHECK_INTERVAL = "node.dynamicConfig.checkInterval";

  // node - unsolidified
  public static final String UNSOLIDIFIED_BLOCK_CHECK = "node.unsolidifiedBlockCheck";
  public static final String MAX_UNSOLIDIFIED_BLOCKS = "node.maxUnsolidifiedBlocks";

  // node - misc
  public static final String OPEN_PRINT_LOG = "node.openPrintLog";
  public static final String OPEN_TRANSACTION_SORT = "node.openTransactionSort";

  // committee
  public static final String COMMITTEE_ALLOW_CREATION_OF_CONTRACTS =
      "committee.allowCreationOfContracts";
  public static final String COMMITTEE_ALLOW_MULTI_SIGN = "committee.allowMultiSign";
  public static final String COMMITTEE_ALLOW_ADAPTIVE_ENERGY = "committee.allowAdaptiveEnergy";
  public static final String COMMITTEE_ALLOW_DELEGATE_RESOURCE =
      "committee.allowDelegateResource";
  public static final String COMMITTEE_ALLOW_SAME_TOKEN_NAME = "committee.allowSameTokenName";
  public static final String COMMITTEE_ALLOW_TVM_TRANSFER_TRC10 =
      "committee.allowTvmTransferTrc10";
  public static final String COMMITTEE_ALLOW_TVM_CONSTANTINOPLE =
      "committee.allowTvmConstantinople";
  public static final String COMMITTEE_ALLOW_TVM_SOLIDITY059 = "committee.allowTvmSolidity059";
  public static final String COMMITTEE_FORBID_TRANSFER_TO_CONTRACT =
      "committee.forbidTransferToContract";
  public static final String COMMITTEE_ALLOW_SHIELDED_TRC20_TRANSACTION =
      "committee.allowShieldedTRC20Transaction";
  public static final String COMMITTEE_ALLOW_TVM_ISTANBUL = "committee.allowTvmIstanbul";
  public static final String COMMITTEE_ALLOW_MARKET_TRANSACTION =
      "committee.allowMarketTransaction";
  public static final String COMMITTEE_ALLOW_PROTO_FILTER_NUM =
      "committee.allowProtoFilterNum";
  public static final String COMMITTEE_ALLOW_ACCOUNT_STATE_ROOT =
      "committee.allowAccountStateRoot";
  public static final String COMMITTEE_ALLOW_PBFT = "committee.allowPBFT";
  public static final String COMMITTEE_PBFT_EXPIRE_NUM = "committee.pBFTExpireNum";
  public static final String COMMITTEE_ALLOW_TRANSACTION_FEE_POOL =
      "committee.allowTransactionFeePool";
  public static final String COMMITTEE_ALLOW_BLACK_HOLE_OPTIMIZATION =
      "committee.allowBlackHoleOptimization";
  public static final String COMMITTEE_ALLOW_NEW_RESOURCE_MODEL =
      "committee.allowNewResourceModel";
  public static final String COMMITTEE_ALLOW_RECEIPTS_MERKLE_ROOT =
      "committee.allowReceiptsMerkleRoot";
  public static final String COMMITTEE_ALLOW_TVM_FREEZE = "committee.allowTvmFreeze";
  public static final String COMMITTEE_ALLOW_TVM_VOTE = "committee.allowTvmVote";
  public static final String COMMITTEE_UNFREEZE_DELAY_DAYS = "committee.unfreezeDelayDays";
  public static final String COMMITTEE_ALLOW_TVM_LONDON = "committee.allowTvmLondon";
  public static final String COMMITTEE_ALLOW_TVM_COMPATIBLE_EVM =
      "committee.allowTvmCompatibleEvm";
  public static final String COMMITTEE_ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX =
      "committee.allowHigherLimitForMaxCpuTimeOfOneTx";
  public static final String COMMITTEE_ALLOW_NEW_REWARD_ALGORITHM =
      "committee.allowNewRewardAlgorithm";
  public static final String COMMITTEE_ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID =
      "committee.allowOptimizedReturnValueOfChainId";
  public static final String COMMITTEE_CHANGED_DELEGATION = "committee.changedDelegation";
  public static final String COMMITTEE_ALLOW_TVM_SHANGHAI = "committee.allowTvmShangHai";
  public static final String COMMITTEE_ALLOW_OLD_REWARD_OPT = "committee.allowOldRewardOpt";
  public static final String COMMITTEE_ALLOW_ENERGY_ADJUSTMENT =
      "committee.allowEnergyAdjustment";
  public static final String COMMITTEE_ALLOW_STRICT_MATH = "committee.allowStrictMath";
  public static final String COMMITTEE_CONSENSUS_LOGIC_OPTIMIZATION =
      "committee.consensusLogicOptimization";
  public static final String COMMITTEE_ALLOW_TVM_CANCUN = "committee.allowTvmCancun";
  public static final String COMMITTEE_ALLOW_TVM_BLOB = "committee.allowTvmBlob";
  public static final String COMMITTEE_PROPOSAL_EXPIRE_TIME = "committee.proposalExpireTime";
  public static final String ALLOW_ACCOUNT_ASSET_OPTIMIZATION =
      "committee.allowAccountAssetOptimization";
  public static final String ALLOW_ASSET_OPTIMIZATION = "committee.allowAssetOptimization";
  public static final String ALLOW_NEW_REWARD = "committee.allowNewReward";
  public static final String MEMO_FEE = "committee.memoFee";
  public static final String ALLOW_DELEGATE_OPTIMIZATION =
      "committee.allowDelegateOptimization";
  public static final String ALLOW_DYNAMIC_ENERGY = "committee.allowDynamicEnergy";
  public static final String DYNAMIC_ENERGY_THRESHOLD = "committee.dynamicEnergyThreshold";
  public static final String DYNAMIC_ENERGY_INCREASE_FACTOR =
      "committee.dynamicEnergyIncreaseFactor";
  public static final String DYNAMIC_ENERGY_MAX_FACTOR = "committee.dynamicEnergyMaxFactor";

  // storage
  public static final String STORAGE_NEEDTO_UPDATE_ASSET = "storage.needToUpdateAsset";
  public static final String STORAGE_BACKUP_ENABLE = "storage.backup.enable";
  public static final String STORAGE_BACKUP_PROP_PATH = "storage.backup.propPath";
  public static final String STORAGE_BACKUP_BAK1PATH = "storage.backup.bak1path";
  public static final String STORAGE_BACKUP_BAK2PATH = "storage.backup.bak2path";
  public static final String STORAGE_BACKUP_FREQUENCY = "storage.backup.frequency";
  public static final String STORAGE_DB_SETTING = "storage.dbSettings.";
  public static final String HISTORY_BALANCE_LOOKUP = "storage.balance.history.lookup";

  // event
  public static final String EVENT_SUBSCRIBE = "event.subscribe";
  public static final String EVENT_SUBSCRIBE_FILTER = "event.subscribe.filter";
  public static final String EVENT_SUBSCRIBE_VERSION = "event.subscribe.version";
  public static final String EVENT_SUBSCRIBE_START_SYNC_BLOCK_NUM =
      "event.subscribe.startSyncBlockNum";
  public static final String EVENT_SUBSCRIBE_PATH = "event.subscribe.path";
  public static final String EVENT_SUBSCRIBE_SERVER = "event.subscribe.server";
  public static final String EVENT_SUBSCRIBE_DB_CONFIG = "event.subscribe.dbconfig";
  public static final String EVENT_SUBSCRIBE_TOPICS = "event.subscribe.topics";
  public static final String EVENT_SUBSCRIBE_FROM_BLOCK = "event.subscribe.filter.fromblock";
  public static final String EVENT_SUBSCRIBE_TO_BLOCK = "event.subscribe.filter.toblock";
  public static final String EVENT_SUBSCRIBE_CONTRACT_ADDRESS =
      "event.subscribe.filter.contractAddress";
  public static final String EVENT_SUBSCRIBE_CONTRACT_TOPIC =
      "event.subscribe.filter.contractTopic";
  public static final String USE_NATIVE_QUEUE = "event.subscribe.native.useNativeQueue";
  public static final String NATIVE_QUEUE_BIND_PORT = "event.subscribe.native.bindport";
  public static final String NATIVE_QUEUE_SEND_LENGTH =
      "event.subscribe.native.sendqueuelength";

  // rate limiter
  public static final String RATE_LIMITER = "rate.limiter";
  public static final String RATE_LIMITER_GLOBAL_QPS = "rate.limiter.global.qps";
  public static final String RATE_LIMITER_GLOBAL_IP_QPS = "rate.limiter.global.ip.qps";
  public static final String RATE_LIMITER_GLOBAL_API_QPS = "rate.limiter.global.api.qps";
  public static final String RATE_LIMITER_HTTP = "rate.limiter.http";
  public static final String RATE_LIMITER_RPC = "rate.limiter.rpc";
  public static final String RATE_LIMITER_P2P_SYNC_BLOCK_CHAIN =
      "rate.limiter.p2p.syncBlockChain";
  public static final String RATE_LIMITER_P2P_FETCH_INV_DATA = "rate.limiter.p2p.fetchInvData";
  public static final String RATE_LIMITER_P2P_DISCONNECT = "rate.limiter.p2p.disconnect";

  // metrics
  public static final String METRICS_STORAGE_ENABLE = "node.metrics.storageEnable";
  public static final String METRICS_INFLUXDB_IP = "node.metrics.influxdb.ip";
  public static final String METRICS_INFLUXDB_PORT = "node.metrics.influxdb.port";
  public static final String METRICS_INFLUXDB_DATABASE = "node.metrics.influxdb.database";
  public static final String METRICS_REPORT_INTERVAL =
      "node.metrics.influxdb.metricsReportInterval";
  public static final String METRICS_PROMETHEUS_ENABLE = "node.metrics.prometheus.enable";
  public static final String METRICS_PROMETHEUS_PORT = "node.metrics.prometheus.port";

  // seed
  public static final String SEED_NODE_IP_LIST = "seed.node.ip.list";

  // transaction
  public static final String TRX_REFERENCE_BLOCK = "trx.reference.block";
  public static final String TRX_EXPIRATION_TIME_IN_MILLIS_SECONDS =
      "trx.expiration.timeInMilliseconds";

  // energy
  public static final String ENERGY_LIMIT_BLOCK_NUM = "enery.limit.block.num";

  // actuator
  public static final String ACTUATOR_WHITELIST = "actuator.whitelist";
}
