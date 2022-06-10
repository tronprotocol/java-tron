package org.tron.core.store;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.db.TronDatabase;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.protos.Protocol;

import java.util.HashMap;
import java.util.Map;

@Component
public class AccountAssetStore extends TronDatabase<byte[]> {

  @Autowired
  protected AccountAssetStore(@Value("account-asset") String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, byte[] item) {
    dbSource.putData(key, item);
  }

  @Override
  public void delete(byte[] key) {
    dbSource.deleteData(key);
  }

  @Override
  public byte[] get(byte[] key) {
    return dbSource.getData(key);
  }

  @Override
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
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
