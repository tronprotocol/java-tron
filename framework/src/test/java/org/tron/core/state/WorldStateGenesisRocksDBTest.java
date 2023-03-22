package org.tron.core.state;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.tuweni.bytes.Bytes;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.tron.common.utils.FileUtil;

public class WorldStateGenesisRocksDBTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testPrefixQuery() throws IOException {
    Path parentPath = temporaryFolder.newFolder().toPath();
    String dbName = "test_prefix";
    Path dbPath = Paths.get(parentPath.toString(), dbName);

    try {
      Options options = new Options();
      options.setCreateIfMissing(true);
      RocksDB rocksDB = RocksDB.open(options, dbPath.toString());
      Bytes key1 = Bytes.wrap("a1".getBytes());
      Bytes key2 = Bytes.wrap("b1".getBytes());
      Bytes key3 = Bytes.wrap("c1".getBytes());
      Bytes key4 = Bytes.wrap("b2".getBytes());
      Bytes key5 = Bytes.wrap("b3".getBytes());
      Bytes value1 = Bytes.wrap("value1".getBytes());
      Bytes value2 = Bytes.wrap("value2".getBytes());
      Bytes value3 = Bytes.wrap("value3".getBytes());
      Bytes value4 = Bytes.wrap("value4".getBytes());
      Bytes value5 = Bytes.wrap("value5".getBytes());
      rocksDB.put(key1.toArray(), value1.toArray());
      rocksDB.put(key2.toArray(), value2.toArray());
      rocksDB.put(key3.toArray(), value3.toArray());
      rocksDB.put(key4.toArray(), value4.toArray());
      rocksDB.put(key5.toArray(), value5.toArray());
      options.close();
      rocksDB.close();

      WorldStateGenesis.RocksDB db =
          new WorldStateGenesis.RocksDB(parentPath, dbName);
      Map<Bytes, Bytes> result = db.prefixQuery("b".getBytes());
      Assert.assertEquals(3, result.size());
      Assert.assertEquals(value2, result.get(key2));
      Assert.assertEquals(value4, result.get(key4));
      Assert.assertEquals(value5, result.get(key5));
      db.close();
    } catch (RocksDBException | IOException e) {
      Assert.fail();
    } finally {
      FileUtil.deleteDir(parentPath.toFile());
    }
  }
}
