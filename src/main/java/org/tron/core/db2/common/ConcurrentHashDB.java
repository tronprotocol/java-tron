package org.tron.core.db2.common;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashDB implements DB<byte[], byte[]> {

  private Map<byte[], byte[]> db = new ConcurrentHashMap<>();


  @Override
  public byte[] get(byte[] bytes) {
    return db.get(bytes);
  }

  @Override
  public void put(byte[] bytes, byte[] bytes2) {
    db.put(bytes, bytes2);
  }

  @Override
  public void remove(byte[] bytes) {
    db.remove(bytes);
  }

  @Override
  public Iterator<Entry<byte[], byte[]>> iterator() {
    return db.entrySet().iterator();
  }

}
