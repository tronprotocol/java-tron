package org.tron.common.validator;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractSizeNotEqualToOneException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.ValidateSignatureException;

@Component
@Slf4j(topic = "transactionValidator")
public class TransactionValidator {

  @Autowired
  private Validator<Pair<response_code, String>, TransactionCapsule> dupTransactionValidator;
  @Autowired
  private Validator<Pair<response_code, String>, TransactionCapsule> bigTransactionValidator;
  @Autowired
  private Validator<Pair<response_code, String>, TransactionCapsule> expiredTransactionValidator;
  @Autowired
  private Validator<Pair<response_code, String>, TransactionCapsule> contractSizeValidator;
  @Autowired
  private Validator<Pair<response_code, String>, TransactionCapsule> taposTransactionValidator;
  @Autowired
  private Validator<Pair<response_code, String>, TransactionCapsule> signatureValidator;

  @PostConstruct
  private void prepare() {
    dupTransactionValidator
        .nextValidator(bigTransactionValidator)
        .nextValidator(expiredTransactionValidator)
        .nextValidator(contractSizeValidator)
        .nextValidator(taposTransactionValidator)
        .nextValidator(signatureValidator);
  }

  /**
   * validate transaction for block production.
   *
   * @param trx transaction to be validated
   * @return true if transaction is valid, false otherwise
   */
  public boolean silentValidate(final TransactionCapsule trx) {
    try {
      validate(trx);
      return true;
    } catch (ContractSizeNotEqualToOneException | DupTransactionException
        | TooBigTransactionException | TransactionExpirationException | ValidateSignatureException
        | TaposException e) {
      logger.info("invalid transaction {}, {}.", trx.getTransactionId(), e.getMessage());
      return false;
    } catch (Exception e) {
      logger.warn("validate transaction {}.", trx.getTransactionId(), e);
      return false;
    }
  }

  /**
   * validate transaction for block push or transaction broadcast.
   *
   * @param trx transaction to be validated
   * @throws TooBigTransactionException when transaction size is too big.
   * @throws ContractSizeNotEqualToOneException when contract size is not equal to one.
   * @throws DupTransactionException when transaction is duplicated.
   * @throws TransactionExpirationException when transaction expiration time is invalid.
   * @throws ValidateSignatureException when transaction signature is invalid.
   * @throws TaposException when transaction tapos is invalid.
   */
  public void validate(final TransactionCapsule trx) throws TooBigTransactionException,
      ContractSizeNotEqualToOneException, DupTransactionException, TransactionExpirationException,
      ValidateSignatureException, TaposException {
    Pair<response_code, String> ret = dupTransactionValidator.validate(trx);
    switch (ret.getKey()) {
      case TOO_BIG_TRANSACTION_ERROR:
        throw new TooBigTransactionException(ret.getValue());
      case CONTRACT_VALIDATE_ERROR:
        throw new ContractSizeNotEqualToOneException(ret.getValue());
      case DUP_TRANSACTION_ERROR:
        throw new DupTransactionException(ret.getValue());
      case TRANSACTION_EXPIRATION_ERROR:
        throw new TransactionExpirationException(ret.getValue());
      case SIGERROR:
        throw new ValidateSignatureException(ret.getValue());
      case TAPOS_ERROR:
        throw new TaposException(ret.getValue());
      case SUCCESS:
        break;
      default:
        throw new IllegalStateException("Unexpected: " + ret.getKey());
    }
  }
}
