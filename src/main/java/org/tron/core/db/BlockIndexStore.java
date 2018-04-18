package org.tron.core.db;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.exception.ItemNotFoundException;

public class BlockIndexStore extends TronStoreWithRevoking<BytesCapsule> {

  public BlockIndexStore(String dbName) {
    super(dbName);

  }

  private static BlockIndexStore instance;

  public static void destroy() {
    instance = null;
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static BlockIndexStore create(String dbName) {
    if (instance == null) {
      synchronized (BlockIndexStore.class) {
        if (instance == null) {
          instance = new BlockIndexStore(dbName);
        }
      }
    }
    return instance;
  }

  public void put(BlockId id) {
    put(ByteArray.fromLong(id.getNum()), new BytesCapsule(id.getBytes()));
  }


  public BlockId get(Long num)
      throws ItemNotFoundException {
    return new BlockId(Sha256Hash.wrap(get(ByteArray.fromLong(num)).getData()),
        num);
  }

  @Override
  public BytesCapsule get(byte[] key)
      throws ItemNotFoundException {
    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException("number: " + key + " is not found!");
    }
    return new BytesCapsule(value);
  }


  @Override
  public boolean has(byte[] key) {
    return false;
  }
}