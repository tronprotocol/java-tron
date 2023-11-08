package org.tron.common.validator;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.TransactionCapsule;

@Component
@Slf4j(topic = "transactionValidator")
public class TransactionValidator {

  @Autowired
  private Validator<TransactionCapsule> dupTransactionValidator;
  @Autowired
  private Validator<TransactionCapsule> bigTransactionValidator;
  @Autowired
  private Validator<TransactionCapsule> expiredTransactionValidator;
  @Autowired
  private Validator<TransactionCapsule> contractSizeValidator;
  @Autowired
  private Validator<TransactionCapsule> taposTransactionValidator;
  @Autowired
  private Validator<TransactionCapsule> signatureValidator;

  @PostConstruct
  private void prepare() {
    dupTransactionValidator
        .nextValidator(bigTransactionValidator)
        .nextValidator(expiredTransactionValidator)
        .nextValidator(contractSizeValidator)
        .nextValidator(taposTransactionValidator)
        .nextValidator(signatureValidator);
  }

  public boolean validate(final TransactionCapsule trx) {
    try {
      String info = dupTransactionValidator.validate(trx);
      if (info != null) {
        logger.info("invalid transaction {}, {}.", trx.getTransactionId(), info);
        return false;
      }
      return true;
    } catch (Exception e) {
      logger.warn("validate transaction {}.", trx.getTransactionId(), e);
      return false;
    }
  }
}
