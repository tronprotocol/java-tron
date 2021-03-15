package org.tron.core.store;

import com.typesafe.config.ConfigObject;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.util.Args;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.BlockQueueFactoryUtil;
import org.tron.common.utils.Commons;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ThreadPoolUtil;
import org.tron.core.capsule.AccountAssetIssueCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountAssetIssue;

@Slf4j(topic = "DB")
@Component
public class AccountAssetIssueStore extends TronStoreWithRevoking<AccountAssetIssueCapsule> {

  private static Map<String, byte[]> assertsAddress = new HashMap<>();
  public static final long ACCOUNT_ESTIMATED_COUNT = 25_000_000;
  private AccountConvertQueue accountConvertQueue;
  @Autowired
  private AccountStore accountStore;
  // key = name , value = address
  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;
  private AtomicBoolean readFinish = new AtomicBoolean(false);
  private AtomicLong readCount = new AtomicLong(0);
  private AtomicLong readCost = new AtomicLong(0);
  private AtomicLong writeCount = new AtomicLong(0);
  private AtomicLong writeCost = new AtomicLong(0);
  private List<Future<?>> writeFutures = new ArrayList<>();
  private Timer timer;

  @Autowired
  protected AccountAssetIssueStore(@Value("account-asset-issue") String dbName) {
    super(dbName);
  }

  public static void setAccountAssetIssue(com.typesafe.config.Config config) {
    List<? extends ConfigObject> list = config.getObjectList("genesis.block.assets");
    for (ConfigObject obj : list) {
      String accountName = obj.get("accountName").unwrapped().toString();
      byte[] address = Commons.decodeFromBase58Check(obj.get("address").unwrapped().toString());
      assertsAddress.put(accountName, address);
    }
  }

