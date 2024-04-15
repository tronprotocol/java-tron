package org.tron.core.db;

import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDB;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

public class TronDatabaseTest extends TronDatabase<String> {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  static {
    RocksDB.loadLibrary();
  }

  @BeforeClass
  public static void initArgs() throws IOException {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
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
