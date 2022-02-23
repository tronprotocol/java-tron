package org.tron.core.db2.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HashDB implements DB<Key, Value> {

  private Map<Key, Value> db = new ConcurrentHashMap<>();
  private String name;

  public HashDB(String name) {
    this.name = name;
  }

  @Override
  public Value get(Key key) {
    return db.get(key);
  }

  @Override
  public void put(Key key, Value value) {
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
  public String getDbName() {
    return name;
  }

  @Override
  public Iterator<Map.Entry<Key, Value>> iterator() {
    return db.entrySet().iterator();
  }

  @Override
  public void close() {
    db.clear();
  }

  @Override
  public HashDB newInstance() {
    return new HashDB(name);
  }
}
