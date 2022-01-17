package org.tron.core.db2.common;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.primitives.Longs;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.WriteOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.common.utils.StorageUtils;
import org.tron.core.db.common.iterator.DBIterator;

@Slf4j(topic = "DB")
@Component
public class TxCacheDB implements DB<byte[], byte[]>, Flusher {

  // > 65_536(= 2^16) blocks, that is the number of the reference block
  private static final int BLOCK_COUNT = 70_000;

  private Map<Key, Long> db = Collections.synchronizedMap(new WeakHashMap<>());
  private Multimap<Long, Key> blockNumMap = Multimaps.synchronizedMultimap(ArrayListMultimap.create());
  private final String name;
  @Getter
  private final CountDownLatch initDone;

  // add a persistent storage, the store name is: trans-cache
  // when fullnode startup, transactionCache initializes transactions from this store
  private DB<byte[], byte[]> persistentStore;

  @Autowired
  public TxCacheDB(@Value("trans-cache") String name) {
    this.name = name;
    this.initDone = new CountDownLatch(1);
    int dbVersion = CommonParameter.getInstance().getStorage().getDbVersion();
    String dbEngine = CommonParameter.getInstance().getStorage().getDbEngine();
    if (dbVersion == 2) {
      if ("LEVELDB".equalsIgnoreCase(dbEngine)) {
        this.persistentStore = new LevelDB(
                        new LevelDbDataSourceImpl(StorageUtils.getOutputDirectoryByDbName(name),
                                name, StorageUtils.getOptionsByDbName(name),
                                new WriteOptions().sync(CommonParameter.getInstance()
                                        .getStorage().isDbSync())));
      } else if ("ROCKSDB".equalsIgnoreCase(dbEngine)) {
        String parentPath = Paths
                .get(StorageUtils.getOutputDirectoryByDbName(name), CommonParameter
                        .getInstance().getStorage().getDbDirectory()).toString();

        this.persistentStore = new RocksDB(
                        new RocksDbDataSourceImpl(parentPath,
                                name, CommonParameter.getInstance()
                                .getRocksDBCustomSettings()));
      } else {
        throw new RuntimeException("db type is not supported.");
      }
    } else {
      throw new RuntimeException("db version is not supported.");
    }
    // init cache from persistent store
    init();
  }

  /**
   * this method only used for init, put all data in tran-cache into the two maps.
   */
  private void init() {
    Thread t = new Thread(()-> {
      long s = System.currentTimeMillis();
      DBIterator iterator = (DBIterator) persistentStore.iterator();
      while (iterator.hasNext()) {
        Entry<byte[], byte[]> entry = iterator.next();
        byte[] key = entry.getKey();
        byte[] value = entry.getValue();
        if (key == null || value == null) {
          return;
        }
        Key k = Key.copyOf(key);
        Long v = Longs.fromByteArray(value);
        blockNumMap.put(v, k);
        db.put(k, v);
      }
      logger.info("Init tx cache done with {} ms" , System.currentTimeMillis() - s);
      this.initDone.countDown();
    });
    t.setName("init-tx-cache");
    t.start();
  }

  @Override
  public byte[] get(byte[] key) {
    Long v = db.get(Key.of(key));
    return v == null ? null : Longs.toByteArray(v);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    if (key == null || value == null) {
      return;
    }

    Key k = Key.copyOf(key);
    Long v = Longs.fromByteArray(value);
    blockNumMap.put(v, k);
    db.put(k, v);
    // put the data into persistent storage
    persistentStore.put(key, value);
    removeEldest();
  }

  private void removeEldest() {
    Set<Long> keys = blockNumMap.keySet();
    if (keys.size() > BLOCK_COUNT) {
      keys.stream()
          .min(Long::compareTo)
          .ifPresent(k -> {
            Collection<Key> trxHashs = blockNumMap.get(k);
            // remove transaction from persistentStore,
            // if foreach is inefficient, change remove-foreach to remove-batch
            trxHashs.forEach(key -> persistentStore.remove(key.getBytes()));
            blockNumMap.removeAll(k);
            logger.debug("******removeEldest block number:{}, block count:{}", k, keys.size());
          });
    }
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
  public void remove(byte[] key) {
    if (key != null) {
      db.remove(Key.of(key));
    }
  }

  @Override
  public String getDbName() {
    return name;
  }

  @Override
  public Iterator<Entry<byte[], byte[]>> iterator() {
    return Iterators.transform(db.entrySet().iterator(),
        e -> Maps.immutableEntry(e.getKey().getBytes(), Longs.toByteArray(e.getValue())));
  }

  @Override
  public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
    batch.forEach((k, v) -> this.put(k.getBytes(), v.getBytes()));
  }

  @Override
  public void close() {
    reset();
    db = null;
    blockNumMap = null;
    persistentStore.close();
  }

  @Override
  public void reset() {
    db.clear();
    blockNumMap.clear();
  }

  @Override
  public TxCacheDB newInstance() {
    return new TxCacheDB(name);
  }
}
