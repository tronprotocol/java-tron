package org.tron.core.db;

import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
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
      indexHelper.add(item.getInstance());
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

  void destroy() {
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
  public Iterator<TransactionCapsule> iterator() {
    return new TransactionIterator(dbSource.iterator());
  }

  public static void main(String[] args) {
    for (int i = 0; i < 4; i++) {
      ECKey ecKey = new ECKey(Utils.getRandom());
      System.out.println("privateKey:" + ByteArray.toHexString(ecKey.getPrivKey().toByteArray()));
      System.out.println("publicKey:" + ByteArray.toHexString(ecKey.getPubKey()));
      System.out.println("address:" + ByteArray.toHexString(ecKey.getAddress()));
    }

  }

}
