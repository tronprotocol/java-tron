package org.tron.core.store;

import com.typesafe.config.ConfigObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.BlockQueueFactoryUtil;
import org.tron.common.utils.Commons;
import org.tron.common.utils.ThreadPoolUtil;
import org.tron.common.utils.TimerUtil;
import org.tron.core.capsule.AccountAssetIssueCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountAssetIssue;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

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

    public void RollbackAssetIssueToAccount() {
        long start = System.currentTimeMillis();
        logger.info("rollback asset to account store");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Timer timer = null;
        if (dynamicPropertiesStore.getAllowAssetImport() == 0L) {
            logger.info("");
            return;
        }
        AccountAssetIssueRecordQueue accountRecordQueue = new AccountAssetIssueRecordQueue(
                BlockQueueFactoryUtil.getInstance(),
                countDownLatch);
        accountRecordQueue.fetchAccountAssetIssue(this.getRevokingDB());
        AccountConvertQueue accountConvertQueue = new AccountConvertQueue(
                BlockQueueFactoryUtil.getInstance(),
                this,
                accountStore);
        accountConvertQueue.convertAccountAssetIssueToAccount();

        try {
            timer = TimerUtil.countDown("rollback account asset issue to account time spent ");
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            TimerUtil.cancel(timer);
        }
        logger.info("import asset time: {}", (System.currentTimeMillis() - start) / 1000);
    }

    public void convertAccountAssert() {
        long start = System.currentTimeMillis();
        logger.info("import asset of account store to account asset store ");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Timer timer = null;
        if (dynamicPropertiesStore.getAllowAssetImport() == 0L) {
            try {
                AccountAssetIssueRecordQueue accountRecordQueue = new AccountAssetIssueRecordQueue(
                        BlockQueueFactoryUtil.getInstance(),
                        countDownLatch);
                accountRecordQueue.fetchAccount(accountStore.getRevokingDB());
                AccountConvertQueue accountConvertQueue = new AccountConvertQueue(
                        BlockQueueFactoryUtil.getInstance(),
                        this,
                        accountStore);
                accountConvertQueue.convert();
                timer = TimerUtil.countDown("import asset time spent ");
                countDownLatch.await();
                dynamicPropertiesStore.setAllowAssetImport(1L);
            } catch (InterruptedException e) {
                logger.error("convert asset of account to account store exception: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
            } finally {
                TimerUtil.cancel(timer);
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

        private AccountStore accountStore;

        public AccountConvertQueue(BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue,
                                   AccountAssetIssueStore accountAssetIssueStore) {
            this.convertQueue = convertQueue;
            this.accountAssetIssueStore = accountAssetIssueStore;
        }

        public AccountConvertQueue(BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue,
                                   AccountAssetIssueStore accountAssetIssueStore, AccountStore accountStore) {
            this.convertQueue = convertQueue;
            this.accountAssetIssueStore = accountAssetIssueStore;
            this.accountStore = accountStore;
        }

        public void convert() {
            threadPoolExecutor = ThreadPoolUtil.getThreadPoolExecutor(40000);
            for (int i = 0; i < threadPoolExecutor.getMaximumPoolSize(); i++) {
                threadPoolExecutor.execute(() -> {
                    try {
                        while (true) {
                            Map.Entry<byte[], byte[]> accountEntry = convertQueue.take();
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
                                    .putAllLatestAssetOperationTimeV2(accountCapsule.getLatestAssetOperationTimeMapV2())
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
                        }
                    } catch (InterruptedException e) {
                        logger.error("account convert asset exception: {}", e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }

        public void convertAccountAssetIssueToAccount () {
            threadPoolExecutor = ThreadPoolUtil.getThreadPoolExecutor(40000);
            for (int i = 0; i < threadPoolExecutor.getMaximumPoolSize(); i++) {
                threadPoolExecutor.execute(() -> {
                    try {
                        while (true) {
                            Map.Entry<byte[], byte[]> accountAssetIssue = convertQueue.take();
                            AccountAssetIssueCapsule accountAssetIssueCapsule = new AccountAssetIssueCapsule(accountAssetIssue.getValue());
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
                                    .putAllLatestAssetOperationTime(accountAssetIssueCapsule.getLatestAssetOperationTimeMap())
                                    .putAllLatestAssetOperationTimeV2(accountAssetIssueCapsule.getLatestAssetOperationTimeMapV2())
                                    .build();
                            accountCapsule.setInstance(account);
                            accountStore.put(address, accountCapsule);
                        }
                    } catch (InterruptedException e) {
                        logger.error("convert account asset assue to account exception: {}", e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
    }

}
