package org.tron.core.db;

import java.util.Iterator;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.common.iterator.TransactionIterator;

@Slf4j
@Component
public class TransactionStore extends TronStoreWithRevoking<TransactionCapsule> {

  @Autowired
  private TransactionStore(@Qualifier("trans") String dbName) {
    super(dbName);
  }

  @Override
  public TransactionCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new TransactionCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] transaction = dbSource.getData(key);
    logger.info("address is {}, transaction is {}", key, transaction);
    return null != transaction;
  }

  @Override
  public void put(byte[] key, TransactionCapsule item) {
    if (indexHelper != null) {
      indexHelper.update(item.getInstance());
    }
    super.put(key, item);
  }

  /**
   * get total transaction.
   */
  public long getTotalTransactions() {
    return dbSource.getTotal();
  }

  private static TransactionStore instance;

  public static void destory() {
    instance = null;
  }

  public static void destroy() {
    instance = null;
  }

  /**
   * create Fun.
   */
  public static TransactionStore create(String dbName) {
    if (instance == null) {
      synchronized (AccountStore.class) {
        if (instance == null) {
          instance = new TransactionStore(dbName);
        }
      }
    }
    return instance;
  }

  /**
   * find a transaction  by it's id.
   */
  public byte[] findTransactionByHash(byte[] trxHash) {
    return dbSource.getData(trxHash);
  }

  @Override
  public Iterator<Entry<byte[], TransactionCapsule>> iterator() {
    return new TransactionIterator(dbSource.iterator());
  }

}
