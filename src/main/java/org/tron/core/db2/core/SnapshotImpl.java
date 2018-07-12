package org.tron.core.db2.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;
import org.tron.core.db2.common.HashDB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;

import java.util.Iterator;
import java.util.Map;

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

  //todo current cache need iterator
  @Override
  public Iterator<Map.Entry<byte[],byte[]>> iterator() {
    return previous.iterator();
  }

  @Override
  public void close() {
    previous.close();
  }
}
