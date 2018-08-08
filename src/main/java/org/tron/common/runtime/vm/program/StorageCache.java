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
package org.tron.common.runtime.vm.program;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.ethereum.crypto.HashUtil;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.db.Manager;
import org.tron.core.db.StorageRowStore;

public class StorageCache {

  private byte[] address;  // contract address
  private Manager manager;
  private final Map<DataWord, StorageRowCapsule> rowCache = new HashMap<>();

  // for composor
  public static final int PREFIX_BYTES = 16;
  public static final int HASH_LEN = 32;


  public StorageCache(byte[] address, Manager manager) {
    this.address = address;
    this.manager = manager;
  }

  public DataWord getValue(DataWord key) {
    if (rowCache.containsKey(key)) {
      return rowCache.get(key).getValue();
    } else {
      StorageRowStore store = manager.getStorageRowStore();
      StorageRowCapsule row = store.get(compose(key.getData(), address));
      if (row == null) {
        return null;
      }
      rowCache.put(key, row);
      return row.getValue();
    }
  }

  public void put(DataWord key, DataWord value) {
    if (rowCache.containsKey(key)) {
      rowCache.get(key).setValue(value);
    } else {
      StorageRowStore store = manager.getStorageRowStore();
      byte[] composedKey = compose(key.getData(), address);
      StorageRowCapsule row = store.get(composedKey);

      if (row == null) {
        row = new StorageRowCapsule(composedKey, value.getData());
      }
      rowCache.put(key, row);
    }
  }

  private static byte[] compose(byte[] key, byte[] addrOrHash) {
    return composeInner(key, addrHash(addrOrHash));
  }

  private static byte[] composeInner(byte[] key, byte[] addrhash) {
    byte[] derivative = new byte[key.length];
    System.arraycopy(key, 0, derivative, 0, PREFIX_BYTES);
    System.arraycopy(addrhash, 0, derivative, PREFIX_BYTES, PREFIX_BYTES);
    return derivative;
  }

  private static byte[] addrHash(byte[] addrHash) {
    return addrHash.length == HASH_LEN ? addrHash : HashUtil.sha3(addrHash);
  }

  public long computeSize() {
    AtomicLong size = new AtomicLong();
    rowCache.forEach((key, value) -> {
      size.getAndAdd(value.getInstance().getSerializedSize());
    });
    return size.get();
  }

  public void commit() {
    StorageRowStore store = manager.getStorageRowStore();
    rowCache.forEach((key, value) -> {
      byte[] composedKey = compose(key.getData(), address);
      StorageRowCapsule row = store.get(composedKey);
      store.put(composedKey, row);
    });
  }
}
