package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.TransactionResultListCapsule;

@Slf4j(topic = "DB")
@Component
public class TransactionResultListStore  extends TronStoreWithRevoking<TransactionResultListCapsule>  {

  @Autowired
  public TransactionResultListStore(@Value("transactionResultListStore") String dbName) {
    super(dbName);
  }
}
