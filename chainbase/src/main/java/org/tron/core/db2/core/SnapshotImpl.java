package org.tron.core.db2.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import org.tron.core.capsule.Proto;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.db2.common.HashDB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;
import org.tron.core.db2.common.Value.Operator;
import org.tron.core.db2.common.WrappedByteArray;

public class SnapshotImpl<T extends ProtoCapsule> extends AbstractSnapshot<Key, Value<T>, T> {

  @Getter
  protected Snapshot root;
  private Class<T> clz;

  SnapshotImpl(Snapshot snapshot, Class<T> clz) {
    root = snapshot.getRoot();
    synchronized (this) {
      db = new HashDB<>(SnapshotImpl.class.getSimpleName() + ":" + root.getDbName());
    }
    previous = snapshot;
    snapshot.setNext(this);
    this.clz = clz;
    // inherit
    isOptimized = snapshot.isOptimized();
    // merge for DynamicPropertiesStore，about 100 keys
    if (isOptimized) {
      if (root == previous) {
        Streams.stream(((SnapshotRoot) root).iterator()).forEach(e -> put(e.getKey(),
            Proto.of(e.getValue(), clz)));
      } else {
        merge(previous);
      }
    }
  }

  @Override
  public boolean isRoot() {
    return false;
  }

  @Override
  public boolean isImpl() {
    return true;
  }

  @Override
  public T get(byte[] key) {
    return get(this, key);
  }

  private T get(Snapshot<T> head, byte[] key) {
    Snapshot<T> snapshot = head;
    Value<T> value;
    if (isOptimized) {
      value = db.get(Key.of(key));
      return value == null ? null : value.getData();
    }
    while (snapshot.isImpl()) {
      if ((value = ((SnapshotImpl<T>) snapshot).db.get(Key.of(key))) != null) {
        return value.getData();
      }
      snapshot = snapshot.getPrevious();
    }

    return Proto.of(((SnapshotRoot) snapshot.getRoot()).get(key), clz);
  }

  @Override
  public void put(byte[] key, T value) {
    Preconditions.checkNotNull(key, "key in db is not null.");
    Preconditions.checkNotNull(value, "value in db is not null.");

    db.put(Key.copyOf(key), new Value<>(Value.Operator.PUT, value));
  }

  @Override
  public void remove(byte[] key) {
    Preconditions.checkNotNull(key, "key in db is not null.");
    db.put(Key.of(key),  new Value<>(Value.Operator.DELETE, null));
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
    SnapshotImpl<T> fromImpl = (SnapshotImpl<T>) from;
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
  public Iterator<Map.Entry<byte[], T>> iterator() {
    Map<WrappedByteArray, T> all = new HashMap<>();
    collect(all);
    Set<WrappedByteArray> keys = new HashSet<>(all.keySet());
    all.entrySet()
        .removeIf(entry -> entry.getValue() == null || entry.getValue().getData() == null);
    return Iterators.concat(
        Iterators.transform(all.entrySet().iterator(),
            e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue())),
        Iterators.filter(Iterators.transform(((SnapshotRoot) getRoot()).iterator(),
                e -> Maps.immutableEntry(e.getKey(), Proto.of(e.getValue(), clz))),
            e -> !keys.contains(WrappedByteArray.of(e.getKey()))));
  }

  synchronized void collect(Map<WrappedByteArray, T> all) {
    Snapshot<T> next = getRoot().getNext();
    while (next != null) {
      Streams.stream(((SnapshotImpl<T>) next).db)
          .forEach(e -> all.put(WrappedByteArray.of(e.getKey().getBytes()),
              e.getValue().getData()));
      next = next.getNext();
    }
  }

  synchronized void collect(Map<WrappedByteArray, T> all, byte[] prefix) {
    Snapshot next = getRoot().getNext();
    while (next != null) {
      Streams.stream(((SnapshotImpl<T>) next).db).filter(e -> Bytes.indexOf(
              Objects.requireNonNull(e.getKey().getBytes()), prefix) == 0)
          .forEach(e -> all.put(WrappedByteArray.of(e.getKey().getBytes()),
              e.getValue().getData()));
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
      Streams.stream(((SnapshotImpl<T>) next).db)
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
    return new SnapshotImpl(this, this.clz);
  }
}
