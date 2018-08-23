package org.tron.common.runtime.vm.program;

import static java.lang.System.arraycopy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.db.Manager;
import org.tron.core.db.StorageRowStore;

public class Storage {

  private byte[] addrHash;  // contract address
  private Manager manager;
  private final Map<DataWord, StorageRowCapsule> rowCache = new HashMap<>();
  private long beforeUseSize = 0;

  private static final int PREFIX_BYTES = 16;

  public Storage(byte[] address, Manager manager) {
    addrHash = addrHash(address);
    this.manager = manager;
  }

  public DataWord getValue(DataWord key) {
    if (rowCache.containsKey(key)) {
      return rowCache.get(key).getValue();
    } else {
      StorageRowStore store = manager.getStorageRowStore();
      StorageRowCapsule row = store.get(compose(key.getData(), addrHash));
      if (row == null || row.getInstance() == null) {
        return null;
      } else {
        beforeUseSize += row.getInstance().length;
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
      byte[] rowKey = compose(key.getData(), addrHash);
      StorageRowCapsule row = store.get(rowKey);
      if (row == null || row.getInstance() == null) {
        row = new StorageRowCapsule(rowKey, value.getData());
      } else {
        beforeUseSize += row.getInstance().length;
      }
      rowCache.put(key, row);
    }
  }

  private static byte[] compose(byte[] key, byte[] addrHash) {
    byte[] result = new byte[key.length];
    arraycopy(addrHash, 0, result, 0, PREFIX_BYTES);
    arraycopy(key, PREFIX_BYTES, result, PREFIX_BYTES, PREFIX_BYTES);
    return result;
  }

  // 32 bytes
  private static byte[] addrHash(byte[] address) {
    return Hash.sha3(address);
  }

  public long computeSize() {
    AtomicLong size = new AtomicLong();
    rowCache.forEach((key, value) -> {
      if (!value.getValue().isZero()) {
        size.getAndAdd(value.getInstance().length);
      }
    });
    return size.get();
  }

  public long getBeforeUseSize() {
    return this.beforeUseSize;
  }

  public void commit() {
    rowCache.forEach((key, value) -> {
      if (value.isDirty()) {
        if (value.getValue().isZero()) {
          manager.getStorageRowStore().delete(value.getRowKey());
        } else {
          manager.getStorageRowStore().put(value.getRowKey(), value);
        }
      }
    });
  }
}
