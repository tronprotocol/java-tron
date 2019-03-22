package org.tron.stresstest.generator;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.WalletGrpc;
import org.tron.common.application.TronApplicationContext;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.stresstest.dispatch.TransactionFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ReplayTransactionGenerator {

  private TronApplicationContext context;
  private int startNum;
  private int endNum;
  private String outputFile;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = "60.205.215.34:50051";
  public static List<Transaction> transactionsOfReplay = new ArrayList<>();
  public static AtomicInteger indexOfReplayTransaction = new AtomicInteger();
  private int count = 0;

  private volatile boolean isGenerate = true;
  private volatile boolean isReplayGenerate = true;
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

  public ReplayTransactionGenerator(TronApplicationContext context, String outputFile, int startNum, int endNUm) {
    this.context = context;
    this.outputFile = outputFile;
    this.startNum = startNum;
    this.endNum = endNUm;
  }

  public ReplayTransactionGenerator(TronApplicationContext context, int startNum, int endNUm) {
    this(context, "transaction.csv", startNum,endNUm);
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

    logger.info("transactions size is " + transactions.size());
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
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    System.out.println("Start replay generate transaction");

    GrpcAPI.BlockLimit.Builder builder = GrpcAPI.BlockLimit.newBuilder();
    builder.setStartNum(2);
    builder.setEndNum(4);
    GrpcAPI.BlockList blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    Optional<BlockList> result = Optional.ofNullable(blockList);

    int step = 50;
    System.out.println(String.format("提取从%s块～～%s块的交易!", startNum, endNum));
    for (int i = startNum; i < endNum; i = i + step) {
      builder.setStartNum(i);
      builder.setEndNum(i + step);
      blockList = blockingStubFull.getBlockByLimitNext(builder.build());
      result = Optional.ofNullable(blockList);
      //if (true) {
      if (result.isPresent()) {
        blockList = result.get();
        if (blockList.getBlockCount() > 0) {
          for (Block block : blockList.getBlockList()) {
            if (block.getTransactionsCount() > 0) {
              transactionsOfReplay.addAll(block.getTransactionsList());

            }
          }
        }
      }
      System.out.println(String.format("已提取%s块～～%s块的交易!", i, i + step));
    }

    System.out.println("总交易数量：" + transactionsOfReplay.size());
    this.count = transactionsOfReplay.size();


    savePool.submit(() -> {
      while (isReplayGenerate) {
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
      //countDownLatch = new CountDownLatch(indexOfReplayTransaction.get());

/*      LongStream.range(0L, this.count).forEach(l -> {
        generatePool.execute(() -> {
          Optional.ofNullable(TransactionFactory.newTransaction()).ifPresent(transactions::add);
        });
      });*/

/*      LongStream.range(0L, transactionsOfReplay.size() - 1).forEach(l -> {
        generatePool.execute(() -> {
          Optional.ofNullable(transactionsOfReplay.get(indexOfReplayTransaction.get())).ifPresent(transactions::add);
          logger.info("index is " + indexOfReplayTransaction.incrementAndGet());
        });
      });*/
      while (indexOfReplayTransaction.get() < transactionsOfReplay.size()) {
        transactions.add(transactionsOfReplay.get(indexOfReplayTransaction.get()));
        indexOfReplayTransaction.incrementAndGet();
      }




/*      for( indexOfReplayTransaction = 0 ; indexOfReplayTransaction < transactionsOfReplay.size() ; indexOfReplayTransaction.incrementAndGet()) {
        transactions.add(transactionsOfReplay.get(i));
        logger.info("transactions size are " + transactions.size());
      }*/

      countDownLatch.await();

      isReplayGenerate = false;

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