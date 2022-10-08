package org.tron.core.db2.common;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.tron.core.capsule.ProtoCapsule;

public class HashDB<T extends ProtoCapsule> implements DB<Key, Value<T>> {

  private Map<Key, Value<T>> db = new ConcurrentHashMap<>();
  private String name;

  public HashDB(String name) {
    this.name = name;
  }

  @Override
  public Value<T> get(Key key) {
    return db.get(key);
  }

  @Override
  public void put(Key key, Value<T> value) {
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
  public Iterator<Map.Entry<Key, Value<T>>> iterator() {
    return db.entrySet().iterator();
  }

  @Override
  public void close() {
    db.clear();
  }

  @Override
  public HashDB<T> newInstance() {
    return new HashDB<>(name);
  }

  @Override
  public void stat() {

  }
}
