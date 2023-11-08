package org.tron.common.validator;

import org.springframework.stereotype.Component;
import org.tron.core.capsule.TransactionCapsule;

@Component("contractSizeValidator")
public class ContractSizeValidator extends AbstractTransactionValidator {

  @Override
  protected String doValidate(TransactionCapsule trx) {
    int contractSize = trx.getContractSize();
    if (contractSize != 1) {
      return String.format("contract size should be exactly 1, this is extend feature ,actual :%d",
          contractSize);
    }
    return null;
  }
}
