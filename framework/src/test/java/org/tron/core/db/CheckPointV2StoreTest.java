package org.tron.core.db;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
import org.tron.core.store.CheckPointV2Store;

/**
 * Unit tests for {@link CheckPointV2Store}.
 */
public class CheckPointV2StoreTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  static {
    RocksDB.loadLibrary();
  }

  /**
   * Initializes test arguments before running all tests.
   *
   * @throws IOException if an I/O error occurs
   */
  @BeforeClass
  public static void initArgs() throws IOException {
    Args.setParam(
        new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF
    );
  }

  /**
   * Clears test arguments after all tests have run.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  /**
   * Tests that {@link CheckPointV2Store#close()} properly calls {@code super.close()}
   * to ensure parent resources are released.
   *
   * @throws Exception if an error occurs during the test
   */
  @Test
  public void testCloseCallsSuperClose() throws Exception {
    CheckPointV2Store store = new CheckPointV2Store("test-close-super");
    
    // Get the parent class's writeOptions field
    Field parentWriteOptionsField = TronDatabase.class.getDeclaredField("writeOptions");
    parentWriteOptionsField.setAccessible(true);
    WriteOptionsWrapper originalParentWriteOptions =
        (WriteOptionsWrapper) parentWriteOptionsField.get(store);
    
    // Save the original rocks object reference for subsequent verification
    org.rocksdb.WriteOptions originalRocks = originalParentWriteOptions.rocks;
    
    // Create a spy to monitor the parent class's writeOptions
    WriteOptionsWrapper spyParentWriteOptions = spy(originalParentWriteOptions);
    parentWriteOptionsField.set(store, spyParentWriteOptions);
    
    // Create a spy to monitor the rocks.close() method
    org.rocksdb.WriteOptions spyRocks = spy(originalRocks);
    spyParentWriteOptions.rocks = spyRocks;
    
    try {
      // Verify that the parent class's writeOptions and dbSource exist
      Assert.assertNotNull(spyParentWriteOptions);
      Assert.assertNotNull(spyRocks);
      Assert.assertNotNull(store.getDbSource());
      
      // Close the store
      store.close();
      
      // Verify that the parent class's writeOptions.close() was called (via super.close())
      verify(spyParentWriteOptions, times(1)).close();
      
      // Verify that rocks.close() was called (resources are actually closed)
      verify(spyRocks, times(1)).close();
    } finally {
      // Cleanup: restore original state to avoid cross-test contamination
      if (store != null) {
        store.close();
      }
      // Restore original writeOptions to remove spies
      parentWriteOptionsField.set(store, originalParentWriteOptions);
      // Restore original rocks reference
      originalParentWriteOptions.rocks = originalRocks;
    }
  }
}
