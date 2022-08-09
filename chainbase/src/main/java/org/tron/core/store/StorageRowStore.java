package org.tron.core.store;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.db.TronStoreWithRevoking;

@Slf4j(topic = "DB")
@Component
public class StorageRowStore extends TronStoreWithRevoking<StorageRowCapsule> {

  private Cache<String, String> cache = CacheBuilder.newBuilder()
          .maximumSize(1000).initialCapacity(1000).build();

  @Autowired
  private StorageRowStore(@Value("storage-row") String dbName) {
    super(dbName);
  }

  @Override
  public StorageRowCapsule get(byte[] key) {
    StorageRowCapsule row = getUnchecked(key);
    row.setRowKey(key);

    String sKey = Hex.encodeHexString(key);
    if (row == null || row.getData() == null) {
      logger.info("### get k:{}, v:null", sKey);
    } else {
      String value = Hex.encodeHexString(row.getData());
      String vv = cache.getIfPresent(sKey);
      if (vv == null || !vv.equals(value)) {
        logger.info("### get k:{}, v:{}", sKey, value);
      }
      cache.put(sKey, value);
    }

    return row;
  }

  @Override
  public void put(byte[] k, StorageRowCapsule capsule) {
    String sKey = Hex.encodeHexString(k);
    if (capsule == null || capsule.getData() == null) {
      logger.info("### put k:{}, v:null", sKey);
    } else {
      String vv = cache.getIfPresent(sKey);
      String value = Hex.encodeHexString(capsule.getData());
      if (vv == null || !vv.equals(value)) {
        logger.info("### put k:{}, v:{}", sKey, value);
      }

      cache.put(sKey, value);
    }

    super.put(k, capsule);
  }


}
