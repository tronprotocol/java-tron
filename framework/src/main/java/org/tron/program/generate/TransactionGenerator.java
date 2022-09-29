package org.tron.program.generate;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.program.design.factory.GeneratorFactory;
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
@Slf4j
public class TransactionGenerator {

  private ExecutorService generatePool = Executors.newFixedThreadPool(4, r -> new Thread(r, "TransactionGenerator"));

  /**
   *
   * @param count
   * @param type
   * @return
   */
  public List<String> createTransactions(int count, String type) {
    TransactionCreator generator = GeneratorFactory.getGenerator(type);

    if (null == generator) {
      throw new IllegalArgumentException("generator not exists.");
    }
    logger.info("generator type: {}", generator.getClass().getTypeName());
    CountDownLatch countDownLatch = new CountDownLatch(count);
    List<String> transactions = new ArrayList<>(count * 2);
    for (int i = 0; i < count; i++) {
      generatePool.execute(() -> {
        // test account
        Protocol.Transaction transaction = generator.create();
        transactions.add(Hex.toHexString(transaction.toByteArray()));
        countDownLatch.countDown();
      });
    }
    try {
      countDownLatch.await();
      generatePool.shutdown();
      logger.info("generate completed, transaction count: {}", transactions.size());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return transactions;
  }

}
