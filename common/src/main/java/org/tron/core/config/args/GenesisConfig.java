package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Genesis block configuration bean.
 * Field names match config.conf keys under "genesis.block".
 * Assets and witnesses are stored as raw bean lists; address decoding
 * (e.g. Base58Check) is done in the bridge method, not here.
 */
@Slf4j
@Getter
@Setter
public class GenesisConfig {

  private String timestamp = "";
  private String parentHash = "";
  private List<AssetConfig> assets = new ArrayList<>();
  private List<WitnessConfig> witnesses = new ArrayList<>();

  @Getter
  @Setter
  public static class AssetConfig {
    private String accountName = "";
    private String accountType = "";
    private String address = "";
    private String balance = "";
  }

  @Getter
  @Setter
  public static class WitnessConfig {
    private String address = "";
    private String url = "";
    private long voteCount = 0;
  }

  private static final Config DEFAULTS = ConfigFactory.parseString(
      "timestamp = \"\"\n"
          + "parentHash = \"\"\n"
          + "assets = []\n"
          + "witnesses = []\n"
  );

  public static GenesisConfig fromConfig(Config config) {
    if (!config.hasPath("genesis.block")) {
      return new GenesisConfig();
    }
    Config section = config.getConfig("genesis.block").withFallback(DEFAULTS);
    return ConfigBeanFactory.create(section, GenesisConfig.class);
  }
}
