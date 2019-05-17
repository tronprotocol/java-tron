package org.tron.core.db;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.capsule.TransactionResultListCapsule;
import org.tron.core.exception.BadItemException;
import java.util.Objects;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionResultList;

@Slf4j(topic = "DB")
@Component
public class TransactionResultListStore  extends TronStoreWithRevoking<TransactionResultListCapsule>  {

  @Autowired
  private TransactionStore transactionStore;

  @Autowired
  public TransactionResultListStore(@Value("transactionResultListStore") String dbName) {
    super(dbName);
  }

  public TransactionInfoCapsule getTransactionInfo(byte[] key) throws BadItemException {
    TransactionCapsule transactionCapsule = transactionStore.get(key);
    if (Objects.isNull(transactionCapsule)) {
      return null;
    }
    byte[] value = revokingDB.getUnchecked(ByteArray.fromLong(transactionCapsule.getBlockNum()));
    TransactionResultListCapsule result = new TransactionResultListCapsule(value);
    if (Objects.isNull(result)) {
      return null;
    }
    for (TransactionInfo transactionResultInfo : result.getInstance().getTransactioninfoList()) {
      if (transactionResultInfo.getId().equals(Sha256Hash.wrap(key))) {
        return new TransactionInfoCapsule(transactionResultInfo);
      }
    }
    return null;
  }

}
