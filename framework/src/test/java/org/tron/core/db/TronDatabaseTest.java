package org.tron.core.db;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.rocksdb.RocksDB;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

public class TronDatabaseTest extends TronDatabase<String> {

  static {
    RocksDB.loadLibrary();
  }

  @Override
  public void put(byte[] key, String item) {

  }

  @Override
  public void delete(byte[] key) {

  }

  @Override
  public String get(byte[] key) {
    return "test";
  }

  @Override
  public boolean has(byte[] key) {
    return false;
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void TestInit() {
    TronDatabaseTest db = new TronDatabaseTest();
    Assert.assertNull(db.getDbSource());
    Assert.assertNull(db.getDbName());
  }

  @Test
  public void TestIterator() {
    TronDatabaseTest db = new TronDatabaseTest();
    thrown.expect(UnsupportedOperationException.class);
    db.iterator();
  }

  @Test
  public void TestIsNotEmpty() {
    TronDatabaseTest db = new TronDatabaseTest();
    thrown.expect(UnsupportedOperationException.class);
    db.isNotEmpty();
  }

  @Test
  public void TestGetUnchecked() {
    TronDatabaseTest db = new TronDatabaseTest();
    Assert.assertNull(db.getUnchecked("test".getBytes()));
  }

  @Test
  public void TestClose() {
    TronDatabaseTest db = new TronDatabaseTest();
    db.close();
  }

  @Test
  public void TestGetFromRoot() throws
      InvalidProtocolBufferException, BadItemException, ItemNotFoundException {
    TronDatabaseTest db = new TronDatabaseTest();
    Assert.assertEquals(db.getFromRoot("test".getBytes()),
        "test");
  }
}
