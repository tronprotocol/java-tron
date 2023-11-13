package org.tron.common.validator;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionCapsule;

@Component("bigTransactionValidator")
public class BigTransactionValidator extends AbstractTransactionValidator {

  @Override
  protected Pair<GrpcAPI.Return.response_code, String> doValidate(TransactionCapsule trx) {
    if (trx.getSerializedSize() > Constant.TRANSACTION_MAX_BYTE_SIZE) {
      return buildResponse(GrpcAPI.Return.response_code.TOO_BIG_TRANSACTION_ERROR,
          "too big transaction, the size is %d bytes", trx.getSerializedSize());
    }
    return SUCCESS;
  }
}
