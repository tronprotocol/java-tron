package org.tron.core.db2;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
import org.tron.common.utils.Pair;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.db2.core.Snapshot;
import org.tron.core.db2.core.SnapshotRoot;

@Slf4j
public class ChainbaseTest {

  private static final String dbPath = "output-chainbase-test";
  private Chainbase chainbase = null;

  private byte[] value1 = "10000".getBytes();
  private byte[] value2 = "20000".getBytes();
  private byte[] value3 = "30000".getBytes();
  private byte[] value4 = "40000".getBytes();
  private byte[] value5 = "50000".getBytes();
  private byte[] value6 = "60000".getBytes();

  private byte[] key1 = "00000001aa".getBytes();
  private byte[] key2 = "00000002aa".getBytes();
  private byte[] key3 = "00000003aa".getBytes();
  private byte[] key4 = "00000004aa".getBytes();
  private byte[] key5 = "00000005aa".getBytes();
  private byte[] key6 = "00000006aa".getBytes();

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
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
  }

  @Test
  public void testPrefixQueryForLeveldb() {
    byte[] prefix = "0000000".getBytes();

    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
            Args.getInstance().getOutputDirectory(), "testPrefixQueryForLeveldb");
    dataSource.initDB();
    this.chainbase = new Chainbase(new SnapshotRoot(
            new LevelDB(dataSource)));

    putAndAssert(chainbase, prefix);

    // test leveldb
    List<String> resultInDb = dataSource.prefixQuery(prefix)
            .keySet()
            .stream()
            .map(ByteArray::toStr)
            .collect(Collectors.toList());
    List<String> listforDb = Arrays.asList(
            ByteArray.toStr(key2),
            ByteArray.toStr(key3),
            ByteArray.toStr(key6));

    Assert.assertEquals(listforDb.size(), resultInDb.size());
    listforDb.forEach(entry -> Assert.assertTrue(resultInDb.contains(entry)));

    chainbase.reset();
    chainbase.close();
  }

  @Test
  public void testPrefixQueryForRocksdb() {
    byte[] prefix = "0000000".getBytes();

    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
            Args.getInstance().getOutputDirectory(), "testPrefixQueryForRocksdb");
    dataSource.initDB();
    this.chainbase = new Chainbase(new SnapshotRoot(
            new org.tron.core.db2.common.RocksDB(dataSource)));

    putAndAssert(chainbase, prefix);

    // test leveldb
    List<String> resultInDb = dataSource.prefixQuery(prefix)
            .keySet()
            .stream()
            .map(ByteArray::toStr)
            .collect(Collectors.toList());
    List<String> listforDb = Arrays.asList(
            ByteArray.toStr(key2),
            ByteArray.toStr(key3),
            ByteArray.toStr(key6));

    Assert.assertEquals(listforDb.size(), resultInDb.size());
    listforDb.forEach(entry -> Assert.assertTrue(resultInDb.contains(entry)));

    chainbase.reset();
    chainbase.close();
  }

  private void putAndAssert(Chainbase chainbase, byte[] prefix) {
    byte[] keyNotQuery1 = "123".getBytes();
    byte[] keyNotQuery2 = "0000001".getBytes();
    byte[] valueNotQuery1 = "v123".getBytes();
    byte[] valueNotQuery2 = "v0000001".getBytes();

    chainbase.setHead(chainbase.getHead().advance());
    Snapshot head = chainbase.getHead();
    Snapshot root = head.getRoot();
    // put some data in head
    head.put(key1, value1);
    // put some data in root
    root.put(key2, value2);
    root.put(key6, value6);
    root.put(key3, value3);
    root.put(keyNotQuery1, valueNotQuery1);
    // advance head and put some data again
    head = head.advance();
    head.put(key4, value4);
    head.put(key5, value5);
    head.put(keyNotQuery2, valueNotQuery2);

    // test for all, both in snapshotImpl and leveldb
    List<String> result = chainbase.prefixQuery(prefix)
            .stream()
            .map(Pair::getKey)
            .map(ByteArray::toStr)
            .collect(Collectors.toList());
    List<String> list = Arrays.asList(
            ByteArray.toStr(key1),
            ByteArray.toStr(key2),
            ByteArray.toStr(key3),
            ByteArray.toStr(key4),
            ByteArray.toStr(key5),
            ByteArray.toStr(key6));

    Assert.assertEquals(list.size(), result.size());
    list.forEach(entry -> Assert.assertTrue(result.contains(entry)));
  }

}
