package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class StorageConfigTest {

  @Test
  public void testDefaults() {
    Config empty = ConfigFactory.empty();
    StorageConfig sc = StorageConfig.fromConfig(empty);
    assertEquals("LEVELDB", sc.getDb().getEngine());
    assertFalse(sc.getDb().isSync());
    assertEquals("database", sc.getDb().getDirectory());
    assertEquals("index", sc.getIndex().getDirectory());
    assertTrue(sc.isNeedToUpdateAsset());
    assertFalse(sc.getBackup().isEnable());
    assertEquals(10000, sc.getBackup().getFrequency());
    assertEquals(7, sc.getDbSettings().getLevelNumber());
    assertEquals(5000, sc.getDbSettings().getMaxOpenFiles());
  }

  @Test
  public void testFromConfig() {
    Config config = ConfigFactory.parseString(
        "storage { db { engine = ROCKSDB, sync = true, directory = mydb },"
            + " backup { enable = true, frequency = 5000 },"
            + " dbSettings { levelNumber = 5, maxOpenFiles = 3000 } }");
    StorageConfig sc = StorageConfig.fromConfig(config);
    assertEquals("ROCKSDB", sc.getDb().getEngine());
    assertTrue(sc.getDb().isSync());
    assertEquals("mydb", sc.getDb().getDirectory());
    assertTrue(sc.getBackup().isEnable());
    assertEquals(5000, sc.getBackup().getFrequency());
    assertEquals(5, sc.getDbSettings().getLevelNumber());
    assertEquals(3000, sc.getDbSettings().getMaxOpenFiles());
  }

  @Test
  public void testCheckpointDefaults() {
    Config empty = ConfigFactory.empty();
    StorageConfig sc = StorageConfig.fromConfig(empty);
    assertEquals(1, sc.getCheckpoint().getVersion());
    assertTrue(sc.getCheckpoint().isSync());
  }

  @Test
  public void testBalanceHistoryLookup() {
    Config config = ConfigFactory.parseString(
        "storage { balance { history { lookup = true } } }");
    StorageConfig sc = StorageConfig.fromConfig(config);
    assertTrue(sc.getBalance().getHistory().isLookup());
  }
}
