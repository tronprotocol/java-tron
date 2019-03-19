package org.tron.core.db;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.DeferredTransactionCapsule;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.common.DeferredTransactionIdIndexCacheDB;

@Slf4j(topic = "DB")
public class DeferredTransactionIdIndexCache extends TronStoreWithRevoking<BytesCapsule>{

  @Autowired(required = false)
  public DeferredTransactionIdIndexCache(@Value("deferred-transactionid-index-cache") String dbName) {
    super(dbName, DeferredTransactionIdIndexCacheDB.class);
  }

  public void put(DeferredTransactionCapsule deferredTransactionCapsule) {
    byte[] trxId = deferredTransactionCapsule.getTransactionId().toByteArray();
    super.put(trxId, new BytesCapsule(deferredTransactionCapsule.getKey()));
  }

  public void put(WrappedByteArray key, WrappedByteArray value){
    super.put(key.getBytes(), new BytesCapsule(value.getBytes()));
  }

  public void removeDeferredTransactionIdIndex(ByteString transactionId) {
    super.delete(transactionId.toByteArray());
  }

  public byte[] getDeferredTransactionKeyById(ByteString transactionId) {
    byte[] value = null;
    try {
      value = super.get(transactionId.toByteArray()).getData();
    } catch (Exception ex) {
      logger.warn("get deferred transaction key by id {} failed, caused by {}", transactionId, ex);
    }

    return value;
  }
}
