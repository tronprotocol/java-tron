package org.tron.core.db2.common;

import org.tron.core.capsule.BytesCapsule;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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
  public Iterator<Entry<byte[], BytesCapsule>> iterator() {
    return null;
  }
}
