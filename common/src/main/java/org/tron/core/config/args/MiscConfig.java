package org.tron.core.config.args;

import com.typesafe.config.Config;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.Constant;

/**
 * Miscellaneous small config domains that don't warrant their own bean class.
 * Covers: storage (partial), trx, energy, crypto, seed.
 *
 * <p>These use manual reads because they span multiple unrelated config.conf
 * top-level sections and some have non-standard key naming (e.g. "enery" typo).
 */
@Slf4j
@Getter
public class MiscConfig {

  private boolean needToUpdateAsset = true;
  private boolean historyBalanceLookup = false;
  private String trxReferenceBlock = "solid";
  private long trxExpirationTimeInMilliseconds = Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME;
  private long blockNumForEnergyLimit = 4727890L;
  private String cryptoEngine = Constant.ECKey_ENGINE;
  private List<String> seedNodeIpList = new ArrayList<>();

  public static MiscConfig fromConfig(Config config) {
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

    // crypto
    mc.cryptoEngine = config.hasPath("crypto.engine")
        ? config.getString("crypto.engine") : Constant.ECKey_ENGINE;

    // seed node
    mc.seedNodeIpList = config.hasPath("seed.node.ip.list")
        ? config.getStringList("seed.node.ip.list") : new ArrayList<>();

    return mc;
  }
}
