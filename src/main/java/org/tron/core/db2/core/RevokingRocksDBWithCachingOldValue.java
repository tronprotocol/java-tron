package org.tron.core.db2.core;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.storage.leveldb.RocksDbDataSourceImpl;
import org.tron.core.config.args.Args;
import org.tron.core.db.AbstractRevokingStore;
import org.tron.core.db.RevokingStore;
import org.tron.core.db.RevokingStoreRocks;
import org.tron.core.db.RevokingStoreRocks.RevokingTuple;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j
@NoArgsConstructor
public class RevokingRocksDBWithCachingOldValue implements IRevokingDB {

  private RevokingStoreRocks revokingDatabase;

  @Getter
  private RocksDbDataSourceImpl dbSource;

  public RevokingRocksDBWithCachingOldValue(String dbName) {
    this(dbName, RevokingStoreRocks.getInstance());
  }

  // only for unit test
  public RevokingRocksDBWithCachingOldValue(String dbName, RevokingStoreRocks revokingDatabase) {
    dbSource = new RocksDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName(dbName),
        dbName);
    dbSource.initDB();
    this.revokingDatabase = revokingDatabase;
  }

  @Override
  public void put(byte[] key, byte[] value) {
    if (Objects.isNull(key) || Objects.isNull(value)) {
      return;
    }
    byte[] oldValue = dbSource.getData(key);
    if (ArrayUtils.isNotEmpty(oldValue)) {
      onModify(key, oldValue);
    }

    dbSource.putData(key, value);

    if (ArrayUtils.isEmpty(oldValue)) {
      onCreate(key);
    }
  }

  @Override
  public void delete(byte[] key) {
    onDelete(key);
    dbSource.deleteData(key);
  }

  @Override
  public boolean hasOnSolidity(byte[] key) {
    return false;
  }

  @Override
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
  }

  @Override
  public byte[] get(byte[] key) throws ItemNotFoundException {
    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException();
    }
    return value;
  }

  @Override
  public byte[] getUnchecked(byte[] key) {
    try {
      return get(key);
    } catch (ItemNotFoundException e) {
      return null;
    }
  }

  @Override
  public byte[] getOnSolidity(byte[] key) throws ItemNotFoundException {
    return new byte[0];
  }

  @Override
  public byte[] getUncheckedOnSolidity(byte[] key) {
    return new byte[0];
  }

  @Override
  public void close() {
    dbSource.closeDB();
  }

  @Override
  public void reset() {
    dbSource.resetDb();
  }

  /**
   * This should be called just after an object is created
   */
  private void onCreate(byte[] key) {
    revokingDatabase.onCreate(new RevokingTuple(dbSource, key), null);
  }

  /**
   * This should be called just before an object is modified
   */
  private void onModify(byte[] key, byte[] value) {
    revokingDatabase.onModify(new RevokingTuple(dbSource, key), value);
  }

  /**
   * This should be called just before an object is removed.
   */
  private void onDelete(byte[] key) {
    byte[] value;
    if (Objects.nonNull(value = dbSource.getData(key))) {
      revokingDatabase.onRemove(new RevokingTuple(dbSource, key), value);
    }
  }

  @Override
  public Iterator<Entry<byte[], byte[]>> iterator() {
    return dbSource.iterator();
  }

  @Override
  public void forEach(Consumer<? super Entry<byte[], byte[]>> action) {

  }

  @Override
  public Spliterator<Entry<byte[], byte[]>> spliterator() {
    return null;
  }

  @Override
  public Set<byte[]> getlatestValues(long limit) {
    return dbSource.getlatestValues(limit);
  }

  @Override
  public Set<byte[]> getValuesNext(byte[] key, long limit) {
    return dbSource.getValuesNext(key, limit);
  }
}
