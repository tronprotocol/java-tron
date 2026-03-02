package org.tron.core.db;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.tron.core.db.common.iterator.RockStoreIterator;
import org.tron.core.db.common.iterator.StoreIterator;

public class DBIteratorTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public final ExpectedException thrown = ExpectedException.none();


  @Test
  public void testLevelDb() throws IOException {
    File file = temporaryFolder.newFolder();
    try (DB db = factory.open(file, new Options().createIfMissing(true))) {
      db.put("1".getBytes(StandardCharsets.UTF_8), "1".getBytes(StandardCharsets.UTF_8));
      db.put("2".getBytes(StandardCharsets.UTF_8), "2".getBytes(StandardCharsets.UTF_8));
      StoreIterator iterator = new StoreIterator(db.iterator());
      iterator.seekToFirst();
      Assert.assertArrayEquals("1".getBytes(StandardCharsets.UTF_8), iterator.getKey());
      Assert.assertArrayEquals("1".getBytes(StandardCharsets.UTF_8), iterator.next().getValue());
      Assert.assertTrue(iterator.hasNext());

      Assert.assertArrayEquals("2".getBytes(StandardCharsets.UTF_8), iterator.getValue());
      Assert.assertArrayEquals("2".getBytes(StandardCharsets.UTF_8), iterator.next().getKey());
      Assert.assertFalse(iterator.hasNext());

      try {
        iterator.seekToLast();
      } catch (Exception e) {
        Assert.assertTrue(e instanceof  IllegalStateException);
      }

      iterator = new StoreIterator(db.iterator());
      iterator.seekToLast();
      Assert.assertArrayEquals("2".getBytes(StandardCharsets.UTF_8), iterator.getKey());
      Assert.assertArrayEquals("2".getBytes(StandardCharsets.UTF_8), iterator.getValue());
      iterator.seekToFirst();
      while (iterator.hasNext()) {
        iterator.next();
      }
      Assert.assertFalse(iterator.hasNext());
      try {
        iterator.getKey();
      } catch (Exception e) {
        Assert.assertTrue(e instanceof IllegalStateException);
      }
      try {
        iterator.getValue();
      } catch (Exception e) {
        Assert.assertTrue(e instanceof IllegalStateException);
      }
      thrown.expect(NoSuchElementException.class);
      iterator.next();
    }


  }

  @Test
  public void testRocksDb() throws RocksDBException, IOException {
    File file = temporaryFolder.newFolder();
    try (org.rocksdb.Options options = new org.rocksdb.Options().setCreateIfMissing(true);
         RocksDB db = RocksDB.open(options, file.toString())) {
      db.put("1".getBytes(StandardCharsets.UTF_8), "1".getBytes(StandardCharsets.UTF_8));
      db.put("2".getBytes(StandardCharsets.UTF_8), "2".getBytes(StandardCharsets.UTF_8));
      RockStoreIterator iterator = new RockStoreIterator(db.newIterator());
      iterator.seekToFirst();
      Assert.assertArrayEquals("1".getBytes(StandardCharsets.UTF_8), iterator.getKey());
      Assert.assertArrayEquals("1".getBytes(StandardCharsets.UTF_8), iterator.next().getValue());
      Assert.assertTrue(iterator.hasNext());

      Assert.assertArrayEquals("2".getBytes(StandardCharsets.UTF_8), iterator.getValue());
      Assert.assertArrayEquals("2".getBytes(StandardCharsets.UTF_8), iterator.next().getKey());
      Assert.assertFalse(iterator.hasNext());

      try {
        iterator.seekToLast();
      } catch (Exception e) {
        Assert.assertTrue(e instanceof  IllegalStateException);
      }

      iterator = new RockStoreIterator(db.newIterator());
      iterator.seekToLast();
      Assert.assertArrayEquals("2".getBytes(StandardCharsets.UTF_8), iterator.getKey());
      Assert.assertArrayEquals("2".getBytes(StandardCharsets.UTF_8), iterator.getValue());
      iterator.seekToFirst();
      while (iterator.hasNext()) {
        iterator.next();
      }
      Assert.assertFalse(iterator.hasNext());
      try {
        iterator.getKey();
      } catch (Exception e) {
        Assert.assertTrue(e instanceof  IllegalStateException);
      }
      try {
        iterator.getValue();
      } catch (Exception e) {
        Assert.assertTrue(e instanceof  IllegalStateException);
      }
      thrown.expect(NoSuchElementException.class);
      iterator.next();
    }
  }

}
