package org.tron.core.db;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionTrace.TimeResultType;

@Slf4j(topic = "DB")
public class PendingManager implements AutoCloseable {

  @Getter
  private List<TransactionCapsule> tmpTransactions = new ArrayList<>();
  private Manager dbManager;

  public PendingManager(Manager db) {

    this.dbManager = db;
    tmpTransactions.addAll(db.getPendingTransactions());
    db.getPendingTransactions().clear();
    db.getSession().reset();
    db.getShieldedTransInPendingCounts().set(0);
  }

  @Override
  public void close() {

    for (TransactionCapsule tx : tmpTransactions) {
      try {
        if (tx.getTrxTrace() != null &&
            tx.getTrxTrace().getTimeResultType().equals(TimeResultType.NORMAL)) {
          dbManager.getRepushTransactions().put(tx);
        }
      } catch (InterruptedException e) {
        logger.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }

    if (tmpTransactions.size() > 0 && dbManager.getRepushTransactions().size() == 0) {
      for (TransactionCapsule tx : tmpTransactions) {
        logger.info("txid: {}, TimeResultType: {}", Hex.toHexString(tx.getTransactionId().getBytes()), tx.getTrxTrace().getTimeResultType());
      }
    }

    tmpTransactions.clear();

    for (TransactionCapsule tx : dbManager.getPoppedTransactions()) {
      try {
        if (tx.getTrxTrace() != null &&
            tx.getTrxTrace().getTimeResultType().equals(TimeResultType.NORMAL)) {
          dbManager.getRepushTransactions().put(tx);
        }
      } catch (InterruptedException e) {
        logger.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    dbManager.getPoppedTransactions().clear();
  }
}
