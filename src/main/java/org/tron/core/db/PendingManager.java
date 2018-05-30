package org.tron.core.db;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.ValidateSignatureException;

@Slf4j
public class PendingManager implements AutoCloseable {

  @Getter
  static List<TransactionCapsule> tmpTransactions = new ArrayList<>();
  Manager dbManager;

  public PendingManager(Manager db) {
    this.dbManager = db;
    tmpTransactions.addAll(db.getPendingTransactions());
    db.getPendingTransactions().clear();
    db.getDialog().reset();
  }

  @Override
  public void close() {
    rePush(this.tmpTransactions);
    rePush(dbManager.getPoppedTransactions());
    dbManager.getPoppedTransactions().clear();
    tmpTransactions.clear();
  }

  private void rePush(List<TransactionCapsule> txs) {
    txs.stream()
        .filter(
            trx -> {
              try {
                return
                    dbManager.getTransactionStore().get(trx.getTransactionId().getBytes()) == null;
              } catch (BadItemException e) {
                return true;
              }
            })
        .forEach(trx -> {
          try {
            dbManager.pushTransactions(trx);
          } catch (ValidateSignatureException e) {
            logger.debug(e.getMessage(), e);
          } catch (ContractValidateException e) {
            logger.debug(e.getMessage(), e);
          } catch (ContractExeException e) {
            logger.debug(e.getMessage(), e);
          } catch (AccountResourceInsufficientException e) {
            logger.debug(e.getMessage(), e);
          } catch (DupTransactionException e) {
            logger.debug("pending manager: dup trans", e);
          } catch (TaposException e) {
            logger.debug("pending manager: tapos exception", e);
          } catch (TooBigTransactionException e) {
            logger.debug("too big transaction");
          } catch (TransactionExpirationException e) {
            logger.debug("expiration transaction");
          }
        });
  }
}
