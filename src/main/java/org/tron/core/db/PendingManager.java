package org.tron.core.db;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionCapsule;

@Slf4j
public class PendingManager implements AutoCloseable {

  @Getter
  static List<TransactionCapsule> tmpTransactions = new ArrayList<>();
  Manager dbManager;

  public PendingManager(Manager db) {
    this.dbManager = db;
    tmpTransactions.addAll(db.getPendingTransactions());
    db.getPendingTransactions().clear();
    db.getSession().reset();
  }

  @Override
  public void close() {

    for (TransactionCapsule tx : this.tmpTransactions) {
      try {
        dbManager.getRepushTransactions().put(tx);
      } catch (InterruptedException e) {
        continue;
      }
    }

    for (TransactionCapsule tx : dbManager.getPoppedTransactions()) {
      try {
        dbManager.getRepushTransactions().put(tx);
      } catch (InterruptedException e) {
        continue;
      }
    }

    dbManager.getPoppedTransactions().clear();
    tmpTransactions.clear();

  }
}
