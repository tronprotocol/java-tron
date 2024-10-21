package org.tron.common.storage.leveldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDBException;
import org.tron.common.error.TronDBException;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.StorageUtils;
import org.tron.core.config.args.Args;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.exception.TronError;

@Slf4j
public class RocksDbDataSourceImplTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();
  private static RocksDbDataSourceImpl dataSourceTest;

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

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  @BeforeClass
  public static void initDb() throws IOException {
    Args.setParam(new String[]{"--output-directory",
        temporaryFolder.newFolder().toString()}, "config-test-dbbackup.conf");
    dataSourceTest = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory() + File.separator, "test_rocksDb");
  }

  @Test
  public void testPutGet() {
    dataSourceTest.resetDb();
    String key1 = PublicMethod.getRandomPrivateKey();
    byte[] key = key1.getBytes();
    dataSourceTest.initDB();
    String value1 = "50000";
    byte[] value = value1.getBytes();

    dataSourceTest.putData(key, value);

    assertNotNull(dataSourceTest.getData(key));
    assertEquals(1, dataSourceTest.allKeys().size());
    assertEquals(1, dataSourceTest.getTotal());
    assertEquals(1, dataSourceTest.allValues().size());
    assertEquals("50000", ByteArray.toStr(dataSourceTest.getData(key1.getBytes())));
    dataSourceTest.deleteData(key);
    assertNull(dataSourceTest.getData(key));
    assertEquals(0, dataSourceTest.getTotal());
    dataSourceTest.iterator().forEachRemaining(entry -> Assert.fail("iterator should be empty"));
    dataSourceTest.stat();
    dataSourceTest.closeDB();
    dataSourceTest.stat(); // stat again
    expectedException.expect(TronDBException.class);
    dataSourceTest.deleteData(key);
  }

  @Test
  public void testReset() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_reset");
    dataSource.resetDb();
    assertEquals(0, dataSource.allKeys().size());
    assertEquals("ROCKSDB", dataSource.getEngine());
    assertEquals("test_reset", dataSource.getName());
    assertEquals(Sets.newHashSet(), dataSource.getlatestValues(0));
    assertEquals(Collections.emptyMap(), dataSource.getNext(key1, 0));
    assertEquals(new ArrayList<>(), dataSource.getKeysNext(key1, 0));
    assertEquals(Sets.newHashSet(), dataSource.getValuesNext(key1, 0));
    assertEquals(Sets.newHashSet(), dataSource.getlatestValues(0));
    dataSource.closeDB();
  }

  @Test
  public void testupdateByBatchInner() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_updateByBatch");
    dataSource.initDB();
    dataSource.resetDb();
    String key1 = PublicMethod.getRandomPrivateKey();
    String value1 = "50000";
    String key2 = PublicMethod.getRandomPrivateKey();
    String value2 = "10000";

    Map<byte[], byte[]> rows = new HashMap<>();
    rows.put(key1.getBytes(), value1.getBytes());
    rows.put(key2.getBytes(), value2.getBytes());

    dataSource.updateByBatch(rows);

    assertEquals("50000", ByteArray.toStr(dataSource.getData(key1.getBytes())));
    assertEquals("10000", ByteArray.toStr(dataSource.getData(key2.getBytes())));
    assertEquals(2, dataSource.allKeys().size());

    rows.clear();
    rows.put(key1.getBytes(), null);
    rows.put(key2.getBytes(), null);
    dataSource.updateByBatch(rows, WriteOptionsWrapper.getInstance());
    assertEquals(0, dataSource.allKeys().size());

    rows.clear();
    rows.put(key1.getBytes(), value1.getBytes());
    rows.put(key2.getBytes(), null);
    dataSource.updateByBatch(rows);
    assertEquals("50000", ByteArray.toStr(dataSource.getData(key1.getBytes())));
    assertEquals(1, dataSource.allKeys().size());
    rows.clear();
    rows.put(null, null);
    expectedException.expect(RuntimeException.class);
    dataSource.updateByBatch(rows);
    dataSource.closeDB();
  }

  @Test
  public void testdeleteData() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_delete");
    dataSource.initDB();
    String key1 = PublicMethod.getRandomPrivateKey();
    byte[] key = key1.getBytes();
    dataSource.deleteData(key);
    byte[] value = dataSource.getData(key);
    String s = ByteArray.toStr(value);
    assertNull(s);
    dataSource.closeDB();
  }

  @Test
  public void testallKeys() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_find_key");
    dataSource.initDB();
    dataSource.resetDb();

    String key1 = PublicMethod.getRandomPrivateKey();
    byte[] key = key1.getBytes();

    String value1 = "50000";
    byte[] value = value1.getBytes();

    dataSource.putData(key, value);
    String key3 = PublicMethod.getRandomPrivateKey();
    byte[] key2 = key3.getBytes();

    String value3 = "30000";
    byte[] value2 = value3.getBytes();

    dataSource.putData(key2, value2);
    assertEquals(2, dataSource.allKeys().size());
    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test(timeout = 1000)
  public void testLockReleased() {
    dataSourceTest.initDB();
    // normal close
    dataSourceTest.closeDB();
    // closing already closed db.
    dataSourceTest.closeDB();
    // closing again to make sure the lock is free. If not test will hang.
    dataSourceTest.closeDB();

    assertFalse("Database is still alive after closing.", dataSourceTest.isAlive());
  }

  @Test
  public void allKeysTest() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_allKeysTest_key");
    dataSource.initDB();
    dataSource.resetDb();

    byte[] key = "0000000987b10fbb7f17110757321".getBytes();
    byte[] value = "50000".getBytes();
    byte[] key2 = "000000431cd8c8d5a".getBytes();
    byte[] value2 = "30000".getBytes();

    dataSource.putData(key, value);
    dataSource.putData(key2, value2);
    dataSource.allKeys().forEach(keyOne -> {
      logger.info(ByteArray.toStr(keyOne));
    });
    assertEquals(2, dataSource.allKeys().size());
    dataSource.resetDb();
    dataSource.closeDB();
  }

  private void putSomeKeyValue(RocksDbDataSourceImpl dataSource) {
    value1 = "10000".getBytes();
    value2 = "20000".getBytes();
    value3 = "30000".getBytes();
    value4 = "40000".getBytes();
    value5 = "50000".getBytes();
    value6 = "60000".getBytes();
    key1 = "00000001aa".getBytes();
    key2 = "00000002aa".getBytes();
    key3 = "00000003aa".getBytes();
    key4 = "00000004aa".getBytes();
    key5 = "00000005aa".getBytes();
    key6 = "00000006aa".getBytes();

    dataSource.putData(key1, value1);
    dataSource.putData(key6, value6);
    dataSource.putData(key2, value2);
    dataSource.putData(key5, value5);
    dataSource.putData(key3, value3);
    dataSource.putData(key4, value4);
  }

  @Test
  public void seekTest() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_seek_key");
    dataSource.initDB();
    dataSource.resetDb();

    putSomeKeyValue(dataSource);
    Assert.assertTrue(true);
    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test
  public void getValuesNext() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_getValuesNext_key");
    dataSource.initDB();
    dataSource.resetDb();
    putSomeKeyValue(dataSource);
    Set<byte[]> seekKeyLimitNext = dataSource.getValuesNext("0000000300".getBytes(), 2);
    HashSet<String> hashSet = Sets.newHashSet(ByteArray.toStr(value3), ByteArray.toStr(value4));
    seekKeyLimitNext.forEach(
        value -> Assert.assertTrue("getValuesNext", hashSet.contains(ByteArray.toStr(value))));
    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test
  public void testCheckOrInitEngine() {
    String dir =
        Args.getInstance().getOutputDirectory() + Args.getInstance().getStorage().getDbDirectory();
    String enginePath = dir + File.separator + "test_engine" + File.separator + "engine.properties";
    FileUtil.createDirIfNotExists(dir + File.separator + "test_engine");
    FileUtil.createFileIfNotExists(enginePath);
    PropUtil.writeProperty(enginePath, "ENGINE", "ROCKSDB");
    Assert.assertEquals("ROCKSDB", PropUtil.readProperty(enginePath, "ENGINE"));

    RocksDbDataSourceImpl dataSource;
    dataSource = new RocksDbDataSourceImpl(dir, "test_engine");
    dataSource.initDB();
    Assert.assertNotNull(dataSource.getDatabase());
    dataSource.closeDB();

    dataSource = null;
    System.gc();
    PropUtil.writeProperty(enginePath, "ENGINE", "LEVELDB");
    Assert.assertEquals("LEVELDB", PropUtil.readProperty(enginePath, "ENGINE"));
    dataSource = new RocksDbDataSourceImpl(dir, "test_engine");
    try {
      dataSource.initDB();
    } catch (Exception e) {
      Assert.assertEquals(String.format(
              "Cannot open LevelDB database '%s' with RocksDB engine."
                  + " Set db.engine=LEVELDB or use RocksDB database. ", "test_engine"),
              e.getMessage());
    }
    Assert.assertNull(dataSource.getDatabase());
    PropUtil.writeProperty(enginePath, "ENGINE", "ROCKSDB");
  }

  @Test
  public void testGetNext() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_getNext_key");
    dataSource.initDB();
    dataSource.resetDb();
    putSomeKeyValue(dataSource);
    // case: normal
    Map<byte[], byte[]> seekKvLimitNext = dataSource.getNext("0000000300".getBytes(), 2);
    Map<String, String> hashMap = Maps.newHashMap();
    hashMap.put(ByteArray.toStr(key3), ByteArray.toStr(value3));
    hashMap.put(ByteArray.toStr(key4), ByteArray.toStr(value4));
    seekKvLimitNext.forEach((key, value) -> {
      String keyStr = ByteArray.toStr(key);
      Assert.assertTrue("getNext", hashMap.containsKey(keyStr));
      Assert.assertEquals(ByteArray.toStr(value), hashMap.get(keyStr));
    });
    // case: targetKey greater than all existed keys
    seekKvLimitNext = dataSource.getNext("0000000700".getBytes(), 2);
    Assert.assertEquals(0, seekKvLimitNext.size());
    // case: limit<=0
    seekKvLimitNext = dataSource.getNext("0000000300".getBytes(), 0);
    Assert.assertEquals(0, seekKvLimitNext.size());
    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test
  public void testGetlatestValues() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_getlatestValues_key");
    dataSource.initDB();
    dataSource.resetDb();
    putSomeKeyValue(dataSource);
    // case: normal
    Set<byte[]> seekKeyLimitNext = dataSource.getlatestValues(2);
    Set<String> hashSet = Sets.newHashSet(ByteArray.toStr(value5), ByteArray.toStr(value6));
    seekKeyLimitNext.forEach(value -> {
      Assert.assertTrue(hashSet.contains(ByteArray.toStr(value)));
    });
    // case: limit<=0
    seekKeyLimitNext = dataSource.getlatestValues(0);
    assertEquals(0, seekKeyLimitNext.size());
    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test
  public void getKeysNext() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_getKeysNext_key");
    dataSource.initDB();
    dataSource.resetDb();
    putSomeKeyValue(dataSource);

    int limit = 2;
    List<byte[]> seekKeyLimitNext = dataSource.getKeysNext("0000000300".getBytes(), limit);
    List<byte[]> list = Arrays.asList(key3, key4);

    for (int i = 0; i < limit; i++) {
      Assert.assertArrayEquals(list.get(i), seekKeyLimitNext.get(i));
    }

    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test
  public void prefixQueryTest() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_prefixQuery");
    dataSource.initDB();
    dataSource.resetDb();

    putSomeKeyValue(dataSource);
    // put a kv that will not be queried.
    byte[] key7 = "0000001".getBytes();
    byte[] value7 = "0000001v".getBytes();
    dataSource.putData(key7, value7);

    byte[] prefix = "0000000".getBytes();

    List<String> result = dataSource.prefixQuery(prefix)
        .keySet()
        .stream()
        .map(WrappedByteArray::getBytes)
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

    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test
  public void initDbTest() {
    makeExceptionDb("test_initDb");
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_initDb");
    TronError thrown = assertThrows(TronError.class, dataSource::initDB);
    assertEquals(TronError.ErrCode.ROCKSDB_INIT, thrown.getErrCode());
  }

  @Test
  public void testRocksDbOpenLevelDb() {
    String name = "test_openLevelDb";
    String output = Paths
        .get(StorageUtils.getOutputDirectoryByDbName(name), CommonParameter
            .getInstance().getStorage().getDbDirectory()).toString();
    LevelDbDataSourceImpl levelDb = new LevelDbDataSourceImpl(
        StorageUtils.getOutputDirectoryByDbName(name), name);
    levelDb.initDB();
    levelDb.putData(key1, value1);
    levelDb.closeDB();
    RocksDbDataSourceImpl rocksDb = new RocksDbDataSourceImpl(output, name);
    expectedException.expectMessage(
        String.format(
            "Cannot open LevelDB database '%s' with RocksDB engine."
                + " Set db.engine=LEVELDB or use RocksDB database. ", name));
    rocksDb.initDB();
  }

  @Test
  public void testRocksDbOpenLevelDb2() {
    String name = "test_openLevelDb2";
    String output = Paths
        .get(StorageUtils.getOutputDirectoryByDbName(name), CommonParameter
            .getInstance().getStorage().getDbDirectory()).toString();
    LevelDbDataSourceImpl levelDb = new LevelDbDataSourceImpl(
        StorageUtils.getOutputDirectoryByDbName(name), name);
    levelDb.initDB();
    levelDb.putData(key1, value1);
    levelDb.closeDB();
    // delete engine.properties file to simulate the case that db.engine is not set.
    File engineFile = Paths
        .get(output, name, "engine.properties").toFile();
    if (engineFile.exists()) {
      engineFile.delete();
    }
    Assert.assertFalse(engineFile.exists());
    RocksDbDataSourceImpl rocksDb = new RocksDbDataSourceImpl(output, name);
    expectedException.expectMessage(
        String.format(
            "Cannot open LevelDB database '%s' with RocksDB engine."
                + " Set db.engine=LEVELDB or use RocksDB database. ", name));
    rocksDb.initDB();
  }

  @Test
  public void testNewInstance() {
    dataSourceTest.closeDB();
    RocksDbDataSourceImpl newInst = dataSourceTest.newInstance();
    newInst.initDB();
    assertFalse(newInst.flush());
    newInst.closeDB();
    RocksDbDataSourceImpl empty = new RocksDbDataSourceImpl();
    empty.setDBName("empty");
    assertEquals("empty", empty.getDBName());
    String output = Paths
        .get(StorageUtils.getOutputDirectoryByDbName("newInst2"), CommonParameter
            .getInstance().getStorage().getDbDirectory()).toString();
    RocksDbDataSourceImpl newInst2 = new RocksDbDataSourceImpl(output, "newInst2");
    newInst2.initDB();
    newInst2.closeDB();
  }

  @Test
  public void backupAndDelete() throws RocksDBException {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "backupAndDelete");
    dataSource.initDB();
    putSomeKeyValue(dataSource);
    Path dir = Paths.get(Args.getInstance().getOutputDirectory(), "backup");
    String path = dir + File.separator;
    FileUtil.createDirIfNotExists(path);
    dataSource.backup(path);
    File backDB = Paths.get(dir.toString(),dataSource.getDBName()).toFile();
    Assert.assertTrue(backDB.exists());
    dataSource.deleteDbBakPath(path);
    Assert.assertFalse(backDB.exists());
    dataSource.closeDB();
  }

  @Test
  public void testGetTotal() {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_getTotal_key");
    dataSource.initDB();
    dataSource.resetDb();

    Map<byte[], byte[]> dataMapset = Maps.newHashMap();
    dataMapset.put(key1, value1);
    dataMapset.put(key2, value2);
    dataMapset.put(key3, value3);
    dataMapset.forEach(dataSource::putData);
    Assert.assertEquals(dataMapset.size(), dataSource.getTotal());
    dataSource.resetDb();
    dataSource.closeDB();
  }

  private void makeExceptionDb(String dbName) {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_initDb");
    dataSource.initDB();
    dataSource.closeDB();
    FileUtil.saveData(dataSource.getDbPath().toString() + "/CURRENT",
        "...", Boolean.FALSE);
  }
}
