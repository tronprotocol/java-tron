package org.tron.core;

public class Constant {

  //config for testnet, mainnet, beta
  public static final String TESTNET_CONF = "config.conf";

  //config for junit test
  public static final String TEST_CONF = "config-test.conf";

  // locate in storageDbDirectory, store the db infos,
  // now only has the split block number
  public static final String INFO_FILE_NAME = "info.properties";
  // the block number that split between the snapshot and history
  public static final String SPLIT_BLOCK_NUM = "split_block_num";

  public static final byte ADD_PRE_FIX_BYTE_MAINNET = (byte) 0x41;   //41 + address
  public static final String ADD_PRE_FIX_STRING_MAINNET = "41";
  public static final byte ADD_PRE_FIX_BYTE_TESTNET = (byte) 0xa0;   //a0 + address
  public static final String ADD_PRE_FIX_STRING_TESTNET = "a0";
  public static final int STANDARD_ADDRESS_SIZE = 20;
  public static final int TRON_ADDRESS_SIZE = 21;

  public static final int NODE_TYPE_FULL_NODE = 0;
  public static final int NODE_TYPE_LIGHT_NODE = 1;

  // config for transaction
  public static final long TRANSACTION_MAX_BYTE_SIZE = 500 * 1_024L;
  public static final int CREATE_ACCOUNT_TRANSACTION_MIN_BYTE_SIZE = 500;
  public static final int CREATE_ACCOUNT_TRANSACTION_MAX_BYTE_SIZE = 10000;
  public static final long MAXIMUM_TIME_UNTIL_EXPIRATION = 24 * 60 * 60 * 1_000L; //one day
  public static final long TRANSACTION_DEFAULT_EXPIRATION_TIME = 60 * 1_000L; //60 seconds
  public static final long TRANSACTION_FEE_POOL_PERIOD = 1; //1 blocks
  // config for smart contract
  public static final long SUN_PER_ENERGY = 100; // 1 us = 100 SUN = 100 * 10^-6 TRX
  public static final long ENERGY_LIMIT_IN_CONSTANT_TX = 3_000_000L; // ref: 1 us = 1 energy
  public static final long MAX_RESULT_SIZE_IN_TX = 64; // max 8 * 8 items in result
  public static final long PER_SIGN_LENGTH = 65L;
  public static final long MAX_CONTRACT_RESULT_SIZE = 2L;
  public static final long PB_DEFAULT_ENERGY_LIMIT = 0L;
  public static final long CREATOR_DEFAULT_ENERGY_LIMIT = 1000 * 10_000L;


  // Numbers
  public static final int ONE_HUNDRED = 100;
  public static final int ONE_THOUSAND = 1000;

  public static final byte[] ZTRON_EXPANDSEED_PERSONALIZATION = {'Z', 't', 'r', 'o', 'n', '_', 'E',
      'x',
      'p', 'a', 'n', 'd', 'S', 'e', 'e', 'd'};
  public static final int ZC_DIVERSIFIER_SIZE = 11;
  public static final int ZC_OUTPUT_DESC_MAX_SIZE = 10;


  /**
   * normal transaction is 0 representing normal transaction unexecuted deferred transaction is 1
   * representing unexecuted deferred transaction executing deferred transaction is 2 representing
   * executing deferred transaction
   */
  public static final int NORMALTRANSACTION = 0;
  public static final int UNEXECUTEDDEFERREDTRANSACTION = 1;
  public static final int EXECUTINGDEFERREDTRANSACTION = 2;


  // Configuration items
  public static final String NET_TYPE = "net.type";
  public static final String TESTNET = "testnet";
  public static final String LOCAL_WITNESS = "localwitness";
  public static final String LOCAL_WITNESS_ACCOUNT_ADDRESS = "localWitnessAccountAddress";
  public static final String LOCAL_WITNESS_KEYSTORE = "localwitnesskeystore";
  public static final String VM_SUPPORT_CONSTANT = "vm.supportConstant";
  public static final String VM_MAX_ENERGY_LIMIT_FOR_CONSTANT = "vm.maxEnergyLimitForConstant";
  public static final String VM_LRU_CACHE_SIZE = "vm.lruCacheSize";
  public static final String VM_MIN_TIME_RATIO = "vm.minTimeRatio";
  public static final String VM_MAX_TIME_RATIO = "vm.maxTimeRatio";
  public static final String VM_LONG_RUNNING_TIME = "vm.longRunningTime";
  public static final String VM_ESTIMATE_ENERGY = "vm.estimateEnergy";

