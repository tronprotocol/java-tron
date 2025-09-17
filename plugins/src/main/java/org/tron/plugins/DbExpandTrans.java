package org.tron.plugins;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.FileUtils;
import picocli.CommandLine;

@CommandLine.Command(name = "expandTrans",
    description = "expand db trans size .")
@Slf4j(topic = "expand")
public class DbExpandTrans implements Callable<Integer> {

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;

  @CommandLine.Option(names = {"-d", "--database"},
      defaultValue = "output-directory/database",
      description = "database directory path. Default: ${DEFAULT-VALUE}")
  private Path database;

  @CommandLine.Option(names = {"--target-database"},
      defaultValue = "target/database")
  private Path targetDatabase;

  private String targetDb = "trans";

  @CommandLine.Option(names = {"-h", "--help"}, help = true, description = "display a help message")
  boolean help;

  private  DB db = null;
  private int generatorThreads = 0;
  private long totalRecords = 0;
  private int queueCapacity = 0;
  private int generateBatchSize = 0;
  private int  writeBatchZize =0;
  private final AtomicLong recordsGenerated = new AtomicLong(0);
  private final AtomicLong recordsWritten = new AtomicLong(0);
  private final AtomicLong bytesWritten = new AtomicLong(0);

  // 内存池大小 - 分配6GB直接内存
  private static final int MEMORY_POOL_SIZE = 1 * 1024 * 1024 * 1024;



  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }
    long start = System.currentTimeMillis();
    this.run();
    long cost = System.currentTimeMillis() - start;
    spec.commandLine().getOut().println(String.format("Expand %s done,cost:%s seconds", targetDb,
        cost / 1000));
    return 0;
  }

  private void run() throws Exception {
    Path targetPath = Paths.get(this.targetDatabase.toString(), targetDb);
    FileUtils.createDirIfNotExists(targetPath.toString());
    // first copy db
    copy(database, targetDatabase, targetDb);
    // update
    int generatorThreads = Runtime.getRuntime().availableProcessors(); // 与CPU核心数匹配
    long totalRecords = 11055562803L*3; // 330亿条记录
    //queueCapacity*generateBatchSize* 40byte = 队列总容量，应该为1.5G
    int queueCapacity = 40000;
    int generateBatchSize = 1000;

    // writeBatchSize* generateBatchSize*40byte 为每次写入数据量，按照levelDB 的方式，最好控制在10M
    //
    int writeBatchSize = 250 ;
    DbExpandTrans writer = new DbExpandTrans(targetPath, generatorThreads, totalRecords, queueCapacity,generateBatchSize,writeBatchSize);

    try {
      writer.writeData();
    } finally {
      writer.close();
    }
  }

  public void copy(Path source, Path dest, String db) {
    FileUtils.createDirIfNotExists(Paths.get(dest.toString(), db).toString());
    logger.info("Copy database {} start", db);
    FileUtils.copyDir(source, dest, db);
    logger.info("Copy database {} end", db);
  }

  public DbExpandTrans() {
  }

  public DbExpandTrans(Path dbPath, int generatorThreads, long totalRecords, int queueCapacity,int generateBatchSize,int writeBatchSize) throws IOException {
    // 设置内存池
    JniDBFactory.pushMemoryPool(MEMORY_POOL_SIZE);

    Options option = DBUtils.newDefaultLevelDbOptions(targetDb);
    //调整大小增加写入性能
    option.writeBufferSize(256 * 1024 * 1024);
    option.maxOpenFiles(10000);
    //纯写入，可以设置比较小，甚至为0
    option.cacheSize(4 *  1024 * 1024L);
    this.db = DBUtils.newLevelDb(dbPath, option);
    this.generatorThreads = generatorThreads;
    this.totalRecords = totalRecords;
    this.queueCapacity = queueCapacity;
    this.generateBatchSize = generateBatchSize;
    this.writeBatchZize = writeBatchSize;
  }

  // TODO 阅读理解
  public void writeData() throws InterruptedException {
    // 创建有界阻塞队列
    BlockingQueue<byte[][]> dataQueue = new ArrayBlockingQueue<>(queueCapacity);

    // 启动数据生成器线程池
    ExecutorService generatorExecutor = Executors.newFixedThreadPool(generatorThreads);
    List<Future<?>> generatorFutures = new ArrayList<>();

    // 启动监控线程
    ExecutorService monitorExecutor = Executors.newSingleThreadExecutor();
    Future<?> monitorFuture = monitorExecutor.submit(new MonitoringTask(recordsGenerated, recordsWritten, bytesWritten, totalRecords));

    // 计算每个生成器线程需要生成的数据量
    long recordsPerThread = totalRecords / generatorThreads;

    // 启动数据生成器线程
    for (int i = 0; i < generatorThreads; i++) {
      long start = i * recordsPerThread;
      long end = (i == generatorThreads - 1) ? totalRecords : start + recordsPerThread;
      DataGeneratorTask task = new DataGeneratorTask(dataQueue, start, end,this.generateBatchSize, recordsGenerated);
      generatorFutures.add(generatorExecutor.submit(task));
    }

    // 启动单线程写入器
    SingleThreadWriter writer = new SingleThreadWriter(db,this.writeBatchZize, dataQueue, recordsWritten, bytesWritten, generatorThreads);
    Thread writerThread = new Thread(writer);
    writerThread.start();

    // 等待所有生成器完成
    for (Future<?> future : generatorFutures) {
      try {
        future.get();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    }

    // 通知写入器所有数据已生成
    writer.setAllDataGenerated(true);

    // 等待写入器完成
    writerThread.join();

    // 等待监控线程完成
    monitorExecutor.shutdown();
    try {
      monitorFuture.get();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }

    generatorExecutor.shutdown();
  }

  public void close() throws IOException {
    db.close();
    JniDBFactory.popMemoryPool();
  }

  static class DataGeneratorTask implements Runnable {
    private final BlockingQueue<byte[][]> queue;
    private final long start;
    private final long end;
    private final AtomicLong recordsGenerated;
    private final Random random = new Random();
    private  int batchSize ; // 每批生成1000条记录

    public DataGeneratorTask(BlockingQueue<byte[][]> queue, long start, long end, int batchSize,AtomicLong recordsGenerated) {
      this.queue = queue;
      this.start = start;
      this.end = end;
      this.recordsGenerated = recordsGenerated;
      this.batchSize = batchSize;
    }

    @Override
    public void run() {
      try {
        for (long i = start; i < end; i += batchSize) {
          long currentBatchSize = Math.min(batchSize, end - i);
          byte[][] batch = new byte[(int) currentBatchSize * 2][]; // 每个记录包含key和value

          for (int j = 0; j < currentBatchSize; j++) {
            byte[] key = new byte[32];
            byte[] value = new byte[8];
            random.nextBytes(key);
            random.nextBytes(value);

            batch[j * 2] = key;
            batch[j * 2 + 1] = value;
          }

          // 将批次放入队列
          queue.put(batch);
          recordsGenerated.addAndGet(currentBatchSize);

          Runtime runtime = Runtime.getRuntime();
          long maxMemory = runtime.maxMemory();
          long usedMemory = runtime.totalMemory() - runtime.freeMemory();
          double memoryUsage = (double) usedMemory / maxMemory;

          if (memoryUsage > 0.8) {
            // 记录内存使用情况
            System.out.printf("Memory usage high: %.2f%%, pausing data generation%n",
                memoryUsage * 100);

            // 暂停数据生成
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  static class SingleThreadWriter implements Runnable {
    private final DB db;
    private final BlockingQueue<byte[][]> queue;
    private final AtomicLong recordsWritten;
    private final AtomicLong bytesWritten;
    private final int expectedGenerators;
    private volatile boolean allDataGenerated = false;
    private int completedGenerators = 0;
    private int writeBatchSize = 0;

    public SingleThreadWriter(DB db,int writeBatchSize, BlockingQueue<byte[][]> queue, AtomicLong recordsWritten, AtomicLong bytesWritten, int expectedGenerators) {
      this.db = db;
      this.queue = queue;
      this.recordsWritten = recordsWritten;
      this.bytesWritten = bytesWritten;
      this.expectedGenerators = expectedGenerators;
      this.writeBatchSize = writeBatchSize;
    }

    public void setAllDataGenerated(boolean allDataGenerated) {
      this.allDataGenerated = allDataGenerated;
    }

    @Override
    public void run() {
      WriteBatch batch = db.createWriteBatch();
      int batchCount = 0;
      final int maxBatchCount = 100; // 每100个小批次提交一次 1M条记录

      try {
        // expectedGenerators： 创建数据的线程数
        // allDataGenerated 再全部写线程完成时设置
        // 重复写线程次没有新数据则退出
        while (completedGenerators < expectedGenerators || !queue.isEmpty()) {
          byte[][] data = queue.poll(100, TimeUnit.MILLISECONDS);

          if (data != null) {
            int recordCount = data.length / 2;

            for (int i = 0; i < recordCount; i++) {
              batch.put(data[i * 2], data[i * 2 + 1]);
            }

            batchCount++;
            recordsWritten.addAndGet(recordCount);
            bytesWritten.addAndGet(recordCount * 40L); // 32字节key +8字节value

            // 定期提交批次
            if (batchCount >= this.writeBatchSize) {
              db.write(batch);
              batch.close();
              batch = db.createWriteBatch();
              batchCount = 0;
            }
          } else if (allDataGenerated) {
            // 队列为空且所有数据已生成，增加完成计数器
            completedGenerators++;
          }
        }

        // 提交最后一批
        if (batchCount > 0) {
          db.write(batch);
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        try {
          batch.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  static class MonitoringTask implements Runnable {
    private final AtomicLong recordsGenerated;
    private final AtomicLong recordsWritten;
    private final AtomicLong bytesWritten;
    private final long totalRecords;

    public MonitoringTask(AtomicLong recordsGenerated, AtomicLong recordsWritten, AtomicLong bytesWritten, long totalRecords) {
      this.recordsGenerated = recordsGenerated;
      this.recordsWritten = recordsWritten;
      this.bytesWritten = bytesWritten;
      this.totalRecords = totalRecords;
    }

    @Override
    public void run() {
      long startTime = System.currentTimeMillis();
      long lastRecordsGenerated = 0;
      long lastRecordsWritten = 0;
      long lastBytesWritten = 0;
      long lastTime = startTime;

      try {
        while (recordsWritten.get() < totalRecords) {
          Thread.sleep(10 * 1000); // 每10秒报告一次,测试时10s，运行时改为60s

          long currentRecordsGenerated = recordsGenerated.get();
          long currentRecordsWritten = recordsWritten.get();
          long currentBytesWritten = bytesWritten.get();
          long currentTime = System.currentTimeMillis();

          long recordsGenDelta = currentRecordsGenerated - lastRecordsGenerated;
          long recordsWriteDelta = currentRecordsWritten - lastRecordsWritten;
          long bytesWriteDelta = currentBytesWritten - lastBytesWritten;
          long timeDelta = currentTime - lastTime;

          double recordsGenPerSec = recordsGenDelta / (timeDelta / 1000.0);
          double recordsWritePerSec = recordsWriteDelta / (timeDelta / 1000.0);
          double bytesWritePerSec = bytesWriteDelta / (timeDelta / 1000.0);
          double progress = (currentRecordsWritten * 100.0) / totalRecords;

          System.out.printf("进度: %.2f%%, 生成速度: %,.0f 记录/秒, 写入速度: %,.0f 记录/秒, %,.2f MB/秒%n",
              progress, recordsGenPerSec, recordsWritePerSec, bytesWritePerSec / (1024 * 1024));

          lastRecordsGenerated = currentRecordsGenerated;
          lastRecordsWritten = currentRecordsWritten;
          lastBytesWritten = currentBytesWritten;
          lastTime = currentTime;
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("写入完成! 总时间: %d 秒, 平均速度: %,.0f 记录/秒%n",
            totalTime / 1000, totalRecords / (totalTime / 1000.0));
      } catch (InterruptedException e) {
        // 正常退出
      }
    }
  }


}