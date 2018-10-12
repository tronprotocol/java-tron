package org.tron.core.net.node;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.message.TransactionsMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j
@Component
public class TrxHandler {

  @Autowired
  NodeImpl nodeImpl;

  private static int MAX_TRX_SIZE = 10_000;

  private BlockingQueue<TrxEvent> smartContractQueue = new LinkedBlockingQueue(MAX_TRX_SIZE);

  private BlockingQueue<Runnable> queue = new LinkedBlockingQueue();

  private ExecutorService trxHandlePool = new ThreadPoolExecutor(6, 6,


      gs0L, TimeUnit.MILLISECONDS, queue);

  private ScheduledExecutorService smartContractExecutor = Executors
      .newSingleThreadScheduledExecutor();

  private void handleSmartContract() {
    smartContractExecutor.scheduleWithFixedDelay(() -> {
      try {
        while (queue.size() < 10 && smartContractQueue.size() > 0) {
          TrxEvent event = smartContractQueue.take();
          if (System.currentTimeMillis() - event.getTime() > 3600){
            logger.warn("Drop smart contract {} from peer {}");
            continue;
          }
          trxHandlePool.submit(() -> nodeImpl.onHandleTransactionMessage(event.getPeer(), event.getMsg()));
        }
      } catch (Exception e) {
        logger.error("Handle smart contract exception", e);
      }
    }, 1000, 30, TimeUnit.MILLISECONDS);
  }

  public void handleTransactionsMessage(PeerConnection peer, TransactionsMessage msg) {
//    if (isBusy()){
//      logger.warn("Drop trx from peer {}, trx size {} smart contract queue size {} queue size {}.",
//          peer.getInetAddress(), msg.getTransactions().getTransactionsCount(), smartContractQueue.size(), queue.size());
//      return;
//    }
    for (Transaction trx : msg.getTransactions().getTransactionsList()) {
      int type = trx.getRawData().getContract(0).getType().getNumber();
      if (type == ContractType.TriggerSmartContract_VALUE || type == ContractType.CreateSmartContract_VALUE) {
        if (!smartContractQueue.offer(new TrxEvent(peer, new TransactionMessage(trx)))){
          logger.warn("Add smart contract from peer {} failed, smartContractQueue size {} queueSize {}",
              peer.getInetAddress(), smartContractQueue.size(), queue.size());
        }
      }else {
        trxHandlePool.submit(() -> nodeImpl.onHandleTransactionMessage(peer, new TransactionMessage(trx)));
      }
    }

  }

  public boolean isBusy() {
    return queue.size() > MAX_TRX_SIZE;
  }

  class TrxEvent {
    @Getter
    private PeerConnection peer;
    @Getter
    private TransactionMessage msg;
    @Getter
    private long time;

    public TrxEvent (PeerConnection peer, TransactionMessage msg){
      this.peer = peer;
      this.msg = msg;
      this.time = System.currentTimeMillis();
    }
  }
}