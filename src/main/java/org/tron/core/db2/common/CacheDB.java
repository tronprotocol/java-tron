package org.tron.core.db2.common;

import org.tron.core.db.common.iterator.DBIterator;

import java.util.HashMap;
import java.util.Map;

public class CacheDB implements DB<Key, Value> {
  private Map<Key, Value> db = new HashMap<>();

  @Override
  public Value get(Key key) {
    return db.get(key);
  }

  @Override
  public void put(Key key, Value value) {
    db.put(key, value);
  }

  @Override
  public void remove(Key key) {
    db.remove(key);
  }

  @Override
  public DBIterator iterator() {
    return null;
  }
}