  public static final String VM_ESTIMATE_ENERGY_MAX_RETRY = "vm.estimateEnergyMaxRetry";

  public static final String ROCKSDB = "ROCKSDB";

  public static final String GENESIS_BLOCK = "genesis.block";
  public static final String GENESIS_BLOCK_TIMESTAMP = "genesis.block.timestamp";
  public static final String GENESIS_BLOCK_PARENTHASH = "genesis.block.parentHash";
  public static final String GENESIS_BLOCK_ASSETS = "genesis.block.assets";
  public static final String GENESIS_BLOCK_WITNESSES = "genesis.block.witnesses";

  public static final String BLOCK_NEED_SYNC_CHECK = "block.needSyncCheck";
  public static final String NODE_DISCOVERY_ENABLE = "node.discovery.enable";
  public static final String NODE_DISCOVERY_PERSIST = "node.discovery.persist";
  public static final String NODE_EFFECTIVE_CHECK_ENABLE = "node.effectiveCheckEnable";
  public static final String NODE_CONNECTION_TIMEOUT = "node.connection.timeout";
  public static final String NODE_FETCH_BLOCK_TIMEOUT = "node.fetchBlock.timeout";
  public static final String NODE_CHANNEL_READ_TIMEOUT = "node.channel.read.timeout";
  public static final String NODE_MAX_CONNECTIONS = "node.maxConnections";
  public static final String NODE_MIN_CONNECTIONS = "node.minConnections";
  public static final String NODE_MIN_ACTIVE_CONNECTIONS = "node.minActiveConnections";
  public static final String NODE_SYNC_FETCH_BATCH_NUM = "node.syncFetchBatchNum";

  public static final String NODE_MAX_ACTIVE_NODES = "node.maxActiveNodes";
  public static final String NODE_MAX_ACTIVE_NODES_WITH_SAME_IP = "node.maxActiveNodesWithSameIp";
  public static final String NODE_MAX_TPS = "node.maxTps";
  public static final String NODE_CONNECT_FACTOR = "node.connectFactor";
  public static final String NODE_ACTIVE_CONNECT_FACTOR = "node.activeConnectFactor";

  public static final String NODE_MAX_CONNECTIONS_WITH_SAME_IP = "node.maxConnectionsWithSameIp";
  public static final String NODE_MIN_PARTICIPATION_RATE = "node.minParticipationRate";
  public static final String NODE_LISTEN_PORT = "node.listen.port";
  public static final String NODE_P2P_VERSION = "node.p2p.version";
  public static final String NODE_ENABLE_IPV6  = "node.enableIpv6";
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

  public static final String NODE_RPC_PORT = "node.rpc.port";
  public static final String NODE_RPC_SOLIDITY_PORT = "node.rpc.solidityPort";
  public static final String NODE_RPC_PBFT_PORT = "node.rpc.PBFTPort";
  public static final String NODE_HTTP_FULLNODE_PORT = "node.http.fullNodePort";
  public static final String NODE_HTTP_SOLIDITY_PORT = "node.http.solidityPort";
  public static final String NODE_HTTP_FULLNODE_ENABLE = "node.http.fullNodeEnable";
  public static final String NODE_HTTP_SOLIDITY_ENABLE = "node.http.solidityEnable";
  public static final String NODE_HTTP_PBFT_PORT = "node.http.PBFTPort";

