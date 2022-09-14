package org.tron.program.generate;

import org.bouncycastle.util.encoders.Hex;
import org.tron.protos.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author liukai
 * @since 2022/9/9.
 */
public class TransactionGenerator {

  private int count;
  private CountDownLatch countDownLatch = null;

  public TransactionGenerator(int count) {
    this.count = count;
  }

  private ExecutorService generatePool = Executors.newFixedThreadPool(4, r -> new Thread(r, "create-transaction"));

  public List<String> create() {
    //types
    List<String> transactions = new ArrayList<>(count * 2);
    countDownLatch = new CountDownLatch(this.count);
    for (int i = 0; i < count; i++) {
      generatePool.execute(() -> {
        Protocol.Transaction transaction = new AccountTransactionCreator("TXtrbmfwZ2LxtoCveEhZT86fTss1w8rwJE",
                "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04", countDownLatch).create();
        transactions.add(Hex.toHexString(transaction.toByteArray()));
      });
    }
    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return transactions;
  }

}
