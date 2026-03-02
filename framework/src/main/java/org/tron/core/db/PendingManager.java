package org.tron.core.db;

import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;

@Slf4j(topic = "DB")
public class PendingManager implements AutoCloseable {

  private Manager dbManager;
  private long timeout = Args.getInstance().getPendingTransactionTimeout();

  public PendingManager(Manager db) {
    this.dbManager = db;
    db.getSession().reset();
    db.getShieldedTransInPendingCounts().set(0);
  }

  @Override
  public void close() {

    long now = System.currentTimeMillis();
    Iterator<TransactionCapsule> iterator = dbManager.getRePushTransactions().iterator();
    while (iterator.hasNext()) {
      TransactionCapsule tx = iterator.next();
      if (now - tx.getTime() > timeout) {
        iterator.remove();
        Metrics.gaugeInc(MetricKeys.Gauge.MANAGER_QUEUE, -1,
            MetricLabels.Gauge.QUEUE_REPUSH);
        Metrics.counterInc(MetricKeys.Counter.TXS, 1,
            MetricLabels.Counter.TXS_FAIL, MetricLabels.Counter.TXS_FAIL_TIMEOUT);
        if (Args.getInstance().isOpenPrintLog()) {
          logger.warn("Timeout remove tx from repush, txId: {}.", tx.getTransactionId());
        }
      }
    }

    for (TransactionCapsule tx : dbManager.getPendingTransactions()) {
      txIteration(tx);
    }

    dbManager.getPendingTransactions().clear();
    Metrics.gaugeSet(MetricKeys.Gauge.MANAGER_QUEUE, 0,
        MetricLabels.Gauge.QUEUE_PENDING);
    for (TransactionCapsule tx : dbManager.getPoppedTransactions()) {
      tx.setTime(System.currentTimeMillis());
      txIteration(tx);
    }
    dbManager.getPoppedTransactions().clear();
    Metrics.gaugeSet(MetricKeys.Gauge.MANAGER_QUEUE, 0,
        MetricLabels.Gauge.QUEUE_POPPED);
    if (Args.getInstance().isOpenPrintLog()) {
      logger.info("Pending tx size: {}.", dbManager.getRePushTransactions().size());
    }

  }

  private void txIteration(TransactionCapsule tx) {
    try {
      if (System.currentTimeMillis() - tx.getTime() < timeout) {
        dbManager.getRePushTransactions().put(tx);
        Metrics.gaugeInc(MetricKeys.Gauge.MANAGER_QUEUE, 1,
            MetricLabels.Gauge.QUEUE_REPUSH);
      } else {
        Metrics.counterInc(MetricKeys.Counter.TXS, 1,
            MetricLabels.Counter.TXS_FAIL, MetricLabels.Counter.TXS_FAIL_TIMEOUT);
        if (Args.getInstance().isOpenPrintLog()) {
          logger.warn("Timeout remove tx from pending, txId: {}.", tx.getTransactionId());
        }
      }
    } catch (InterruptedException e) {
      logger.error(e.getMessage());
      Thread.currentThread().interrupt();
    }
  }
}
