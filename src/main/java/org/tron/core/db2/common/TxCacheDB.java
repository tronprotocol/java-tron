package org.tron.core.db2.common;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.tron.core.db.KhaosDatabase.KhaosBlock;
import org.tron.core.db.common.WrappedByteArray;

public class TxCacheDB implements DB<byte[], byte[]>, Flusher {
  private final int BLOCK_COUNT = 70_000; // > 65_536(= 2^16) blocks

  private Map<Key, Long> db = new HashMap<>();
  private Map<Long, ArrayList<Key>> blockNumMap = new LinkedHashMap<Long, ArrayList<Key>>() {
    @Override
    protected boolean removeEldestEntry(Map.Entry<Long, ArrayList<Key>> entry) {
      long blockNum = entry.getKey();
      List<Key> txs = entry.getValue();
      txs.forEach(db::remove);
      if (blockNumMap.size() > BLOCK_COUNT) {
        blockNumMap.remove(blockNum);
      }
      return false;
    }
  };

  @Override
  public byte[] get(byte[] key) {
    return Longs.toByteArray(db.get(key));
  }

  @Override
  public void put(byte[] key, byte[] value) {
    Key k = Key.copyOf(key);
    Long v = Longs.fromByteArray(value);
    blockNumMap.computeIfAbsent(Longs.fromByteArray(value), listBlk -> new ArrayList<>()).add(k);
    db.put(k, v);
  }

  @Override
  public long size() {
    return db.size();
  }

  @Override
  public boolean isEmpty() {
    return db.isEmpty();
  }

  @Override
  public void remove(byte[] key) {
    db.remove(key);
  }

  @Override
  public Iterator<Map.Entry<byte[],byte[]>> iterator() {
    return Iterators.transform(db.entrySet().iterator(),
        e -> Maps.immutableEntry(e.getKey().getBytes(), Longs.toByteArray(e.getValue())));
  }

  @Override
  public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
    batch.forEach((k, v) -> this.put(k.getBytes(), v.getBytes()));
  }

  @Override
  public void close() {
    reset();
    db = null;
    blockNumMap = null;
  }

  @Override
  public void reset() {
    db.clear();
    blockNumMap.clear();
  }
}
