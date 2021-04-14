package org.tron.core.capsule;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountAssetIssue;
import org.tron.protos.Protocol.AccountAssetIssue.Frozen;

import java.util.List;
import java.util.Map;

@Slf4j(topic = "capsule")
public class AccountAssetIssueCapsule implements ProtoCapsule<AccountAssetIssue> {

    private AccountAssetIssue accountAssetIssue;

    /**
     * get accountAssetIssue from bytes data.
     */
    public AccountAssetIssueCapsule(byte[] data) {
        try {
            this.accountAssetIssue = AccountAssetIssue.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage());
        }
    }

    /**
     * get account from address.
     */
    public AccountAssetIssueCapsule(ByteString address) {
        this.accountAssetIssue = AccountAssetIssue.newBuilder()
                .setAddress(address)
                .build();
    }

    public AccountAssetIssueCapsule(AccountAssetIssue accountAssetIssue) {
        this.accountAssetIssue = accountAssetIssue;
    }

    public AccountAssetIssueCapsule(ByteString assetIssueName, ByteString address) {
        this.accountAssetIssue = AccountAssetIssue.newBuilder()
                .setAssetIssuedName(assetIssueName)
                .setAddress(address)
                .build();
    }


    @Override
    public byte[] getData() {
        return this.accountAssetIssue.toByteArray();
    }

    @Override
    public AccountAssetIssue getInstance() {
        return this.accountAssetIssue;
    }


    public ByteString getAddress() {
        return this.accountAssetIssue.getAddress();
    }

    public byte[] createDbKey() {
        return getAddress().toByteArray();
    }

    public void setInstance(AccountAssetIssue accountAssetIssue) {
        this.accountAssetIssue = accountAssetIssue;
    }

    /**
     * add asset amount.
     */
    public boolean addAssetAmountV2(byte[] key, long amount,
                                    DynamicPropertiesStore dynamicPropertiesStore, AssetIssueStore assetIssueStore) {
        //key is token name
        if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
            Map<String, Long> assetMap = this.accountAssetIssue.getAssetMap();
            AssetIssueCapsule assetIssueCapsule = assetIssueStore.get(key);
            String tokenID = assetIssueCapsule.getId();
            String nameKey = ByteArray.toStr(key);
            Long currentAmount = assetMap.get(nameKey);
            if (currentAmount == null) {
                currentAmount = 0L;
            }
            this.accountAssetIssue = this.accountAssetIssue.toBuilder()
                    .putAsset(nameKey, Math.addExact(currentAmount, amount))
                    .putAssetV2(tokenID, Math.addExact(currentAmount, amount))
                    .build();
        }
        //key is token id
        if (dynamicPropertiesStore.getAllowSameTokenName() == 1) {
            String tokenIDStr = ByteArray.toStr(key);
            Map<String, Long> assetMapV2 = this.accountAssetIssue.getAssetV2Map();
            Long currentAmount = assetMapV2.get(tokenIDStr);
            if (currentAmount == null) {
                currentAmount = 0L;
            }
            this.accountAssetIssue = this.accountAssetIssue.toBuilder()
                    .putAssetV2(tokenIDStr, Math.addExact(currentAmount, amount))
                    .build();
        }
        return true;
    }

    /**
     * add asset.
     */
    public boolean addAsset(byte[] key, long value) {
        Map<String, Long> assetMap = this.accountAssetIssue.getAssetMap();
        String nameKey = ByteArray.toStr(key);
        if (!assetMap.isEmpty() && assetMap.containsKey(nameKey)) {
            return false;
        }

        this.accountAssetIssue = this.accountAssetIssue.toBuilder().putAsset(nameKey, value).build();

        return true;
    }

    public boolean addAssetV2(byte[] key, long value) {
        String tokenID = ByteArray.toStr(key);
        Map<String, Long> assetV2Map = this.accountAssetIssue.getAssetV2Map();
        if (!assetV2Map.isEmpty() && assetV2Map.containsKey(tokenID)) {
            return false;
        }

        this.accountAssetIssue = this.accountAssetIssue.toBuilder()
                .putAssetV2(tokenID, value)
                .build();
        return true;
    }

    public Map<String, Long> getAssetMap() {
        Map<String, Long> assetMap = this.accountAssetIssue.getAssetMap();
        if (assetMap.isEmpty()) {
            assetMap = Maps.newHashMap();
        }
        return assetMap;
    }

    public ByteString getAssetIssuedID() {
        return getInstance().getAssetIssuedID();
    }

    public ByteString getAssetIssuedName() {
        return getInstance().getAssetIssuedName();
    }

    public Map<String, Long> getAllFreeAssetNetUsage() {
        return this.accountAssetIssue.getFreeAssetNetUsageMap();
    }

    public Map<String, Long> getLatestAssetOperationTimeMap() {
        return this.accountAssetIssue.getLatestAssetOperationTimeMap();
    }

    public Map<String, Long> getAssetMapV2() {
        Map<String, Long> assetMap = this.accountAssetIssue.getAssetV2Map();
        if (assetMap.isEmpty()) {
            assetMap = Maps.newHashMap();
        }

        return assetMap;
    }


    /**
     * reduce asset amount.
     */
    public boolean reduceAssetAmountV2(byte[] key, long amount,
                                       DynamicPropertiesStore dynamicPropertiesStore, AssetIssueStore assetIssueStore) {
        //key is token name
        if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
            Map<String, Long> assetMap = this.accountAssetIssue.getAssetMap();
            AssetIssueCapsule assetIssueCapsule = assetIssueStore.get(key);
            String tokenID = assetIssueCapsule.getId();
            String nameKey = ByteArray.toStr(key);
            Long currentAmount = assetMap.get(nameKey);
            if (amount > 0 && null != currentAmount && amount <= currentAmount) {
                this.accountAssetIssue = this.accountAssetIssue.toBuilder()
                        .putAsset(nameKey, Math.subtractExact(currentAmount, amount))
                        .putAssetV2(tokenID, Math.subtractExact(currentAmount, amount))
                        .build();
                return true;
            }
        }
        //key is token id
        if (dynamicPropertiesStore.getAllowSameTokenName() == 1) {
            String tokenID = ByteArray.toStr(key);
            Map<String, Long> assetMapV2 = this.accountAssetIssue.getAssetV2Map();
            Long currentAmount = assetMapV2.get(tokenID);
            if (amount > 0 && null != currentAmount && amount <= currentAmount) {
                this.accountAssetIssue = this.accountAssetIssue.toBuilder()
                        .putAssetV2(tokenID, Math.subtractExact(currentAmount, amount))
                        .build();
                return true;
            }
        }
        return false;
    }

    public Map<String, Long> getAllFreeAssetNetUsageV2() {
        return this.accountAssetIssue.getFreeAssetNetUsageV2Map();
    }

    public Map<String, Long> getLatestAssetOperationTimeMapV2() {
        return this.accountAssetIssue.getLatestAssetOperationTimeV2Map();
    }

  public List<Frozen> getFrozenSupplyList() {
    return getInstance().getFrozenSupplyList();
  }

    @Override
    public String toString() {
        return this.accountAssetIssue.toString();
    }

}
