package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.utils.MarketUtils;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.common.RocksDB;
import org.tron.core.db2.common.Value;
import org.tron.core.db2.common.Value.Operator;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.exception.ItemNotFoundException;

public class Chainbase implements IRevokingDB {

  // public static Map<String, byte[]> assetsAddress = new HashMap<>(); // key = name , value = address
  public enum Cursor {
    HEAD,
    SOLIDITY,
    PBFT
  }

  //true:fullnode, false:soliditynode
  private ThreadLocal<Cursor> cursor = new ThreadLocal<>();
  private ThreadLocal<Long> offset = new ThreadLocal<>();
  private Snapshot head;

  public Chainbase(Snapshot head) {
    this.head = head;
    cursor.set(Cursor.HEAD);
    offset.set(0L);
  }

  public String getDbName() {
    return head.getDbName();
  }

  @Override
  public void setCursor(Cursor cursor) {
    this.cursor.set(cursor);
  }

  @Override
  public void setCursor(Cursor cursor, long offset) {
    this.cursor.set(cursor);
    this.offset.set(offset);
  }

  private Snapshot head() {
    if (cursor.get() == null) {
      return head;
    }

    switch (cursor.get()) {
      case HEAD:
        return head;
      case SOLIDITY:
        return head.getSolidity();
      case PBFT:
        if (offset.get() == null) {
          return head.getSolidity();
        }

        if (offset.get() >= 0) {
          Snapshot tmp = head;
          for (int i = 0; i < offset.get() && tmp != tmp.getRoot(); i++) {
            tmp = tmp.getPrevious();
          }
          return tmp;
        } else {
          return head.getSolidity();
        }
      default:
        return head;
    }
  }

  public synchronized Snapshot getHead() {
    return head();
  }

  public synchronized void setHead(Snapshot head) {
    this.head = head;
  }

  /**
   * close the database.
   */
  @Override
  public synchronized void close() {
    head().close();
  }

  @Override
  public synchronized void reset() {
    head().reset();
    head().close();
    head = head.getRoot().newInstance();
  }

  @Override
  public synchronized void put(byte[] key, byte[] value) {
    head().put(key, value);
  }

  @Override
  public synchronized void delete(byte[] key) {
    head().remove(key);
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
    return head().get(key);
  }

  @Override
  public synchronized boolean has(byte[] key) {
    return getUnchecked(key) != null;
  }

  @Override
  public synchronized Iterator<Map.Entry<byte[], byte[]>> iterator() {
    return head().iterator();
  }

  @Override
  public Set<byte[]> getValuesNext(byte[] key, long limit) {
    return getValuesNext(head(), key, limit);
  }

