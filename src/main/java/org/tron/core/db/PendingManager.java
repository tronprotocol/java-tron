package org.tron.core.db;

import com.dianping.cat.Cat;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.HighFreqException;
import org.tron.core.exception.ValidateSignatureException;

@Slf4j
public class PendingManager implements AutoCloseable {

  List<TransactionCapsule> tmpTransactions = new ArrayList<>();
  Manager dbManager;

  public PendingManager(Manager db) {
    com.dianping.cat.message.Transaction catTransaction = Cat.newTransaction("Exec", "PendingManager");
    Cat.logMetricForCount("PendingManagerTotalCount");
    try {
      this.dbManager = db;
      tmpTransactions.addAll(db.getPendingTransactions());
      db.getPendingTransactions().clear();
      db.getDialog().reset();
      catTransaction.setStatus(com.dianping.cat.message.Transaction.SUCCESS);
    } finally {
      catTransaction.complete();
    }
  }

  @Override
  public void close() {
    this.tmpTransactions.stream()
        .filter(
            trx -> dbManager.getTransactionStore().get(trx.getTransactionId().getBytes()) == null)
        .forEach(trx -> {
          try {
            dbManager.pushTransactions(trx);
          } catch (ValidateSignatureException e) {
            logger.debug(e.getMessage(), e);
          } catch (ContractValidateException e) {
            logger.debug(e.getMessage(), e);
          } catch (ContractExeException e) {
            logger.debug(e.getMessage(), e);
          } catch (HighFreqException e) {
            logger.debug(e.getMessage(), e);
          } catch (DupTransactionException e) {
            logger.debug("pending manager: dup trans", e);
          }
        });
    dbManager.getPoppedTransactions().stream()
        .filter(
            trx -> dbManager.getTransactionStore().get(trx.getTransactionId().getBytes()) == null)
        .forEach(trx -> {
          try {
            dbManager.pushTransactions(trx);
          } catch (ValidateSignatureException e) {
            logger.debug(e.getMessage(), e);
          } catch (ContractValidateException e) {
            logger.debug(e.getMessage(), e);
          } catch (ContractExeException e) {
            logger.debug(e.getMessage(), e);
          } catch (HighFreqException e) {
            logger.debug(e.getMessage(), e);
          } catch (DupTransactionException e) {
            logger.debug("pending manager: dup trans", e);
          }
        });
    dbManager.getPoppedTransactions().clear();
  }
}
