package org.tron.core.vm.program;

import static java.lang.System.arraycopy;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.ForkController;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.store.StorageRowStore;

public class Storage {

  private static final int PREFIX_BYTES = 16;
  private static final int WORD_BYTES = 32;

  private enum ReadKind {
    NEW, OLD, EMPTY
  }

  @Getter
  private final Map<DataWord, StorageRowCapsule> rowCache = new HashMap<>();
  private final Map<DataWord, DataWord> oldRowKeyOwners = new HashMap<>();
  private final Map<DataWord, ReadKind> readKinds = new HashMap<>();
  private final boolean optimizeTvmStorage;
  private final boolean aliasCheckEnabled;

  @Getter
  private byte[] addrHash;
  @Getter
  private StorageRowStore store;
  @Getter
  private byte[] address;
  @Setter
  private int contractVersion;

  public Storage(byte[] address, StorageRowStore store, boolean optimizeTvmStorage) {
    addrHash = addrHash(address);
    this.address = address;
    this.store = store;
    this.optimizeTvmStorage = optimizeTvmStorage;
    this.aliasCheckEnabled = ForkController.instance().pass(ForkBlockVersionEnum.VERSION_4_8_2_3);
  }

  public Storage(Storage storage) {
    this.addrHash = storage.addrHash.clone();
    this.address = storage.getAddress().clone();
    this.store = storage.store;
    this.contractVersion = storage.contractVersion;
    this.optimizeTvmStorage = storage.optimizeTvmStorage;
    this.aliasCheckEnabled = storage.aliasCheckEnabled;
    storage.getRowCache().forEach((DataWord key, StorageRowCapsule row) -> {
      StorageRowCapsule newRow = new StorageRowCapsule(row);
      this.rowCache.put(key.clone(), newRow);
    });
    storage.oldRowKeyOwners.forEach((DataWord legacy, DataWord owner) ->
        this.oldRowKeyOwners.put(legacy.clone(), owner.clone()));
    storage.readKinds.forEach((DataWord key, ReadKind kind) ->
        this.readKinds.put(key.clone(), kind));
  }

  private byte[] compose(byte[] key, byte[] addrHash) {
    if (contractVersion == 1) {
      key = Hash.sha3(key);
    }
    byte[] result = new byte[key.length];
    arraycopy(addrHash, 0, result, 0, PREFIX_BYTES);
    arraycopy(key, PREFIX_BYTES, result, PREFIX_BYTES, PREFIX_BYTES);
    return result;
  }

  private byte[] getOldRowKey(DataWord key) {
    return compose(key.getData(), addrHash);
  }

  private byte[] getNewRowKey(DataWord key) {
    byte[] result = new byte[PREFIX_BYTES + WORD_BYTES];
    arraycopy(addrHash, 0, result, 0, PREFIX_BYTES);
    arraycopy(Hash.sha3(ByteUtil.merge(addrHash, key.getData())), 0, result, PREFIX_BYTES,
        WORD_BYTES);
    return result;
  }

  // 32 bytes
  private static byte[] addrHash(byte[] address) {
    return Hash.sha3(address);
  }

  private static byte[] addrHash(byte[] address, byte[] trxHash) {
    if (ByteUtil.isNullOrZeroArray(trxHash)) {
      return Hash.sha3(address);
    }
    return Hash.sha3(ByteUtil.merge(address, trxHash));
  }

  public void generateAddrHash(byte[] trxId) {
    // update addreHash for create2
    addrHash = addrHash(address, trxId);
  }

  public DataWord getValue(DataWord key) {
    if (optimizeTvmStorage) {
      return getOptimized(key);
    }
    if (aliasCheckEnabled) {
      checkAlias(key);
    }
    if (rowCache.containsKey(key)) {
      return new DataWord(rowCache.get(key).getValue());
    } else {
      StorageRowCapsule row = store.get(compose(key.getData(), addrHash));
      if (row == null || row.getInstance() == null) {
        return null;
      }
      rowCache.put(key, row);
      return new DataWord(row.getValue());
    }
  }

