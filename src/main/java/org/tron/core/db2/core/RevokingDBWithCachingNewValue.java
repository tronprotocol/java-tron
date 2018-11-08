package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.tron.common.utils.ByteUtil;
import org.tron.core.config.args.Args;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.common.Value;
import org.tron.core.exception.ItemNotFoundException;

public class RevokingDBWithCachingNewValue implements IRevokingDB {

  private Snapshot head;
  @Getter
  private String dbName;

  public RevokingDBWithCachingNewValue(String dbName) {
    this.dbName = dbName;
    head = new SnapshotRoot(Args.getInstance().getOutputDirectoryByDbName(dbName), dbName);
  }

  public synchronized Snapshot getHead() {
    return head;
  }

  public synchronized void setHead(Snapshot head) {
    this.head = head;
  }

  /**
   * close the database.
   */
  @Override
  public synchronized void close() {
    head.close();
  }

  @Override
  public synchronized void reset() {
    head.reset();
    head.close();
    head = new SnapshotRoot(Args.getInstance().getOutputDirectoryByDbName(dbName), dbName);
  }

  @Override
  public synchronized void put(byte[] key, byte[] value) {
    head.put(key, value);
  }

  @Override
  public synchronized void delete(byte[] key) {
    head.remove(key);
  }

  @Override
  public synchronized byte[] get(byte[] key) throws ItemNotFoundException {
    byte[] value = getUnchecked(key);
    if (value == null) {
      throw new ItemNotFoundException();
    }

    return value;
  }

  @Override
  public synchronized byte[] getUnchecked(byte[] key) {
    return head.get(key);
  }

  @Override
  public byte[] getOnSolidity(byte[] key) throws ItemNotFoundException {
    byte[] value = getUncheckedOnSolidity(key);
    if (value == null) {
      throw new ItemNotFoundException();
    }

    return value;
  }

  @Override
  public byte[] getUncheckedOnSolidity(byte[] key) {
    Snapshot solidity = head.getSolidity();
    return solidity.get(key);
  }

  @Override
  public synchronized boolean hasOnSolidity(byte[] key) {
    return getUncheckedOnSolidity(key) != null;
  }

  @Override
  public synchronized boolean has(byte[] key) {
    return getUnchecked(key) != null;
  }

  @Override
  public synchronized Iterator<Map.Entry<byte[], byte[]>> iterator() {
    return head.iterator();
  }

  public synchronized Iterator<Map.Entry<byte[], byte[]>> iteratorOnSolidity() {
    return head.getSolidity().iterator();
  }

  //for blockstore
  @Override
  public Set<byte[]> getlatestValues(long limit) {
    return getlatestValues(head, limit);
  }

  //for blockstore
  public Set<byte[]> getlatestValuesOnSolidity(long limit) {
    return getlatestValues(head.getSolidity(), limit);
  }

  //for blockstore
  private synchronized Set<byte[]> getlatestValues(Snapshot head, long limit) {
    if (limit <= 0) {
      return Collections.emptySet();
    }

    Set<byte[]> result = new HashSet<>();
    Snapshot snapshot = head;
    long tmp = limit;
    for (; tmp > 0 && snapshot.getPrevious() != null; snapshot = snapshot.getPrevious()) {
      if (!((SnapshotImpl) snapshot).db.isEmpty()) {
        --tmp;
        Streams.stream(((SnapshotImpl) snapshot).db)
            .map(Map.Entry::getValue)
            .map(Value::getBytes)
            .forEach(result::add);
      }
    }

    if (snapshot.getPrevious() == null && tmp != 0) {
      result.addAll(((LevelDB) ((SnapshotRoot) snapshot).db).getDb().getlatestValues(tmp));
    }

    return result;
  }

  //for blockstore
  private Set<byte[]> getValuesNext(Snapshot head, byte[] key, long limit) {
    if (limit <= 0) {
      return Collections.emptySet();
    }

    Map<WrappedByteArray, WrappedByteArray> collection = new HashMap<>();
    if (head.getPrevious() != null) {
      ((SnapshotImpl) head).collect(collection);
    }

    Map<WrappedByteArray, WrappedByteArray> levelDBMap = new HashMap<>();
    ((LevelDB) ((SnapshotRoot) head.getRoot()).db).getDb().getNext(key, limit).entrySet().stream()
        .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey()), WrappedByteArray.of(e.getValue())))
        .forEach(e -> levelDBMap.put(e.getKey(), e.getValue()));

    levelDBMap.putAll(collection);

    return levelDBMap.entrySet().stream()
        .sorted((e1, e2) -> ByteUtil.compare(e1.getKey().getBytes(), e2.getKey().getBytes()))
        .filter(e -> ByteUtil.greaterOrEquals(e.getKey().getBytes(), key))
        .limit(limit)
        .map(Map.Entry::getValue)
        .map(WrappedByteArray::getBytes)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<byte[]> getValuesNext(byte[] key, long limit) {
    return getValuesNext(head, key, limit);
  }

  public synchronized Set<byte[]> getValuesNextOnSolidity(byte[] key, long limit) {
    return getValuesNext(head.getSolidity(), key, limit);
  }

}