  public static final String NODE_JSONRPC_HTTP_FULLNODE_ENABLE = "node.jsonrpc.httpFullNodeEnable";
  public static final String NODE_JSONRPC_HTTP_FULLNODE_PORT = "node.jsonrpc.httpFullNodePort";
  public static final String NODE_JSONRPC_HTTP_SOLIDITY_ENABLE = "node.jsonrpc.httpSolidityEnable";
  public static final String NODE_JSONRPC_HTTP_SOLIDITY_PORT = "node.jsonrpc.httpSolidityPort";
  public static final String NODE_JSONRPC_HTTP_PBFT_ENABLE = "node.jsonrpc.httpPBFTEnable";
  public static final String NODE_JSONRPC_HTTP_PBFT_PORT = "node.jsonrpc.httpPBFTPort";

  public static final String NODE_DISABLED_API_LIST = "node.disabledApi";

  public static final String NODE_RPC_THREAD = "node.rpc.thread";
  public static final String NODE_SOLIDITY_THREADS = "node.solidity.threads";

  public static final String NODE_RPC_MAX_CONCURRENT_CALLS_PER_CONNECTION = "node.rpc.maxConcurrentCallsPerConnection";
  public static final String NODE_RPC_FLOW_CONTROL_WINDOW = "node.rpc.flowControlWindow";
  public static final String NODE_RPC_MAX_CONNECTION_IDLE_IN_MILLIS = "node.rpc.maxConnectionIdleInMillis";
  public static final String NODE_PRODUCED_TIMEOUT = "node.blockProducedTimeOut";
  public static final String NODE_MAX_HTTP_CONNECT_NUMBER = "node.maxHttpConnectNumber";

  public static final String NODE_NET_MAX_TRX_PER_SECOND = "node.netMaxTrxPerSecond";
  public static final String NODE_RPC_MAX_CONNECTION_AGE_IN_MILLIS = "node.rpc.maxConnectionAgeInMillis";
  public static final String NODE_RPC_MAX_MESSAGE_SIZE = "node.rpc.maxMessageSize";

  public static final String NODE_RPC_MAX_HEADER_LIST_SIZE = "node.rpc.maxHeaderListSize";

  public static final String NODE_RPC_REFLECTION_SERVICE = "node.rpc.reflectionService";

  public static final String NODE_OPEN_HISTORY_QUERY_WHEN_LITEFN = "node.openHistoryQueryWhenLiteFN";

  public static final String BLOCK_MAINTENANCE_TIME_INTERVAL = "block.maintenanceTimeInterval";
  public static final String BLOCK_PROPOSAL_EXPIRE_TIME = "block.proposalExpireTime";

  public static final String BLOCK_CHECK_FROZEN_TIME = "block.checkFrozenTime";

  public static final String COMMITTEE_ALLOW_CREATION_OF_CONTRACTS = "committee.allowCreationOfContracts";

  public static final String COMMITTEE_ALLOW_MULTI_SIGN = "committee.allowMultiSign";

  public static final String COMMITTEE_ALLOW_ADAPTIVE_ENERGY = "committee.allowAdaptiveEnergy";

  public static final String COMMITTEE_ALLOW_DELEGATE_RESOURCE = "committee.allowDelegateResource";

  public static final String COMMITTEE_ALLOW_SAME_TOKEN_NAME = "committee.allowSameTokenName";

  public static final String COMMITTEE_ALLOW_TVM_TRANSFER_TRC10 = "committee.allowTvmTransferTrc10";

  public static final String COMMITTEE_ALLOW_TVM_CONSTANTINOPLE = "committee.allowTvmConstantinople";

  public static final String COMMITTEE_ALLOW_TVM_SOLIDITY059 = "committee.allowTvmSolidity059";

  public static final String COMMITTEE_FORBID_TRANSFER_TO_CONTRACT = "committee.forbidTransferToContract";

  public static final String NODE_TCP_NETTY_WORK_THREAD_NUM = "node.tcpNettyWorkThreadNum";

  public static final String NODE_UDP_NETTY_WORK_THREAD_NUM = "node.udpNettyWorkThreadNum";

  public static final String NODE_TRUST_NODE = "node.trustNode";

  public static final String NODE_VALIDATE_SIGN_THREAD_NUM = "node.validateSignThreadNum";

  public static final String NODE_WALLET_EXTENSION_API = "node.walletExtensionApi";

