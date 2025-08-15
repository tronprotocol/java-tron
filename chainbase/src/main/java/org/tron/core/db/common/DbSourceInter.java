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
import java.util.Map;
import java.util.Set;
import org.tron.core.db2.common.WrappedByteArray;

public interface DbSourceInter<V> extends BatchSourceInter<byte[], V>,
    Iterable<Map.Entry<byte[], V>> {

  String getDBName();

  void setDBName(String name);

  void initDB();

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

}
