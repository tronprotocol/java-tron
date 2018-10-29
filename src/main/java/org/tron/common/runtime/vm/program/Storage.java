package org.tron.common.runtime.vm.program;

import static java.lang.System.arraycopy;

import lombok.Getter;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.cache.CachedSource;
import org.tron.common.utils.ByteArraySet;
import org.tron.core.capsule.StorageRowCapsule;


public class Storage {
  private static final int PREFIX_BYTES = 16;
  private byte[] addrHash;  // contract address

  public Storage(byte[] address,
      CachedSource<byte[], StorageRowCapsule> backingSource) {
    this.addrHash = addrHash(address);
    this.cachedSource = backingSource;
    keysToDelete = new ByteArraySet();
  }

  private CachedSource<byte[], StorageRowCapsule> cachedSource;
  @Getter
  private ByteArraySet keysToDelete;

  public DataWord getValue(DataWord key) {
    byte[] rowKey = compose(key.getData(), addrHash);
    StorageRowCapsule row = cachedSource.get(rowKey);
    if (row == null || row.getInstance() == null) {
      return null;
    } else {
      return row.getValue();
    }
  }

  public void put(DataWord key, DataWord value) {
    byte[] rowKey = compose(key.getData(), addrHash);
    StorageRowCapsule row = new StorageRowCapsule(rowKey, value.getData());
    cachedSource.put(rowKey, row);

    if (value.isZero()) {
      keysToDelete.add(rowKey);
    } else {
      keysToDelete.remove(rowKey);
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

  public void commit() {
    keysToDelete.forEach(key -> {
      this.cachedSource.put(key, null);
    });
  }
}
