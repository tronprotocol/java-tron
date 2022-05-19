package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.primitives.Longs;
import lombok.Getter;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.utils.AssetUtil;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.Flusher;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.store.AccountAssetStore;

public class SnapshotRoot extends AbstractSnapshot<byte[], byte[]> {

  @Getter
  private Snapshot solidity;
  private boolean isAccountDB;

  public SnapshotRoot(DB<byte[], byte[]> db) {
    this.db = db;
    solidity = this;
    isOptimized = "properties".equalsIgnoreCase(db.getDbName());
    isAccountDB = "account".equalsIgnoreCase(db.getDbName());
  }

  @Override
  public byte[] get(byte[] key) {
    return db.get(key);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    if (isAccountDB && AssetUtil.isAllowAssetOptimization()) {
      AccountAssetStore store = ChainBaseManager.getInstance().getAccountAssetStore();
      AccountCapsule item = new AccountCapsule(value);
      item.getInstance().getAssetV2Map().forEach((k, v) ->
              store.put(item, k.getBytes(), Longs.toByteArray(v)));
      item.clearAsset();
      db.put(key, item.getData());
    } else {
      db.put(key, value);
    }
  }

  @Override
  public void remove(byte[] key) {
    if (isAccountDB && AssetUtil.isAllowAssetOptimization()) {
      AccountAssetStore store = ChainBaseManager.getInstance().getAccountAssetStore();
      byte[] vale = get(key);
      if (vale.length > 0) {
        AccountCapsule item = new AccountCapsule(get(key));
        item.getAssetMapV2().forEach((k, v) ->
                store.put(item, k.getBytes(), Longs.toByteArray(0)));
      }
    }
    db.remove(key);
  }

  @Override
  public void merge(Snapshot from) {
    SnapshotImpl snapshot = (SnapshotImpl) from;
    Map<WrappedByteArray, WrappedByteArray> batch = Streams.stream(snapshot.db)
        .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey().getBytes()),
            WrappedByteArray.of(e.getValue().getBytes())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    ((Flusher) db).flush(batch);
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

    ((Flusher) db).flush(batch);
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
