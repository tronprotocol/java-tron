package org.tron.common.runtime.vm.program;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.ethereum.crypto.HashUtil;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.storage.Key;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.db.Manager;
import org.tron.core.db.StorageRowStore;

public class StorageCache {

  private byte[] address;  // contract address
  private Manager manager;
  private final Map<Key, StorageRowCapsule> rowCache = new HashMap<>();
  private long beforeUseSize = 0;

  // for composor
  private static final int PREFIX_BYTES = 16;
  private static final int HASH_LEN = 32;


  public StorageCache(byte[] address, Manager manager) {
    this.address = address;
    this.manager = manager;
  }

  public DataWord getValue(DataWord key) {
    Key k = Key.create(key.getData());
    if (rowCache.containsKey(k)) {
      return rowCache.get(k).getValue();
    } else {
      StorageRowStore store = manager.getStorageRowStore();
      StorageRowCapsule row = store.get(compose(key.getData(), address));
      if (row == null) {
        return null;
      } else {
        beforeUseSize += row.getInstance().getSerializedSize();
      }
      rowCache.put(k, row);
      return row.getValue();
    }
  }

  public void put(DataWord key, DataWord value) {
    Key k = Key.create(key.getData());
    if (rowCache.containsKey(k)) {
      rowCache.get(k).setValue(value);
    } else {
      StorageRowStore store = manager.getStorageRowStore();
      byte[] composedKey = compose(key.getData(), address);
      StorageRowCapsule row = store.get(composedKey);

      if (row == null) {
        row = new StorageRowCapsule(composedKey, value.getData());
      } else {
        beforeUseSize += row.getInstance().getSerializedSize();
      }
      rowCache.put(k, row);
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

  public long getBeforeUseSize() {
    return this.beforeUseSize;
  }

  public void commit() {
    // TODO can just write dirty row
    StorageRowStore store = manager.getStorageRowStore();
    rowCache.forEach((key, value) -> {
      byte[] composedKey = compose(key.getData(), address);
      store.put(composedKey, value);
    });
  }
}
