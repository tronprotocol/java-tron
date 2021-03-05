package org.tron.core.store;

import com.typesafe.config.ConfigObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.AccountAssetIssueCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.protos.Protocol.AccountAssetIssue;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j(topic = "DB")
@Component
public class AccountAssetIssueStore extends TronStoreWithRevoking<AccountAssetIssueCapsule> {

    @Autowired
    private AccountStore accountStore;

    @Autowired
    private DynamicPropertiesStore dynamicPropertiesStore;

    private static Map<String, byte[]> assertsAddress = new HashMap<>(); // key = name , value = address

    @Autowired
    protected AccountAssetIssueStore(@Value("account-asset-issue") String dbName) {
        super(dbName);
    }

    public static void setAccountAssetIssue(com.typesafe.config.Config config) {
        List list = config.getObjectList("genesis.block.assets");
        for (int i = 0; i < list.size(); i++) {
            ConfigObject obj = (ConfigObject) list.get(i);
            String accountName = obj.get("accountName").unwrapped().toString();
            byte[] address = Commons.decodeFromBase58Check(obj.get("address").unwrapped().toString());
            assertsAddress.put(accountName, address);
        }
    }

    public Timer countDown() {
        Timer timer = new Timer();
        AtomicInteger count = new AtomicInteger();
        timer.schedule(new TimerTask() {
            public void run() {
                int second = count.incrementAndGet();
                if (second % 5 == 0) {
                    logger.info("import asset current second: {} S", second);
                }
                ;
            }
        }, 0, 1000);
        return timer;
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

    public void convertAccountAssert() {
        long start = System.currentTimeMillis();
        logger.info("import asset of account store to account asset store ");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Timer timer = null;
        if (dynamicPropertiesStore.getAllowAssetImport() == 0L) {
            try {
                AccountAssetIssueRecordQueue accountRecordQueue = new AccountAssetIssueRecordQueue(BlockQueueFactory.getInstance(), countDownLatch);
                accountRecordQueue.fetchAccount(accountStore.getRevokingDB());

                AccountConvertQueue accountConvertQueue = new AccountConvertQueue(BlockQueueFactory.getInstance(), this);
                accountConvertQueue.convert();
                timer = countDown();
                logger.debug("import asset await");
                dynamicPropertiesStore.setAllowAssetImport(1L);
            } finally {
                try {
                    countDownLatch.await();
                    if (null != timer) {
                        timer.cancel();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        logger.info("import asset time: {}", (System.currentTimeMillis() - start) / 1000);
    }

    public static class AccountAssetIssueRecordQueue {

        private final CountDownLatch countDownLatch;

        private BlockingQueue productQueue;

        public AccountAssetIssueRecordQueue(BlockingQueue<Map.Entry<byte[], byte[]>> productQueue, CountDownLatch countDownLatch) {
            this.productQueue = productQueue;
            this.countDownLatch = countDownLatch;
        }

        public void put(Map.Entry<byte[], byte[]> accountByte) {
            try {
                productQueue.put(accountByte);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void fetchAccount(IRevokingDB revokingDB) {
            new Thread(() -> {
                for (Map.Entry<byte[], byte[]> accountRecord : revokingDB) {
                    put(accountRecord);
                }
                countDownLatch.countDown();
            }).start();
        }
    }


    public static class AccountConvertQueue {

        private BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue;

        private AccountAssetIssueStore accountAssetIssueStore;

        private ThreadPoolExecutor threadPoolExecutor;

        private static final int CORE_POOL_SIZE = 4;

        public static final int MAX_POOL_SIZE = 8;

        public static final int KEEP_ALIVE_SECONDES = 30;

        public AccountConvertQueue(BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue) {
            this.convertQueue = convertQueue;
        }

        public AccountConvertQueue(BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue, AccountAssetIssueStore accountAssetIssueStore) {
            this.convertQueue = convertQueue;
            this.accountAssetIssueStore = accountAssetIssueStore;
        }

        public void convert() {
            threadPoolExecutor = getThreadPoolExecutor();
            for (int i = 0; i < MAX_POOL_SIZE; i++) {
                threadPoolExecutor.execute(() -> {
                    try {
                        while (true) {
                            Map.Entry<byte[], byte[]> accountEntry = convertQueue.take();
                            if (null != accountEntry) {
                                AccountAssetIssueCapsule accountAssetIssueCapsule = new AccountAssetIssueCapsule();
                                AccountCapsule accountCapsule = new AccountCapsule(accountEntry.getValue());

                                AccountAssetIssue instance = AccountAssetIssue.newBuilder()
                                        .setAddress(accountCapsule.getAddress())
                                        .setAssetIssuedID(accountCapsule.getAssetIssuedID())
                                        .setAssetIssuedName(accountCapsule.getAssetIssuedName())
                                        .putAllAsset(accountCapsule.getAssetMap())
                                        .putAllAssetV2(accountCapsule.getAssetMapV2())

                                        .putAllFreeAssetNetUsage(accountCapsule.getAllFreeAssetNetUsage())
                                        .putAllFreeAssetNetUsageV2(accountCapsule.getAllFreeAssetNetUsageV2())
                                        .putAllLatestAssetOperationTime(accountCapsule.getLatestAssetOperationTimeMap())
                                        .putAllLatestAssetOperationTimeV2(accountCapsule.getLatestAssetOperationTimeMapV2())
                                        .build();
                                accountAssetIssueCapsule.setInstance(instance);
                                accountAssetIssueStore.put(accountCapsule.getAddress().toByteArray(), accountAssetIssueCapsule);

                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        private ThreadPoolExecutor getThreadPoolExecutor() {
            return new ThreadPoolExecutor(
                    CORE_POOL_SIZE,
                    MAX_POOL_SIZE,
                    KEEP_ALIVE_SECONDES,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(40000)
            );
        }

        public void closeTask() {
            threadPoolExecutor.shutdown();
        }
    }


    public static class BlockQueueFactory {

        private static BlockingQueue queue;

        public static BlockingQueue getInstance() {
            if (null == queue) {
                synchronized (BlockQueueFactory.class) {
                    if (null == queue) {
                        queue = new LinkedBlockingDeque(20000);
                    }
                }
            }
            return queue;
        }

        public BlockingQueue getInstance(int capcity) {
            if (null == queue) {
                synchronized (this) {
                    if (null == queue) {
                        queue = new LinkedBlockingDeque<>(capcity);
                    }
                }
            }
            return queue;
        }
    }
}
