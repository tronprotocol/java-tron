package org.tron.core.db;

import static org.tron.core.config.Parameter.NodeConstant.MAX_TRANSACTION_PENDING;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.DeferredTransactionCapsule;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.common.DeferredTransactionCacheDB;

@Slf4j(topic = "DB")
public class DeferredTransactionCache extends TronStoreWithRevoking<BytesCapsule>  {
  @Autowired(required = false)
  public DeferredTransactionCache(@Value("deferred-transaction-cache") String dbName) {
    super(dbName, DeferredTransactionCacheDB.class);
  }

  @Autowired(required = false)
  private DeferredTransactionIdIndexCache deferredTransactionIdIndexCache;

  public void put(DeferredTransactionCapsule deferredTransactionCapsule) {
    super.put(deferredTransactionCapsule.getKey(), new BytesCapsule(deferredTransactionCapsule.getData()));
  }

  public void put(WrappedByteArray key, WrappedByteArray value){
    super.put(key.getBytes(), new BytesCapsule(value.getBytes()));
  }

  public DeferredTransactionCapsule getByTransactionId(ByteString transactionId) {
    DeferredTransactionCapsule deferredTransactionCapsule = null;
    try {
      BytesCapsule key = deferredTransactionIdIndexCache.get(transactionId.toByteArray());
      if (Objects.isNull(key)) {
        return null;
      }

      BytesCapsule value = super.get(key.getData());
      if (Objects.isNull(value)) {
        return null;
      }

      deferredTransactionCapsule = new DeferredTransactionCapsule(value.getData());
    } catch (Exception e){
      logger.error("{}", e);
    }
    return deferredTransactionCapsule;
  }

  public void removeDeferredTransaction(DeferredTransactionCapsule deferredTransactionCapsule) {
    super.delete(deferredTransactionCapsule.getKey());
  }

  public List<DeferredTransactionCapsule> getScheduledTransactions (long time){

    return revokingDB.getValuesPrevious(Longs.toByteArray(time), MAX_TRANSACTION_PENDING).stream()
        .filter(Objects::nonNull)
        .map(DeferredTransactionCapsule::new)
        .collect(Collectors.toList());
  }

}
