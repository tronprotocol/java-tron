package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountBalanceCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.store.AccountBalanceStore;
import org.tron.core.store.AccountStore;
import org.tron.protos.Protocol.AccountBalance;
import org.tron.protos.Protocol.AccountType;

import java.util.Map;

public class AccountBalanceStoreTest {
    private static final long BALANCE = 99999;
    private static final long NEW_BALANCE = 2222;
    private static AccountStore accountStore;
    private static AccountBalanceStore accountBalanceStore;
    private static TronApplicationContext context;
    private static Manager dbManager;

    private static final byte[] data = TransactionStoreTest.randomBytes(32);
    private static byte[] address = TransactionStoreTest.randomBytes(32);

    private static String dbPath = "output_AccountStore_test";
    private static String dbDirectory = "db_AccountStore_test";
    private static String indexDirectory = "index_AccountStore_test";

    static {
        Args.setParam(
                new String[]{
                        "--output-directory", dbPath,
                        "--storage-db-directory", dbDirectory,
                        "--storage-index-directory", indexDirectory
                },
                Constant.TEST_CONF
        );
        context = new TronApplicationContext(DefaultConfig.class);
    }

    @BeforeClass
    public static void init() {
        accountBalanceStore = context.getBean(AccountBalanceStore.class);
        AccountBalanceCapsule accountBalanceCapsule = new AccountBalanceCapsule(
                ByteString.copyFrom(address),
                AccountType.forNumber(1),
                BALANCE);
        accountBalanceStore.put(data, accountBalanceCapsule);
        dbManager = context.getBean(Manager.class);
        initAccount();
    }

    public static void initAccount() {
        byte[] accountName = TransactionStoreTest.randomBytes(32);
        accountStore = context.getBean(AccountStore.class);
        AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom(address),
                ByteString.copyFrom(accountName),
                AccountType.forNumber(1),
                BALANCE
        );
        accountStore.put(data, accountCapsule);
        accountStore.setAccountBalanceStore(accountBalanceStore);
    }


    @Test
    public void convertAccountToAccountBalance() {
        for (Map.Entry<byte[], AccountCapsule> next : accountStore) {
            AccountCapsule accountCapsule = next.getValue();
            accountCapsule.setAccountBalanceStore(dbManager.getChainBaseManager().getAccountBalanceStore());
            AccountBalanceCapsule accountBalance = new AccountBalanceCapsule(accountCapsule.getAddress(), accountCapsule.getOriginalBalance(), accountCapsule.getType());
            accountBalanceStore.put(accountCapsule.getAddress().toByteArray(), accountBalance);
        }
    }

    @Test
    public void getBalance() {
        AccountBalance instance = accountBalanceStore.get(data).getInstance();
        Assert.assertEquals(instance.getBalance(), BALANCE);
    }


    @Test
    public void getAccountBalance() {
        convertAccountToAccountBalance();
        AccountCapsule accountCapsule = accountStore.get(data);
        Assert.assertEquals(accountCapsule.getBalance(), BALANCE);
    }


    @Test
    public void setAccountBalance() {
        convertAccountToAccountBalance();
        AccountCapsule accountCapsule = accountStore.get(data);
        accountCapsule.setBalance(BALANCE - NEW_BALANCE);
        accountStore.put(accountCapsule.getAddress().toByteArray(), accountCapsule);
        Assert.assertEquals(
                accountStore.get(accountCapsule.getAddress().toByteArray()).getBalance(),
                BALANCE - NEW_BALANCE);
    }
}
