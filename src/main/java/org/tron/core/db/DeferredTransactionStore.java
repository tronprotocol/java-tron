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
import org.springframework.stereotype.Component;
import org.tron.core.capsule.DeferredTransactionCapsule;

@Slf4j(topic = "DB")
@Component
public class DeferredTransactionStore extends TronStoreWithRevoking<DeferredTransactionCapsule> {

  @Autowired
  private DeferredTransactionIdIndexStore deferredTransactionIdIndexStore;

  @Autowired
  private DeferredTransactionStore(@Value("deferred_transaction") String dbName) {
    super(dbName);
  }

  public void put(DeferredTransactionCapsule deferredTransactionCapsule) {
    super.put(deferredTransactionCapsule.getKey(), deferredTransactionCapsule);
  }

  public List<DeferredTransactionCapsule> getScheduledTransactions(long time) {
    return revokingDB.getValuesPrevious(Longs.toByteArray(time), MAX_TRANSACTION_PENDING).stream()
            .filter(Objects::nonNull)
            .map(DeferredTransactionCapsule::new)
            .collect(Collectors.toList());
  }

  public List<DeferredTransactionCapsule> getScheduledTransactions() {
    return getScheduledTransactions(System.currentTimeMillis());
  }

  public void removeDeferredTransaction(DeferredTransactionCapsule deferredTransactionCapsule) {
    revokingDB.delete(deferredTransactionCapsule.getKey());
  }

  public DeferredTransactionCapsule getByTransactionId(ByteString transactionId) {
    DeferredTransactionCapsule deferredTransactionCapsule = null;
    try {
      byte[] key = deferredTransactionIdIndexStore.revokingDB.get(transactionId.toByteArray());
      if (ArrayUtils.isEmpty(key)) {
        return null;
      }

      byte[] value = revokingDB.get(key);
      if (ArrayUtils.isEmpty(value)) {
        return null;
      }

      deferredTransactionCapsule = new DeferredTransactionCapsule(value);
    } catch (Exception e) {
      logger.info("{}", e);
    }
    return deferredTransactionCapsule;
  }
}
