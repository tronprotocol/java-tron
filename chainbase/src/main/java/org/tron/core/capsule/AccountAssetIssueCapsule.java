package org.tron.core.capsule;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountAssetIssue;

import java.util.Map;

@Slf4j(topic = "capsule")
public class AccountAssetIssueCapsule implements ProtoCapsule<AccountAssetIssue>, Comparable<AccountAssetIssue> {

    private AccountAssetIssue accountAssetIssue;

    public AccountAssetIssueCapsule(){}

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
    public boolean addAssetAmount(byte[] key, long amount) {
        Map<String, Long> assetMap = this.accountAssetIssue.getAssetMap();
        String nameKey = ByteArray.toStr(key);
        Long currentAmount = assetMap.get(nameKey);
        if (currentAmount == null) {
            currentAmount = 0L;
        }
        this.accountAssetIssue = this.accountAssetIssue.toBuilder().putAsset(nameKey, Math.addExact(currentAmount, amount))
                .build();
        return true;
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

    /**
     * add asset.
     */
    public boolean addAssetMapV2(Map<String, Long> assetMap) {
        this.accountAssetIssue = this.accountAssetIssue.toBuilder().putAllAssetV2(assetMap).build();
        return true;
    }

    public Map<String, Long> getAssetMap() {
        Map<String, Long> assetMap = this.accountAssetIssue.getAssetMap();
        if (assetMap.isEmpty()) {
            assetMap = Maps.newHashMap();
        }
        return assetMap;
    }

    public void setAssetIssuedName(byte[] nameKey) {
        ByteString assetIssuedName = ByteString.copyFrom(nameKey);
        this.accountAssetIssue = this.accountAssetIssue.toBuilder().setAssetIssuedName(assetIssuedName).build();
    }

    public ByteString getAssetIssuedID() {
        return getInstance().getAssetIssuedID();
    }

    public void setAssetIssuedID(byte[] id) {
        ByteString assetIssuedID = ByteString.copyFrom(id);
        this.accountAssetIssue = this.accountAssetIssue.toBuilder().setAssetIssuedID(assetIssuedID).build();
    }

    public void clearAssetV2() {
        this.accountAssetIssue = this.accountAssetIssue.toBuilder()
                .clearAssetV2()
                .build();
    }

    public ByteString getAssetIssuedName() {
        return getInstance().getAssetIssuedName();
    }

    public void clearFreeAssetNetUsageV2() {
        this.accountAssetIssue = this.accountAssetIssue.toBuilder()
                .clearFreeAssetNetUsageV2()
                .build();
    }

    public Map<String, Long> getAllFreeAssetNetUsage() {
        return this.accountAssetIssue.getFreeAssetNetUsageMap();
    }


    public boolean addAllFreeAssetNetUsageV2(Map<String, Long> map) {
        this.accountAssetIssue = this.accountAssetIssue.toBuilder().putAllFreeAssetNetUsageV2(map).build();
        return true;

    }

    public void clearLatestAssetOperationTimeV2() {
        this.accountAssetIssue = this.accountAssetIssue.toBuilder()
                .clearLatestAssetOperationTimeV2()
                .build();
    }

    public Map<String, Long> getLatestAssetOperationTimeMap() {
        return this.accountAssetIssue.getLatestAssetOperationTimeMap();
    }

    public boolean addAllLatestAssetOperationTimeV2(Map<String, Long> map) {
        this.accountAssetIssue = this.accountAssetIssue.toBuilder().putAllLatestAssetOperationTimeV2(map).build();
        return true;
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
    public boolean reduceAssetAmount(byte[] key, long amount) {
        Map<String, Long> assetMap = this.accountAssetIssue.getAssetMap();
        String nameKey = ByteArray.toStr(key);
        Long currentAmount = assetMap.get(nameKey);
        if (amount > 0 && null != currentAmount && amount <= currentAmount) {
            this.accountAssetIssue = this.accountAssetIssue.toBuilder()
                    .putAsset(nameKey, Math.subtractExact(currentAmount, amount)).build();
            return true;
        }

        return false;
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

    public long getFreeAssetNetUsage(String assetName) {
        return this.accountAssetIssue.getFreeAssetNetUsageOrDefault(assetName, 0);
    }

    public long getFreeAssetNetUsageV2(String assetName) {
        return this.accountAssetIssue.getFreeAssetNetUsageV2OrDefault(assetName, 0);
    }

    public Map<String, Long> getAllFreeAssetNetUsageV2() {
        return this.accountAssetIssue.getFreeAssetNetUsageV2Map();
    }

    public void putFreeAssetNetUsage(String s, long freeAssetNetUsage) {
        this.accountAssetIssue = this.accountAssetIssue.toBuilder()
                .putFreeAssetNetUsage(s, freeAssetNetUsage).build();
    }

    public void putFreeAssetNetUsageV2(String s, long freeAssetNetUsage) {
        this.accountAssetIssue = this.accountAssetIssue.toBuilder()
                .putFreeAssetNetUsageV2(s, freeAssetNetUsage).build();
    }

    public Map<String, Long> getLatestAssetOperationTimeMapV2() {
        return this.accountAssetIssue.getLatestAssetOperationTimeV2Map();
    }

    public long getLatestAssetOperationTime(String assetName) {
        return this.accountAssetIssue.getLatestAssetOperationTimeOrDefault(assetName, 0);
    }

    public long getLatestAssetOperationTimeV2(String assetName) {
        return this.accountAssetIssue.getLatestAssetOperationTimeV2OrDefault(assetName, 0);
    }

    public void putLatestAssetOperationTimeMap(String key, Long value) {
        this.accountAssetIssue = this.accountAssetIssue.toBuilder().putLatestAssetOperationTime(key, value).build();
    }

    public void putLatestAssetOperationTimeMapV2(String key, Long value) {
        this.accountAssetIssue = this.accountAssetIssue.toBuilder().putLatestAssetOperationTimeV2(key, value).build();
    }

    /**
     * asset balance enough
     */
    public boolean assetBalanceEnough(byte[] key, long amount) {
        Map<String, Long> assetMap = this.accountAssetIssue.getAssetMap();
        String nameKey = ByteArray.toStr(key);
        Long currentAmount = assetMap.get(nameKey);

        return amount > 0 && null != currentAmount && amount <= currentAmount;
    }

    public boolean assetBalanceEnoughV2(byte[] key, long amount,
                                      DynamicPropertiesStore dynamicPropertiesStore) {
        Map<String, Long> assetMap;
        String nameKey;
        Long currentAmount;
        if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
            assetMap = this.accountAssetIssue.getAssetMap();
            nameKey = ByteArray.toStr(key);
            currentAmount = assetMap.get(nameKey);
        } else {
            String tokenID = ByteArray.toStr(key);
            assetMap = this.accountAssetIssue.getAssetV2Map();
            currentAmount = assetMap.get(tokenID);
        }
    return amount > 0 && null != currentAmount && amount <= currentAmount;
  }

    @Override
    public String toString() {
        return this.accountAssetIssue.toString();
    }

    @Override
    public int compareTo(AccountAssetIssue o) {
        return 0;
    }
}
