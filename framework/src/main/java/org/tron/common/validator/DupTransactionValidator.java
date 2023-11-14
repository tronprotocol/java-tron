package org.tron.common.validator;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionCache;
import org.tron.core.db.TransactionStore;

@Component("dupTransactionValidator")
public class DupTransactionValidator extends AbstractTransactionValidator {

  @Autowired
  private TransactionStore transactionStore;

  @Autowired
  private TransactionCache transactionCache;

  @Override
  protected Pair<GrpcAPI.Return.response_code, String> doValidate(TransactionCapsule trx) {
    byte[] transactionId = trx.getTransactionId().getBytes();
    if (transactionCache.has(transactionId) && transactionStore.has(transactionId)) {
      return buildResponse(GrpcAPI.Return.response_code.DUP_TRANSACTION_ERROR, "dup trans : %s ",
          trx.getTransactionId());
    }
    return SUCCESS;
  }
}
