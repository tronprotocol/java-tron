package org.tron.common.validator;

import org.apache.commons.lang3.tuple.Pair;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.core.capsule.TransactionCapsule;

public abstract class AbstractTransactionValidator implements Validator<
    Pair<response_code, String>, TransactionCapsule> {
  private Validator<Pair<response_code, String>, TransactionCapsule> next;
  protected static final Pair<response_code, String> SUCCESS = Pair.of(response_code.SUCCESS, null);

  @Override
  public Validator<Pair<response_code, String>, TransactionCapsule> nextValidator(
      Validator<Pair<response_code, String>, TransactionCapsule> next) {
    this.next = next;
    return this.next;
  }

  @Override
  public Pair<response_code, String> validate(final TransactionCapsule trx) {
    Pair<response_code, String> ret = doValidate(trx);
    if (ret.getKey() == response_code.SUCCESS && this.next != null) {
      return this.next.validate(trx);
    }
    return ret;
  }

  protected abstract Pair<response_code, String> doValidate(final TransactionCapsule trx);

  protected Pair<response_code, String> buildResponse(
      response_code code, String format, Object... args) {
    return Pair.of(code, String.format(format, args));
  }
}
