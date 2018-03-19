package org.tron.core.config.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.typesafe.config.ConfigObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Args {

  private static final Logger logger = LoggerFactory.getLogger("Args");

  private static final Args INSTANCE = new Args();

  @Parameter(names = {"-d", "--output-directory"}, description = "Directory")
  private String outputDirectory = "output-directory";

  @Parameter(names = {"-h", "--help"}, help = true, description = "Directory")
  private boolean help = false;

  @Parameter(names = {"-w", "--witness"})
  private boolean witness = false;

  @Parameter(description = "--seed-nodes")
  private List<String> seedNodes = new ArrayList<>();

  @Parameter(names = {"-p", "--private-key"}, description = "private-key")
  private String privateKey = "";

  @Parameter(names = {"--storage-directory"}, description = "Storage directory")
  private String storageDirectory = "";

  @Parameter(names = {"--overlay-port"}, description = "Overlay port")
  private int overlayPort = 0;

  private Storage storage;
  private Overlay overlay;
  private SeedNode seedNode;
  private GenesisBlock genesisBlock;
  private String chainId;
  private LocalWitnesses localWitness;
  private long blockInterval;
  private boolean needSyncCheck;

  private Args() {

  }

  /**
   * set parameters.
   */
  public static void setParam(final String[] args, final com.typesafe.config.Config config) {
    JCommander.newBuilder().addObject(INSTANCE).build().parse(args);

    if (StringUtils.isBlank(INSTANCE.privateKey) && config.hasPath("private.key")) {
      INSTANCE.privateKey = config.getString("private.key");

      if (INSTANCE.privateKey != null && INSTANCE.privateKey.toUpperCase().startsWith("0X")) {
        INSTANCE.privateKey = INSTANCE.privateKey.substring(2);
      }

      if (INSTANCE.privateKey != null && INSTANCE.privateKey.length() != 0
          && INSTANCE.privateKey.length() != 64) {
        throw new IllegalArgumentException(
            "Private key(" + INSTANCE.privateKey + ") must be 64-bits hex string.");
      }
    }
    logger.info("private.key = {}", INSTANCE.privateKey);

    INSTANCE.storage = new Storage();
    INSTANCE.storage.setDirectory(Optional.ofNullable(INSTANCE.storageDirectory)
        .filter(StringUtils::isNotEmpty)
        .orElse(config.getString("storage.directory")));

    INSTANCE.overlay = new Overlay();
    INSTANCE.overlay.setPort(Optional.ofNullable(INSTANCE.overlayPort)
        .filter(i -> 0 != i)
        .orElse(config.getInt("overlay.port")));

    INSTANCE.seedNode = new SeedNode();
    INSTANCE.seedNode.setIpList(Optional.ofNullable(INSTANCE.seedNodes)
        .filter(seedNode -> 0 != seedNode.size())
        .orElse(config.getStringList("seed.node.ip.list")));

    if (config.hasPath("localwitness")) {
      INSTANCE.localWitness = new LocalWitnesses();
      List<String> localwitness = config.getStringList("localwitness");
      if (localwitness.size() > 1) {
        logger.warn("localwitness size must be one,get the first one");
        localwitness = localwitness.subList(0, 1);
      }
      INSTANCE.localWitness.setPrivateKeys(localwitness);
    }

    if (config.hasPath("genesis.block")) {
      INSTANCE.genesisBlock = new GenesisBlock();

      INSTANCE.genesisBlock.setTimestamp(config.getString("genesis.block.timestamp"));
      INSTANCE.genesisBlock.setParentHash(config.getString("genesis.block.parentHash"));

      if (config.hasPath("genesis.block.assets")) {
        INSTANCE.genesisBlock.setAssets(getAccountsFromConfig(config));
      }
      if (config.hasPath("genesis.block.witnesses")) {
        INSTANCE.genesisBlock.setWitnesses(getWitnessesFromConfig(config));
      }
    } else {
      INSTANCE.genesisBlock = GenesisBlock.getDefault();
    }
    INSTANCE.blockInterval = config.getLong("block.interval");
    INSTANCE.needSyncCheck = config.getBoolean("block.needSyncCheck");
  }


  private static List<Witness> getWitnessesFromConfig(final com.typesafe.config.Config config) {
    return config.getObjectList("genesis.block.witnesses").stream()
        .map(Args::createWitness)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static Witness createWitness(final ConfigObject witnessAccount) {
    final Witness witness = new Witness();
    witness.setAddress(witnessAccount.get("address").unwrapped().toString());
    witness.setUrl(witnessAccount.get("url").unwrapped().toString());
    witness.setVoteCount(witnessAccount.toConfig().getLong("voteCount"));
    return witness;
  }

  private static List<Account> getAccountsFromConfig(final com.typesafe.config.Config config) {
    return config.getObjectList("genesis.block.assets").stream()
        .map(Args::createAccount)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static Account createAccount(final ConfigObject asset) {
    final Account account = new Account();
    account.setAccountName(asset.get("accountName").unwrapped().toString());
    account.setAccountType(asset.get("accountType").unwrapped().toString());
    account.setAddress(asset.get("address").unwrapped().toString());
    account.setBalance(asset.get("balance").unwrapped().toString());
    return account;
  }

  public static Args getInstance() {
    return INSTANCE;
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

  public boolean isHelp() {
    return this.help;
  }

  public List<String> getSeedNodes() {
    return this.seedNodes;
  }

  public String getPrivateKey() {
    return this.privateKey;
  }

  public Storage getStorage() {
    return this.storage;
  }

  public Overlay getOverlay() {
    return this.overlay;
  }

  public SeedNode getSeedNode() {
    return this.seedNode;
  }

  public GenesisBlock getGenesisBlock() {
    return this.genesisBlock;
  }

  public String getChainId() {
    return this.chainId;
  }

  public void setChainId(final String chainId) {
    this.chainId = chainId;
  }

  public boolean isWitness() {
    return this.witness;
  }

  public LocalWitnesses getLocalWitnesses() {
    return this.localWitness;
  }

  public void setLocalWitness(final LocalWitnesses localWitness) {
    this.localWitness = localWitness;
  }

  public long getBlockInterval() {
    return this.blockInterval;
  }

  public void setBlockInterval(final long blockInterval) {
    this.blockInterval = blockInterval;
  }

  public boolean isNeedSyncCheck() {
    return needSyncCheck;
  }

  public void setNeedSyncCheck(boolean needSyncCheck) {
    this.needSyncCheck = needSyncCheck;
  }
}
