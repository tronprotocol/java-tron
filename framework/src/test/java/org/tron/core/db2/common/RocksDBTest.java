package org.tron.core.db2.common;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

@Slf4j(topic = "DB")
public class RocksDBTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();



  @BeforeClass
  public static void initArgs() throws IOException {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
  }

  @AfterClass
  public static void clear() {
    Args.clearParam();
  }


  @Test
  public void destroy() throws IOException {
    String dbName = "destroy";
    String dbPath = temporaryFolder.newFolder().toString();
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(dbPath, "destroy");
    dataSource.initDB();
    RocksDB db = new RocksDB(dataSource);
    db.put("test".getBytes(), "test".getBytes());
    db.stat();
    // RocksDBException: RocksDB not close before destroy;
    RocksDB.destroy(dbName, dbPath);
    db.close();
    // ok
    RocksDB.destroy(dbName, dbPath);
    // destroy twice
    RocksDB.destroy(dbName, dbPath);
  }

  @Test
  public void backup() throws IOException {
    String dbName = "backup";
    String dbPath = temporaryFolder.newFolder().toString();
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(dbPath, dbName);
    dataSource.initDB();
    RocksDB db = new RocksDB(dataSource);
    db.put("test".getBytes(), "test".getBytes());
    db.stat();
    db.close();
    // IllegalStateException: RocksDB is already closed;
    db.backup(dbPath);
  }

}
