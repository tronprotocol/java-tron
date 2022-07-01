package org.tron.core.db2;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rocksdb.RocksDB;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.db.common.DbSourceInter;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.db2.core.Snapshot;
import org.tron.core.db2.core.SnapshotRoot;

@Slf4j
public class ChainbaseTest {

  private static final String dbPath = "output-chainbase-test";
  private Chainbase chainbase = null;

  private final byte[] value0 = "00000".getBytes();
  private final byte[] value1 = "10000".getBytes();
  private final byte[] value2 = "20000".getBytes();
  private final byte[] value3 = "30000".getBytes();
  private final byte[] value4 = "40000".getBytes();
  private final byte[] value5 = "50000".getBytes();
  private final byte[] value6 = "60000".getBytes();
  private final byte[] value7 = "70000".getBytes();
  private final byte[] value8 = "80000".getBytes();
  private final byte[] valueRoot7 = "root70000".getBytes();
  private final byte[] valueRoot8 = "root80000".getBytes();
  private final byte[] value9 = "90000".getBytes();

  private final byte[] key0 = "0aa".getBytes();
  private final byte[] key1 = "10000001aa".getBytes();
  private final byte[] key2 = "10000002aa".getBytes();
  private final byte[] key3 = "10000003aa".getBytes();
  private final byte[] key4 = "10000004aa".getBytes();
  private final byte[] key5 = "10000005aa".getBytes();
  private final byte[] key6 = "10000006aa".getBytes();
  private final byte[] key7 = "10000006ac".getBytes();
  private final byte[] key8 = "10000006ab".getBytes();
  private final byte[] key9 = "10000006dd".getBytes();

  private final byte[] prefix = "1000000".getBytes();
  private final byte[] prefix2 = "2000000".getBytes();
  private final byte[] prefix3 = "0000000".getBytes();

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Before
  public void initDb() {
    RocksDB.loadLibrary();
    Args.setParam(new String[] {"--output-directory", dbPath}, Constant.TEST_CONF);
  }

  @Test
  public void testPrefixQueryForLeveldb() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "testPrefixQueryForLeveldb");
    dataSource.initDB();
    this.chainbase = new Chainbase(new SnapshotRoot(
        new LevelDB(dataSource)));
    testDb(chainbase);
    testRoot(dataSource);
    chainbase.reset();
    chainbase.close();
  }

  @Test
  public void testPrefixQueryForRocksdb() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "testPrefixQueryForRocksdb");
    dataSource.initDB();
    this.chainbase = new Chainbase(new SnapshotRoot(
        new org.tron.core.db2.common.RocksDB(dataSource)));
    testDb(chainbase);
    testRoot(dataSource);
    chainbase.reset();
    chainbase.close();
  }


  private void testRoot(DbSourceInter<byte[]> dbSource) {
    Map<String,String> result = new HashMap<>();
    dbSource.prefixQuery(prefix).forEach((k, v) ->
        result.put(ByteArray.toStr(k.getBytes()), ByteArray.toStr(v)));

    Map<String,String> expect = new HashMap<>();
    expect.put(ByteArray.toStr(key2),ByteArray.toStr(value2));
    expect.put(ByteArray.toStr(key6),ByteArray.toStr(value6));
    expect.put(ByteArray.toStr(key7),ByteArray.toStr(valueRoot7));
    expect.put(ByteArray.toStr(key8),ByteArray.toStr(valueRoot8));
    Assert.assertEquals(expect, result);
    Assert.assertTrue(dbSource.prefixQuery(prefix2).isEmpty());
    Assert.assertTrue(dbSource.prefixQuery(prefix3).isEmpty());
  }

  private void testDb(Chainbase chainbase) {
    byte[] keyNotQuery1 = "123".getBytes();
    byte[] keyNotQuery2 = "0000001".getBytes();
    byte[] valueNotQuery1 = "v123".getBytes();
    byte[] valueNotQuery2 = "v0000001".getBytes();

    chainbase.setHead(chainbase.getHead().advance());
    Snapshot head = chainbase.getHead();
    Snapshot root = head.getRoot();
    // put some data in head
    head.put(key0, value0);
    head.put(key1, value1);
    head.put(key7, value7);
    head.put(key3, value3);
    head.put(key8, value8);
    head.remove(key7);
    // put some data in root
    root.put(key2, value2);
    root.put(key6, value6);
    root.put(key3, value3);
    root.put(key7, valueRoot7);
    root.put(key8, valueRoot8);
    root.put(keyNotQuery1, valueNotQuery1);
    // advance head and put some data again
    head = head.advance();
    head.put(key4, value4);
    head.put(key5, value5);
    head.put(key9,value9);
    head.remove(key8);
    head.put(keyNotQuery2, valueNotQuery2);

    head = head.advance();
    head.remove(key9);
    root.remove(key3);

    // test for all, both in snapshotImpl and leveldb
    Map<String,String> result = new HashMap<>();
    chainbase.prefixQuery(prefix).forEach((k, v) ->
        result.put(ByteArray.toStr(k.getBytes()), ByteArray.toStr(v)));

    Map<String,String> expect = new HashMap<>();
    expect.put(ByteArray.toStr(key1),ByteArray.toStr(value1));
    expect.put(ByteArray.toStr(key2),ByteArray.toStr(value2));
    expect.put(ByteArray.toStr(key3),ByteArray.toStr(value3));
    expect.put(ByteArray.toStr(key4),ByteArray.toStr(value4));
    expect.put(ByteArray.toStr(key5),ByteArray.toStr(value5));
    expect.put(ByteArray.toStr(key6),ByteArray.toStr(value6));
    Assert.assertEquals(expect, result);
    Assert.assertTrue(chainbase.prefixQuery(prefix2).isEmpty());
    Assert.assertTrue(chainbase.prefixQuery(prefix3).isEmpty());
  }

}