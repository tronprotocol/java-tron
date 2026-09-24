package org.tron.core.config.args;

import com.typesafe.config.Config;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.Constant;

/**
 * Miscellaneous small config domains that don't warrant their own bean class.
 * Covers: storage (partial), trx, energy, seed, and legacy crypto validation.
 *
 * <p>These use manual reads because they span multiple unrelated config.conf
 * top-level sections and some have non-standard key naming (e.g. "enery" typo).
 */
@Slf4j
@Getter
public class MiscConfig {

  private static final String LEGACY_CRYPTO_ENGINE_KEY = "crypto.engine";
  private static final String SUPPORTED_CRYPTO_ENGINE = "eckey";

  private boolean needToUpdateAsset = true;
  private boolean historyBalanceLookup = false;
  private String trxReferenceBlock = "solid";
  private long trxExpirationTimeInMilliseconds = Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME;
  private long blockNumForEnergyLimit = 4727890L;
  private List<String> seedNodeIpList = new ArrayList<>();

  public static MiscConfig fromConfig(Config config) {
    validateLegacyCryptoEngine(config);
    MiscConfig mc = new MiscConfig();

    // storage
    mc.needToUpdateAsset = !config.hasPath("storage.needToUpdateAsset")
        || config.getBoolean("storage.needToUpdateAsset");
    mc.historyBalanceLookup = config.hasPath("storage.balance.history.lookup")
        && config.getBoolean("storage.balance.history.lookup");

    // trx
    mc.trxReferenceBlock = config.hasPath("trx.reference.block")
        ? config.getString("trx.reference.block") : "solid";
    String trxExpirationKey = "trx.expiration.timeInMilliseconds";
    if (config.hasPath(trxExpirationKey)
        && config.getLong(trxExpirationKey) > 0) {
      mc.trxExpirationTimeInMilliseconds = config.getLong(trxExpirationKey);
    }

    // energy (note: config key has typo "enery" — preserved for backward compat)
    mc.blockNumForEnergyLimit = config.hasPath("enery.limit.block.num")
        ? config.getInt("enery.limit.block.num") : 4727890L;

    // seed node
    mc.seedNodeIpList = config.hasPath("seed.node.ip.list")
        ? config.getStringList("seed.node.ip.list") : new ArrayList<>();

    return mc;
  }

  private static void validateLegacyCryptoEngine(Config config) {
    if (!config.hasPathOrNull(LEGACY_CRYPTO_ENGINE_KEY)) {
      return;
    }

    Object engine = config.getIsNull(LEGACY_CRYPTO_ENGINE_KEY)
        ? null : config.getAnyRef(LEGACY_CRYPTO_ENGINE_KEY);

    if (!(engine instanceof String)
        || !SUPPORTED_CRYPTO_ENGINE.equalsIgnoreCase((String) engine)) {
      throw new IllegalArgumentException(
          "SM2/SM3 support has been removed; crypto.engine only accepts eckey. "
              + "For ECKey networks, remove the setting or use eckey. "
              + "For existing SM2/SM3 networks, remain on a compatible release until migration; "
              + "do not reuse the chain database with ECKey/SHA-256.");
    }

    logger.warn("crypto.engine is deprecated and ignored; ECKey and SHA-256 are always used");
  }
}
