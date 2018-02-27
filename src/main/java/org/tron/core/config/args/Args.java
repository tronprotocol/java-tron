package org.tron.core.config.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.typesafe.config.ConfigObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Args {

  private static final Args INSTANCE = new Args();


  @Parameter(names = {"-d", "--output-directory"}, description = "Directory")
  private String outputDirectory = new String("");

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

    INSTANCE.genesisBlock = new GenesisBlock();
    INSTANCE.genesisBlock.setTimeStamp(config.getString("genesis.block.timestamp"));
    INSTANCE.genesisBlock.setParentHash(config.getString("genesis.block.parentHash"));
    INSTANCE.genesisBlock.setHash(config.getString("genesis.block.hash"));
    INSTANCE.genesisBlock.setNumber(config.getString("genesis.block.number"));

    if (config.hasPath("genesis.block.transactions")) {
      List<? extends ConfigObject> trx = config.getObjectList("genesis.block.transactions");

      List<SeedNodeAddress> seedNodeAddresses = new ArrayList<>();
      trx.forEach(t -> {
        SeedNodeAddress seedNodeAddress = new SeedNodeAddress();
        seedNodeAddress.setAddress(t.get("address").toString());
        seedNodeAddress.setBalance(t.get("balance").toString());
        seedNodeAddresses.add(seedNodeAddress);
      });

      INSTANCE.genesisBlock.setTransactions(seedNodeAddresses);
    }
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
}
