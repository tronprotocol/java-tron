package org.tron.core.db;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.lang.reflect.Field;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDB;
import org.tron.common.TestConstants;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.core.config.args.Args;
import org.tron.core.db.common.DbSourceInter;
import org.tron.core.store.CheckPointV2Store;

public class CheckPointV2StoreTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  static {
    RocksDB.loadLibrary();
  }

  @BeforeClass
  public static void initArgs() throws IOException {
    Args.setParam(
        new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF
    );
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  @Test
  public void testStubMethods() throws Exception {
    CheckPointV2Store store = new CheckPointV2Store("test-stubs");
    try {
      byte[] key = "key".getBytes();

      store.put(key, new byte[]{});
      Assert.assertNull(store.get(key));
      Assert.assertFalse(store.has(key));
      store.forEach(item -> {
      });
      Assert.assertNull(store.spliterator());

      Field dbSourceField = TronDatabase.class.getDeclaredField("dbSource");
      dbSourceField.setAccessible(true);
      DbSourceInter<byte[]> originalDbSource =
          (DbSourceInter<byte[]>) dbSourceField.get(store);
      DbSourceInter<byte[]> mockDbSource = mock(DbSourceInter.class);
      dbSourceField.set(store, mockDbSource);
      store.delete(key);
      dbSourceField.set(store, originalDbSource);

      java.lang.reflect.Method initMethod =
          CheckPointV2Store.class.getDeclaredMethod("init");
      initMethod.setAccessible(true);
      initMethod.invoke(store);
    } finally {
      store.close();
    }
  }

  @Test
  public void testCloseWithRealResources() {
    CheckPointV2Store store = new CheckPointV2Store("test-close-real");
    // Exercises the real writeOptions.close() and dbSource.closeDB() code paths
    store.close();
  }

  @Test
  public void testCloseReleasesAllResources() throws Exception {
    CheckPointV2Store store = new CheckPointV2Store("test-close");

    // Replace dbSource with a mock so we can verify closeDB()
    Field dbSourceField = TronDatabase.class.getDeclaredField("dbSource");
    dbSourceField.setAccessible(true);
    DbSourceInter<byte[]> originalDbSource = (DbSourceInter<byte[]>) dbSourceField.get(store);
    DbSourceInter<byte[]> mockDbSource = mock(DbSourceInter.class);
    dbSourceField.set(store, mockDbSource);

    try {
      store.close();

      verify(mockDbSource).closeDB();
    } finally {
      originalDbSource.closeDB();
    }
  }

  @Test
  public void testCloseWhenDbSourceThrows() throws Exception {
    CheckPointV2Store store = new CheckPointV2Store("test-close-dbsource-throws");

    Field dbSourceField = TronDatabase.class.getDeclaredField("dbSource");
    dbSourceField.setAccessible(true);
    DbSourceInter<byte[]> originalDbSource = (DbSourceInter<byte[]>) dbSourceField.get(store);
    DbSourceInter<byte[]> mockDbSource = mock(DbSourceInter.class);
    doThrow(new RuntimeException("simulated dbSource failure")).when(mockDbSource).closeDB();
    dbSourceField.set(store, mockDbSource);

    try {
      store.close();
    } finally {
      originalDbSource.closeDB();
    }
  }

  @Test
  public void testCloseDbSourceWhenWriteOptionsThrows() throws Exception {
    CheckPointV2Store store = new CheckPointV2Store("test-close-exception");

    // Replace child writeOptions with a spy that throws on close
    Field childWriteOptionsField = CheckPointV2Store.class.getDeclaredField("writeOptions");
    childWriteOptionsField.setAccessible(true);
    WriteOptionsWrapper childWriteOptions =
        (WriteOptionsWrapper) childWriteOptionsField.get(store);
    WriteOptionsWrapper spyChildWriteOptions = spy(childWriteOptions);
    doThrow(new RuntimeException("simulated writeOptions failure"))
        .when(spyChildWriteOptions).close();
    childWriteOptionsField.set(store, spyChildWriteOptions);

    // Replace parent writeOptions with a spy that throws on close
    Field parentWriteOptionsField = TronDatabase.class.getDeclaredField("writeOptions");
    parentWriteOptionsField.setAccessible(true);
    WriteOptionsWrapper parentWriteOptions =
        (WriteOptionsWrapper) parentWriteOptionsField.get(store);
    WriteOptionsWrapper spyParentWriteOptions = spy(parentWriteOptions);
    doThrow(new RuntimeException("simulated parent writeOptions failure"))
        .when(spyParentWriteOptions).close();
    parentWriteOptionsField.set(store, spyParentWriteOptions);

    // Replace dbSource with a mock
    Field dbSourceField = TronDatabase.class.getDeclaredField("dbSource");
    dbSourceField.setAccessible(true);
    DbSourceInter<byte[]> originalDbSource = (DbSourceInter<byte[]>) dbSourceField.get(store);
    DbSourceInter<byte[]> mockDbSource = mock(DbSourceInter.class);
    dbSourceField.set(store, mockDbSource);

    try {
      store.close();

      // dbSource.closeDB() must be called even though both writeOptions threw
      verify(mockDbSource).closeDB();
    } finally {
      originalDbSource.closeDB();
    }
  }
}
