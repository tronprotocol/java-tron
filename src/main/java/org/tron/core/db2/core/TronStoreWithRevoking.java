package org.tron.core.db2.core;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.common.iterator.AbstractIterator;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

import java.util.Iterator;
import java.util.Map.Entry;

@Slf4j
public abstract class TronStoreWithRevoking<T extends ProtoCapsule> extends TronDatabase<T> {

  @Setter
  @Getter
  private Snapshot head;
  @Getter
  private String dbName;
  @Autowired
  private SnapshotManager snapshotManager;

  protected TronStoreWithRevoking(String dbName) {
    this.dbName = dbName;
    head = new SnapshotRoot(Args.getInstance().getOutputDirectoryByDbName(dbName), dbName);
    snapshotManager.add(this);
  }

  /**
   * close the database.
   */
  @Override
  public void close() {
    head.close();
  }

  public void put(byte[] key, T item) {
    head.put(key, item.getData());
  }

  public void delete(byte[] key) {
    head.remove(key);
  }

  protected abstract T of(byte[] value) throws BadItemException;

  public T get(byte[] key) throws ItemNotFoundException, BadItemException {
    byte[] value = head.get(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException();
    }
    return of(value);

  }

  public boolean has(byte[] key) {
    return head.get(key) != null;
  }

  @Override
  public Iterator<Entry<byte[], T>> iterator() {
    return new AbstractIterator<T>(head.iterator()) {
      @Override
      protected T of(byte[] value) {
        try {
          return TronStoreWithRevoking.this.of(value);
        } catch (BadItemException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }
}
