package org.tron.common.validator;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;

@Component("signatureValidator")
public class SignatureValidator extends AbstractTransactionValidator {

  @Autowired
  private AccountStore accountStore;
  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;

  @Override
  protected Pair<GrpcAPI.Return.response_code, String> doValidate(TransactionCapsule trx) {
    try {
      trx.validateSignature(accountStore, dynamicPropertiesStore);
      return SUCCESS;
    } catch (ValidateSignatureException e) {
      return buildResponse(GrpcAPI.Return.response_code.SIGERROR, e.getMessage());
    }
  }
}
