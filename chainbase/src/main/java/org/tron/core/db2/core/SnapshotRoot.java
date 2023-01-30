package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import org.tron.common.cache.CacheManager;
import org.tron.common.cache.CacheType;
import org.tron.common.cache.TronCache;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.Flusher;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.store.AccountAssetStore;

public class SnapshotRoot extends AbstractSnapshot<byte[], byte[]> {

  @Getter
  private Snapshot solidity;
  private boolean isAccountDB;

  private TronCache<WrappedByteArray, WrappedByteArray> cache;
  private static final List<String> CACHE_DBS = CommonParameter.getInstance()
      .getStorage().getCacheDbs();

  public SnapshotRoot(DB<byte[], byte[]> db) {
    this.db = db;
    solidity = this;
    isAccountDB = "account".equalsIgnoreCase(db.getDbName());
    if (CACHE_DBS.contains(this.db.getDbName())) {
      this.cache = CacheManager.allocate(CacheType.findByType(this.db.getDbName()));
    }
  }

  private boolean needOptAsset() {
    return isAccountDB && ChainBaseManager.getInstance().getDynamicPropertiesStore()
            .getAllowAccountAssetOptimizationFromRoot() == 1;
  }

  @Override
  public byte[] get(byte[] key) {
    WrappedByteArray cache = getCache(key);
    if (cache != null) {
      return cache.getBytes();
    }
    byte[] value = db.get(key);
    putCache(key, value);
    return value;
  }

  @Override
  public void put(byte[] key, byte[] value) {
    byte[] v = value;
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
      v = item.getData();
    }
    db.put(key, v);
    putCache(key, v);
  }

  @Override
  public void remove(byte[] key) {
    if (needOptAsset()) {
      ChainBaseManager.getInstance().getAccountAssetStore().deleteAccount(key);
    }
    db.remove(key);
    putCache(key, null);
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
      putCache(batch);
    }
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
      putCache(batch);
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
    putCache(accounts);
    if (assets.size() > 0) {
      assetStore.updateByBatch(AccountAssetStore.convert(assets));
    }
  }

  private boolean cached() {
    return Objects.nonNull(this.cache);
  }

  private void putCache(byte[] key, byte[] value) {
    if (cached()) {
      cache.put(WrappedByteArray.of(key), WrappedByteArray.of(value));
    }
  }

  private void putCache(Map<WrappedByteArray, WrappedByteArray> values) {
    if (cached()) {
      values.forEach(cache::put);
    }
  }

  private WrappedByteArray getCache(byte[] key) {
    if (cached()) {
      return cache.getIfPresent(WrappedByteArray.of(key));
    }
    return null;
  }

  // second cache

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
    if (cached()) {
      CacheManager.release(cache);
    }
    ((Flusher) db).close();
  }

  @Override
  public void reset() {
    if (cached()) {
      CacheManager.release(cache);
    }
    ((Flusher) db).reset();
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
