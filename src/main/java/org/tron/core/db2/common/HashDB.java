package org.tron.core.db2.common;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HashDB implements DB<Key, Value> {
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
  public void putAll(Map<Key, Value> map) {
    db.putAll(map);
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
  public void clear() {
    db.clear();
  }

  @Override
  public Map<Key, Value> asMap() {
    return ImmutableMap.copyOf(db);
  }

  @Override
  public Iterator<Map.Entry<Key,Value>> iterator() {
    return db.entrySet().iterator();
  }
}
