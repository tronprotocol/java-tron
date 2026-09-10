package org.tron.core.db2.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.primitives.Longs;
import org.junit.Test;
import org.tron.common.BaseMethodTest;
import org.tron.core.config.args.Args;
import org.tron.core.config.args.Storage;
import org.tron.core.db2.ISession;
import org.tron.core.store.AccountAssetStore;

/** Exercises Manager startup registration, fresh bootstrap and Store routing via Spring. */
public class P66SnapshotManagerIntegrationTest extends BaseMethodTest {
  @Override
  protected void beforeContext() {
    Storage storage = Args.getInstance().getStorage();
    storage.setCommonCheckpointEnabled(true);
    storage.setP66SnapshotEnabled(true);
    storage.setStateArchiveEnabled(true);
    storage.setPathStateRootEnabled(true);
    storage.setPathStateRootEngine("LEVELDB");
    storage.setStateArchiveServingIndexEngine("LEVELDB");
  }

  @Test
  public void managerBootstrapsPhysicalModeAndRegistersRevocableAccountAssetStore() {
    SnapshotManager snapshots = context.getBean(SnapshotManager.class);
    AccountAssetStore assets = chainBaseManager.getAccountAssetStore();
    assertEquals(1, snapshots.getDbs().stream()
        .filter(db -> "account-asset".equals(db.getDbName())).count());
    byte[] key = new byte[22];
    key[0] = 0x41;
    key[21] = '1';
    assertNull(assets.get(key));
    try (ISession session = snapshots.buildSession()) {
      assets.put(key, Longs.toByteArray(19));
      assertArrayEquals(Longs.toByteArray(19), assets.get(key));
      assertNull(assets.getFromRoot(key));
    }
    assertNull(assets.get(key));
    assertTrue(dbManager.getPathStateSnapshotHead() != null);
  }
}
