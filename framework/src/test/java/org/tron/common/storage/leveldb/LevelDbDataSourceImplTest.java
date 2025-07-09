/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PublicMethod;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.exception.TronError;

@Slf4j
public class LevelDbDataSourceImplTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();
  private static LevelDbDataSourceImpl dataSourceTest;

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
  }

  @Before
  public void initDb() throws IOException {
    Args.setParam(new String[]{"--output-directory",
        temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
    dataSourceTest = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory() + File.separator, "test_levelDb");
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
    assertEquals("50000", ByteArray.toStr(dataSourceTest.getData(key1.getBytes())));
    dataSourceTest.closeDB();
  }

  @Test
  public void testReset() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_reset");
    dataSource.resetDb();
    assertEquals(0, dataSource.allKeys().size());
    assertEquals("LEVELDB", dataSource.getEngine());
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
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_updateByBatch");
    dataSource.initDB();
    dataSource.resetDb();
    String key1 = PublicMethod.getRandomPrivateKey();
    String value1 = "50000";
    String key2 =  PublicMethod.getRandomPrivateKey();
    String value2 = "10000";

    Map<byte[], byte[]> rows = new HashMap<>();
    rows.put(key1.getBytes(), value1.getBytes());
    rows.put(key2.getBytes(), value2.getBytes());

    dataSource.updateByBatch(rows);

    assertEquals("50000", ByteArray.toStr(dataSource.getData(key1.getBytes())));
    assertEquals("10000", ByteArray.toStr(dataSource.getData(key2.getBytes())));
    assertEquals(2, dataSource.allKeys().size());
    dataSource.closeDB();
  }

  @Test
  public void testdeleteData() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
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
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
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
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
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

  private void putSomeKeyValue(LevelDbDataSourceImpl dataSource) {
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
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
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
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_getValuesNext_key");
    dataSource.initDB();
    dataSource.resetDb();

    putSomeKeyValue(dataSource);
    Set<byte[]> seekKeyLimitNext = dataSource.getValuesNext("0000000300".getBytes(), 2);
    HashSet<String> hashSet = Sets.newHashSet(ByteArray.toStr(value3), ByteArray.toStr(value4));
    seekKeyLimitNext.forEach(valeu -> {
      Assert.assertTrue("getValuesNext", hashSet.contains(ByteArray.toStr(valeu)));
    });
    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test
  public void testGetTotal() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
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

  @Test
  public void getKeysNext() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
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
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
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
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_initDb");
    TronError thrown = assertThrows(TronError.class, dataSource::initDB);
    assertEquals(TronError.ErrCode.LEVELDB_INIT, thrown.getErrCode());
  }

  private void makeExceptionDb(String dbName) {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "test_initDb");
    dataSource.initDB();
    dataSource.closeDB();
    FileUtil.saveData(dataSource.getDbPath().toString() + "/CURRENT",
        "...", Boolean.FALSE);
  }

}
