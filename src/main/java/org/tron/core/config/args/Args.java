package org.tron.core.config.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.typesafe.config.ConfigObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.core.Sha256Hash;

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
        List<? extends ConfigObject> assets = config.getObjectList("genesis.block.assets");

        List<Account> accounts = new ArrayList<>();
        assets.forEach(t -> {
          Account account = new Account();
          account.setAddress(t.get("address").toString());
          account.setBalance(t.get("balance").toString());
          accounts.add(account);
        });

        INSTANCE.genesisBlock.setAssets(accounts);
      }
    } else {
      INSTANCE.genesisBlock = GenesisBlock.getDefault();
    }

    INSTANCE.chainId = Sha256Hash.wrap(Hash.sha256(ByteArray.fromObject(INSTANCE.genesisBlock)))
        .toString();
    logger.info("chain id = {}", INSTANCE.chainId);
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
