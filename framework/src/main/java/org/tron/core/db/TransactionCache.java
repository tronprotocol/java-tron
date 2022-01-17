package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db2.common.TxCacheDB;

@Slf4j(topic = "db")
@Component
@Order
public class TransactionCache extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  private TxCacheDB txCacheDB;

  @Autowired
  public TransactionCache(TxCacheDB txCacheDB) {
    super(txCacheDB);
    this.txCacheDB = txCacheDB;
  }

  public void initDone() throws InterruptedException {
    txCacheDB.getInitDone().await();
  }
}
