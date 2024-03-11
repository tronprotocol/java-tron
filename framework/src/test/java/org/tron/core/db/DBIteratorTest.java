package org.tron.core.db;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.tron.common.utils.FileUtil;
import org.tron.core.db.common.iterator.RockStoreIterator;
import org.tron.core.db.common.iterator.StoreIterator;

public class DBIteratorTest {

  @BeforeClass
  public static void init() {
    File file = Paths.get("database-iterator").toFile();
    if (!file.exists()) {
      file.mkdirs();
    }
  }

  @AfterClass
  public static void clear() {
    File file = Paths.get("database-iterator").toFile();
    if (file.exists()) {
      FileUtil.deleteDir(Paths.get("database-iterator").toFile());
    }
  }


  @Test
  public void testLevelDb() throws IOException {
    File file = new File("database-iterator/testLevelDb");
    try {
      DB db = factory.open(file, new Options().createIfMissing(true));
      db.put("1".getBytes(StandardCharsets.UTF_8), "1".getBytes(StandardCharsets.UTF_8));
      db.put("2".getBytes(StandardCharsets.UTF_8), "2".getBytes(StandardCharsets.UTF_8));
      StoreIterator  iterator = new StoreIterator(db.iterator());
      while (iterator.hasNext()) {
        iterator.next();
      }
      Assert.assertFalse(iterator.hasNext());
      try {
        iterator.next();
      } catch (Exception e) {
        Assert.assertTrue(e instanceof  NoSuchElementException);
      }
      db.close();
    } finally {
      factory.destroy(file, new Options());
    }


  }

  @Test
  public void testRocksDb() throws RocksDBException {
    File file = new File("database-iterator/testRocksDb");
    try (org.rocksdb.Options options = new org.rocksdb.Options().setCreateIfMissing(true)) {
      RocksDB db = RocksDB.open(options, file.toString());
      db.put("1".getBytes(StandardCharsets.UTF_8), "1".getBytes(StandardCharsets.UTF_8));
      db.put("2".getBytes(StandardCharsets.UTF_8), "2".getBytes(StandardCharsets.UTF_8));
      RockStoreIterator iterator = new RockStoreIterator(db.newIterator());
      while (iterator.hasNext()) {
        iterator.next();
      }
      Assert.assertFalse(iterator.hasNext());
      try {
        iterator.next();
      } catch (Exception e) {
        Assert.assertTrue(e instanceof  NoSuchElementException);
      }
      db.close();
    } finally {
      RocksDB.destroyDB(file.toString(), new org.rocksdb.Options());
    }

  }

}
