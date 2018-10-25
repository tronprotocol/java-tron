package org.tron.common.runtime.vm.program;

import static java.lang.System.arraycopy;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.db.StorageRowStore;

@Slf4j(topic = "vm_storage")
public class Storage {

  private byte[] addrHash;  // contract address
  private StorageRowStore store;
  private final Map<DataWord, StorageRowCapsule> rowCache = new HashMap<>();

  private static final int PREFIX_BYTES = 16;

  public Storage(byte[] address, StorageRowStore store) {
    addrHash = addrHash(address);
    this.store = store;
  }

  public DataWord getValue(DataWord key) {
    StorageRowCapsule capsule = rowCache.get(key);
    if (capsule == null && !rowCache.containsKey(key)){
      capsule = store.get(compose(key));
      rowCache.put(key, capsule);
    }
    return (capsule == null) ? null : capsule.getValue();
  }

  public void put(DataWord key, DataWord value) {

    StorageRowCapsule capsule = rowCache.get(key);
    if (capsule == null){
      rowCache.put(key, new StorageRowCapsule(compose(key), value.getData()));
    }else {
      capsule.setValue(value);
    }
  }

  private byte[] compose(DataWord key) {
    byte[] realKey = key.getData();
    byte[] result = new byte[realKey.length];
    arraycopy(addrHash, 0, result, 0, PREFIX_BYTES);
    arraycopy(realKey, PREFIX_BYTES, result, PREFIX_BYTES, PREFIX_BYTES);
    return result;
  }

  // 32 bytes
  private static byte[] addrHash(byte[] address) {
    return Hash.sha3(address);
  }

  public void commit() {
    rowCache.forEach((key, value) -> {
      if (value.isDirty()) {
        byte[] rowKey = value.getRowKey();
        if (value.getValue().isZero()) {
          this.store.delete(rowKey);
        } else {
          this.store.put(rowKey, value);
        }
      }
    });
  }
}
