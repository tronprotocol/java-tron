package org.tron.core.db;

import org.tron.core.capsule.BytesCapsule;
import org.tron.core.exception.BadItemException;
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

  @Override
  public BytesCapsule get(byte[] key)
      throws ItemNotFoundException, BadItemException {
    return new BytesCapsule(dbSource.getData(key));
  }


  @Override
  public boolean has(byte[] key) {
    return false;
  }
}