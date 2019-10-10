package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import lombok.Getter;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.common.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SnapshotRoot extends AbstractSnapshot<byte[], byte[]> {

  @Getter
  private Snapshot solidity;

  public SnapshotRoot(String parentName, String name, Class<? extends DB> clz) {
    try {
      if (clz == LevelDB.class || clz == RocksDB.class) {
        Constructor constructor = clz.getConstructor(String.class, String.class);
        @SuppressWarnings("unchecked")
        DB<byte[], byte[]> db = (DB<byte[], byte[]>) constructor
            .newInstance((Object) parentName, (Object) name);
        this.db = db;
      } else if (clz == TxCacheDB.class) {
        @SuppressWarnings("unchecked")
        DB<byte[], byte[]> db = (DB<byte[], byte[]>) clz.newInstance();
        this.db = db;
      } else {
        throw new IllegalArgumentException();
      }
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new IllegalArgumentException();
    }

    solidity = this;
  }

  @Override
  public byte[] get(byte[] key) {
    return db.get(key);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    db.put(key, value);
  }

  @Override
  public void remove(byte[] key) {
    db.remove(key);
  }

  @Override
  public void merge(Snapshot from) {
    SnapshotImpl snapshot = (SnapshotImpl) from;
    Map<WrappedByteArray, WrappedByteArray> batch = Streams.stream(snapshot.db)
        .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey().getBytes()),
            WrappedByteArray.of(e.getValue().getBytes())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    ((Flusher) db).flush(batch);
  }

  public void merge(List<Snapshot> snapshots) {
    Map<WrappedByteArray, WrappedByteArray> batch = new HashMap<>();
    for (Snapshot snapshot : snapshots) {
      SnapshotImpl from = (SnapshotImpl) snapshot;
      Streams.stream(from.db)
          .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey().getBytes()),
              WrappedByteArray.of(e.getValue().getBytes())))
          .forEach(e -> batch.put(e.getKey(), e.getValue()));
    }

    ((Flusher) db).flush(batch);
  }

  @Override
  public Snapshot retreat() {
    return this;
  }

  @Override
  public Snapshot getRoot() {
    return this;
  }

  @Override
  public Iterator<Map.Entry<byte[], byte[]>> iterator() {
    return db.iterator();
  }

  @Override
  public void close() {
    ((Flusher) db).close();
  }

  @Override
  public void reset() {
    ((Flusher) db).reset();
  }

  @Override
  public void resetSolidity() {
    solidity = this;
  }

  @Override
  public void updateSolidity() {
    solidity = solidity.getNext();
  }
}
