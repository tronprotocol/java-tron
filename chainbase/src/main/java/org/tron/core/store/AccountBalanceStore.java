package org.tron.core.store;

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

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @PostConstruct
    public void convertAccountToAccountBalance() {
        for (Map.Entry<byte[], AccountCapsule> next : accountStore) {
            AccountCapsule account = next.getValue();
            AccountBalanceCapsule accountBalanceCapsule = new AccountBalanceCapsule(account.getAddress(), account.getOriginalBalance(), account.getType());
            this.put(account.getAddress().toByteArray(), accountBalanceCapsule);
        }
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