  // for blockstore
  private Set<byte[]> getValuesNext(Snapshot head, byte[] key, long limit) {
    if (limit <= 0) {
      return Collections.emptySet();
    }

    Map<WrappedByteArray, WrappedByteArray> collection = new HashMap<>();
    if (head.getPrevious() != null) {
      ((SnapshotImpl) head).collect(collection);
    }

    Map<WrappedByteArray, WrappedByteArray> levelDBMap = new HashMap<>();

    if (((SnapshotRoot) head.getRoot()).db.getClass() == LevelDB.class) {
      ((LevelDB) ((SnapshotRoot) head.getRoot()).db).getDb().getNext(key, limit).entrySet().stream()
          .map(e -> Maps
              .immutableEntry(WrappedByteArray.of(e.getKey()), WrappedByteArray.of(e.getValue())))
          .forEach(e -> levelDBMap.put(e.getKey(), e.getValue()));
    } else if (((SnapshotRoot) head.getRoot()).db.getClass() == RocksDB.class) {
      ((RocksDB) ((SnapshotRoot) head.getRoot()).db).getDb().getNext(key, limit).entrySet().stream()
          .map(e -> Maps
              .immutableEntry(WrappedByteArray.of(e.getKey()), WrappedByteArray.of(e.getValue())))
          .forEach(e -> levelDBMap.put(e.getKey(), e.getValue()));
    }

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
  public List<byte[]> getKeysNext(byte[] key, long limit) {
    return getKeysNext(head(), key, limit);
  }

  /**
   * Notes: For now, this function is just used for Market, because it should use
   * MarketUtils.comparePriceKey as its comparator. It need to use MarketUtils.createPairPriceKey to
   * create the key.
   */
  // for market
  private List<byte[]> getKeysNext(Snapshot head, byte[] key, long limit) {
    if (limit <= 0) {
      return Collections.emptyList();
    }

    Map<WrappedByteArray, Operator> collectionList = new HashMap<>();
    if (head.getPrevious() != null) {
      ((SnapshotImpl) head).collectUnique(collectionList);
    }

    // just get the same token pair
    List<WrappedByteArray> snapshotList = new ArrayList<>();
    if (!collectionList.isEmpty()) {
      snapshotList = collectionList.keySet().stream()
          .filter(e -> MarketUtils.pairKeyIsEqual(e.getBytes(), key))
          .collect(Collectors.toList());
    }

    // for delete operation
    long limitLevelDB = limit + collectionList.size();

    List<WrappedByteArray> levelDBList = new ArrayList<>();
    if (((SnapshotRoot) head.getRoot()).db.getClass() == LevelDB.class) {
      ((LevelDB) ((SnapshotRoot) head.getRoot()).db).getDb().getKeysNext(key, limitLevelDB)
          .forEach(e -> levelDBList.add(WrappedByteArray.of(e)));
    } else if (((SnapshotRoot) head.getRoot()).db.getClass() == RocksDB.class) {
      ((RocksDB) ((SnapshotRoot) head.getRoot()).db).getDb().getKeysNext(key, limitLevelDB)
          .forEach(e -> levelDBList.add(WrappedByteArray.of(e)));
    }

    // just get the same token pair
    List<WrappedByteArray> levelDBListFiltered = levelDBList.stream()
        .filter(e -> MarketUtils.pairKeyIsEqual(e.getBytes(), key))
        .collect(Collectors.toList());

    List<WrappedByteArray> keyList = new ArrayList<>();
    keyList.addAll(levelDBListFiltered);

    // snapshot and levelDB will have duplicated key, so need to check it before,
    // and remove the key which has been deleted
    snapshotList.forEach(ssKey -> {
      if (!keyList.contains(ssKey)) {
        keyList.add(ssKey);
      }
      if (collectionList.get(ssKey) == Operator.DELETE) {
        keyList.remove(ssKey);
      }
    });

    return keyList.stream()
        .filter(e -> MarketUtils.greaterOrEquals(e.getBytes(), key))
        .sorted((e1, e2) -> MarketUtils.comparePriceKey(e1.getBytes(), e2.getBytes()))
        .limit(limit)
        .map(WrappedByteArray::getBytes)
        .collect(Collectors.toList());
  }

  // for blockstore
  @Override
  public Set<byte[]> getlatestValues(long limit) {
    return getlatestValues(head(), limit);
  }

  // for blockstore
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
      if (((SnapshotRoot) head.getRoot()).db.getClass() == LevelDB.class) {
        result.addAll(((LevelDB) ((SnapshotRoot) snapshot).db).getDb().getlatestValues(tmp));
      } else if (((SnapshotRoot) head.getRoot()).db.getClass() == RocksDB.class) {
        result.addAll(((RocksDB) ((SnapshotRoot) snapshot).db).getDb().getlatestValues(tmp));
      }
    }

    return result;
  }

  // for accout-trace
  @Override
  public Map<byte[], byte[]> getNext(byte[] key, long limit) {
    return getNext(head(), key, limit);
  }

  // for accout-trace
  private Map<byte[], byte[]> getNext(Snapshot head, byte[] key, long limit) {
    if (limit <= 0) {
      return Collections.emptyMap();
    }

    Map<WrappedByteArray, WrappedByteArray> collection = new HashMap<>();
    if (head.getPrevious() != null) {
      ((SnapshotImpl) head).collect(collection);
    }

    Map<WrappedByteArray, WrappedByteArray> levelDBMap = new HashMap<>();

    if (((SnapshotRoot) head.getRoot()).db.getClass() == LevelDB.class) {
      ((LevelDB) ((SnapshotRoot) head.getRoot()).db).getDb().getNext(key, limit).entrySet().stream()
          .map(e -> Maps
              .immutableEntry(WrappedByteArray.of(e.getKey()), WrappedByteArray.of(e.getValue())))
          .forEach(e -> levelDBMap.put(e.getKey(), e.getValue()));
    } else if (((SnapshotRoot) head.getRoot()).db.getClass() == RocksDB.class) {
      ((RocksDB) ((SnapshotRoot) head.getRoot()).db).getDb().getNext(key, limit).entrySet().stream()
          .map(e -> Maps
              .immutableEntry(WrappedByteArray.of(e.getKey()), WrappedByteArray.of(e.getValue())))
          .forEach(e -> levelDBMap.put(e.getKey(), e.getValue()));
    }

    levelDBMap.putAll(collection);

    return levelDBMap.entrySet().stream()
        .map(e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes()))
        .sorted((e1, e2) -> ByteUtil.compare(e1.getKey(), e2.getKey()))
        .filter(e -> ByteUtil.greaterOrEquals(e.getKey(), key))
        .limit(limit)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
