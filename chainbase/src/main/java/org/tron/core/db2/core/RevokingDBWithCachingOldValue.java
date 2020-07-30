package org.tron.core.db2.core;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteOptions;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.StorageUtils;
import org.tron.core.db.AbstractRevokingStore;
import org.tron.core.db.RevokingStore;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j
public class RevokingDBWithCachingOldValue implements IRevokingDB {

  private AbstractRevokingStore revokingDatabase;
  @Getter
  private LevelDbDataSourceImpl dbSource;

  public RevokingDBWithCachingOldValue(String dbName) {
    this(dbName, RevokingStore.getInstance());
  }

  // add for user defined option, ex: comparator
  public RevokingDBWithCachingOldValue(String dbName, Options options) {
    this(dbName, options, RevokingStore.getInstance());
  }

  // set public only for unit test
  public RevokingDBWithCachingOldValue(String dbName, AbstractRevokingStore revokingDatabase) {
    dbSource = new LevelDbDataSourceImpl(StorageUtils.getOutputDirectoryByDbName(dbName),
        dbName,
        StorageUtils.getOptionsByDbName(dbName),
        new WriteOptions().sync(CommonParameter.getInstance().getStorage().isDbSync()));
    dbSource.initDB();
    this.revokingDatabase = revokingDatabase;
  }

  public RevokingDBWithCachingOldValue(String dbName, Options options,
      AbstractRevokingStore revokingDatabase) {
    dbSource = new LevelDbDataSourceImpl(StorageUtils.getOutputDirectoryByDbName(dbName),
        dbName,
        options,
        new WriteOptions().sync(CommonParameter.getInstance().getStorage().isDbSync()));
    dbSource.initDB();
    this.revokingDatabase = revokingDatabase;
  }

  @Override
  public void put(byte[] key, byte[] newValue) {
    if (Objects.isNull(key) || Objects.isNull(newValue)) {
      return;
    }
    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isNotEmpty(value)) {
      onModify(key, value);
    }

    dbSource.putData(key, newValue);

    if (ArrayUtils.isEmpty(value)) {
      onCreate(key);
    }
  }

  @Override
  public void delete(byte[] key) {
    onDelete(key);
    dbSource.deleteData(key);
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
  public void close() {
    dbSource.closeDB();
  }

  @Override
  public void reset() {
    dbSource.resetDb();
  }

  @Override
  public void setCursor(Chainbase.Cursor cursor) {
  }

  @Override
  public void setCursor(Chainbase.Cursor cursor, long offset) {

  }

  /**
   * This should be called just after an object is created
   */
  private void onCreate(byte[] key) {
    revokingDatabase.onCreate(new AbstractRevokingStore.RevokingTuple(dbSource, key), null);
  }

  /**
   * This should be called just before an object is modified
   */
  private void onModify(byte[] key, byte[] value) {
    revokingDatabase.onModify(new AbstractRevokingStore.RevokingTuple(dbSource, key), value);
  }

  /**
   * This should be called just before an object is removed.
   */
  private void onDelete(byte[] key) {
    byte[] value;
    if (Objects.nonNull(value = dbSource.getData(key))) {
      revokingDatabase.onRemove(new AbstractRevokingStore.RevokingTuple(dbSource, key), value);
    }
  }

  @Override
  public Iterator<Map.Entry<byte[], byte[]>> iterator() {
    return dbSource.iterator();
  }

  @Override
  public Set<byte[]> getlatestValues(long limit) {
    return dbSource.getlatestValues(limit);
  }

  @Override
  public Set<byte[]> getValuesNext(byte[] key, long limit) {
    return dbSource.getValuesNext(key, limit);
  }

  @Override
  public List<byte[]> getKeysNext(byte[] key, long limit) {
    return dbSource.getKeysNext(key, limit);
  }
}
