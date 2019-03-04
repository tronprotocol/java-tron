package org.tron.stresstest.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.application.TronApplicationContext;
import org.tron.protos.Protocol.Transaction;
import org.tron.stresstest.dispatch.TransactionFactory;

@Slf4j
public class TransactionGenerator {

  private TronApplicationContext context;
  private int count;
  private String outputFile;

  private volatile boolean isGenerate = true;
  private ConcurrentLinkedQueue<Transaction> transactions = new ConcurrentLinkedQueue<>();
  FileOutputStream fos = null;
  CountDownLatch countDownLatch = null;
  private ExecutorService savePool = Executors.newFixedThreadPool(1, new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, "save-transaction");
    }
  });

  private ExecutorService generatePool = Executors.newFixedThreadPool(2, new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, "generate-transaction");
    }
  });

  public TransactionGenerator(TronApplicationContext context, String outputFile, int count) {
    this.context = context;
    this.outputFile = outputFile;
    this.count = count;
  }

  public TransactionGenerator(TronApplicationContext context, int count) {
    this(context, "transaction.csv", count);
  }

  private void consumerGenerateTransaction() throws IOException {
    if (transactions.isEmpty()) {
      try {
        Thread.sleep(100);
        return;
      } catch (InterruptedException e) {
        System.out.println(e);
      }
    }

    Transaction transaction = transactions.poll();
    transaction.writeDelimitedTo(fos);

    long count = countDownLatch.getCount();
    if (count % 10000 == 0) {
      fos.flush();
      logger.info("Generate transaction success ------- ------- ------- ------- ------- Remain: " + countDownLatch.getCount() + ", Pending size: " + transactions.size());
    }

    countDownLatch.countDown();
  }

  public void start() throws FileNotFoundException {
    System.out.println("Start generate transaction");

    savePool.submit(() -> {
      while (isGenerate) {
        try {
          consumerGenerateTransaction();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });

    try {
      fos = new FileOutputStream(new File(this.outputFile));
      TransactionFactory.init(context);

      countDownLatch = new CountDownLatch(this.count);

      LongStream.range(0L, this.count).forEach(l -> {
        generatePool.execute(() -> {
          Optional.ofNullable(TransactionFactory.newTransaction()).ifPresent(transactions::add);
        });
      });

      countDownLatch.await();

      isGenerate = false;

      fos.flush();
      fos.close();
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
    } finally {
      generatePool.shutdown();

      while (true) {
        if (generatePool.isTerminated()) {
          break;
        }

        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      savePool.shutdown();

      while (true) {
        if (savePool.isTerminated()) {
          break;
        }

        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }
}