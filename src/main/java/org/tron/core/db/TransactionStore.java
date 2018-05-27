package org.tron.core.db;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.common.iterator.TransactionIterator;

@Slf4j
@Component
public class TransactionStore extends TronStoreWithRevoking<TransactionCapsule> {

  @Autowired
  private TransactionStore(@Value("trans") String dbName) {
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
    super.put(key, item);
    if (Objects.nonNull(indexHelper)) {
      indexHelper.update(item.getInstance());
    }
  }

  /**
   * get total transaction.
   */
  public long getTotalTransactions() {
    return dbSource.getTotal();
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

  @Override
  public void delete(byte[] key) {
    deleteIndex(key);
    super.delete(key);
  }

  private void deleteIndex(byte[] key) {
    if (Objects.nonNull(indexHelper)) {
      TransactionCapsule item = get(key);
      if (Objects.nonNull(item)) {
        indexHelper.remove(item.getInstance());
      }
    }
  }
}