  public static final String NODE_RECEIVE_TCP_MIN_DATA_LENGTH = "node.receiveTcpMinDataLength";

  public static final String NODE_IS_OPEN_FULL_TCP_DISCONNECT = "node.isOpenFullTcpDisconnect";

  public static final String NODE_INACTIVE_THRESHOLD = "node.inactiveThreshold";

  public static final String NODE_DETECT_ENABLE = "node.nodeDetectEnable";

  public static final String NODE_MAX_TRANSACTION_PENDING_SIZE = "node.maxTransactionPendingSize";

  public static final String NODE_PENDING_TRANSACTION_TIMEOUT = "node.pendingTransactionTimeout";

  public static final String STORAGE_NEEDTO_UPDATE_ASSET = "storage.needToUpdateAsset";

  public static final String TRX_REFERENCE_BLOCK = "trx.reference.block";

  public static final String TRX_EXPIRATION_TIME_IN_MILLIS_SECONDS = "trx.expiration.timeInMilliseconds";

  public static final String NODE_RPC_MIN_EFFECTIVE_CONNECTION = "node.rpc.minEffectiveConnection";

  public static final String NODE_RPC_TRX_CACHE_ENABLE = "node.rpc.trxCacheEnable";

  public static final String ENERGY_LIMIT_BLOCK_NUM = "enery.limit.block.num";

  public static final String VM_TRACE = "vm.vmTrace";

  public static final String VM_SAVE_INTERNAL_TX = "vm.saveInternalTx";

  public static final String VM_SAVE_FEATURED_INTERNAL_TX = "vm.saveFeaturedInternalTx";

  // public static final String COMMITTEE_ALLOW_SHIELDED_TRANSACTION = "committee.allowShieldedTransaction";

  public static final String COMMITTEE_ALLOW_SHIELDED_TRC20_TRANSACTION = "committee"
      + ".allowShieldedTRC20Transaction";

  public static final String COMMITTEE_ALLOW_TVM_ISTANBUL = "committee"
      + ".allowTvmIstanbul";

  public static final String COMMITTEE_ALLOW_MARKET_TRANSACTION =
      "committee.allowMarketTransaction";

  public static final String EVENT_SUBSCRIBE = "event.subscribe";

  public static final String EVENT_SUBSCRIBE_FILTER = "event.subscribe.filter";

  public static final String NODE_FULLNODE_ALLOW_SHIELDED_TRANSACTION = "node"
      + ".fullNodeAllowShieldedTransaction";

  public static final String NODE_ZEN_TOKENID = "node.zenTokenId";

  public static final String COMMITTEE_ALLOW_PROTO_FILTER_NUM = "committee.allowProtoFilterNum";

  public static final String COMMITTEE_ALLOW_ACCOUNT_STATE_ROOT = "committee.allowAccountStateRoot";

  public static final String NODE_VALID_CONTRACT_PROTO_THREADS = "node.validContractProto.threads";

  public static final String NODE_ACTIVE = "node.active";

  public static final String NODE_PASSIVE = "node.passive";

  public static final String NODE_FAST_FORWARD = "node.fastForward";

  public static final String NODE_MAX_FAST_FORWARD_NUM = "node.maxFastForwardNum";

  public static final String NODE_SHIELDED_TRANS_IN_PENDING_MAX_COUNTS = "node.shieldedTransInPendingMaxCounts";

  public static final String RATE_LIMITER = "rate.limiter";

  public static final String RATE_LIMITER_GLOBAL_QPS = "rate.limiter.global.qps";

  public static final String RATE_LIMITER_GLOBAL_IP_QPS = "rate.limiter.global.ip.qps";

  public static final String RATE_LIMITER_GLOBAL_API_QPS = "rate.limiter.global.api.qps";

  public static final String COMMITTEE_CHANGED_DELEGATION = "committee.changedDelegation";

  public static final String CRYPTO_ENGINE = "crypto.engine";

  public static final String ECKey_ENGINE = "ECKey";

  public static final String USE_NATIVE_QUEUE = "event.subscribe.native.useNativeQueue";

