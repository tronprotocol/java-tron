package org.tron.core.db2.core;

import com.google.common.collect.Iterators;
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
import lombok.Getter;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.Proto;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.capsule.utils.MarketUtils;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.common.RocksDB;
import org.tron.core.db2.common.Value;
import org.tron.core.db2.common.Value.Operator;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.exception.ItemNotFoundException;

public class Chainbase<T extends ProtoCapsule> implements IRevokingDB<T> {

  // public static Map<String, byte[]> assetsAddress = new HashMap<>(); // key = name , value = address
  public enum Cursor {
    HEAD,
    SOLIDITY,
    PBFT
  }

  //true:fullnode, false:soliditynode
  private ThreadLocal<Cursor> cursor = new ThreadLocal<>();
  private ThreadLocal<Long> offset = new ThreadLocal<>();
  private Snapshot<T> head;
  @Getter
  Class<T> clz;

  public Chainbase(Snapshot<T> head, Class<T> clz) {
    this.head = head;
    cursor.set(Cursor.HEAD);
    offset.set(0L);
    this.clz = clz;
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

  @Override
  public Cursor getCursor() {
    if (cursor.get() == null) {
      return Cursor.HEAD;
    } else {
      return cursor.get();
    }
  }

  private Snapshot<T> head() {
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
          Snapshot<T> tmp = head;
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

  public Snapshot<T> getHead() {
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
    head = head.getRoot();
  }

  @Override
  public synchronized void put(byte[] key, T value) {
    if (head().isRoot()) {
       head().getRoot().put(key, value.getData());
      return;
    }
    head().put(key, value);
  }

  @Override
  public synchronized void delete(byte[] key) {
    head().remove(key);
  }

  @Override
  public T get(byte[] key) throws ItemNotFoundException {
    T value = getUnchecked(key);
    if (value == null) {
      throw new ItemNotFoundException();
    }

    return value;
  }

  @Override
  public T getFromRoot(byte[] key) throws ItemNotFoundException {
    T value = Proto.of(((SnapshotRoot) (head().getRoot())).get(key), clz);
    if (value == null) {
      throw new ItemNotFoundException();
    }
    return value;
  }

  @Override
  public T getUnchecked(byte[] key) {
    if (head().isRoot()) {
      return Proto.of(((SnapshotRoot) (head().getRoot())).get(key), clz);
    }
    return head().get(key);
  }

  @Override
  public boolean has(byte[] key) {
    return getUnchecked(key) != null;
  }

  @Override
  public synchronized Iterator<Map.Entry<byte[], T>> iterator() {
    if (head().isRoot()) {
      return Iterators.transform(((SnapshotRoot) (head().getRoot())).iterator(),
          e -> Maps.immutableEntry(e.getKey(), Proto.of(e.getValue(), clz)));
    }
    return head().iterator();
  }


  @Override
  public Set<T> getValuesNext(byte[] key, long limit) {
    return getValuesNext(head(), key, limit);
  }

  // for blockstore
  private Set<T> getValuesNext(Snapshot head, byte[] key, long limit) {
    if (limit <= 0) {
      return Collections.emptySet();
    }

    Map<WrappedByteArray, T> collection = new HashMap<>();
    if (head.getPrevious() != null && head.isImpl()) {
      ((SnapshotImpl<T>) head).collect(collection);
    }

    Map<WrappedByteArray, T> levelDBMap = new HashMap<>();

    if (((SnapshotRoot) head.getRoot()).db.getClass() == LevelDB.class) {
      ((LevelDB) ((SnapshotRoot) head.getRoot()).db).getDb().getNext(key, limit).entrySet().stream()
          .map(e -> Maps
              .immutableEntry(WrappedByteArray.of(e.getKey()), Proto.of(e.getValue(), clz)))
          .forEach(e -> levelDBMap.put(e.getKey(), e.getValue()));
    } else if (((SnapshotRoot) head.getRoot()).db.getClass() == RocksDB.class) {
      ((RocksDB) ((SnapshotRoot) head.getRoot()).db).getDb().getNext(key, limit).entrySet().stream()
          .map(e -> Maps
              .immutableEntry(WrappedByteArray.of(e.getKey()), Proto.of(e.getValue(), clz)))
          .forEach(e -> levelDBMap.put(e.getKey(), e.getValue()));
    }

    levelDBMap.putAll(collection);

    return levelDBMap.entrySet().stream()
        .sorted((e1, e2) -> ByteUtil.compare(e1.getKey().getBytes(), e2.getKey().getBytes()))
        .filter(e -> ByteUtil.greaterOrEquals(e.getKey().getBytes(), key))
        .limit(limit)
        .map(Map.Entry::getValue)
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
    if (head.getPrevious() != null && head.isImpl()) {
      ((SnapshotImpl<T>) head).collectUnique(collectionList);
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
  public Set<T> getlatestValues(long limit) {
    return getlatestValues(head(), limit);
  }

  // for blockstore
  private synchronized Set<T> getlatestValues(Snapshot head, long limit) {
    if (limit <= 0) {
      return Collections.emptySet();
    }

    Set<T> result = new HashSet<>();
    Snapshot snapshot = head;
    long tmp = limit;
    for (; tmp > 0 && snapshot.getPrevious() != null && snapshot.isImpl();
         snapshot = snapshot.getPrevious()) {
      if (!((SnapshotImpl<T>) snapshot).db.isEmpty()) {
        --tmp;
        Streams.stream(((SnapshotImpl<T>) snapshot).db)
            .map(Map.Entry::getValue)
            .map(Value::getData)
            .forEach(result::add);
      }
    }

    if (snapshot.getPrevious() == null && tmp != 0) {
      if (((SnapshotRoot) head.getRoot()).db.getClass() == LevelDB.class) {
        result.addAll(((LevelDB) ((SnapshotRoot) snapshot).db).getDb().getlatestValues(tmp).stream()
            .map(v -> Proto.of(v, clz)).collect(Collectors.toList()));
      } else if (((SnapshotRoot) head.getRoot()).db.getClass() == RocksDB.class) {
        result.addAll(((RocksDB) ((SnapshotRoot) snapshot).db).getDb().getlatestValues(tmp).stream()
            .map(v -> Proto.of(v, clz)).collect(Collectors.toList()));
      }
    }

    return result;
  }

  // for accout-trace
  @Override
  public Map<byte[], T> getNext(byte[] key, long limit) {
    return getNext(head(), key, limit);
  }

  // for accout-trace
  private Map<byte[], T> getNext(Snapshot head, byte[] key, long limit) {
    if (limit <= 0) {
      return Collections.emptyMap();
    }

    Map<WrappedByteArray, T> collection = new HashMap<>();
    if (head.getPrevious() != null && head.isImpl()) {
      ((SnapshotImpl<T>) head).collect(collection);
    }

    Map<WrappedByteArray, T> levelDBMap = new HashMap<>();

    if (((SnapshotRoot) head.getRoot()).db.getClass() == LevelDB.class) {
      ((LevelDB) ((SnapshotRoot) head.getRoot()).db).getDb().getNext(key, limit).entrySet().stream()
          .map(e -> Maps
              .immutableEntry(WrappedByteArray.of(e.getKey()), e.getValue()))
          .forEach(e -> levelDBMap.put(e.getKey(), Proto.of(e.getValue(),clz)));
    } else if (((SnapshotRoot) head.getRoot()).db.getClass() == RocksDB.class) {
      ((RocksDB) ((SnapshotRoot) head.getRoot()).db).getDb().getNext(key, limit).entrySet().stream()
          .map(e -> Maps
              .immutableEntry(WrappedByteArray.of(e.getKey()), e.getValue()))
          .forEach(e -> levelDBMap.put(e.getKey(), Proto.of(e.getValue(),clz)));
    }

    levelDBMap.putAll(collection);

    return levelDBMap.entrySet().stream()
        .map(e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue()))
        .sorted((e1, e2) -> ByteUtil.compare(e1.getKey(), e2.getKey()))
        .filter(e -> ByteUtil.greaterOrEquals(e.getKey(), key))
        .limit(limit)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Map<WrappedByteArray, T> prefixQuery(byte[] key) {
    Map<WrappedByteArray, T> result = prefixQueryRoot(key);
    Map<WrappedByteArray, T>  snapshot = prefixQuerySnapshot(key);
    result.putAll(snapshot);
    result.entrySet().removeIf(e -> e.getValue() == null);
    return result;
  }

  private Map<WrappedByteArray, T> prefixQueryRoot(byte[] key) {
    Map<WrappedByteArray, T> result = new HashMap<>();
    if (((SnapshotRoot) head.getRoot()).db.getClass() == LevelDB.class) {
      result = ((LevelDB) ((SnapshotRoot) head.getRoot()).db).getDb().prefixQuery(key)
          .entrySet().stream().map(e -> Maps.immutableEntry(e.getKey(), Proto.of(e.getValue(),clz)))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    } else if (((SnapshotRoot) head.getRoot()).db.getClass() == RocksDB.class) {
      result = ((RocksDB) ((SnapshotRoot) head.getRoot()).db).getDb().prefixQuery(key)
          .entrySet().stream().map(e -> Maps.immutableEntry(e.getKey(), Proto.of(e.getValue(),clz)))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    return result;
  }

  private Map<WrappedByteArray, T> prefixQuerySnapshot(byte[] key) {
    Map<WrappedByteArray, T> result = new HashMap<>();
    Snapshot snapshot = head();
    if (snapshot.isImpl()) {
      Map<WrappedByteArray, T> all = new HashMap<>();
      ((SnapshotImpl<T>) snapshot).collect(all, key);
      result.putAll(all);
    }
    return result;
  }

}
