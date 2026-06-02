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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
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

  private static void setupStorage() {
    Config cfg = ConfigFactory.parseString(
        "storage.default.maxOpenFiles = 50\n"
        + "storage.defaultM.maxOpenFiles = 500\n"
        + "storage.defaultL.maxOpenFiles = 1000\n"
        + "storage.properties = [\n"
        + "  { name = account, path = storage_directory_test,\n"
        + "    blockSize = 4096, writeBufferSize = 10485760, cacheSize = 10485760,\n"
        + "    maxOpenFiles = 100 },\n"
        + "  { name = \"account-index\", path = storage_directory_test,\n"
        + "    blockSize = 4096, writeBufferSize = 10485760, cacheSize = 10485760,\n"
        + "    maxOpenFiles = 100 },\n"
        + "  { name = test_name, path = test_path,\n"
        + "    blockSize = 2, writeBufferSize = 3, cacheSize = 4, maxOpenFiles = 5 },\n"
        // name/path-only entries: LevelDB options omitted, must inherit per-tier defaults
        + "  { name = delegation, path = test_path },\n"
        + "  { name = code, path = test_path }\n"
        + "]"
    ).withFallback(ConfigFactory.load(TestConstants.TEST_CONF));
    StorageConfig sc = StorageConfig.fromConfig(cfg);
    storage.setDefaultDbOptions(sc);
    storage.setPropertyMapFromBean(sc.getProperties());
  }

  @AfterClass
  public static void cleanup() {
    Args.clearParam();
    FileUtil.deleteDir(new File("test_path"));
  }

  @Test
  public void getDirectory() {
    Assert.assertEquals("database", storage.getDbDirectory());
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
    Assert.assertEquals(4096, options.blockSize());
    Assert.assertEquals(10485760, options.writeBufferSize());
    Assert.assertEquals(10485760L, options.cacheSize());
    Assert.assertEquals(100, options.maxOpenFiles());

    options = StorageUtils.getOptionsByDbName("test_name");
    Assert.assertEquals(2, options.blockSize());
    Assert.assertEquals(3, options.writeBufferSize());
    Assert.assertEquals(4L, options.cacheSize());
    Assert.assertEquals(5, options.maxOpenFiles());

    options = StorageUtils.getOptionsByDbName("some_name_not_exists");
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

  /**
   * A properties entry that only sets name/path (all LevelDB options omitted) must inherit
   * the per-tier defaults from newDefaultDbOptions instead of resetting them to the
   * PropertyConfig defaults. Both "delegation" (DB_L) and "code" (DB_M) are listed with
   * name/path only, so they must keep their tier writeBufferSize/maxOpenFiles.
   */
  @Test
  public void nameAndPathOnlyInheritsTierDefaults() {
    Options ldb = StorageUtils.getOptionsByDbName("delegation");
    Assert.assertEquals(64 * 1024 * 1024, ldb.writeBufferSize());
    Assert.assertEquals(1000, ldb.maxOpenFiles());
    // unset cacheSize/blockSize inherit the base defaults, not PropertyConfig's old 10 MB
    Assert.assertEquals(32 * 1024 * 1024L, ldb.cacheSize());
    Assert.assertEquals(4 * 1024, ldb.blockSize());

    Options mdb = StorageUtils.getOptionsByDbName("code");
    Assert.assertEquals(64 * 1024 * 1024, mdb.writeBufferSize());
    Assert.assertEquals(500, mdb.maxOpenFiles());
  }

}
