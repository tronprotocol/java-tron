package org.tron.core.db;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.DeferredTransactionCapsule;

@Slf4j(topic = "DB")
@Component
public class DeferredTransactionIdIndexStore extends TronStoreWithRevoking<DeferredTransactionCapsule>  {
  @Autowired
  private DeferredTransactionIdIndexStore(@Value("deferred_transactionId_index") String dbName) {
    super(dbName);
  }

  public void put(DeferredTransactionCapsule deferredTransactionCapsule){
    byte[] trxId = deferredTransactionCapsule.getTransactionId().toByteArray();
    revokingDB.put(trxId, deferredTransactionCapsule.getKey());
  }

  public void put(byte[] key, byte[] value) {
    revokingDB.put(key, value);
  }

  public void removeDeferredTransactionIdIndex(ByteString transactionId) {
    revokingDB.delete(transactionId.toByteArray());
  }

  public byte[] getDeferredTransactionKeyById(ByteString transactionId) {
    return revokingDB.getUnchecked(transactionId.toByteArray());
  }
}