  public void put(DataWord key, DataWord value) {
    if (!optimizeTvmStorage && aliasCheckEnabled) {
      checkAlias(key);
    }
    if (rowCache.containsKey(key)) {
      rowCache.get(key).setValue(value.getData());
    } else {
      byte[] rowKey = optimizeTvmStorage ? getNewRowKey(key) : getOldRowKey(key);
      StorageRowCapsule row = new StorageRowCapsule(rowKey, value.getData());
      rowCache.put(key, row);
    }
    if (optimizeTvmStorage) {
      ownOldKey(key);
    }
  }

  private DataWord getOptimized(DataWord key) {
    if (rowCache.containsKey(key)) {
      return new DataWord(rowCache.get(key).getValue());
    }
    if (readKinds.get(key) == ReadKind.EMPTY
        || readKinds.get(key) == ReadKind.NEW) {
      return null;
    }

    byte[] newRowKey = getNewRowKey(key);
    StorageRowCapsule newRow = store.get(newRowKey);
    if (newRow != null && newRow.getValue() != null) {
      readKinds.put(key.clone(), ReadKind.NEW);
      if (DataWord.isZero(newRow.getValue())) {
        return null;
      }
      return cacheRead(key, newRowKey, newRow.getValue());
    }

    if (oldKeyTaken(key)) {
      return null;
    }

    byte[] oldRowKey = getOldRowKey(key);
    StorageRowCapsule oldRow = store.get(oldRowKey);
    if (oldRow != null && oldRow.getValue() != null) {
      readKinds.put(key.clone(), ReadKind.OLD);
      return cacheRead(key, oldRowKey, oldRow.getValue());
    }

    readKinds.put(key.clone(), ReadKind.EMPTY);
    return null;
  }

  private DataWord cacheRead(DataWord key, byte[] rowKey, byte[] value) {
    StorageRowCapsule row = new StorageRowCapsule(value.clone());
    row.setRowKey(rowKey);
    rowCache.put(key, row);
    ownOldKey(key);
    return new DataWord(row.getValue());
  }

  private void checkAlias(DataWord key) {
    DataWord oldRowKey = new DataWord(getOldRowKey(key));
    DataWord owner = oldRowKeyOwners.get(oldRowKey);
    if (owner == null) {
      oldRowKeyOwners.put(oldRowKey, key.clone());
    } else if (!owner.equals(key)) {
      throw new Program.OutOfTimeException("CPU timeout for storage check");
    }
  }

  private void ownOldKey(DataWord key) {
    oldRowKeyOwners.putIfAbsent(new DataWord(getOldRowKey(key)), key.clone());
  }

  private boolean oldKeyTaken(DataWord key) {
    DataWord owner = oldRowKeyOwners.get(new DataWord(getOldRowKey(key)));
    return owner != null && !owner.equals(key);
  }

  public void commit() {
    if (optimizeTvmStorage) {
      commitOptimized();
      return;
    }
    rowCache.forEach((DataWord key, StorageRowCapsule row) -> {
      if (row.isDirty()) {
        if (new DataWord(row.getValue()).isZero()) {
          this.store.delete(row.getRowKey());
        } else {
          this.store.put(row.getRowKey(), row);
        }
      }
    });
  }

  private void commitOptimized() {
    rowCache.forEach((DataWord key, StorageRowCapsule row) -> {
      if (!row.isDirty()) {
        return;
      }
      putNew(key, row.getValue());
      ReadKind kind = readKinds.get(key);
      if (kind == null || kind == ReadKind.OLD) {
        store.delete(getOldRowKey(key));
      }
    });
    readKinds.forEach((DataWord key, ReadKind kind) -> {
      StorageRowCapsule row = rowCache.get(key);
      if (row != null && row.isDirty()) {
        return;
      }
      if (kind == ReadKind.OLD) {
        putNew(key, row.getValue());
        store.delete(getOldRowKey(key));
      }
    });
  }

  private void putNew(DataWord key, byte[] value) {
    store.put(getNewRowKey(key), new StorageRowCapsule(value));
  }

}
