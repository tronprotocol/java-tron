package org.tron.core.db2.core;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import lombok.Getter;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.common.HashDB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.common.Value;
import org.tron.core.db2.common.Value.Operator;

public class SnapshotRoot extends AbstractSnapshot<byte[], byte[]> {
  @Getter
  private HashDB cache = new HashDB();

  public SnapshotRoot(String parentName, String name) {
    db = new LevelDB(parentName, name);
  }

  @Override
  public synchronized byte[] get(byte[] key) {
    Value value = cache.get(Key.of(key));
    if (value != null) {
      return value.getBytes();
    }

    return db.get(key);
  }

  @Override
  public synchronized void put(byte[] key, byte[] value) {
    cache.put(Key.of(key), Value.copyOf(Operator.MODIFY, value));
  }

  @Override
  public synchronized void remove(byte[] key) {
    cache.put(Key.of(key), Value.of(Operator.DELETE, null));
  }

  @Override
  public synchronized void merge(Snapshot from) {
    cache.putAll(((SnapshotImpl) from).db.asMap());
  }

  public synchronized void flush() {
    Map<WrappedByteArray, WrappedByteArray> batch = new HashMap<>((int) cache.size());
    Streams.stream(cache)
        .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey().getBytes()),
            WrappedByteArray.of(e.getValue().getBytes())))
        .forEach(e -> batch.put(e.getKey(), e.getValue()));
    ((LevelDB) db).flush(batch);
    cache.clear();
  }

  @Override
  public synchronized Snapshot retreat() {
    return this;
  }

  @Override
  public synchronized Snapshot getRoot() {
    return this;
  }

  @Override
  public synchronized Snapshot getSolidity() {
    return this;
  }

  @Override
  public synchronized Iterator<Map.Entry<byte[], byte[]>> iterator() {
    return Iterators.concat(
        Iterators.transform(
            Iterators.filter(cache.iterator(), e -> e.getValue().getBytes() != null),
            e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes())),
        Iterators.filter(db.iterator(), e -> cache.get(Key.of(e.getKey())) == null));
  }

  @Override
  public synchronized void close() {
    ((LevelDB) db).close();
  }

  @Override
  public synchronized void reset() {
    ((LevelDB) db).reset();
  }
}
