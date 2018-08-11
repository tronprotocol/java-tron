package org.tron.core.db;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.OutOfSlotTimeException;
import org.tron.core.exception.ReceiptException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.TransactionTraceException;
import org.tron.core.exception.ValidateSignatureException;

@Slf4j
public class PendingManager implements AutoCloseable {

  @Getter
  static List<TransactionCapsule> tmpTransactions = new ArrayList<>();
  Manager dbManager;

  public PendingManager(Manager db) {

    long jack_pendingManager_init_start = System.nanoTime() / 1000000;

    this.dbManager = db;
    tmpTransactions.addAll(db.getPendingTransactions());
    db.getPendingTransactions().clear();
    db.getSession().reset();

    logger.error("pending to block total consume: {} ms",
        System.nanoTime() / 1000000 - jack_pendingManager_init_start);
  }

  @Override
  public void close() {

    long jack_repush_start = System.nanoTime() / 1000000;

    rePush(this.tmpTransactions);
    rePush(dbManager.getPoppedTransactions());
    dbManager.getPoppedTransactions().clear();
    tmpTransactions.clear();

    logger.error("repush total consume: {} ms",
        System.nanoTime() / 1000000 - jack_repush_start);

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
          } catch (ReceiptException e) {
            logger.info("Receipt exception," + e.getMessage());
          } catch (TransactionExpirationException e) {
            logger.debug("expiration transaction");
          } catch (TransactionTraceException e) {
            logger.debug("transactionTrace transaction");
          } catch (OutOfSlotTimeException e) {
            logger.debug("outOfSlotTime transaction");
          }
        });
  }
}
