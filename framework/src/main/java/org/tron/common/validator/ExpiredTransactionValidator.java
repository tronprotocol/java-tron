package org.tron.common.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionCapsule;

@Component("expiredTransactionValidator")
public class ExpiredTransactionValidator extends AbstractTransactionValidator {

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Override
  protected String doValidate(TransactionCapsule trx) {
    long transactionExpiration = trx.getExpiration();
    long headBlockTime = chainBaseManager.getHeadBlockTimeStamp();
    if (transactionExpiration <= headBlockTime
        || transactionExpiration > headBlockTime + Constant.MAXIMUM_TIME_UNTIL_EXPIRATION) {
      return String.format(
          "Transaction expiration, transaction expiration time is %d, but headBlockTime is %d",
          transactionExpiration, headBlockTime);
    }
    return null;
  }
}
