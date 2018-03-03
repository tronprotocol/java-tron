package org.tron.core.config.args;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.typesafe.config.ConfigObject;

public class Args {

  private static final Logger logger = LoggerFactory.getLogger("Args");

  private static final Args INSTANCE = new Args();

  @Parameter(names = {"-d", "--output-directory"}, description = "Directory")
  private String outputDirectory = new String("output-directory");

  @Parameter(names = {"-h", "--help"}, help = true, description = "Directory")
  private boolean help = false;

  @Parameter(description = "--seed-nodes")
  private List<String> seedNodes = new ArrayList<>();

  @Parameter(names = {"-p", "--private-key"}, description = "private-key")
  private String privateKey = new String("");

  @Parameter(names = {"--storage-directory"}, description = "Storage directory")
  private String storageDirectory = new String("");

  @Parameter(names = {"--overlay-port"}, description = "Overlay port")
  private int overlayPort = 0;

  private Storage storage;
  private Overlay overlay;
  private SeedNode seedNode;
  private GenesisBlock genesisBlock;
  private String chainId;

  private Args() {

  }

  /**
   * set parameters.
   */
  public static void setParam(String[] args, com.typesafe.config.Config config) {
    JCommander.newBuilder().addObject(INSTANCE).build().parse(args);

    INSTANCE.storage = new Storage();
    INSTANCE.storage.setDirectory(config.getString("storage.directory"));
    if (!INSTANCE.storageDirectory.isEmpty()) {
      INSTANCE.storage.setDirectory(INSTANCE.storageDirectory);
    }

    INSTANCE.overlay = new Overlay();
    INSTANCE.overlay.setPort(config.getInt("overlay.port"));
    if (INSTANCE.overlayPort != 0) {
      INSTANCE.overlay.setPort(INSTANCE.overlayPort);
    }

    INSTANCE.seedNode = new SeedNode();
    INSTANCE.seedNode.setIpList(config.getStringList("seed.node.ip.list"));
    if (INSTANCE.seedNodes.size() != 0) {
      INSTANCE.seedNode.setIpList(INSTANCE.seedNodes);
    }

    if (config.hasPath("genesis.block")) {
      INSTANCE.genesisBlock = new GenesisBlock();

      INSTANCE.genesisBlock.setTimeStamp(config.getString("genesis.block.timestamp"));
      INSTANCE.genesisBlock.setParentHash(config.getString("genesis.block.parentHash"));
      INSTANCE.genesisBlock.setHash(config.getString("genesis.block.hash"));
      INSTANCE.genesisBlock.setNumber(config.getString("genesis.block.number"));

      if (config.hasPath("genesis.block.assets")) {
        List<Account> accounts = getAccountsFromConfig(config);

        INSTANCE.genesisBlock.setAssets(accounts);
      }
    } else {
      INSTANCE.genesisBlock = GenesisBlock.getDefault();
    }
  }

  private static List<Account> getAccountsFromConfig(com.typesafe.config.Config config) {
    List<? extends ConfigObject> assets = config.getObjectList("genesis.block.assets");

    List<Account> accounts = new ArrayList<>();
    assets.forEach(asset -> accounts.add(createAccount(asset)));
    return accounts;
  }

  private static Account createAccount(ConfigObject asset) {
    Account account = new Account();
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
    if (!outputDirectory.equals("") && !outputDirectory.endsWith(File.separator)) {
      return outputDirectory + File.separator;
    }
    return outputDirectory;
  }

  public boolean isHelp() {
    return help;
  }

  public List<String> getSeedNodes() {
    return seedNodes;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public Storage getStorage() {
    return storage;
  }

  public Overlay getOverlay() {
    return overlay;
  }

  public SeedNode getSeedNode() {
    return seedNode;
  }

  public GenesisBlock getGenesisBlock() {
    return genesisBlock;
  }

  public String getChainId() {
    return chainId;
  }

  public void setChainId(String chainId) {
    this.chainId = chainId;
  }
}
