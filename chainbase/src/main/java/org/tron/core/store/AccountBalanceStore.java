package org.tron.core.store;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.typesafe.config.ConfigObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.AccountBalanceCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db.accountstate.AccountStateCallBackUtils;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.exception.BadItemException;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

@Slf4j(topic = "DB")
@Component
public class AccountBalanceStore extends TronStoreWithRevoking<AccountBalanceCapsule> {

    @Autowired
    private AccountStore accountStore;

    private static Map<String, byte[]> assertsAddress = new HashMap<>(); // key = name , value = address

    @Autowired
    private AccountStateCallBackUtils accountStateCallBackUtils;

    @Autowired
    private AccountBalanceStore(@Value("account-balance") String dbName) {
        super(dbName);
    }


    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private static final int CORE_POOL_SIZE = Math.max(2, Math.max(CPU_COUNT - 1, 4));

    public static final int MAX_POOL_SIZE = CPU_COUNT * 2 + 1;

    private static final int KEEP_ALIVE_SECONDS = 30;


    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<>();

    public static final Executor THREAD_POOL_EXECUTOR;

    static {

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                sPoolWorkQueue,
                sThreadFactory

        );
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }


    @PostConstruct
    public void convertAccountToAccountBalance() {
        long start = System.currentTimeMillis();
        int count = 0;
        logger.info("import balance of account to account balance store ");
        logger.info("start time: {}", start);
        for (Map.Entry<byte[], AccountCapsule> next : accountStore) {
            AccountCapsule account = next.getValue();
            AccountBalanceCapsule accountBalanceCapsule = new AccountBalanceCapsule(account.getAddress(), account.getOriginalBalance(), account.getType());
            THREAD_POOL_EXECUTOR.execute(() -> {
                put(account.getAddress().toByteArray(), accountBalanceCapsule);
            });
            count++;
        }

        logger.info("import balance time: {}, count: {}", System.currentTimeMillis() - start, count);
    }

    public void convertAccountToAccountBalance2() {
        IRevokingDB revokingDB = accountStore.getRevokingDB();




        System.out.println("debug...");
    }





    public static void setAccount(com.typesafe.config.Config config) {
        List list = config.getObjectList("genesis.block.assets");
        for (int i = 0; i < list.size(); i++) {
            ConfigObject obj = (ConfigObject) list.get(i);
            String accountName = obj.get("accountName").unwrapped().toString();
            byte[] address = Commons.decodeFromBase58Check(obj.get("address").unwrapped().toString());
            assertsAddress.put(accountName, address);
        }
    }

    @Override
    public AccountBalanceCapsule get(byte[] key) {
        byte[] value = revokingDB.getUnchecked(key);
        return ArrayUtils.isEmpty(value) ? null : new AccountBalanceCapsule(value);
    }

    @Override
    public void put(byte[] key, AccountBalanceCapsule item) {
        super.put(key, item);
        accountStateCallBackUtils.accountBalanceCallBack(key, item);
    }

    /**
     * Min TRX account.
     */
    public AccountBalanceCapsule getBlackhole() {
        return getUnchecked(assertsAddress.get("Blackhole"));
    }

}
