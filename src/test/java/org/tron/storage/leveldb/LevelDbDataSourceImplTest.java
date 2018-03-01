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

package org.tron.storage.leveldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;

@Ignore
public class LevelDbDataSourceImplTest {

  @Before
  public void init() {
    Args.setParam(new String[]{}, Configuration.getByPath(Constant.TEST_CONF));
  }

  @Test
  public void testGet() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(Constant.OUTPUT_DIR,"test");
    dataSource.initDB();
    String key1 = "000134yyyhy";
    byte[] key = key1.getBytes();
    byte[] value = dataSource.getData(key);
    String s = ByteArray.toStr(value);
    dataSource.closeDB();
    System.out.println(s);
  }

  @Test
  public void testGetBlock() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(Constant.OUTPUT_DIR,
        "properties");
    dataSource.initDB();
    String key1 = "latest_block_header_number";
    byte[] key = key1.getBytes();
    byte[] value = dataSource.getData(key);
    Long s = ByteArray.toLong(value);
    dataSource.closeDB();
    System.out.println(s);
  }

  @Test
  public void testPutBloc() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(Constant.OUTPUT_DIR,
        "block");
    dataSource.initDB();
    String key1 = "latest_block_header_number";
    byte[] key = key1.getBytes();

    String value1 = "50000";
    byte[] value = value1.getBytes();

    dataSource.putData(key, value);

    assertNotNull(dataSource.getData(key));
    assertEquals(1, dataSource.allKeys().size());

    dataSource.closeDB();
  }

  @Test
  public void testPut() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(Constant.OUTPUT_DIR,
        "test");
    dataSource.initDB();
    String key1 = "000134yyyhy";
    byte[] key = key1.getBytes();

    String value1 = "50000";
    byte[] value = value1.getBytes();

    dataSource.putData(key, value);

    assertNotNull(dataSource.getData(key));
    assertEquals(1, dataSource.allKeys().size());

    dataSource.closeDB();
  }

  @Test
  public void testRest() {

    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(Constant.OUTPUT_DIR, "test");
    dataSource.resetDb();
    dataSource.closeDB();
  }

}