/*
 * Copyright (c) [2016] [ <ether.camp> ] This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with the ethereumJ
 * library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.storage.leveldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Constant;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;

public class LevelDbDataSourceImplTest {

  private LevelDbDataSourceImpl dataSource;

  @Before
  public void setup() {
    Args.setParam(new String[]{}, Configuration.getByPath(Constant.TEST_CONF));
    dataSource = new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectory(), "test");
  }

  @Test
  public void testGet() {
    dataSource.initDB();
    String key1 = "000134yyyhy";
    byte[] key = key1.getBytes();
    byte[] value = dataSource.getData(key);
    String s = ByteArray.toStr(value);
    dataSource.closeDB();
    System.out.println(s);
  }

  @Test
  public void testPut() {
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
    dataSource.resetDb();
    dataSource.closeDB();
  }

  @Test(timeout = 100)
  public void testLockReleased() {
    dataSource.initDB();
    // normal close
    dataSource.closeDB();
    // closing already closed db.
    dataSource.closeDB();
    // closing again to make sure the lock is free. If not test will hang.
    dataSource.closeDB();

    assertFalse("Database is still alive after closing.", dataSource.isAlive());
  }

}
