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

package org.tron.core.db.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.exception.TronError;

public interface DbSourceInter<V> extends BatchSourceInter<byte[], V>,
    Iterable<Map.Entry<byte[], V>> {

  String ENGINE_KEY = "ENGINE";
  String ENGINE_FILE = "engine.properties";
  String ROCKSDB = "ROCKSDB";
  String LEVELDB = "LEVELDB";

  String getDBName();

  void setDBName(String name);

  boolean isAlive();

  void closeDB();

  void resetDb();

  @VisibleForTesting
  @Deprecated
  Set<byte[]> allKeys() throws RuntimeException;

  @VisibleForTesting
  @Deprecated
  Set<byte[]> allValues() throws RuntimeException;

  @VisibleForTesting
  @Deprecated
  long getTotal() throws RuntimeException;

  void stat();

  Map<WrappedByteArray, byte[]> prefixQuery(byte[] key);

  static void checkOrInitEngine(String expectedEngine, String dir, TronError.ErrCode errCode) {
    String engineFile = Paths.get(dir, ENGINE_FILE).toString();
    File currentFile = new File(dir, "CURRENT");
    if (ROCKSDB.equals(expectedEngine) && currentFile.exists()
        && !Paths.get(engineFile).toFile().exists()) {
      // if the CURRENT file exists, but the engine.properties file does not exist, it is LevelDB
      // 000003.log        CURRENT           LOCK              MANIFEST-000002
      throw new TronError(
          String.format("Cannot open %s database with %s engine.", LEVELDB, ROCKSDB), errCode);
    }
    if (FileUtil.createDirIfNotExists(dir)) {
      if (!FileUtil.createFileIfNotExists(engineFile)) {
        throw new TronError(String.format("Cannot create file: %s.", engineFile), errCode);
      }
    } else {
      throw new TronError(String.format("Cannot create dir: %s.", dir), errCode);
    }
    String actualEngine = PropUtil.readProperty(engineFile, ENGINE_KEY);
    // engine init
    if (Strings.isNullOrEmpty(actualEngine)
        && !PropUtil.writeProperty(engineFile, ENGINE_KEY, expectedEngine)) {
      throw new TronError(String.format("Cannot write file: %s.", engineFile), errCode);
    }
    actualEngine = PropUtil.readProperty(engineFile, ENGINE_KEY);
    if (!expectedEngine.equals(actualEngine)) {
      throw new TronError(String.format(
          "Cannot open %s database with %s engine.",
          actualEngine, expectedEngine), errCode);
    }
  }
}
