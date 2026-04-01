package org.tron.core.db;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.TestConstants;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.core.config.args.Args;
import org.tron.core.db.common.DbSourceInter;
import org.tron.core.store.CheckPointV2Store;

/**
 * Verifies that close() methods properly release all resources even when
 * one resource's close() throws an exception.
 */
public class ResourceCloseTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  static {
    org.rocksdb.RocksDB.loadLibrary();
  }

  @BeforeClass
  public static void init() throws Exception {
    Args.setParam(
        new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  @Test
  public void testTronDatabase_closeDbSource_whenWriteOptionsThrows() throws Exception {
    CheckPointV2Store store = new CheckPointV2Store("test-close-safety");

    // Get parent class fields via reflection
    Field writeOptionsField = TronDatabase.class.getDeclaredField("writeOptions");
    writeOptionsField.setAccessible(true);
    WriteOptionsWrapper originalWriteOptions =
        (WriteOptionsWrapper) writeOptionsField.get(store);

    Field dbSourceField = TronDatabase.class.getDeclaredField("dbSource");
    dbSourceField.setAccessible(true);
    DbSourceInter<?> originalDbSource =
        (DbSourceInter<?>) dbSourceField.get(store);

    // Replace with spies
    WriteOptionsWrapper spyWriteOptions = spy(originalWriteOptions);
    doThrow(new RuntimeException("writeOptions close failed"))
        .when(spyWriteOptions).close();
    writeOptionsField.set(store, spyWriteOptions);

    DbSourceInter<?> spyDbSource = spy(originalDbSource);
    dbSourceField.set(store, spyDbSource);

    // Also spy the child's writeOptions
    Field childWriteOptionsField = CheckPointV2Store.class.getDeclaredField("writeOptions");
    childWriteOptionsField.setAccessible(true);
    WriteOptionsWrapper childOriginal =
        (WriteOptionsWrapper) childWriteOptionsField.get(store);
    WriteOptionsWrapper spyChildWriteOptions = spy(childOriginal);
    doThrow(new RuntimeException("child writeOptions close failed"))
        .when(spyChildWriteOptions).close();
    childWriteOptionsField.set(store, spyChildWriteOptions);

    // close() should not throw, and dbSource should still be closed
    store.close();

    verify(spyChildWriteOptions).close();
    verify(spyWriteOptions).close();
    verify(spyDbSource).closeDB();
  }
}
