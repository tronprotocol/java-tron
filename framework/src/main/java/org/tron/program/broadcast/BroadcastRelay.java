package org.tron.program.broadcast;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.protos.Protocol.Transaction;

@Slf4j(topic = "broadcastRelay")
public class BroadcastRelay {

  private volatile boolean isFinishSend = false;
  private final ConcurrentLinkedQueue<Transaction> transactionIDs = new ConcurrentLinkedQueue<>();

  private final TronNetService tronNetService;
  private final Manager dbManager;

  private final String output = "stress-test-output";

  public int TPS = 100;

  private final ExecutorService saveTransactionIDPool = Executors
      .newFixedThreadPool(1, r -> new Thread(r, "save-relay-tx-id"));

  private final Random random = new Random(System.currentTimeMillis());

  public BroadcastRelay(TronNetService tronNetService, Manager dbManager) {
    this.tronNetService = tronNetService;
    this.dbManager = dbManager;
  }


  public void broadcastTransactions() {
    long trxCount = 0;

    long startTime = System.currentTimeMillis();
    logger.info("Start to process relay transaction broadcast task");
    try (FileInputStream fis = new FileInputStream(output + File.separator + "relay-tx.csv")) {
      Transaction transaction;
      int cnt = 0;
      long startTps = System.currentTimeMillis();
      long endTps;
      while ((transaction = Transaction.parseDelimitedFrom(fis)) != null) {
        trxCount++;
        if (cnt > TPS) {
          endTps = System.currentTimeMillis();
          if (endTps - startTps < 1000) {
            Thread.sleep(1000 - (endTps - startTps));
          }
          cnt = 0;
          startTps = System.currentTimeMillis();
        } else {
          try {
            TransactionMessage message = new TransactionMessage(transaction);
            TransactionCapsule trx = new TransactionCapsule(transaction);
            logger.info("dbManager push transaction start");
            dbManager.pushTransaction(trx);
            logger.info("dbManager process transaction success");
//            int peerCnt = tronNetService.fastBroadcastTransaction(message);
//            while (peerCnt <= 0) {
//              logger.warn("broadcast relay task has no available peers to broadcast, please wait");
//              Thread.sleep(100);
//              peerCnt = tronNetService.fastBroadcastTransaction(message);
//            }
//            if (trxCount % 1000 == 0) {
//              logger.info("total broadcast tx num: {}", trxCount);
//            }
          } catch (Exception e) {
            logger.info("dbManager process transaction failed");
            e.printStackTrace();
          }

          cnt++;
        }
      }

      isFinishSend = true;

    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    long cost = System.currentTimeMillis() - startTime;
    logger.info("relay trx size: {}, cost: {}, tps: {}", trxCount, cost,
        1.0 * trxCount / cost * 1000);
  }
}
