package org.tron.common.validator;

import org.tron.core.capsule.TransactionCapsule;

public abstract class AbstractTransactionValidator implements Validator<TransactionCapsule> {
  private Validator<TransactionCapsule> next;

  @Override
  public Validator<TransactionCapsule> nextValidator(Validator<TransactionCapsule> next) {
    this.next = next;
    return this.next;
  }

  @Override
  public String validate(final TransactionCapsule trx) {
    String ret = doValidate(trx);
    if (ret == null && this.next != null) {
      return this.next.validate(trx);
    }
    return ret;
  }

  protected abstract String doValidate(final TransactionCapsule trx);

}
