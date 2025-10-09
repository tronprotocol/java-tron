package org.tron.program.broadcast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.protos.Protocol.Transaction;

@Slf4j(topic = "broadcastRelay")
public class BroadcastRelay {

  private final TronNetService tronNetService;
  private final Manager dbManager;

  private final String output = "stress-test-output";

  public BroadcastRelay(TronNetService tronNetService, Manager dbManager) {
    this.tronNetService = tronNetService;
    this.dbManager = dbManager;
  }

  // Transaction types that should be skipped during broadcast
  private static final Transaction.Contract.ContractType[] SKIP_TRANSACTION_TYPES = {
      Transaction.Contract.ContractType.VoteWitnessContract,
      Transaction.Contract.ContractType.WitnessUpdateContract
  };

  public boolean needSkip(TransactionCapsule trx) {
    Transaction.Contract.ContractType trxType = trx.getInstance().getRawData().getContract(0).getType();

    // Check if the transaction type is in the skip list
    for (Transaction.Contract.ContractType skipType : SKIP_TRANSACTION_TYPES) {
      if (trxType == skipType) {
        logger.info("Skipping transaction type: {} id: {}", trxType, trx.getTransactionId());
        return true;
      }
    }
    return false;
  }

  public void broadcastTransactions() {
    long trxCount = 0;
    long skipCount = 0;
    AtomicLong failedCount = new AtomicLong();
    long startTime = System.currentTimeMillis();
    long batchStartTime = startTime;
    long batchCount = 0;
    final int QPS_LIMIT = 100;
    final long BATCH_INTERVAL_MS = 1000; // 1 second
    
    // Create a thread pool to manage async tasks
    // Thread pool size set to twice the QPS limit to handle concurrent tasks
    ExecutorService executorService = Executors.newFixedThreadPool(QPS_LIMIT * 2);
    
    logger.info("Start to process relay transaction broadcast task");
    try (FileInputStream fis = new FileInputStream(output + File.separator + "relay-tx.csv")) {
      Transaction transaction;

      while ((transaction = Transaction.parseDelimitedFrom(fis)) != null) {

        TransactionCapsule trx = new TransactionCapsule(transaction);
        if (trx.getContractCount() == 0) {
          continue;
        }
        // Skip VoteWitnessContract and WitnessUpdateContract transactions
        if (needSkip(trx)) {
          skipCount++;
          continue;
        }

        trxCount++;

        try {
          // 跟随wallet广播逻辑，如果pending太多也要等待
          while (dbManager.isTooManyPending()) {
            logger.warn("too many pending transactions, please wait");
            Thread.sleep(200);
          }
            
          // Save transaction reference for lambda expression
          final Transaction finalTransaction = transaction;
          final TransactionCapsule finalTrx = trx;
          // Update transaction timestamp to current time to avoid PendingManager remove
          finalTrx.setTime(System.currentTimeMillis());
          // Submit task to thread pool
          executorService.submit(() -> {
            try {
              logger.info("dbManager push transaction start id: {}", finalTrx.getTransactionId());
             
              dbManager.pushTransaction(finalTrx);
              logger.info("dbManager process transaction success id: {}", finalTrx.getTransactionId());
            } catch (Exception e) {
              failedCount.getAndIncrement();
              logger.error("dbManager process transaction failed id: {}", finalTrx.getTransactionId(), e);
            }
          });

          TransactionMessage message = new TransactionMessage(finalTransaction);
          int peerCnt = tronNetService.fastBroadcastTransaction(message);
          while (peerCnt <= 0 ) { 
              logger.warn("broadcast relay task has no available peers to broadcast, please wait");
              Thread.sleep(100);
              peerCnt = tronNetService.fastBroadcastTransaction(message);
          }

          // QPS control logic
          batchCount++;
          if (batchCount >= QPS_LIMIT) {
            long elapsedTime = System.currentTimeMillis() - batchStartTime;
            if (elapsedTime < BATCH_INTERVAL_MS) {
              long waitTime = BATCH_INTERVAL_MS - elapsedTime;
              logger.info("Reached QPS limit of {}. Waiting for {}ms", QPS_LIMIT, waitTime);
              Thread.sleep(waitTime);
            }
            batchCount = 0;
            batchStartTime = System.currentTimeMillis();
          }
          
          if (trxCount % 1000 == 0) {
            logger.info("total broadcast tx num: {}", trxCount);
          }
        } catch (Exception e) {
          logger.info("dbManager process transaction failed");
          e.printStackTrace();
        }

      }

    } catch (IOException e) {
      e.printStackTrace();
    } 
    
    // Wait for all async tasks to complete before exiting
    try {
      logger.info("Waiting for all async tasks to complete...");
      executorService.shutdown();
      // Wait for up to 5 minutes for all tasks to complete
      if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
        logger.warn("Some tasks may not have completed within the timeout period");
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      logger.error("Thread interrupted while waiting for tasks to complete", e);
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }

    logger.info("relay trx size: {}, skip trx size:{}", trxCount, skipCount);
    logger.info("relay trx failed size: {}", failedCount);
  }
}