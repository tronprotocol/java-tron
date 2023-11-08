package org.tron.common.validator;

import org.springframework.stereotype.Component;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionCapsule;

@Component("bigTransactionValidator")
public class BigTransactionValidator extends AbstractTransactionValidator {

  @Override
  protected String doValidate(TransactionCapsule trx) {
    if (trx.getSerializedSize() > Constant.TRANSACTION_MAX_BYTE_SIZE) {
      return String.format("too big transaction, the size is %d bytes", trx.getSerializedSize());
    }
    return null;
  }
}
