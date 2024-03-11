package org.tron.core.db2.common;

import com.google.common.collect.Maps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.core.db.common.iterator.DBIterator;

@Slf4j(topic = "DB")
public class RocksDB implements DB<byte[], byte[]>, Flusher {

  @Getter
  private RocksDbDataSourceImpl db;

  private WriteOptionsWrapper optionsWrapper = WriteOptionsWrapper.getInstance()
      .sync(CommonParameter.getInstance().getStorage().isDbSync());

  public RocksDB(RocksDbDataSourceImpl db) {
    this.db = db;
  }

  @Override
  public byte[] get(byte[] key) {
    return db.getData(key);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    db.putData(key, value);
  }

  @Override
  public long size() {
    return db.getTotal();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public void remove(byte[] key) {
    db.deleteData(key);
  }

  @Override
  public String getDbName() {
    return db.getDBName();
  }

  @Override
  public DBIterator iterator() {
    return db.iterator();
  }

  @Override
  public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
    Map<byte[], byte[]> rows = batch.entrySet().stream()
        .map(e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes()))
        .collect(HashMap::new, (m, k) -> m.put(k.getKey(), k.getValue()), HashMap::putAll);
    db.updateByBatch(rows, optionsWrapper);
  }

  @Override
  public void close() {
    db.closeDB();
  }

  @Override
  public void reset() {
    db.resetDb();
  }

  @Override
  public DB<byte[], byte[]> newInstance() {
    return new RocksDB(db.newInstance());
  }

  @Override
  public void stat() {
    this.db.stat();
  }

  public void backup(String dir) {
    try {
      db.backup(dir);
    } catch (RocksDBException | IllegalArgumentException | IllegalStateException e) {
      logger.warn("Backup {} to {} failed: {}", this.getDbName(), dir, e.getMessage());
    }
  }

  public static void destroy(String name, String dir) {
    try {
      // delete engine.properties first
      Files.deleteIfExists(Paths.get(dir, name, "engine.properties"));
      org.rocksdb.RocksDB.destroyDB(Paths.get(dir, name).toString(), new Options());
    } catch (RocksDBException | IOException e) {
      logger.warn("Destroy {} from {} failed: {}", name, dir, e.getMessage());
    }
  }
}
