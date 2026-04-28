package org.tron.core.db;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.lang.reflect.Field;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDB;
import org.tron.common.TestConstants;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.core.config.args.Args;
import org.tron.core.db.common.DbSourceInter;
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
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
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

  @Test
  public void testDoCloseDbSourceCalledWhenWriteOptionsThrows() throws Exception {
    TronDatabase<String> db = new TronDatabase<String>("test-do-close") {

      @Override
      public void put(byte[] key, String item) {
      }

      @Override
      public void delete(byte[] key) {
      }

      @Override
      public String get(byte[] key) {
        return null;
      }

      @Override
      public boolean has(byte[] key) {
        return false;
      }
    };

    Field writeOptionsField = TronDatabase.class.getDeclaredField("writeOptions");
    writeOptionsField.setAccessible(true);
    WriteOptionsWrapper spyWriteOptions = spy((WriteOptionsWrapper) writeOptionsField.get(db));
    doThrow(new RuntimeException("simulated writeOptions failure")).when(spyWriteOptions).close();
    writeOptionsField.set(db, spyWriteOptions);

    Field dbSourceField = TronDatabase.class.getDeclaredField("dbSource");
    dbSourceField.setAccessible(true);
    DbSourceInter<byte[]> originalDbSource = (DbSourceInter<byte[]>) dbSourceField.get(db);
    DbSourceInter<byte[]> mockDbSource = mock(DbSourceInter.class);
    dbSourceField.set(db, mockDbSource);

    try {
      db.doClose();
      verify(spyWriteOptions).close();
      verify(mockDbSource).closeDB();
    } finally {
      originalDbSource.closeDB();
    }
  }
}
