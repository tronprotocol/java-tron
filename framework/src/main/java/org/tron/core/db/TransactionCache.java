package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db2.common.TxCacheDB;

@Slf4j(topic = "db")
public class TransactionCache extends TronStoreWithRevoking<BytesCapsule> {

  private static final String DB_NAME = "trans-cache";
  private static final TxCacheDB txCacheDB = new TxCacheDB(DB_NAME);

  @Autowired
  public TransactionCache(@Value("trans-cache") String dbName) {
    super(txCacheDB);
  }

  public void initCache() {
    long s = System.currentTimeMillis();
    txCacheDB.init();
    logger.info("Init tx-cache done with {} ms", System.currentTimeMillis() - s);
  }
}
