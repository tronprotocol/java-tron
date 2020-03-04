package org.tron.core.db;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionTrace.TimeResultType;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsService;

@Slf4j(topic = "DB")
public class PendingManager implements AutoCloseable {

  @Autowired
  private MetricsService metricsService;

  @Getter
  private List<TransactionCapsule> tmpTransactions = new ArrayList<>();
  private Manager dbManager;
  private long timeout = 60_000;

  public PendingManager(Manager db) {
    this.dbManager = db;
    db.getPendingTransactions().forEach(transactionCapsule -> {
      if (System.currentTimeMillis() - transactionCapsule.getTime() < timeout) {
        tmpTransactions.add(transactionCapsule);
      }
    });
    metricsService.meterMark(MetricsKey.BLOCKCHAIN_MISS_TRANSACTION, tmpTransactions.size());
    db.getPendingTransactions().clear();
    db.getSession().reset();
    db.getShieldedTransInPendingCounts().set(0);
  }

  @Override
  public void close() {

    for (TransactionCapsule tx : tmpTransactions) {
      try {
        if (tx.getTrxTrace() != null
            && tx.getTrxTrace().getTimeResultType().equals(TimeResultType.NORMAL)) {
          dbManager.getRePushTransactions().put(tx);
        }
      } catch (InterruptedException e) {
        logger.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    tmpTransactions.clear();

    for (TransactionCapsule tx : dbManager.getPoppedTransactions()) {
      try {
        if (tx.getTrxTrace() != null
            && tx.getTrxTrace().getTimeResultType().equals(TimeResultType.NORMAL)) {
          dbManager.getRePushTransactions().put(tx);
        }
      } catch (InterruptedException e) {
        logger.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    dbManager.getPoppedTransactions().clear();
  }
}
