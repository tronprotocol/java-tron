package org.tron.core.db2.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Getter;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.Flusher;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.store.AccountAssetStore;

public class SnapshotRoot extends AbstractSnapshot<byte[], byte[]> {

  @Getter
  private Snapshot solidity;
  private boolean isAccountDB;

  protected Cache<Key, byte[]> level2Cache;

  private static Set<String> notUpdateLevel2CacheBbNameSet = new HashSet<>();
  private static Set<String> notContainLevel2CacheBbNameSet = new HashSet<>();
  private static Map<String, Integer> level2CacheSizeMap = new HashMap<>();

  static {
    notUpdateLevel2CacheBbNameSet.add("block-index");
    notUpdateLevel2CacheBbNameSet.add("block");
    notUpdateLevel2CacheBbNameSet.add("recent-block");

    notContainLevel2CacheBbNameSet.add("market_pair_price_to_order");
    notContainLevel2CacheBbNameSet.add("transactionHistoryStore");
    notContainLevel2CacheBbNameSet.add("transactionRetStore");
    notContainLevel2CacheBbNameSet.add("trans");
    notContainLevel2CacheBbNameSet.add("recent-transaction");

    level2CacheSizeMap.put("block", 100);
    level2CacheSizeMap.put("recent-block", 100);
    level2CacheSizeMap.put("block-index", 100);
  }

  public SnapshotRoot(DB<byte[], byte[]> db) {
    this.db = db;
    if (!isNotInitLevel2Cache()) {
      this.level2Cache = CacheBuilder.newBuilder().initialCapacity(100).maximumSize(getMaxSize())
          .expireAfterAccess(1, TimeUnit.MINUTES).build();
    }
    solidity = this;
    isOptimized = "properties".equalsIgnoreCase(db.getDbName());
    isAccountDB = "account".equalsIgnoreCase(db.getDbName());
  }

  private boolean needOptAsset() {
    return isAccountDB && ChainBaseManager.getInstance().getDynamicPropertiesStore()
        .getAllowAccountAssetOptimizationFromRoot() == 1;
  }

  private boolean isNotUpdate() {
    return notUpdateLevel2CacheBbNameSet.contains(db.getDbName());
  }

  private boolean isNotInitLevel2Cache() {
    return notContainLevel2CacheBbNameSet.contains(db.getDbName());
  }

  private int getMaxSize() {
    return level2CacheSizeMap.getOrDefault(db.getDbName(), 100000);
  }

  private void clearCache() {
    level2Cache.invalidateAll();
  }

  private void putCache(byte[] key, byte[] value) {
    if (key == null || level2Cache == null || isNotUpdate()) {
      return;
    }
    if (value == null) {
      level2Cache.invalidate(Key.copyOf(key));
    } else {
      level2Cache.put(Key.copyOf(key), value);
    }
  }

  private byte[] getFromCache(byte[] key) {
    if (level2Cache == null) {
      return null;
    }
    return level2Cache.getIfPresent(Key.copyOf(key));
  }

  private void removeFromCache(byte[] key) {
    if (level2Cache == null) {
      return;
    }
    level2Cache.invalidate(Key.copyOf(key));
  }

  private void updateCache(Map<WrappedByteArray, WrappedByteArray> batch) {
    batch.forEach((k, v) -> {
      putCache(k.getBytes(), v.getBytes());
    });
  }

  @Override
  public byte[] get(byte[] key) {
    byte[] value = getFromCache(key);
    if (value == null) {
      value = db.get(key);
      putCache(key, value);
    }
    return value;
  }

  @Override
  public void put(byte[] key, byte[] value) {
    byte[] insertValue = value;
    if (needOptAsset()) {
      if (ByteArray.isEmpty(value)) {
        remove(key);
        return;
      }
      AccountAssetStore assetStore =
          ChainBaseManager.getInstance().getAccountAssetStore();
      AccountCapsule item = new AccountCapsule(value);
      if (!item.getAssetOptimized()) {
        assetStore.deleteAccount(item.createDbKey());
        item.setAssetOptimized(true);
      }
      assetStore.putAccount(item.getInstance());
      item.clearAsset();
      insertValue = item.getData();
    }
    db.put(key, insertValue);
    putCache(key, insertValue);
  }

  @Override
  public void remove(byte[] key) {
    if (needOptAsset()) {
      ChainBaseManager.getInstance().getAccountAssetStore().deleteAccount(key);
    }
    db.remove(key);
    removeFromCache(key);
  }

  @Override
  public void merge(Snapshot from) {
    SnapshotImpl snapshot = (SnapshotImpl) from;
    Map<WrappedByteArray, WrappedByteArray> batch = Streams.stream(snapshot.db)
        .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey().getBytes()),
            WrappedByteArray.of(e.getValue().getBytes())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    if (needOptAsset()) {
      processAccount(batch);
    } else {
      ((Flusher) db).flush(batch);
    }
    updateCache(batch);
  }

  public void merge(List<Snapshot> snapshots) {
    Map<WrappedByteArray, WrappedByteArray> batch = new HashMap<>();
    for (Snapshot snapshot : snapshots) {
      SnapshotImpl from = (SnapshotImpl) snapshot;
      Streams.stream(from.db)
          .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey().getBytes()),
              WrappedByteArray.of(e.getValue().getBytes())))
          .forEach(e -> batch.put(e.getKey(), e.getValue()));
    }
    if (needOptAsset()) {
      processAccount(batch);
    } else {
      ((Flusher) db).flush(batch);
      updateCache(batch);
    }
  }

  private void processAccount(Map<WrappedByteArray, WrappedByteArray> batch) {
    AccountAssetStore assetStore = ChainBaseManager.getInstance().getAccountAssetStore();
    Map<WrappedByteArray, WrappedByteArray> accounts = new HashMap<>();
    Map<WrappedByteArray, WrappedByteArray> assets = new HashMap<>();
    batch.forEach((k, v) -> {
      if (ByteArray.isEmpty(v.getBytes())) {
        accounts.put(k, v);
        assets.putAll(assetStore.getDeletedAssets(k.getBytes()));
      } else {
        AccountCapsule item = new AccountCapsule(v.getBytes());
        if (!item.getAssetOptimized()) {
          assets.putAll(assetStore.getDeletedAssets(k.getBytes()));
          item.setAssetOptimized(true);
        }
        assets.putAll(assetStore.getAssets(item.getInstance()));
        item.clearAsset();
        accounts.put(k, WrappedByteArray.of(item.getData()));
      }
    });
    ((Flusher) db).flush(accounts);
    updateCache(accounts);
    if (assets.size() > 0) {
      assetStore.updateByBatch(AccountAssetStore.convert(assets));
    }
  }

  @Override
  public Snapshot retreat() {
    return this;
  }

  @Override
  public Snapshot getRoot() {
    return this;
  }

  @Override
  public Iterator<Map.Entry<byte[], byte[]>> iterator() {
    return db.iterator();
  }

  @Override
  public void close() {
    ((Flusher) db).close();
  }

  @Override
  public void reset() {
    ((Flusher) db).reset();
    clearCache();
  }

  @Override
  public void resetSolidity() {
    solidity = this;
  }

  @Override
  public void updateSolidity() {
    solidity = solidity.getNext();
  }

  @Override
  public String getDbName() {
    return db.getDbName();
  }

  @Override
  public Snapshot newInstance() {
    return new SnapshotRoot(db.newInstance());
  }
}
