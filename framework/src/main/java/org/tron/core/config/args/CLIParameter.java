package org.tron.core.config.args;

import com.beust.jcommander.Parameter;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;

/**
 * CLI parameter definitions parsed by JCommander.
 * Fields here have NO default values — defaults live in CommonParameter.
 * JCommander only populates fields that are explicitly passed on the
 * command line.
 *
 * <p>Parameters marked {@code @Deprecated} are scheduled for removal.
 * Use the corresponding config-file options instead.</p>
 */
@NoArgsConstructor
public class CLIParameter {

  // -- Startup parameters --

  @Parameter(names = {"-c", "--config"}, description = "Config file (default:config.conf)")
  public String shellConfFileName;

  @Parameter(names = {"-d", "--output-directory"}, description = "Data directory for the "
      + "databases (default:output-directory)")
  public String outputDirectory;

  @Parameter(names = {"--log-config"}, description = "Logback config file")
  public String logbackPath;

  @Parameter(names = {"-h", "--help"}, help = true, description = "Show help message")
  public boolean help;

  @Parameter(names = {"-v", "--version"}, description = "Output code version", help = true)
  public boolean version;

  @Parameter(names = {"-w", "--witness"}, description = "Is witness node")
  public boolean witness;

  @Parameter(names = {"-p", "--private-key"}, description = "Witness private key")
  public String privateKey;

  @Parameter(names = {"--witness-address"}, description = "witness-address")
  public String witnessAddress;

  @Parameter(names = {"--password"}, description = "password")
  public String password;

  @Parameter(names = {"--solidity"}, description = "running a solidity node for java tron")
  public boolean solidityNode;

  @Parameter(names = {"--keystore-factory"}, description = "running KeystoreFactory")
  public boolean keystoreFactory;

  @Deprecated
  @Parameter(names = {"--fast-forward"})
  public boolean fastForward;

  @Deprecated
  @Parameter(names = {"--es"}, description = "Start event subscribe server")
  public boolean eventSubscribe;

  @Parameter(names = {"--p2p-disable"}, description = "Switch for p2p module initialization. "
      + "(default: false)", arity = 1)
  public boolean p2pDisable;

  @Deprecated
  @Parameter(description = "--seed-nodes")
  public List<String> seedNodes = new ArrayList<>();

  // -- Storage parameters (deprecated, use config file instead) --

  @Deprecated
  @Parameter(names = {"--storage-db-directory"}, description = "Storage db directory")
  public String storageDbDirectory;

  @Deprecated
  @Parameter(names = {"--storage-db-engine"},
      description = "Storage db engine.(leveldb or rocksdb)")
  public String storageDbEngine;

  @Deprecated
  @Parameter(names = {"--storage-db-synchronous"},
      description = "Storage db is synchronous or not.(true or false)")
  public String storageDbSynchronous;

  @Deprecated
  @Parameter(names = {"--storage-index-directory"}, description = "Storage index directory")
  public String storageIndexDirectory;

  @Deprecated
  @Parameter(names = {"--storage-index-switch"},
      description = "Storage index switch.(on or off)")
  public String storageIndexSwitch;

  @Deprecated
  @Parameter(names = {"--storage-transactionHistory-switch"},
      description = "Storage transaction history switch.(on or off)")
  public String storageTransactionHistorySwitch;

  @Deprecated
  @Parameter(names = {"--contract-parse-enable"}, description = "Switch for contract parses in "
      + "java-tron. (default: true)")
  public String contractParseEnable;

  // -- Runtime parameters (deprecated except --debug, use config file instead) --

  @Deprecated
  @Parameter(names = {"--support-constant"}, description = "Support constant calling for TVM. "
      + "(default: false)")
  public boolean supportConstant;

  @Deprecated
  @Parameter(names = {"--max-energy-limit-for-constant"},
      description = "Max energy limit for constant calling. (default: 100,000,000)")
  public long maxEnergyLimitForConstant;

  @Deprecated
  @Parameter(names = {"--lru-cache-size"}, description = "Max LRU size for caching bytecode and "
      + "result of JUMPDEST analysis. (default: 500)")
  public int lruCacheSize;

  @Parameter(names = {"--debug"}, description = "Switch for TVM debug mode. In debug model, TVM "
      + "will not check for timeout. (default: false)")
  public boolean debug;

  @Deprecated
  @Parameter(names = {"--min-time-ratio"}, description = "Minimum CPU tolerance when executing "
      + "timeout transactions while synchronizing blocks. (default: 0.0)")
  public double minTimeRatio;

  @Deprecated
  @Parameter(names = {"--max-time-ratio"}, description = "Maximum CPU tolerance when executing "
      + "non-timeout transactions while synchronizing blocks. (default: 5.0)")
  public double maxTimeRatio;

  @Deprecated
  @Parameter(names = {"--save-internaltx"}, description = "Save internal transactions generated "
      + "during TVM execution, such as create, call and suicide. (default: false)")
  public boolean saveInternalTx;

  @Deprecated
  @Parameter(names = {"--save-featured-internaltx"}, description = "Save featured internal "
      + "transactions generated during TVM execution, such as freeze, vote and so on. "
      + "(default: false)")
  public boolean saveFeaturedInternalTx;

  @Deprecated
  @Parameter(names = {"--save-cancel-all-unfreeze-v2-details"},
      description = "Record the details of the internal transactions generated by the "
          + "CANCELALLUNFREEZEV2 opcode, such as bandwidth/energy/tronpower cancel amount. "
          + "(default: false)")
  public boolean saveCancelAllUnfreezeV2Details;

  @Deprecated
  @Parameter(names = {"--long-running-time"})
  public int longRunningTime;

  @Deprecated
  @Parameter(names = {"--max-connect-number"}, description = "Http server max connect number "
      + "(default:50)")
  public int maxHttpConnectNumber;

  @Deprecated
  @Parameter(names = {"--rpc-thread"}, description = "Num of gRPC thread")
  public int rpcThreadNum;

  @Deprecated
  @Parameter(names = {"--solidity-thread"}, description = "Num of solidity thread")
  public int solidityThreads;

  @Deprecated
  @Parameter(names = {"--validate-sign-thread"}, description = "Num of validate thread")
  public int validateSignThreadNum;

  @Deprecated
  @Parameter(names = {"--trust-node"}, description = "Trust node addr")
  public String trustNodeAddr;

  @Deprecated
  @Parameter(names = {"--history-balance-lookup"})
  public boolean historyBalanceLookup;
}
