package org.tron.core.db2.core;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.config.args.Args;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.exception.ItemNotFoundException;

import java.util.Iterator;
import java.util.Map;

public class RevokingDBWithCachingNewValue implements IRevokingDB {
  @Setter
  @Getter
  private Snapshot head;
  @Getter
  private String dbName;
  @Autowired
  private SnapshotManager snapshotManager;

  public RevokingDBWithCachingNewValue(String dbName) {
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

  @Override
  public void reset() {
    head.reset();
    head = new SnapshotRoot(Args.getInstance().getOutputDirectoryByDbName(dbName), dbName);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    head.put(key, value);
  }

  @Override
  public void delete(byte[] key) {
    head.remove(key);
  }

  @Override
  public byte[] get(byte[] key) throws ItemNotFoundException {
    byte[] value = head.get(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException();
    }
    return value;
  }

  @Override
  public byte[] getUnchecked(byte[] key) {
    try {
      return get(key);
    } catch (ItemNotFoundException e) {
      return null;
    }
  }

  @Override
  public boolean has(byte[] key) {
    return head.get(key) != null;
  }

  @Override
  public Iterator<Map.Entry<byte[], byte[]>> iterator() {
    return head.iterator();
  }

}
