/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.config.args;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.TestConstants;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StorageUtils;

public class StorageTest {

  private static final Storage storage;

  static {
    Args.setParam(new String[]{}, TestConstants.TEST_CONF);
    storage = Args.getInstance().getStorage();
    setupStorage();
  }

  /**
   * set it as following:
   *
   * properties = [
   *     {
   *       name = "account",
   *       path = "storage_directory_test",
   *       createIfMissing = true,
   *       paranoidChecks = true,
   *       verifyChecksums = true,
   *       compressionType = 1,        // compressed with snappy
   *       blockSize = 4096,           // 4  KB =         4 * 1024 B
   *       writeBufferSize = 10485760, // 10 MB = 10 * 1024 * 1024 B
   *       cacheSize = 10485760,       // 10 MB = 10 * 1024 * 1024 B
   *       maxOpenFiles = 100
   *     },
   *     {
   *       name = "account-index",
   *       path = "storage_directory_test",
   *       createIfMissing = true,
   *       paranoidChecks = true,
   *       verifyChecksums = true,
   *       compressionType = 1,        // compressed with snappy
   *       blockSize = 4096,           // 4  KB =         4 * 1024 B
   *       writeBufferSize = 10485760, // 10 MB = 10 * 1024 * 1024 B
   *       cacheSize = 10485760,       // 10 MB = 10 * 1024 * 1024 B
   *       maxOpenFiles = 100
   *     },
   *     { # only for unit test
   *       name = "test_name",
   *       path = "test_path",
   *       createIfMissing = false,
   *       paranoidChecks = false,
   *       verifyChecksums = false,
   *       compressionType = 1,
   *       blockSize = 2,
   *       writeBufferSize = 3,
   *       cacheSize = 4,
   *       maxOpenFiles = 5
   *     },
   *   ]
   */
  private static void setupStorage() {
    StorageConfig sc = new StorageConfig();
    try {
      setPrivateField(sc, "defaultDbOption", makeOverride(50));
      setPrivateField(sc, "defaultMDbOption", makeOverride(500));
      setPrivateField(sc, "defaultLDbOption", makeOverride(1000));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
    storage.setDefaultDbOptions(sc);

    StorageConfig.PropertyConfig account = new StorageConfig.PropertyConfig();
    account.setName("account");
    account.setPath("storage_directory_test");
    account.setCompressionType(1);
    account.setBlockSize(4096);
    account.setWriteBufferSize(10485760);
    account.setCacheSize(10485760);
    account.setMaxOpenFiles(100);

    StorageConfig.PropertyConfig accountIndex = new StorageConfig.PropertyConfig();
    accountIndex.setName("account-index");
    accountIndex.setPath("storage_directory_test");
    accountIndex.setCompressionType(1);
    accountIndex.setBlockSize(4096);
    accountIndex.setWriteBufferSize(10485760);
    accountIndex.setCacheSize(10485760);
    accountIndex.setMaxOpenFiles(100);

    StorageConfig.PropertyConfig testName = new StorageConfig.PropertyConfig();
    testName.setName("test_name");
    testName.setPath("test_path");
    testName.setCreateIfMissing(false);
    testName.setParanoidChecks(false);
    testName.setVerifyChecksums(false);
    testName.setCompressionType(1);
    testName.setBlockSize(2);
    testName.setWriteBufferSize(3);
    testName.setCacheSize(4);
    testName.setMaxOpenFiles(5);

    storage.setPropertyMapFromBean(Arrays.asList(account, accountIndex, testName));
  }

  private static StorageConfig.DbOptionOverride makeOverride(int maxOpenFiles) {
    StorageConfig.DbOptionOverride o = new StorageConfig.DbOptionOverride();
    o.setMaxOpenFiles(maxOpenFiles);
    return o;
  }

  private static void setPrivateField(Object obj, String name, Object value)
      throws ReflectiveOperationException {
    Field f = StorageConfig.class.getDeclaredField(name);
    f.setAccessible(true);
    f.set(obj, value);
  }

  @AfterClass
  public static void cleanup() {
    Args.clearParam();
    FileUtil.deleteDir(new File("test_path"));
  }

  @Test
  public void getDirectory() {
    Assert.assertEquals("database", storage.getDbDirectory());
    Assert.assertEquals("index", storage.getIndexDirectory());
  }

  @Test
  public void getPath() {
    Assert.assertEquals("storage_directory_test", StorageUtils.getPathByDbName("account"));
    Assert.assertEquals("test_path", StorageUtils.getPathByDbName("test_name"));
    Assert.assertNull(StorageUtils.getPathByDbName("some_name_not_exists"));
  }

  @Test
  public void getOptions() {
    Options options = StorageUtils.getOptionsByDbName("account");
    Assert.assertTrue(options.createIfMissing());
    Assert.assertTrue(options.paranoidChecks());
    Assert.assertTrue(options.verifyChecksums());
    Assert.assertEquals(CompressionType.SNAPPY, options.compressionType());
    Assert.assertEquals(4096, options.blockSize());
    Assert.assertEquals(10485760, options.writeBufferSize());
    Assert.assertEquals(10485760L, options.cacheSize());
    Assert.assertEquals(100, options.maxOpenFiles());

    options = StorageUtils.getOptionsByDbName("test_name");
    Assert.assertFalse(options.createIfMissing());
    Assert.assertFalse(options.paranoidChecks());
    Assert.assertFalse(options.verifyChecksums());
    Assert.assertEquals(CompressionType.SNAPPY, options.compressionType());
    Assert.assertEquals(2, options.blockSize());
    Assert.assertEquals(3, options.writeBufferSize());
    Assert.assertEquals(4L, options.cacheSize());
    Assert.assertEquals(5, options.maxOpenFiles());

    options = StorageUtils.getOptionsByDbName("some_name_not_exists");
    Assert.assertTrue(options.createIfMissing());
    Assert.assertTrue(options.paranoidChecks());
    Assert.assertTrue(options.verifyChecksums());
    Assert.assertEquals(CompressionType.SNAPPY, options.compressionType());
    Assert.assertEquals(4 * 1024, options.blockSize());
    Assert.assertEquals(16 * 1024 * 1024, options.writeBufferSize());
    Assert.assertEquals(32 * 1024 * 1024L, options.cacheSize());
    Assert.assertEquals(50, options.maxOpenFiles());

    options = StorageUtils.getOptionsByDbName("code");
    Assert.assertEquals(64 * 1024 * 1024, options.writeBufferSize());
    Assert.assertEquals(500, options.maxOpenFiles());

    options = StorageUtils.getOptionsByDbName("delegation");
    Assert.assertEquals(64 * 1024 * 1024, options.writeBufferSize());
    Assert.assertEquals(1000, options.maxOpenFiles());

    options = StorageUtils.getOptionsByDbName("trans");
    Assert.assertEquals(16 * 1024 * 1024, options.writeBufferSize());
    Assert.assertEquals(50, options.maxOpenFiles());
  }

}
