package org.tron.core.config.args;

import com.typesafe.config.Config;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Local witness configuration bean.
 * Reads top-level config keys: localwitness, localWitnessAccountAddress, localwitnesskeystore.
 * These are not under a sub-section — they are at the root of config.conf.
 */
@Slf4j
@Getter
public class LocalWitnessConfig {

  private List<String> privateKeys = new ArrayList<>();
  private String accountAddress = null;
  private List<String> keystores = new ArrayList<>();

  public static LocalWitnessConfig fromConfig(Config config) {
    LocalWitnessConfig lw = new LocalWitnessConfig();
    if (config.hasPath("localwitness")) {
      lw.privateKeys = config.getStringList("localwitness");
    }
    if (config.hasPath("localWitnessAccountAddress")) {
      lw.accountAddress = config.getString("localWitnessAccountAddress");
    }
    if (config.hasPath("localwitnesskeystore")) {
      lw.keystores = config.getStringList("localwitnesskeystore");
    }
    return lw;
  }
}