  @Override
  public AccountAssetIssueCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountAssetIssueCapsule(value);
  }

  /**
   * Min TRX account.
   */
  public AccountAssetIssueCapsule getBlackhole() {
    return getUnchecked(assertsAddress.get("Blackhole"));
  }

  public void rollbackAssetIssueToAccount(String outputDirectory) {
    long start = System.currentTimeMillis();
    logger.info("rollback asset to account store");
    timer = TimerUtil.countDown("The database is being indexed ",
            readCount, writeCount);

    AccountAssetIssueRecordQueue accountRecordQueue =
            new AccountAssetIssueRecordQueue(BlockQueueFactoryUtil.getInstance());
    accountRecordQueue.fetchAccountAssetIssue(this.getRevokingDB());
    accountConvertQueue =
            new AccountConvertQueue(BlockQueueFactoryUtil.getInstance(), this, accountStore);
    accountConvertQueue.convertAccountAssetIssueToAccount();
    logger.info("The database indexing completed, total time spent:{}s," +
                    " r({}s)/w({}s), total account count:{}",
            (System.currentTimeMillis() - start) / 1000,
            readCost,
            writeCost,
            writeCount);
    logger.info("rollback account asset issue to account successful!!");
    dynamicPropertiesStore.setAllowAssetImport(true);
    removeDB(outputDirectory);
    System.exit(0);
  }

  public void convertAccountAssert() {
    dynamicPropertiesStore.setAllowAssetImport(true);
    if (CommonParameter.getInstance().isRollback()) {
      logger.info("import asset of account store to account asset store has been skipped");
      return;
    }

    if (!dynamicPropertiesStore.getAllowAssetImport()) {
      logger.info("import asset of account store to account asset store has been done, skip");
      return;
    }

    logger.info("begin to index the database");
    timer = TimerUtil.countDown("The database is being indexed ",
            readCount, writeCount);
    AccountAssetIssueRecordQueue accountRecordQueue = new AccountAssetIssueRecordQueue(
            BlockQueueFactoryUtil.getInstance());
    accountRecordQueue.fetchAccount(accountStore.getRevokingDB());
    accountConvertQueue = new AccountConvertQueue(
            BlockQueueFactoryUtil.getInstance(),
            this,
            accountStore);
    accountConvertQueue.convert();
  }

  public void waitUtilConvertAccountFinish() {
    if (CommonParameter.getInstance().isRollback() || !dynamicPropertiesStore.getAllowAssetImport()) {
      return;
    }
    try {
      long start = System.currentTimeMillis();
      accountConvertQueue.waitUtilConvertFinish();
      dynamicPropertiesStore.setAllowAssetImport(false);
      logger.info("The database indexing completed, total time spent:{}s," +
                      " r({}s)/w({}s, total account count:{})",
              (System.currentTimeMillis() - start) / 1000,
              readCost.get(),
              writeCost.get(),
              writeCount);
    } finally {
      if (timer != null) {
        TimerUtil.cancel(timer);
      }
    }
  }

  public class AccountAssetIssueRecordQueue {

    private BlockingQueue<Map.Entry<byte[], byte[]>> productQueue;

    public AccountAssetIssueRecordQueue(BlockingQueue<Map.Entry<byte[], byte[]>> productQueue) {
      this.productQueue = productQueue;
    }

    public void put(Map.Entry<byte[], byte[]> accountByte) {
      try {
        productQueue.put(accountByte);
      } catch (InterruptedException e) {
        logger.error("put account asset issue exception: {}", e.getMessage(), e);
        Thread.currentThread().interrupt();
      }
    }

    public void fetchAccount(IRevokingDB revokingDB) {
      fetch(revokingDB);
    }

    public void fetchAccountAssetIssue(IRevokingDB revokingDB) {
      fetch(revokingDB);
    }

    private void fetch(IRevokingDB revokingDB) {
      new Thread(() -> {
        long start = System.currentTimeMillis();
        for (Map.Entry<byte[], byte[]> accountRecord : revokingDB) {
          put(accountRecord);
          readCount.incrementAndGet();
        }
        readCost.set(System.currentTimeMillis() - start);
        readFinish.set(true);
      }).start();
    }
  }

  public class AccountConvertQueue {

    private BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue;

    private AccountAssetIssueStore accountAssetIssueStore;

    private AccountStore accountStore;

    public AccountConvertQueue(BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue,
                               AccountAssetIssueStore accountAssetIssueStore,
                               AccountStore accountStore) {
      this.convertQueue = convertQueue;
      this.accountAssetIssueStore = accountAssetIssueStore;
      this.accountStore = accountStore;
    }

    public void convert() {
      ExecutorService writeExecutor = Executors.newFixedThreadPool(ThreadPoolUtil.getMaxPoolSize());
      writeCost.set(System.currentTimeMillis());
      for (int i = 0; i < ThreadPoolUtil.getMaxPoolSize(); i++) {
        Future<?> future = writeExecutor.submit(() -> {
          try {
            while (true) {
              Map.Entry<byte[], byte[]> accountEntry = convertQueue.poll();
              if (readFinish.get() && accountEntry == null) {
                break;
              }

              if (accountEntry == null) {
                TimeUnit.MILLISECONDS.sleep(5);
                continue;
              }

              if (accountAssetIssueStore.has(accountEntry.getKey())) {
                continue;
              }

              AccountCapsule accountCapsule = new AccountCapsule(accountEntry.getValue());
              byte[] address = accountCapsule.getAddress().toByteArray();
              AccountAssetIssue accountAssetIssue = AccountAssetIssue.newBuilder()
                  .setAddress(accountCapsule.getAddress())
                  .setAssetIssuedID(accountCapsule.getAssetIssuedID())
                  .setAssetIssuedName(accountCapsule.getAssetIssuedName())
                  .putAllAsset(accountCapsule.getAssetMap())
                  .putAllAssetV2(accountCapsule.getAssetMapV2())
                  .putAllFreeAssetNetUsage(accountCapsule.getAllFreeAssetNetUsage())
                  .putAllFreeAssetNetUsageV2(accountCapsule.getAllFreeAssetNetUsageV2())
                  .putAllLatestAssetOperationTime(accountCapsule.getLatestAssetOperationTimeMap())
                  .putAllLatestAssetOperationTimeV2(
                      accountCapsule.getLatestAssetOperationTimeMapV2())
                  .build();

              accountAssetIssueStore.put(address,
                  new AccountAssetIssueCapsule(accountAssetIssue));
              Account account = accountCapsule.getInstance();
              account = account.toBuilder()
                  .clearAssetIssuedID()
                  .clearAssetIssuedName()
                  .clearAsset()
                  .clearAssetV2()
                  .clearFreeAssetNetUsage()
                  .clearFreeAssetNetUsageV2()
                  .clearLatestAssetOperationTime()
                  .clearLatestAssetOperationTimeV2()
                  .build();


              accountCapsule.setInstance(account);

              //set VotePower
              accountCapsule.setVotePower413(accountCapsule.getTronPower());

              accountStore.put(address, accountCapsule);
              writeCount.incrementAndGet();

            }
          } catch (InterruptedException e) {
            logger.error("account convert asset exception: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
          }
        });
        writeFutures.add(future);
      }
    }

    public void waitUtilConvertFinish() {
      for (Future<?> future : writeFutures) {
        try {
          future.get();
        } catch (InterruptedException | ExecutionException e) {
          logger.error(e.getMessage(), e);
          Thread.currentThread().interrupt();
        }
      }
      writeCost.set(System.currentTimeMillis() - writeCost.get());
    }

    public void convertAccountAssetIssueToAccount() {
      writeCost.set(System.currentTimeMillis());
      ExecutorService writeExecutor = Executors.newFixedThreadPool(ThreadPoolUtil.getMaxPoolSize());
      for (int i = 0; i < ThreadPoolUtil.getMaxPoolSize(); i++) {
        Future<?> future = writeExecutor.submit(() -> {
          try {
            while (true) {
              Map.Entry<byte[], byte[]> accountAssetIssue = convertQueue.poll();
              if (readFinish.get() && accountAssetIssue == null) {
                break;
              }

              if (accountAssetIssue == null) {
                TimeUnit.MILLISECONDS.sleep(5);
                continue;
              }

              AccountAssetIssueCapsule accountAssetIssueCapsule =
                  new AccountAssetIssueCapsule(accountAssetIssue.getValue());
              byte[] address = accountAssetIssueCapsule.getAddress().toByteArray();
              AccountCapsule accountCapsule = accountStore.get(address);
              Account account = accountCapsule.getInstance()
                  .toBuilder()
                  .setAddress(accountAssetIssueCapsule.getAddress())
                  .setAssetIssuedID(accountAssetIssueCapsule.getAssetIssuedID())
                  .setAssetIssuedName(accountAssetIssueCapsule.getAssetIssuedName())
                  .putAllAsset(accountAssetIssueCapsule.getAssetMap())
                  .putAllAssetV2(accountAssetIssueCapsule.getAssetMapV2())
                  .putAllFreeAssetNetUsage(accountAssetIssueCapsule.getAllFreeAssetNetUsage())
                  .putAllFreeAssetNetUsageV2(accountAssetIssueCapsule.getAllFreeAssetNetUsageV2())
                  .putAllLatestAssetOperationTime(
                      accountAssetIssueCapsule.getLatestAssetOperationTimeMap())
                  .putAllLatestAssetOperationTimeV2(
                      accountAssetIssueCapsule.getLatestAssetOperationTimeMapV2())
                  .build();
              accountCapsule.setInstance(account);
              accountStore.put(address, accountCapsule);
              writeCount.incrementAndGet();
            }
          } catch (InterruptedException e) {
            logger.error("convert account asset assue to account exception: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
          }
        });
        writeFutures.add(future);
      }

      waitUtilConvertFinish();
    }
  }

  @Slf4j(topic = "DB")
  public static class TimerUtil {

    public static Timer countDown(String message, AtomicLong readCount, AtomicLong writeCount) {
      Timer timer = new Timer();
      AtomicInteger count = new AtomicInteger();
      timer.schedule(new TimerTask() {
        public void run() {
          int second = count.incrementAndGet();
          if (second % 5 == 0) {
            long completed = writeCount.get() * 100 / ACCOUNT_ESTIMATED_COUNT;
            if (completed > 99) {
              completed = 99;
            }

            logger.info(message + ": {}s, r({})/w({}), Completed {}%",
                second, readCount, writeCount, completed);
          }
        }
      }, 0, 1000);
      return timer;
    }

    public static void cancel (Timer timer) {
      if (null != timer) {
        timer.cancel();
      }
    }
  }

  private void removeDB(String outputDirectory) {
    String accountAssetIssueDB = outputDirectory + File.separator + "database" + File.separator + "account-asset-issue";
    if (FileUtil.deleteDir(new File(accountAssetIssueDB))) {
      logger.info("remove account-asset-issue DB, success");
    } else {
      logger.info("remove account-asset-issue DB, fail");
    }
  }
}
