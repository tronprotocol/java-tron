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
import java.util.stream.LongStream;
import org.tron.common.application.TronApplicationContext;
import org.tron.protos.Protocol.Transaction;
import org.tron.stresstest.dispatch.TransactionFactory;

public class TransactionGenerator {

  private TronApplicationContext context;
  private int count;
  private String outputFile;

  public TransactionGenerator(TronApplicationContext context, String outputFile, int count) {
    this.context = context;
    this.outputFile = outputFile;
    this.count = count;
  }

  public TransactionGenerator(TronApplicationContext context, int count) {
    this(context, "transaction.csv", count);
  }

  public void start() {
    ConcurrentLinkedQueue<Transaction> transactions = new ConcurrentLinkedQueue<>();
    ExecutorService service = Executors.newFixedThreadPool(32);
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(new File(this.outputFile));
      TransactionFactory.init(context);

      CountDownLatch countDownLatch = new CountDownLatch(this.count);

      long t1 = System.currentTimeMillis();
      LongStream.range(0L, this.count).forEach(l -> {
        service.execute(() -> {
          Optional.ofNullable(TransactionFactory.newTransaction()).ifPresent(transactions::add);
          countDownLatch.countDown();
          System.out.print("\r");
          System.out.print("Remain generate transaction: " + countDownLatch.getCount());
        });
      });

      countDownLatch.await();
      System.out.println();
      System.out.println();
      System.out.println("Time consuming: " + (System.currentTimeMillis() - t1) + " ms");
      System.out.println();
      System.out.println();
      int size = 1;
      t1 = System.currentTimeMillis();
      for (Transaction transaction : transactions) {
        transaction.writeDelimitedTo(fos);
        System.out.print("\r");
        System.out.printf("Write transaction count %d", size++);
      }

      fos.flush();
      fos.close();
      System.out.println();
      System.out.println();
      System.out.println("Time consuming: " + (System.currentTimeMillis() - t1) + "ms");
      System.out.println();
      System.out.println();
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
    } finally {
      service.shutdown();

      while (true) {
        if (service.isTerminated()) {
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