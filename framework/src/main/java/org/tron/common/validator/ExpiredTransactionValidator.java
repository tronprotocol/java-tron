package org.tron.common.validator;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionCapsule;

@Component("expiredTransactionValidator")
public class ExpiredTransactionValidator extends AbstractTransactionValidator {

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Override
  protected Pair<GrpcAPI.Return.response_code, String> doValidate(TransactionCapsule trx) {
    long transactionExpiration = trx.getExpiration();
    long headBlockTime = chainBaseManager.getHeadBlockTimeStamp();
    if (transactionExpiration <= headBlockTime
        || transactionExpiration > headBlockTime + Constant.MAXIMUM_TIME_UNTIL_EXPIRATION) {
      return buildResponse(GrpcAPI.Return.response_code.TRANSACTION_EXPIRATION_ERROR,
          "Transaction expiration, transaction expiration time is %d, but headBlockTime is %d",
          transactionExpiration, headBlockTime);
    }
    return SUCCESS;
  }
}
