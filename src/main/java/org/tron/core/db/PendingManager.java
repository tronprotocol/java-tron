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

    long jack_close_start = System.nanoTime() / 1000000;
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

    logger.error("close total consume: {} ms",
        System.nanoTime() / 1000000 - jack_close_start);
  }
}
