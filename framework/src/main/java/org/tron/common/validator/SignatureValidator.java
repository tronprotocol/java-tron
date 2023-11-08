package org.tron.common.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
  protected String doValidate(TransactionCapsule trx) {
    try {
      trx.validateSignature(accountStore, dynamicPropertiesStore);
      return null;
    } catch (ValidateSignatureException e) {
      return e.getMessage();
    }
  }
}
