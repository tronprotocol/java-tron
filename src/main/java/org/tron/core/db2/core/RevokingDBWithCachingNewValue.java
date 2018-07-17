package org.tron.core.db2.core;

import com.google.common.collect.Streams;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.config.args.Args;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.common.Value;
import org.tron.core.exception.ItemNotFoundException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

  @Override
  public Set<byte[]> getlatestValues(long limit) {
    if (limit <= 0) {
      return Collections.emptySet();
    }

    Set<byte[]> result = new HashSet<>();
    Snapshot snapshot = head;
    long tmp = limit;
    for (; tmp > 0 && snapshot.getPrevious() != null; --tmp) {
      Streams.stream(((SnapshotImpl) snapshot).db)
          .map(Map.Entry::getValue)
          .map(Value::getBytes)
          .forEach(result::add);
    }

    if (snapshot.getPrevious() == null && tmp != 0) {
      result.addAll(((LevelDB) ((SnapshotRoot) snapshot).db).getDb().getlatestValues(tmp));
    }

    return result;
  }

  //todo
  @Override
  public Set<byte[]> getValuesNext(byte[] key, long limit) {
    return null;
  }

}
