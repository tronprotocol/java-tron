package org.tron.core.db;

import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.WriteOptions;
import org.rocksdb.DirectComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.storage.metric.DbStatService;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.common.utils.StorageUtils;
import org.tron.core.db.common.DbSourceInter;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.core.ITronChainBase;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j(topic = "DB")
public abstract class TronDatabase<T> implements ITronChainBase<T> {

  protected DbSourceInter<byte[]> dbSource;
  @Getter
  private String dbName;
  private WriteOptionsWrapper writeOptions = WriteOptionsWrapper.getInstance()
          .sync(CommonParameter.getInstance().getStorage().isDbSync());

  @Autowired
  private DbStatService dbStatService;

  protected TronDatabase(String dbName) {
    this.dbName = dbName;

    if ("LEVELDB".equals(CommonParameter.getInstance().getStorage()
        .getDbEngine().toUpperCase())) {
      dbSource =
          new LevelDbDataSourceImpl(StorageUtils.getOutputDirectoryByDbName(dbName),
              dbName,
              getOptionsByDbNameForLevelDB(dbName),
              new WriteOptions().sync(CommonParameter.getInstance()
                  .getStorage().isDbSync()));
    } else if ("ROCKSDB".equals(CommonParameter.getInstance()
        .getStorage().getDbEngine().toUpperCase())) {
      String parentName = Paths.get(StorageUtils.getOutputDirectoryByDbName(dbName),
          CommonParameter.getInstance().getStorage().getDbDirectory()).toString();
      dbSource =
          new RocksDbDataSourceImpl(parentName, dbName, CommonParameter.getInstance()
              .getRocksDBCustomSettings(), getDirectComparator());
    }

    dbSource.initDB();
  }

  @PostConstruct
  protected void init() {
    dbStatService.register(dbSource);
  }

  protected TronDatabase() {
  }

  protected org.iq80.leveldb.Options getOptionsByDbNameForLevelDB(String dbName) {
    return StorageUtils.getOptionsByDbName(dbName);
  }

  protected DirectComparator getDirectComparator() {
    return null;
  }

  public DbSourceInter<byte[]> getDbSource() {
    return dbSource;
  }

  public void updateByBatch(Map<byte[], byte[]> rows) {
    this.dbSource.updateByBatch(rows, writeOptions);
  }

  /**
   * reset the database.
   */
  public void reset() {
    dbSource.resetDb();
  }

  /**
   * close the database.
   */
  @Override
  public void close() {
    dbSource.closeDB();
  }

  public abstract void put(byte[] key, T item);

  public abstract void delete(byte[] key);

  public abstract T get(byte[] key)
      throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException;

  @Override
  public T getFromRoot(byte[] key)
      throws InvalidProtocolBufferException, BadItemException, ItemNotFoundException {
    return get(key);
  }

  public T getUnchecked(byte[] key) {
    return null;
  }

  public Map<WrappedByteArray, byte[]> prefixQuery(byte[] key) {
    return dbSource.prefixQuery(key);
  }

  public abstract boolean has(byte[] key);

  @Override
  public  boolean isNotEmpty() {
    throw new UnsupportedOperationException();
  }

  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Iterator<Entry<byte[], T>> iterator() {
    throw new UnsupportedOperationException();
  }
}
