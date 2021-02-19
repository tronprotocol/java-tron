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
import org.tron.protos.Protocol.AccountAssetIssue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "DB")
@Component
public class AccountAssetIssueStore extends TronStoreWithRevoking<AccountAssetIssueCapsule> {

    @Autowired
    private AccountStore accountStore;

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

    public void convertAccountAssert() {
        int count = 0;
        for (Map.Entry<byte[], byte[]> entry : accountStore.getRevokingDB()) {
            AccountCapsule accountCapsule = new AccountCapsule(entry.getValue());
            accountCapsule.getAddress();
            accountCapsule.getAssetIssuedID();
            accountCapsule.getAssetIssuedName();
            accountCapsule.getAssetMap();
            accountCapsule.getAssetMapV2();

            accountCapsule.getAllFreeAssetNetUsage();
            accountCapsule.getAllFreeAssetNetUsageV2();

            accountCapsule.getLatestAssetOperationTimeMap();
            accountCapsule.getLatestAssetOperationTimeMapV2();

            AccountAssetIssueCapsule accountAssetIssueCapsule = new AccountAssetIssueCapsule();
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
            this.put(entry.getKey(), accountAssetIssueCapsule);
            count++;
        }
        logger.info("convert count: {}", count);
    }
}
