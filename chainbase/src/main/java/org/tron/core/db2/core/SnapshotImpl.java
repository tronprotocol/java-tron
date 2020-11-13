package org.tron.core.db2.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.tron.core.db2.common.HashDB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;
import org.tron.core.db2.common.Value.Operator;
import org.tron.core.db2.common.WrappedByteArray;

public class SnapshotImpl extends AbstractSnapshot<Key, Value> {

  @Getter
  protected Snapshot root;

  SnapshotImpl(Snapshot snapshot) {
    synchronized (this) {
      db = new HashDB(SnapshotImpl.class.getSimpleName());
    }

    root = snapshot.getRoot();
    previous = snapshot;
    snapshot.setNext(this);
  }

  @Override
  public byte[] get(byte[] key) {
    return get(this, key);
  }

  private byte[] get(Snapshot head, byte[] key) {
    Snapshot snapshot = head;
    Value value;
    while (Snapshot.isImpl(snapshot)) {
      if ((value = ((SnapshotImpl) snapshot).db.get(Key.of(key))) != null) {
        return value.getBytes();
      }

      snapshot = snapshot.getPrevious();
    }

    return snapshot == null ? null : snapshot.get(key);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    Preconditions.checkNotNull(key, "key in db is not null.");
    Preconditions.checkNotNull(value, "value in db is not null.");

    db.put(Key.copyOf(key), Value.copyOf(Value.Operator.PUT, value));
  }

  @Override
  public void remove(byte[] key) {
    Preconditions.checkNotNull(key, "key in db is not null.");
    db.put(Key.of(key), Value.of(Value.Operator.DELETE, null));
  }

  // we have a 3x3 matrix of all possibilities when merging previous snapshot and current snapshot :
  //                  -------------- snapshot -------------
  //                 /                                     \
  //                +------------+------------+------------+
  //                | put(Y)     | delete(Y)  | nop        |
  //   +------------+------------+------------+------------+
  // / | put(X)     | put(Y)     | del        | put(X)     |
  // p +------------+------------+------------+------------+
  // r | delete(X)  | put(Y)     | del        | del        |
  // e +------------+------------+------------+------------+
  // | | nop        | put(Y)     | del        | nop        |
  // \ +------------+------------+------------+------------+
  @Override
  public void merge(Snapshot from) {
    SnapshotImpl fromImpl = (SnapshotImpl) from;
    Streams.stream(fromImpl.db).forEach(e -> db.put(e.getKey(), e.getValue()));
  }

  @Override
  public Snapshot retreat() {
    return previous;
  }

  @Override
  public Snapshot getSolidity() {
    return root.getSolidity();
  }

  @Override
  public Iterator<Map.Entry<byte[], byte[]>> iterator() {
    Map<WrappedByteArray, WrappedByteArray> all = new HashMap<>();
    collect(all);
    Set<WrappedByteArray> keys = new HashSet<>(all.keySet());
    all.entrySet()
        .removeIf(entry -> entry.getValue() == null || entry.getValue().getBytes() == null);
    return Iterators.concat(
        Iterators.transform(all.entrySet().iterator(),
            e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes())),
        Iterators.filter(getRoot().iterator(),
            e -> !keys.contains(WrappedByteArray.of(e.getKey()))));
  }

  synchronized void collect(Map<WrappedByteArray, WrappedByteArray> all) {
    Snapshot next = getRoot().getNext();
    while (next != null) {
      Streams.stream(((SnapshotImpl) next).db)
          .forEach(e -> all.put(WrappedByteArray.of(e.getKey().getBytes()),
              WrappedByteArray.of(e.getValue().getBytes())));
      next = next.getNext();
    }
  }

  /**
   * Note: old --> new
   * In the snapshot, there may be same keys.
   * If we use Map to get all the data, the later will overwrite the previous value.
   * So, if we use list, we need to exclude duplicate keys.
   * More than that, there will be some item which has been deleted, but just assigned in Operator,
   * so we need Operator value to determine next step.
   * */
  synchronized void collectUnique(Map<WrappedByteArray, Operator> all) {
    Snapshot next = getRoot().getNext();
    while (next != null) {
      Streams.stream(((SnapshotImpl) next).db)
          .forEach(e -> all.put(WrappedByteArray.of(e.getKey().getBytes()),
              e.getValue().getOperator()));
      next = next.getNext();
    }
  }



  @Override
  public void close() {
    getRoot().close();
  }

  @Override
  public void reset() {
    getRoot().reset();
  }

  @Override
  public void resetSolidity() {
    root.resetSolidity();
  }

  @Override
  public void updateSolidity() {
    root.updateSolidity();
  }

  @Override
  public String getDbName() {
    return root.getDbName();
  }

  @Override
  public Snapshot newInstance() {
    return new SnapshotImpl(this);
  }
}
