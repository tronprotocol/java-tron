package org.tron.core.db2.common;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.tron.core.capsule.BytesCapsule;

public class ConcurrentHashDB implements DB<byte[], BytesCapsule> {

  private Map<Key, BytesCapsule> db = new ConcurrentHashMap<>();


  @Override
  public BytesCapsule get(byte[] bytes) {
    return db.get(Key.of(bytes));
  }

  @Override
  public void put(byte[] bytes, BytesCapsule bytes2) {
    db.put(Key.of(bytes), bytes2);
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
  public void remove(byte[] bytes) {
    db.remove(Key.of(bytes));
  }

  @Override
  public String getDbName() {
    return null;
  }

  @Override
  public Iterator<Entry<byte[], BytesCapsule>> iterator() {
    return null;
  }

  @Override
  public void close() {
    db.clear();
  }

  @Override
  public DB<byte[], BytesCapsule> newInstance() {
    return null;
  }
}
