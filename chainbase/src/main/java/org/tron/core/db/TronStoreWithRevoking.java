package org.tron.core.db;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.iq80.leveldb.WriteOptions;
import org.rocksdb.DirectComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.storage.metric.DbStatService;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.common.utils.StorageUtils;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.db.common.iterator.DBIterator;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.common.RocksDB;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.db2.core.ITronChainBase;
import org.tron.core.db2.core.SnapshotRoot;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;


@Slf4j(topic = "DB")
public abstract class TronStoreWithRevoking<T extends ProtoCapsule> implements ITronChainBase<T> {

  @Getter // only for unit test
  protected IRevokingDB<T> revokingDB;

  @Autowired
  private RevokingDatabase revokingDatabase;

  @Autowired
  private DbStatService dbStatService;

  private DB<byte[], byte[]> db;

  protected TronStoreWithRevoking(String dbName, Class<T> clz) {
    String dbEngine = CommonParameter.getInstance().getStorage().getDbEngine();

    if ("LEVELDB".equalsIgnoreCase(dbEngine)) {
      this.db =  new LevelDB(
          new LevelDbDataSourceImpl(StorageUtils.getOutputDirectoryByDbName(dbName),
              dbName,
              getOptionsByDbNameForLevelDB(dbName),
              new WriteOptions().sync(CommonParameter.getInstance()
                  .getStorage().isDbSync())));
    } else if ("ROCKSDB".equalsIgnoreCase(dbEngine)) {
      String parentPath = Paths
          .get(StorageUtils.getOutputDirectoryByDbName(dbName), CommonParameter
              .getInstance().getStorage().getDbDirectory()).toString();
      this.db =  new RocksDB(
          new RocksDbDataSourceImpl(parentPath,
              dbName, CommonParameter.getInstance()
              .getRocksDBCustomSettings(), getDirectComparator()));
    } else {
      throw new RuntimeException(String.format("db engine %s is error", dbEngine));
    }
    this.revokingDB = new Chainbase(new SnapshotRoot(this.db), clz);

  }

  protected org.iq80.leveldb.Options getOptionsByDbNameForLevelDB(String dbName) {
    return StorageUtils.getOptionsByDbName(dbName);
  }

  protected DirectComparator getDirectComparator() {
    return null;
  }

  protected TronStoreWithRevoking(DB<byte[], byte[]> db, Class<T> clz) {
    int dbVersion = CommonParameter.getInstance().getStorage().getDbVersion();
    if (dbVersion == 2) {
      this.db = db;
      this.revokingDB = new Chainbase(new SnapshotRoot(db), clz);
    } else {
      throw new RuntimeException(String.format("db version is only 2, actual: %d", dbVersion));
    }
  }

  @Override
  public String getDbName() {
    return null;
  }

  @PostConstruct
  private void init() {
    revokingDatabase.add(revokingDB);
    dbStatService.register(db);
  }

  @Override
  public void put(byte[] key, T item) {
    if (Objects.isNull(key) || Objects.isNull(item)) {
      return;
    }

    revokingDB.put(key, item);
  }

  @Override
  public void delete(byte[] key) {
    revokingDB.delete(key);
  }

  @Override
  public T get(byte[] key) throws ItemNotFoundException, BadItemException {
    return revokingDB.get(key);
  }

  public T getNonEmpty(byte[] key) throws ItemNotFoundException, BadItemException {
    T value = revokingDB.getUnchecked(key);
    return Objects.isNull(value) || ArrayUtils.isEmpty(value.getData()) ? null : value;
  }

  @Override
  public T getUnchecked(byte[] key) {
    try {
      return revokingDB.getUnchecked(key);
    } catch (BadItemException e) {
      return null;
    }
  }

  @Override
  public T getFromRoot(byte[] key) throws ItemNotFoundException {
    return revokingDB.getFromRoot(key);
  }


  @Override
  public boolean has(byte[] key) {
    return revokingDB.has(key);
  }

  @Override
  public boolean isNotEmpty() {
    Iterator iterator = revokingDB.iterator();
    boolean value = iterator.hasNext();
    // close jni
    if (value) {
      closeJniIterator(iterator);
    }

    return value;
  }

  private void closeJniIterator(Iterator iterator) {
    if (iterator instanceof DBIterator) {
      try {
        ((DBIterator) iterator).close();
      } catch (IOException e) {
        logger.error("Close jni iterator.", e);
      }
    }
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public void close() {
    revokingDB.close();
  }

  @Override
  public void reset() {
    revokingDB.reset();
  }

  @Override
  public Iterator<Map.Entry<byte[], T>> iterator() {
    return revokingDB.iterator();
  }

  public long size() {
    return Streams.stream(revokingDB.iterator()).count();
  }

  public void setCursor(Chainbase.Cursor cursor) {
    revokingDB.setCursor(cursor);
  }
}
