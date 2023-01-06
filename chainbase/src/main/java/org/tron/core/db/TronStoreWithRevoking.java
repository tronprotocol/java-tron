package org.tron.core.db;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.db2.core.ITronChainBase;
import org.tron.core.db2.core.SnapshotRoot;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;


@Slf4j(topic = "DB")
public abstract class TronStoreWithRevoking<T extends ProtoCapsule> implements ITronChainBase<T> {

  @Getter // only for unit test
  protected IRevokingDB revokingDB;
  private TypeToken<T> token = new TypeToken<T>(getClass()) {
  };

  @Autowired
  private RevokingDatabase revokingDatabase;

  @Autowired
  private DbStatService dbStatService;

  private DB<byte[], byte[]> db;

  protected TronStoreWithRevoking(String dbName) {
    String dbEngine = CommonParameter.getInstance().getStorage().getDbEngine();
    if ("LEVELDB".equals(dbEngine.toUpperCase())) {
      this.db =  new LevelDB(
          new LevelDbDataSourceImpl(StorageUtils.getOutputDirectoryByDbName(dbName),
              dbName,
              getOptionsByDbNameForLevelDB(dbName),
              new WriteOptions().sync(CommonParameter.getInstance()
                  .getStorage().isDbSync())));
    } else if ("ROCKSDB".equals(dbEngine.toUpperCase())) {
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
    this.revokingDB = new Chainbase(new SnapshotRoot(this.db));
  }

  protected org.iq80.leveldb.Options getOptionsByDbNameForLevelDB(String dbName) {
    return StorageUtils.getOptionsByDbName(dbName);
  }

  protected DirectComparator getDirectComparator() {
    return null;
  }

  protected TronStoreWithRevoking(DB<byte[], byte[]> db) {
    this.db = db;
    this.revokingDB = new Chainbase(new SnapshotRoot(db));
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

    revokingDB.put(key, item.getData());
  }

  @Override
  public void delete(byte[] key) {
    revokingDB.delete(key);
  }

  @Override
  public T get(byte[] key) throws ItemNotFoundException, BadItemException {
    return of(revokingDB.get(key));
  }

  @Override
  public T getUnchecked(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);

    try {
      return of(value);
    } catch (BadItemException e) {
      return null;
    }
  }

  @Override
  public T getFromRoot(byte[] key) throws ItemNotFoundException, BadItemException{
    return of(revokingDB.getFromRoot(key)) ;

  }

  public T of(byte[] value) throws BadItemException {
    try {
      Constructor constructor = token.getRawType().getConstructor(byte[].class);
      @SuppressWarnings("unchecked")
      T t = (T) constructor.newInstance(value);
      return t;
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new BadItemException(e.getMessage());
    }
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
    return Iterators.transform(revokingDB.iterator(), e -> {
      try {
        return Maps.immutableEntry(e.getKey(), of(e.getValue()));
      } catch (BadItemException e1) {
        throw new RuntimeException(e1);
      }
    });
  }

  public long size() {
    return Streams.stream(revokingDB.iterator()).count();
  }

  public void setCursor(Chainbase.Cursor cursor) {
    revokingDB.setCursor(cursor);
  }

  public Map<WrappedByteArray, T> prefixQuery(byte[] key) {
    return revokingDB.prefixQuery(key).entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, e -> {
          try {
            return of(e.getValue());
          } catch (BadItemException e1) {
            throw new RuntimeException(e1);
          }
        }
    ));
  }
}
