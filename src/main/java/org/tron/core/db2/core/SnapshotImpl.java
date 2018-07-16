package org.tron.core.db2.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.common.HashDB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SnapshotImpl extends AbstractSnapshot<Key, Value> {

  SnapshotImpl(Snapshot snapshot) {
    previous = snapshot;
    db = new HashDB();
  }

  @Override
  public byte[] get(byte[] key) {
    Value value = db.get(Key.of(key));
    if (value != null) {
      return value.getBytes();
    } else {
      return previous.get(key);
    }
  }

  @Override
  public void put(byte[] key, byte[] value) {
    Preconditions.checkNotNull(key, "key in db is not null.");
    Preconditions.checkNotNull(value, "value in db is not null.");

    Value.Operator operator;
    if (get(key) == null) {
      operator = Value.Operator.CREATE;
    } else {
      operator = Value.Operator.MODIFY;
    }

    db.put(Key.of(key), Value.of(operator, value));
  }

  @Override
  public void remove(byte[] key) {
    Preconditions.checkNotNull(key, "key in db is not null.");

    if (get(key) != null) {
      db.put(Key.of(key), Value.of(Value.Operator.DELETE, null));
    }
  }

  // we have a 4x4 matrix of all possibilities when merging previous snapshot and current snapshot :
  //                  --------------------- snapshot ------------------
  //                 /                                                 \
  //                +------------+------------+------------+------------+
  //                | new(Y)     | upd(Y)     | del        | nop        |
  //   +------------+------------+------------+------------+------------+
  // / | new(X)     | N/A        | new(Y)     | nop        | new(X)     |
  // | +------------+------------+------------+------------+------------+
  // p | upd(X)     | N/A        | upd(Y)     | del        | upd(X)     |
  // r +------------+------------+------------+------------+------------+
  // e | del        | upd(Y)     | N/A        | N/A        | del        |
  // | +------------+------------+------------+------------+------------+
  // \ | nop        | new(Y)     | upd(Y)     | del        | nop        |
  //   +------------+------------+------------+------------+------------+
  @Override
  public void merge(Snapshot from) {
    SnapshotImpl fromImpl = (SnapshotImpl) from;

    Streams.stream(fromImpl.db)
        .filter(e -> e.getValue().getOperator() == Value.Operator.CREATE)
        .forEach(e -> {
          Key k = e.getKey();
          Value v = e.getValue();
          Value value = db.get(k);
          if (value == null) {
            db.put(k, v);
          } else if (value.getOperator() == Value.Operator.DELETE) {
            db.put(k, Value.of(Value.Operator.MODIFY, v.getBytes()));
          }
        });

    Streams.stream(fromImpl.db)
        .filter(e -> e.getValue().getOperator() == Value.Operator.MODIFY)
        .forEach(e -> {
          Key k = e.getKey();
          Value v = e.getValue();
          Value value = db.get(k);
          if (value == null || value.getOperator() == Value.Operator.MODIFY) {
            db.put(k, v);
          } else if (value.getOperator() == Value.Operator.CREATE) {
            db.put(k, Value.of(Value.Operator.CREATE, v.getBytes()));
          }
        });

    Streams.stream(fromImpl.db)
        .filter(e -> e.getValue().getOperator() == Value.Operator.DELETE)
        .map(Map.Entry::getKey)
        .forEach(k -> {
          Value value = db.get(k);
          if (value == null || value.getOperator() == Value.Operator.MODIFY) {
            db.put(k, Value.of(Value.Operator.DELETE, null));
          } else if (value.getOperator() == Value.Operator.CREATE) {
            db.remove(k);
          }
        });
  }

  //todo need to resolve levelDB'iterator close
  @Override
  public Iterator<Map.Entry<byte[],byte[]>> iterator() {
    Set<WrappedByteArray> exists = new HashSet<>();
    return iterator(exists);
  }

  private Iterator<Map.Entry<byte[],byte[]>> iterator(Set<WrappedByteArray> exists) {
    Set<WrappedByteArray> currentExists = new HashSet<>(exists);
    Streams.stream(db)
        .map(e -> WrappedByteArray.of(e.getKey().getBytes()))
        .forEach(currentExists::add);

    Iterator<Map.Entry<byte[],byte[]>> preIterator;
    if (previous.getClass() == SnapshotImpl.class) {
      preIterator = Iterators.filter(((SnapshotImpl) previous).iterator(currentExists),
          e -> !currentExists.contains(WrappedByteArray.of(e.getKey())));
    } else {
      preIterator = Iterators.filter(previous.iterator(), e -> !currentExists.contains(WrappedByteArray.of(e.getKey())));
    }
    return Iterators.concat(
        Iterators.transform(db.iterator(), e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes())),
        preIterator);

  }

  @Override
  public void close() {
    previous.close();
  }

  @Override
  public void reset() {
    previous.reset();
  }
}
