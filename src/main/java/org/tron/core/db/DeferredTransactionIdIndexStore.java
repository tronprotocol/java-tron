package org.tron.core.db;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.DeferredTransactionCapsule;

import java.util.ArrayList;
import java.util.List;

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

  public void removeDeferredTransactionIdIndex(DeferredTransactionCapsule deferredTransactionCapsule) {
    revokingDB.delete(deferredTransactionCapsule.getTransactionId().toByteArray());
  }
}
