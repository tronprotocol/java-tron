package org.tron.core.db;

import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.BadItemException;

@Component
public class TransactionHistoryStore extends TronDatabase<TransactionResultCapsule> {

  @Autowired
  public TransactionHistoryStore(@Value("transactionHistoryStore") String dbName) {
    super(dbName);
  }


  @Override
  public void put(byte[] key, TransactionResultCapsule item) {
    if (Objects.isNull(key) || Objects.isNull(item)) {
      return;
    }
    dbSource.putData(key, item.getData());
  }

  @Override
  public void delete(byte[] key) {
    dbSource.deleteData(key);
  }

  @Override
  public TransactionResultCapsule get(byte[] key) throws BadItemException {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new TransactionResultCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] account = dbSource.getData(key);
    return null != account;
  }


}