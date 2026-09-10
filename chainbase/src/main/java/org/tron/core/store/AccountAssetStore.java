package org.tron.core.store;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.core.db.TronDatabase;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.common.RocksDB;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.db2.core.SnapshotRoot;
import org.tron.protos.Protocol;

@Component
public class AccountAssetStore extends TronDatabase<byte[]> {

  private volatile Chainbase snapshots;
  private boolean recoveringSnapshots;

  /** Shares the existing native DB; registers before recovery and before any sessions exist. */
  public synchronized void enableSnapshots(SnapshotManager manager) {
    enableSnapshots(manager, false);
  }

  public synchronized void enableSnapshots(SnapshotManager manager, boolean recovering) {
    if (snapshots != null) {
      throw new IllegalStateException("AccountAsset Snapshot lane already attached");
    }
    DB<byte[], byte[]> engine;
    if (dbSource instanceof LevelDbDataSourceImpl) {
      engine = new LevelDB(
          (LevelDbDataSourceImpl) dbSource);
    } else if (dbSource instanceof RocksDbDataSourceImpl) {
      engine = new RocksDB(
          (RocksDbDataSourceImpl) dbSource);
    } else {
      throw new IllegalStateException("Unsupported AccountAsset Snapshot engine");
    }
    Chainbase lane = new Chainbase(
        new SnapshotRoot(engine));
    lane.setRegistrationSource(AccountAssetStore.class.getName());
    manager.installP66SnapshotLane(lane, recovering);
    recoveringSnapshots = recovering;
    snapshots = lane;
  }

  public synchronized void finishSnapshotRecovery(SnapshotManager manager) {
    manager.finishP66Recovery();
    recoveringSnapshots = false;
  }

  @Override
  public void close() {
    if (snapshots != null) {
      snapshots.close();
    }
    super.close();
  }

  @Override
  public Map<WrappedByteArray, byte[]> prefixQuery(byte[] key) {
    return snapshots == null ? super.prefixQuery(key) : snapshots.prefixQuery(key);
  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows) {
    if (snapshots == null) {
      super.updateByBatch(rows);
    } else {
      rows.forEach((key, value) -> {
        if (value == null) {
          snapshots.delete(key);
        } else {
          snapshots.put(key, value);
        }
      });
    }
  }

  @Override
  public void updateByBatchSynced(Map<byte[], byte[]> rows) {
    if (snapshots != null && !recoveringSnapshots) {
      throw new IllegalStateException("AccountAsset durability belongs to Common checkpoint");
    }
    super.updateByBatchSynced(rows);
  }

  @Override
  public byte[] getFromRoot(byte[] key) {
    return dbSource.getData(key);
  }

  @Override
  public byte[] getUnchecked(byte[] key) {
    return get(key);
  }

  @Autowired
  protected AccountAssetStore(@Value("account-asset") String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, byte[] item) {
    if (snapshots == null) {
      dbSource.putData(key, item);
    } else {
      snapshots.put(key, item);
    }
  }

  @Override
  public void delete(byte[] key) {
    if (snapshots == null) {
      dbSource.deleteData(key);
    } else {
      snapshots.delete(key);
    }
  }

  @Override
  public byte[] get(byte[] key) {
    return snapshots == null ? dbSource.getData(key) : snapshots.getUnchecked(key);
  }

  @Override
  public boolean has(byte[] key) {
    return get(key) != null;
  }

  public void putAccount(Protocol.Account account) {
    Map<byte[], byte[]> assets = convert(getAssets(account));
    if (!assets.isEmpty()) {
      updateByBatch(assets);
    }
  }

  public void deleteAccount(byte[] key) {
    Map<byte[], byte[]> assets = convert(getDeletedAssets(key));
    if (!assets.isEmpty()) {
      updateByBatch(assets);
    }
  }

  public Map<WrappedByteArray, WrappedByteArray> getAssets(Protocol.Account account) {
    Map<WrappedByteArray, WrappedByteArray> assets = new HashMap<>();
    account.getAssetV2Map().forEach((k, v) -> {
      byte[] key = Bytes.concat(account.getAddress().toByteArray(), k.getBytes());
      if (v == 0) {
        assets.put(WrappedByteArray.of(key), WrappedByteArray.of(null));
      } else {
        assets.put(WrappedByteArray.of(key), WrappedByteArray.of(Longs.toByteArray(v)));
      }
    });
    return assets;
  }

  public Map<WrappedByteArray, WrappedByteArray> getDeletedAssets(byte[] key) {
    Map<WrappedByteArray, WrappedByteArray> assets = new HashMap<>();
    prefixQuery(key).forEach((k, v) ->
            assets.put(WrappedByteArray.of(k.getBytes()), WrappedByteArray.of(null)));
    return assets;
  }

  public static Map<byte[], byte[]> convert(Map<WrappedByteArray, WrappedByteArray> map) {
    Map<byte[], byte[]> assets = new HashMap<>();
    map.forEach((k, v) -> assets.put(k.getBytes(), v.getBytes()));
    return assets;
  }

  public long getBalance(Protocol.Account account, byte[] key) {
    if (!account.getAssetOptimized()) {
      return 0;
    }
    byte[] k = Bytes.concat(account.getAddress().toByteArray(), key);
    byte[] value = get(k);
    if (ArrayUtils.isEmpty(value)) {
      return 0;
    }
    return Longs.fromByteArray(value);
  }

  public Map<String, Long> getAllAssets(Protocol.Account account) {
    Map<String, Long> assets = new HashMap<>();
    if (account.getAssetOptimized()) {
      Map<WrappedByteArray, byte[]> map = prefixQuery(account.getAddress().toByteArray());
      map.forEach((k, v) -> {
        byte[] assetID = ByteArray.subArray(k.getBytes(),
                account.getAddress().toByteArray().length, k.getBytes().length);
        assets.put(ByteArray.toStr(assetID), Longs.fromByteArray(v));
      });
    }
    account.getAssetV2Map().forEach((k, v) -> assets.put(k, v));
    return assets;
  }
}
