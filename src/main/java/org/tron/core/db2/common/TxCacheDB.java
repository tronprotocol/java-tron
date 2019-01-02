package org.tron.core.db2.common;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.tron.core.db.KhaosDatabase.KhaosBlock;

public class TxCacheDB implements DB<Key, Long> {
  private final int BLOCK_COUNT = 65_536;
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
  public Long get(Key key) {
    return db.get(key);
  }

  @Override
  public void put(Key key, Long value) {
    blockNumMap.computeIfAbsent(value, listBlk -> new ArrayList<>()).add(key);
    db.put(key, value);
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
  public void remove(Key key) {
    db.remove(key);
  }

  @Override
  public Iterator<Map.Entry<Key,Long>> iterator() {
    return db.entrySet().iterator();
  }
}
