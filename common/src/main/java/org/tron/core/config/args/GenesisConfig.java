package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
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

  // Defaults come from reference.conf (loaded globally via Configuration.java)

  public static GenesisConfig fromConfig(Config config) {
    Config section = config.getConfig("genesis.block");
    return ConfigBeanFactory.create(section, GenesisConfig.class);
  }
}
