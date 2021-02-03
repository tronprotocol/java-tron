package org.tron.core.store;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AccountAssetIssueCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.protos.Protocol.AccountAssetIssue;

import java.util.Map;

@Slf4j(topic = "DB")
@Component
public class AccountAssetIssueStore extends TronStoreWithRevoking<AccountAssetIssueCapsule> {

    @Autowired
    private AccountStore accountStore;

    @Autowired
    protected AccountAssetIssueStore(@Value("account-asset-issue") String dbName) {
        super(dbName);
    }

    @Override
    public AccountAssetIssueCapsule get(byte[] key) {
        byte[] value = revokingDB.getUnchecked(key);
        return ArrayUtils.isEmpty(value) ? null : new AccountAssetIssueCapsule(value);
    }


    public void convertAccountAssert() {
        int count = 0;
        for (Map.Entry<byte[], byte[]> entry : accountStore.getRevokingDB()) {
            AccountCapsule accountCapsule = new AccountCapsule(entry.getValue());
//            accountCapsule.getAddress();
//            accountCapsule.getAssetIssuedID();
//            accountCapsule.getAssetIssuedName();
//            accountCapsule.getAssetMap();
//            accountCapsule.getAssetMapV2();

//            accountCapsule.getAllFreeAssetNetUsage();
//            accountCapsule.getAllFreeAssetNetUsageV2();

//            accountCapsule.getLatestAssetOperationTimeMap();
//            accountCapsule.getLatestAssetOperationTimeMapV2();

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
