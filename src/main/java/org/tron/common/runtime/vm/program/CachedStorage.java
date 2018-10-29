package org.tron.common.runtime.vm.program;

import static java.lang.System.arraycopy;

import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.cache.CachedSource;
import org.tron.core.capsule.StorageRowCapsule;


public class CachedStorage {
  private static final int PREFIX_BYTES = 16;
  private byte[] addrHash;  // contract address

  public CachedStorage(byte[] addrHash,
      CachedSource<byte[], StorageRowCapsule> backingSource) {
    this.addrHash = addrHash;
    this.cachedSource = backingSource;
  }

  private CachedSource<byte[], StorageRowCapsule> cachedSource;

  public DataWord getValue(DataWord key) {
    StorageRowCapsule row = cachedSource.get(compose(key.getData(), addrHash));
    return row.getInstance() == null ? null : row.getValue();
  }

  public void put(DataWord key, DataWord value) {
    StorageRowCapsule row = cachedSource.get(compose(key.getData(), addrHash));
    row.setValue(value);
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
    this.cachedSource.commit();
  }

}
