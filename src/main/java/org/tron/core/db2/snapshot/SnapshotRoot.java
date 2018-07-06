package org.tron.core.db2.snapshot;

import org.tron.core.db2.common.LevelDB;
import java.util.Iterator;
import java.util.Map;

public class SnapshotRoot extends AbstractSnapshot<byte[], byte[]> {

  public SnapshotRoot(String parentName, String name) {
    db = new LevelDB(parentName, name);
  }

  @Override
  public byte[] get(byte[] key) {
    return db.get(key);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    db.put(key, value);
  }

  @Override
  public void remove(byte[] key) {
    db.remove(key);
  }

  // todo write batch into levelDB
  @Override
  public void merge(Snapshot from) {

  }

  @Override
  public Iterator<Map.Entry<byte[],byte[]>> iterator() {
    return db.iterator();
  }

  @Override
  public void close() {
    ((LevelDB) db).close();
  }
}