  public static final String NATIVE_QUEUE_BIND_PORT = "event.subscribe.native.bindport";

  public static final String NATIVE_QUEUE_SEND_LENGTH = "event.subscribe.native.sendqueuelength";

  public static final String EVENT_SUBSCRIBE_PATH = "event.subscribe.path";
  public static final String EVENT_SUBSCRIBE_SERVER = "event.subscribe.server";
  public static final String EVENT_SUBSCRIBE_DB_CONFIG = "event.subscribe.dbconfig";
  public static final String EVENT_SUBSCRIBE_TOPICS = "event.subscribe.topics";
  public static final String EVENT_SUBSCRIBE_FROM_BLOCK = "event.subscribe.filter.fromblock";
  public static final String EVENT_SUBSCRIBE_TO_BLOCK = "event.subscribe.filter.toblock";
  public static final String EVENT_SUBSCRIBE_CONTRACT_ADDRESS = "event.subscribe.filter.contractAddress";
  public static final String EVENT_SUBSCRIBE_CONTRACT_TOPIC = "event.subscribe.filter.contractTopic";

  public static final String NODE_DISCOVERY_EXTERNAL_IP = "node.discovery.external.ip";

  public static final String NODE_BACKUP_PRIORITY = "node.backup.priority";
  public static final String NODE_BACKUP_PORT = "node.backup.port";
  public static final String NODE_BACKUP_KEEPALIVEINTERVAL = "node.backup.keepAliveInterval";
  public static final String NODE_BACKUP_MEMBERS = "node.backup.members";

  public static final String STORAGE_BACKUP_ENABLE = "storage.backup.enable";
  public static final String STORAGE_BACKUP_PROP_PATH = "storage.backup.propPath";
  public static final String STORAGE_BACKUP_BAK1PATH = "storage.backup.bak1path";
  public static final String STORAGE_BACKUP_BAK2PATH = "storage.backup.bak2path";
  public static final String STORAGE_BACKUP_FREQUENCY = "storage.backup.frequency";
  public static final String STORAGE_DB_SETTING = "storage.dbSettings.";

  public static final String ACTUATOR_WHITELIST = "actuator.whitelist";

  public static final String RATE_LIMITER_HTTP = "rate.limiter.http";
  public static final String RATE_LIMITER_RPC = "rate.limiter.rpc";

  public static final String SEED_NODE_IP_LIST = "seed.node.ip.list";
  public static final String NODE_METRICS_ENABLE = "node.metricsEnable";
  public static final String COMMITTEE_ALLOW_PBFT = "committee.allowPBFT";
  public static final String COMMITTEE_PBFT_EXPIRE_NUM = "committee.pBFTExpireNum";
  public static final String NODE_AGREE_NODE_COUNT = "node.agreeNodeCount";

  public static final String COMMITTEE_ALLOW_TRANSACTION_FEE_POOL = "committee.allowTransactionFeePool";
  public static final String COMMITTEE_ALLOW_BLACK_HOLE_OPTIMIZATION = "committee.allowBlackHoleOptimization";
  public static final String COMMITTEE_ALLOW_NEW_RESOURCE_MODEL = "committee.allowNewResourceModel";
  public static final String COMMITTEE_ALLOW_RECEIPTS_MERKLE_ROOT = "committee.allowReceiptsMerkleRoot";

  public static final String COMMITTEE_ALLOW_TVM_FREEZE = "committee.allowTvmFreeze";
  public static final String COMMITTEE_ALLOW_TVM_VOTE = "committee.allowTvmVote";
  public static final String COMMITTEE_UNFREEZE_DELAY_DAYS = "committee.unfreezeDelayDays";

  public static final String COMMITTEE_ALLOW_TVM_LONDON = "committee.allowTvmLondon";
  public static final String COMMITTEE_ALLOW_TVM_COMPATIBLE_EVM = "committee.allowTvmCompatibleEvm";
  public static final String COMMITTEE_ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX =
      "committee.allowHigherLimitForMaxCpuTimeOfOneTx";
  public static final String COMMITTEE_ALLOW_NEW_REWARD_ALGORITHM = "committee.allowNewRewardAlgorithm";
  public static final String COMMITTEE_ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID =
      "committee.allowOptimizedReturnValueOfChainId";


