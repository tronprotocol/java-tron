package org.tron.core.db;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionTrace.TimeResultType;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;
import org.tron.core.metrics.net.PendingInfo;

@Slf4j(topic = "DB")
public class PendingManager implements AutoCloseable {

  private PendingInfo pendingInfo;

  @Getter
  private List<TransactionCapsule> tmpTransactions = new ArrayList<>();
  private Manager dbManager;
  private long timeout = Args.getInstance().getPendingTransactionTimeout();
  private BlockCapsule blockCapsule;

  public PendingManager(Manager db, BlockCapsule blockCapsule, PendingInfo pendingInfo) {
    this.dbManager = db;
    this.blockCapsule = blockCapsule;
    this.pendingInfo = pendingInfo;
    db.getPendingTransactions().forEach(transactionCapsule -> {
      if (System.currentTimeMillis() - transactionCapsule.getTime() < timeout) {
        tmpTransactions.add(transactionCapsule);
      } else {
        String txId = transactionCapsule.getTransactionId().toString();
        logger.warn("[server busy] discard transaction from pending: {}", txId);
      }
    });

    if (db.getPendingTransactions().size() > tmpTransactions.size()) {
      MetricsUtil.meterMark(MetricsKey.BLOCKCHAIN_MISSED_TRANSACTION,
          db.getPendingTransactions().size() - tmpTransactions.size());
      logger.info("[server busy] discard total: {}",
              db.getPendingTransactions().size() - tmpTransactions.size());
    }

    db.getPendingTransactions().clear();
    db.getSession().reset();
    db.getShieldedTransInPendingCounts().set(0);
  }

  @Override
  public void close() {

    for (TransactionCapsule tx : tmpTransactions) {
      txIteration(tx);
    }
    tmpTransactions.clear();

    for (TransactionCapsule tx : dbManager.getPoppedTransactions()) {
      txIteration(tx);
    }
    dbManager.getPoppedTransactions().clear();

    // debug
    int sum = 0;
    int txFromUser = 0;
    int txFromUserOnChain = 0;
    int txFromNode = 0;
    int txFromNodeOnChain = 0;

    Map<String, String> blockTxs = Maps.newHashMap();
    blockCapsule.getTransactions().forEach(tx -> {
      blockTxs.put(tx.getTransactionId().toString(), "");
    });

    for (TransactionCapsule transactionCapsule: dbManager.getRePushTransactions()) {
      if ("user".equals(transactionCapsule.getSource())) {
        txFromUser++;
        if (blockTxs.containsKey(transactionCapsule.getTransactionId().toString())) {
          txFromUserOnChain++;
        }
      } else if ("node".equals(transactionCapsule.getSource())) {
        txFromNode++;
        if (blockTxs.containsKey(transactionCapsule.getTransactionId().toString())) {
          txFromNodeOnChain++;
        }
      }
      sum++;
      if (Args.getInstance().getPrintPendingTxId()) {
        logger.info("[server busy] tx in repush: {}", transactionCapsule.getTransactionId().toString());
      }
    }
    pendingInfo.getRepushOnChain().addAndGet(txFromUserOnChain);
    pendingInfo.getRepushOnChain().addAndGet(txFromNodeOnChain);
    logger.info("[server busy] repush queue size: {}, txFromUser: {}, txFromNode: {}, " +
                    "txFromUserOnChain:{}, txFromNodeOnChain:{}, blocksize: {}",
            sum, txFromUser, txFromNode, txFromUserOnChain, txFromNodeOnChain, blockTxs.size());

  }

  private void txIteration(TransactionCapsule tx) {
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
}
