package org.tron.core.db2.snapshot;

import org.tron.core.db.common.iterator.DBIterator;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;

import java.util.Iterator;
import java.util.Map;

public class SnapshotImpl extends AbstractSnapshot<Key, Value> {

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
    db.put(Key.of(key), Value.of(Value.Operator.DELETE, null));
  }

  @Override
  public void merge(Snapshot from) {

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
