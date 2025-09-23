package org.tron.program.broadcast;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
    long failedCount = 0;
    logger.info("Start to process relay transaction broadcast task");
    try (FileInputStream fis = new FileInputStream(output + File.separator + "relay-tx.csv")) {
      Transaction transaction;

      while ((transaction = Transaction.parseDelimitedFrom(fis)) != null) {
        // check for a special case of zero signatures, it means the block end, wait a
        // while
        if (transaction.getSignatureCount() == 0) {
          long time = BLOCK_PRODUCED_INTERVAL - System.currentTimeMillis() % BLOCK_PRODUCED_INTERVAL;
          Thread.sleep(time + 50); // add a little more time to ensure block production given higher priority
          logger.info("weak up from block end, wait: {} ms", time + 50);
          continue; // skip this fake transaction
        }

        TransactionCapsule trx = new TransactionCapsule(transaction);
        // Update transaction timestamp to current time to avoid expiration issues
        trx.setTime(System.currentTimeMillis());
        // Skip VoteWitnessContract and WitnessUpdateContract transactions
        if (needSkip(trx)) {
          skipCount++;
          continue;
        }

        trxCount++;

        try {
          logger.info("dbManager push transaction start id: {}", trx.getTransactionId());
          dbManager.pushTransaction(trx);
          logger.info("dbManager process transaction success");

          TransactionMessage message = new TransactionMessage(transaction);
          int peerCnt = tronNetService.fastBroadcastTransaction(message);
          while (peerCnt <= 0 || dbManager.isTooManyPending()) { // 跟随wallet广播逻辑，如果pending太多也要等待
            logger.warn("broadcast relay task has no available peers to broadcast, please wait");
            Thread.sleep(100);
            peerCnt = tronNetService.fastBroadcastTransaction(message);
          }
          if (trxCount % 1000 == 0) {
            logger.info("total broadcast tx num: {}", trxCount);
          }
        } catch (Exception e) {
          logger.info("dbManager process transaction failed");
          failedCount++;
          e.printStackTrace();
        }

      }

    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    logger.info("relay trx size: {}, skip trx size:{}", trxCount, skipCount);
    logger.info("relay trx failed size: {}", failedCount);
  }
}
