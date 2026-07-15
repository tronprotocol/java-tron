package org.tron.core.db.api;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.ChainBaseManager;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.store.AccountIdIndexStore;

/**
 * One-time migration: normalize any Turkish legacy keys (containing
 * dotless-ı U+0131) to ROOT keys (with ASCII 'i') in AccountIdIndexStore.
 *
 * <p>On Turkish/Azerbaijani locales, {@code String.toLowerCase()} maps
 * uppercase 'I' to dotless-ı instead of 'i'. Nodes that ran under such
 * locales wrote different index keys, causing lookup failures.
 * This migration ensures all nodes have identical DB state regardless
 * of their locale history.
 *
 * <p>Called from {@code Manager.init()} via the standard
 * {@code DynamicPropertiesStore} flag pattern.
 *
 * @see AccountIdIndexStore
 */
@Slf4j(topic = "DB")
public class MigrateTurkishKeyHelper {

  private static final char DOTLESS_I = '\u0131'; // ı Turkish dotless-i

  private final ChainBaseManager chainBaseManager;

  public MigrateTurkishKeyHelper(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
  }

  /**
   * Scan AccountIdIndexStore for keys containing Turkish dotless-ı (U+0131),
   * replace them with ROOT-equivalent keys (ı → i), and delete the old keys.
   */
  public void doWork() {
    long start = System.currentTimeMillis();
    logger.info("Start to migrate Turkish legacy keys in AccountIdIndexStore");

    final IRevokingDB revokingDB = chainBaseManager.getAccountIdIndexStore()
        .getRevokingDB();
    long totalKeys = 0;
    List<Map.Entry<byte[], byte[]>> entriesToMigrate = new ArrayList<>();

    // Phase 1: scan for keys containing 'ı' (U+0131)
    for (Map.Entry<byte[], byte[]> entry : revokingDB) {
      totalKeys++;
      String keyStr = new String(entry.getKey(), StandardCharsets.UTF_8);
      if (keyStr.indexOf(DOTLESS_I) >= 0) {
        entriesToMigrate.add(entry);
      }
    }

    // Phase 2: for each Turkish key, write the ROOT-equivalent (if absent)
    // and delete the legacy key.
    for (Map.Entry<byte[], byte[]> entry : entriesToMigrate) {
      String keyStr = new String(entry.getKey(), StandardCharsets.UTF_8);
      byte[] rootKey = keyStr.replace(DOTLESS_I, 'i')
          .getBytes(StandardCharsets.UTF_8);
      // Only write if ROOT key doesn't already exist
      if (ArrayUtils.isEmpty(revokingDB.getUnchecked(rootKey))) {
        revokingDB.put(rootKey, entry.getValue());
      }
      revokingDB.delete(entry.getKey());
    }

    // Phase 3: mark migration as done
    chainBaseManager.getDynamicPropertiesStore().saveTurkishKeyMigrationDone(1);

    logger.info(
        "Complete the Turkish key migration, total time: {} milliseconds,"
            + " total keys: {}, migrated count: {}",
        System.currentTimeMillis() - start, totalKeys, entriesToMigrate.size());
  }
}