  public static final String METRICS_STORAGE_ENABLE = "node.metrics.storageEnable";
  public static final String METRICS_INFLUXDB_IP = "node.metrics.influxdb.ip";
  public static final String METRICS_INFLUXDB_PORT = "node.metrics.influxdb.port";
  public static final String METRICS_INFLUXDB_DATABASE = "node.metrics.influxdb.database";
  public static final String METRICS_REPORT_INTERVAL = "node.metrics.influxdb.metricsReportInterval";
  public static final String METRICS_PROMETHEUS_ENABLE = "node.metrics.prometheus.enable";
  public static final String METRICS_PROMETHEUS_PORT = "node.metrics.prometheus.port";

  public static final String HISTORY_BALANCE_LOOKUP = "storage.balance.history.lookup";
  public static final String OPEN_PRINT_LOG = "node.openPrintLog";
  public static final String OPEN_TRANSACTION_SORT = "node.openTransactionSort";

  public static final String ALLOW_ACCOUNT_ASSET_OPTIMIZATION = "committee.allowAccountAssetOptimization";
  public static final String ALLOW_ASSET_OPTIMIZATION = "committee.allowAssetOptimization";
  public static final String ALLOW_NEW_REWARD = "committee.allowNewReward";
  public static final String MEMO_FEE = "committee.memoFee";
  public static final String ALLOW_DELEGATE_OPTIMIZATION = "committee.allowDelegateOptimization";

  public static final String ALLOW_DYNAMIC_ENERGY = "committee.allowDynamicEnergy";

  public static final String DYNAMIC_ENERGY_THRESHOLD = "committee.dynamicEnergyThreshold";

  public static final String DYNAMIC_ENERGY_INCREASE_FACTOR
      = "committee.dynamicEnergyIncreaseFactor";

  public static final String DYNAMIC_ENERGY_MAX_FACTOR = "committee.dynamicEnergyMaxFactor";

  public static final long DYNAMIC_ENERGY_FACTOR_DECIMAL = 10_000L;

  public static final long DYNAMIC_ENERGY_INCREASE_FACTOR_RANGE = 10_000L;

  public static final long DYNAMIC_ENERGY_MAX_FACTOR_RANGE = 100_000L;

  public static final int DYNAMIC_ENERGY_DECREASE_DIVISION = 4;

  public static final String LOCAL_HOST = "127.0.0.1";

  public static final String NODE_SHUTDOWN_BLOCK_TIME = "node.shutdown.BlockTime";
  public static final String NODE_SHUTDOWN_BLOCK_HEIGHT = "node.shutdown.BlockHeight";
  public static final String NODE_SHUTDOWN_BLOCK_COUNT = "node.shutdown.BlockCount";

  public static final String BLOCK_CACHE_TIMEOUT = "node.blockCacheTimeout";

  public static final String DYNAMIC_CONFIG_ENABLE = "node.dynamicConfig.enable";
  public static final String DYNAMIC_CONFIG_CHECK_INTERVAL = "node.dynamicConfig.checkInterval";

  public static final String COMMITTEE_ALLOW_TVM_SHANGHAI = "committee.allowTvmShangHai";

  public static final String UNSOLIDIFIED_BLOCK_CHECK = "node.unsolidifiedBlockCheck";

  public static final String MAX_UNSOLIDIFIED_BLOCKS = "node.maxUnsolidifiedBlocks";
  public static final String COMMITTEE_ALLOW_OLD_REWARD_OPT = "committee.allowOldRewardOpt";

  public static final String COMMITTEE_ALLOW_ENERGY_ADJUSTMENT = "committee.allowEnergyAdjustment";
  public static final String COMMITTEE_ALLOW_STRICT_MATH = "committee.allowStrictMath";

  public static final String COMMITTEE_CONSENSUS_LOGIC_OPTIMIZATION
      = "committee.consensusLogicOptimization";
}
