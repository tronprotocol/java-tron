package org.tron.common.validator;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.core.capsule.TransactionCapsule;

@Component("contractSizeValidator")
public class ContractSizeValidator extends AbstractTransactionValidator {

  @Override
  protected Pair<GrpcAPI.Return.response_code, String> doValidate(TransactionCapsule trx) {
    int contractSize = trx.getContractSize();
    if (contractSize != 1) {
      return buildResponse(GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR,
          "contract size should be exactly 1, this is extend feature ,actual :%d",
          contractSize);
    }
    return SUCCESS;
  }
}
