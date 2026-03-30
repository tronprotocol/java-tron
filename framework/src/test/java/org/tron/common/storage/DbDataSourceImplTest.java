package org.tron.common.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.rocksdb.RocksDB;
import org.tron.common.TestConstants;
import org.tron.common.arch.Arch;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.PublicMethod;
import org.tron.core.config.args.Args;
import org.tron.core.db.common.DbSourceInter;
import org.tron.core.db2.common.WrappedByteArray;

@Slf4j
@RunWith(Parameterized.class)
public class DbDataSourceImplTest {

  @Parameters(name = "engine={0}")
  public static Collection<Object[]> engines() {
    List<Object[]> params = new ArrayList<>();
    if (!Arch.isArm64()) {
      params.add(new Object[]{"LEVELDB"});
    }
    params.add(new Object[]{"ROCKSDB"});
    return params;
  }

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private final String engineName;
  private DbSourceInter<byte[]> dataSourceTest;

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

  static {
    RocksDB.loadLibrary();
  }

  public DbDataSourceImplTest(String engineName) {
    this.engineName = engineName;
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  @Before
  public void initDb() throws IOException {
    Args.setParam(new String[]{"--output-directory",
        temporaryFolder.newFolder().toString()}, TestConstants.TEST_CONF);
    dataSourceTest = createDataSource(
        Args.getInstance().getOutputDirectory() + File.separator, "test_db");
  }

  private DbSourceInter<byte[]> createDataSource(String outputDir, String name) {
    if ("LEVELDB".equals(engineName)) {
      return new LevelDbDataSourceImpl(outputDir, name);
    } else {
      return new RocksDbDataSourceImpl(outputDir, name);
    }
  }

  private Class<? extends Exception> getCloseException() {
    return "LEVELDB".equals(engineName)
        ? org.iq80.leveldb.DBException.class
        : org.tron.common.error.TronDBException.class;
  }

  @Test
  public void testPutGet() {
    dataSourceTest.resetDb();
    String key1 = PublicMethod.getRandomPrivateKey();
    byte[] key = key1.getBytes();
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
    dataSourceTest.stat();
    exception.expect(getCloseException());
    dataSourceTest.deleteData(key);
  }

  @Test
  public void testReset() {
    DbSourceInter<byte[]> dataSource = createDataSource(
        Args.getInstance().getOutputDirectory(), "test_reset");
    dataSource.resetDb();
    assertEquals(0, dataSource.allKeys().size());
    assertEquals(engineName, getEngine(dataSource));
    assertEquals("test_reset", getName(dataSource));
    assertEquals(Sets.newHashSet(), getlatestValues(dataSource, 0));
    assertEquals(Collections.emptyMap(), getNext(dataSource, key1, 0));
    assertEquals(new ArrayList<>(), doGetKeysNext(dataSource, key1, 0));
    assertEquals(Sets.newHashSet(), doGetValuesNext(dataSource, key1, 0));
    assertEquals(Sets.newHashSet(), getlatestValues(dataSource, 0));
    dataSource.closeDB();
  }

  @Test
  public void testupdateByBatchInner() {
    DbSourceInter<byte[]> dataSource = createDataSource(
        Args.getInstance().getOutputDirectory(), "test_updateByBatch");
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
    try (WriteOptionsWrapper options = WriteOptionsWrapper.getInstance()) {
      dataSource.updateByBatch(rows, options);
    }
    assertEquals(0, dataSource.allKeys().size());

    rows.clear();
    rows.put(key1.getBytes(), value1.getBytes());
    rows.put(key2.getBytes(), null);
    dataSource.updateByBatch(rows);
    assertEquals("50000", ByteArray.toStr(dataSource.getData(key1.getBytes())));
    assertEquals(1, dataSource.allKeys().size());
    rows.clear();
    rows.put(null, null);
    exception.expect(RuntimeException.class);
    dataSource.updateByBatch(rows);
    dataSource.closeDB();
  }

  @Test
  public void testdeleteData() {
    DbSourceInter<byte[]> dataSource = createDataSource(
        Args.getInstance().getOutputDirectory(), "test_delete");
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
    DbSourceInter<byte[]> dataSource = createDataSource(
        Args.getInstance().getOutputDirectory(), "test_find_key");

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
    dataSourceTest.closeDB();
    dataSourceTest.closeDB();
    dataSourceTest.closeDB();
    assertFalse("Database is still alive after closing.", dataSourceTest.isAlive());
  }

  @Test
  public void allKeysTest() {
    DbSourceInter<byte[]> dataSource = createDataSource(
        Args.getInstance().getOutputDirectory(), "test_allKeysTest_key");

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
    dataSource.closeDB();
  }

  private void putSomeKeyValue(DbSourceInter<byte[]> dataSource) {
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
  public void getValuesNext() {
    DbSourceInter<byte[]> dataSource = createDataSource(
        Args.getInstance().getOutputDirectory(), "test_getValuesNext_key");
    putSomeKeyValue(dataSource);
    Set<byte[]> seekKeyLimitNext = doGetValuesNext(dataSource, "0000000300".getBytes(), 2);
    HashSet<String> hashSet = Sets.newHashSet(ByteArray.toStr(value3), ByteArray.toStr(value4));
    seekKeyLimitNext.forEach(value ->
        Assert.assertTrue("getValuesNext", hashSet.contains(ByteArray.toStr(value))));
    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test
  public void testGetTotal() {
    DbSourceInter<byte[]> dataSource = createDataSource(
        Args.getInstance().getOutputDirectory(), "test_getTotal_key");
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

  @Test
  public void getKeysNext() {
    DbSourceInter<byte[]> dataSource = createDataSource(
        Args.getInstance().getOutputDirectory(), "test_getKeysNext_key");
    putSomeKeyValue(dataSource);

    int limit = 2;
    List<byte[]> seekKeyLimitNext = doGetKeysNext(dataSource, "0000000300".getBytes(), limit);
    List<byte[]> list = Arrays.asList(key3, key4);

    for (int i = 0; i < limit; i++) {
      Assert.assertArrayEquals(list.get(i), seekKeyLimitNext.get(i));
    }
    dataSource.closeDB();
  }

  @Test
  public void prefixQueryTest() {
    DbSourceInter<byte[]> dataSource = createDataSource(
        Args.getInstance().getOutputDirectory(), "test_prefixQuery");
    putSomeKeyValue(dataSource);
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

    dataSource.closeDB();
  }

  @Test
  public void testGetNext() {
    DbSourceInter<byte[]> dataSource = createDataSource(
        Args.getInstance().getOutputDirectory(), "test_getNext_key");
    putSomeKeyValue(dataSource);
    Map<byte[], byte[]> seekKvLimitNext = getNext(dataSource, "0000000300".getBytes(), 2);
    Map<String, String> hashMap = Maps.newHashMap();
    hashMap.put(ByteArray.toStr(key3), ByteArray.toStr(value3));
    hashMap.put(ByteArray.toStr(key4), ByteArray.toStr(value4));
    seekKvLimitNext.forEach((key, value) -> {
      String keyStr = ByteArray.toStr(key);
      Assert.assertTrue("getNext", hashMap.containsKey(keyStr));
      Assert.assertEquals(ByteArray.toStr(value), hashMap.get(keyStr));
    });
    seekKvLimitNext = getNext(dataSource, "0000000700".getBytes(), 2);
    Assert.assertEquals(0, seekKvLimitNext.size());
    seekKvLimitNext = getNext(dataSource, "0000000300".getBytes(), 0);
    Assert.assertEquals(0, seekKvLimitNext.size());
    dataSource.closeDB();
  }

  @Test
  public void testGetlatestValues() {
    DbSourceInter<byte[]> dataSource = createDataSource(
        Args.getInstance().getOutputDirectory(), "test_getlatestValues_key");
    putSomeKeyValue(dataSource);
    Set<byte[]> seekKeyLimitNext = getlatestValues(dataSource, 2);
    Set<String> hashSet = Sets.newHashSet(ByteArray.toStr(value5), ByteArray.toStr(value6));
    seekKeyLimitNext.forEach(value -> {
      Assert.assertTrue(hashSet.contains(ByteArray.toStr(value)));
    });
    seekKeyLimitNext = getlatestValues(dataSource, 0);
    assertEquals(0, seekKeyLimitNext.size());
    dataSource.closeDB();
  }

  @Test
  public void testNewInstance() {
    dataSourceTest.closeDB();
    DbSourceInter<byte[]> newInst;
    if (dataSourceTest instanceof LevelDbDataSourceImpl) {
      LevelDbDataSourceImpl lvl = (LevelDbDataSourceImpl) dataSourceTest;
      newInst = lvl.newInstance();
    } else {
      RocksDbDataSourceImpl rks = (RocksDbDataSourceImpl) dataSourceTest;
      newInst = rks.newInstance();
    }
    assertFalse(newInst.flush());
    newInst.closeDB();
  }

  // Helper methods for non-interface methods

  private String getEngine(DbSourceInter<byte[]> ds) {
    if (ds instanceof LevelDbDataSourceImpl) {
      return ((LevelDbDataSourceImpl) ds).getEngine();
    }
    return ((RocksDbDataSourceImpl) ds).getEngine();
  }

  private String getName(DbSourceInter<byte[]> ds) {
    if (ds instanceof LevelDbDataSourceImpl) {
      return ((LevelDbDataSourceImpl) ds).getName();
    }
    return ((RocksDbDataSourceImpl) ds).getName();
  }

  private Set<byte[]> getlatestValues(DbSourceInter<byte[]> ds, long limit) {
    if (ds instanceof LevelDbDataSourceImpl) {
      return ((LevelDbDataSourceImpl) ds).getlatestValues(limit);
    }
    return ((RocksDbDataSourceImpl) ds).getlatestValues(limit);
  }

  private Map<byte[], byte[]> getNext(DbSourceInter<byte[]> ds, byte[] key, long limit) {
    if (ds instanceof LevelDbDataSourceImpl) {
      return ((LevelDbDataSourceImpl) ds).getNext(key, limit);
    }
    return ((RocksDbDataSourceImpl) ds).getNext(key, limit);
  }

  private List<byte[]> doGetKeysNext(DbSourceInter<byte[]> ds, byte[] key, long limit) {
    if (ds instanceof LevelDbDataSourceImpl) {
      return ((LevelDbDataSourceImpl) ds).getKeysNext(key, limit);
    }
    return ((RocksDbDataSourceImpl) ds).getKeysNext(key, limit);
  }

  private Set<byte[]> doGetValuesNext(DbSourceInter<byte[]> ds, byte[] key, long limit) {
    if (ds instanceof LevelDbDataSourceImpl) {
      return ((LevelDbDataSourceImpl) ds).getValuesNext(key, limit);
    }
    return ((RocksDbDataSourceImpl) ds).getValuesNext(key, limit);
  }
}
