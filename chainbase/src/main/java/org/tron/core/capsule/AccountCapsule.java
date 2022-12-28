/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.capsule;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.utils.AssetUtil;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.AccountResource;
import org.tron.protos.Protocol.Account.Builder;
import org.tron.protos.Protocol.Account.Frozen;
import org.tron.protos.Protocol.Account.FreezeV2;
import org.tron.protos.Protocol.Account.UnFreezeV2;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Permission.PermissionType;
import org.tron.protos.Protocol.Vote;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountUpdateContract;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.WINDOW_SIZE_MS;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;
import static org.tron.protos.contract.Common.ResourceCode.TRON_POWER;
import static org.tron.protos.contract.Common.ResourceCode;

@Slf4j(topic = "capsule")
public class AccountCapsule implements ProtoCapsule<Account>, Comparable<AccountCapsule> {

  private Account account;
  private boolean flag = false;

  /**
   * get account from bytes data.
   */
  public AccountCapsule(byte[] data) {
    try {
      this.account = Account.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  /**
   * initial account capsule.
   */
  public AccountCapsule(ByteString accountName, ByteString address, AccountType accountType,
      long balance) {
    this.account = Account.newBuilder()
        .setAccountName(accountName)
        .setType(accountType)
        .setAddress(address)
        .setBalance(balance)
        .build();
  }

  /**
   * construct account from AccountCreateContract.
   */
  public AccountCapsule(final AccountCreateContract contract) {
    this.account = Account.newBuilder()
        .setType(contract.getType())
        .setAddress(contract.getAccountAddress())
        .setTypeValue(contract.getTypeValue())
        .build();
  }

  /**
   * construct account from AccountCreateContract and createTime.
   */
  public AccountCapsule(final AccountCreateContract contract, long createTime,
      boolean withDefaultPermission, DynamicPropertiesStore dynamicPropertiesStore) {
    if (withDefaultPermission) {
      Permission owner = createDefaultOwnerPermission(contract.getAccountAddress());
      Permission active = createDefaultActivePermission(contract.getAccountAddress(),
          dynamicPropertiesStore);

      this.account = Account.newBuilder()
          .setType(contract.getType())
          .setAddress(contract.getAccountAddress())
          .setTypeValue(contract.getTypeValue())
          .setCreateTime(createTime)
          .setOwnerPermission(owner)
          .addActivePermission(active)
          .build();
    } else {
      this.account = Account.newBuilder()
          .setType(contract.getType())
          .setAddress(contract.getAccountAddress())
          .setTypeValue(contract.getTypeValue())
          .setCreateTime(createTime)
          .build();
    }
  }


  /**
   * construct account from AccountUpdateContract
   */
  public AccountCapsule(final AccountUpdateContract contract) {

  }

  /**
   * get account from address and account name.
   */
  public AccountCapsule(ByteString address, ByteString accountName,
      AccountType accountType) {
    this.account = Account.newBuilder()
        .setType(accountType)
        .setAccountName(accountName)
        .setAddress(address)
        .build();
  }

  /**
   * get account from address.
   */
  public AccountCapsule(ByteString address,
      AccountType accountType) {
    this.account = Account.newBuilder()
        .setType(accountType)
        .setAddress(address)
        .build();
  }

  /**
   * get account from address.
   */
  public AccountCapsule(ByteString address,
      AccountType accountType, long createTime,
      boolean withDefaultPermission, DynamicPropertiesStore dynamicPropertiesStore) {
    if (withDefaultPermission) {
      Permission owner = createDefaultOwnerPermission(address);
      Permission active = createDefaultActivePermission(address, dynamicPropertiesStore);

      this.account = Account.newBuilder()
          .setType(accountType)
          .setAddress(address)
          .setCreateTime(createTime)
          .setOwnerPermission(owner)
          .addActivePermission(active)
          .build();
    } else {
      this.account = Account.newBuilder()
          .setType(accountType)
          .setAddress(address)
          .setCreateTime(createTime)
          .build();
    }

  }

  /**
   * get account from address.
   */
  public AccountCapsule(Account account) {
    this.account = account;
  }

  private static ByteString getActiveDefaultOperations(
      DynamicPropertiesStore dynamicPropertiesStore) {
    return ByteString.copyFrom(dynamicPropertiesStore.getActiveDefaultOperations());
  }

  public static Permission createDefaultOwnerPermission(ByteString address) {
    Key.Builder key = Key.newBuilder();
    key.setAddress(address);
    key.setWeight(1);

    Permission.Builder owner = Permission.newBuilder();
    owner.setType(PermissionType.Owner);
    owner.setId(0);
    owner.setPermissionName("owner");
    owner.setThreshold(1);
    owner.setParentId(0);
    owner.addKeys(key);

    return owner.build();
  }

  public static Permission createDefaultActivePermission(ByteString address,
      DynamicPropertiesStore dynamicPropertiesStore) {
    Key.Builder key = Key.newBuilder();
    key.setAddress(address);
    key.setWeight(1);

    Permission.Builder active = Permission.newBuilder();
    active.setType(PermissionType.Active);
    active.setId(2);
    active.setPermissionName("active");
    active.setThreshold(1);
    active.setParentId(0);
    active.setOperations(getActiveDefaultOperations(dynamicPropertiesStore));
    active.addKeys(key);

    return active.build();
  }

  public static Permission createDefaultWitnessPermission(ByteString address) {
    Key.Builder key = Key.newBuilder();
    key.setAddress(address);
    key.setWeight(1);

    Permission.Builder active = Permission.newBuilder();
    active.setType(PermissionType.Witness);
    active.setId(1);
    active.setPermissionName("witness");
    active.setThreshold(1);
    active.setParentId(0);
    active.addKeys(key);

    return active.build();
  }

  public static Permission getDefaultPermission(ByteString owner) {
    return createDefaultOwnerPermission(owner);
  }

  @Override
  public int compareTo(AccountCapsule otherObject) {
    return Long.compare(otherObject.getBalance(), this.getBalance());
  }

  public byte[] getData() {
    return this.account.toByteArray();
  }

  @Override
  public Account getInstance() {
    return this.account;
  }

  public void setInstance(Account account) {
    this.account = account;
  }

  public ByteString getAddress() {
    return this.account.getAddress();
  }

  public byte[] createDbKey() {
    return getAddress().toByteArray();
  }

  public String createReadableString() {
    return ByteArray.toHexString(getAddress().toByteArray());
  }

  public AccountType getType() {
    return this.account.getType();
  }

  public ByteString getAccountName() {
    return this.account.getAccountName();
  }

  /**
   * set account name
   */
  public void setAccountName(byte[] name) {
    this.account = this.account.toBuilder().setAccountName(ByteString.copyFrom(name)).build();
  }

  public ByteString getAccountId() {
    return this.account.getAccountId();
  }

  /**
   * set account id
   */
  public void setAccountId(byte[] id) {
    this.account = this.account.toBuilder().setAccountId(ByteString.copyFrom(id)).build();
  }

  public void setDefaultWitnessPermission(DynamicPropertiesStore dynamicPropertiesStore) {
    Builder builder = this.account.toBuilder();
    Permission witness = createDefaultWitnessPermission(this.getAddress());
    if (!this.account.hasOwnerPermission()) {
      Permission owner = createDefaultOwnerPermission(this.getAddress());
      builder.setOwnerPermission(owner);
    }
    if (this.account.getActivePermissionCount() == 0) {
      Permission active = createDefaultActivePermission(this.getAddress(), dynamicPropertiesStore);
      builder.addActivePermission(active);
    }
    this.account = builder.setWitnessPermission(witness).build();
  }

  public byte[] getWitnessPermissionAddress() {
    if (this.account.getWitnessPermission().getKeysCount() == 0) {
      return getAddress().toByteArray();
    } else {
      return this.account.getWitnessPermission().getKeys(0).getAddress().toByteArray();
    }
  }

  public long getBalance() {
    return this.account.getBalance();
  }

  public void setBalance(long balance) {
    this.account = this.account.toBuilder().setBalance(balance).build();
  }

  public long getLatestOperationTime() {
    return this.account.getLatestOprationTime();
  }

  public void setLatestOperationTime(long latestTime) {
    this.account = this.account.toBuilder().setLatestOprationTime(latestTime).build();
  }

  public long getLatestConsumeTime() {
    return this.account.getLatestConsumeTime();
  }

  public void setLatestConsumeTime(long latestTime) {
    this.account = this.account.toBuilder().setLatestConsumeTime(latestTime).build();
  }

  public long getLatestConsumeFreeTime() {
    return this.account.getLatestConsumeFreeTime();
  }

  public void setLatestConsumeFreeTime(long latestTime) {
    this.account = this.account.toBuilder().setLatestConsumeFreeTime(latestTime).build();
  }

  public void addDelegatedFrozenBalanceForBandwidth(long balance) {
    this.account = this.account.toBuilder().setDelegatedFrozenBalanceForBandwidth(
        this.account.getDelegatedFrozenBalanceForBandwidth() + balance).build();
  }

  public void addDelegatedFrozenV2BalanceForBandwidth(long balance) {
    this.account = this.account.toBuilder().setDelegatedFrozenV2BalanceForBandwidth(
            this.account.getDelegatedFrozenV2BalanceForBandwidth() + balance).build();
  }

  public long getAcquiredDelegatedFrozenBalanceForBandwidth() {
    return this.account.getAcquiredDelegatedFrozenBalanceForBandwidth();
  }

  public long getAcquiredDelegatedFrozenV2BalanceForBandwidth() {
    return this.account.getAcquiredDelegatedFrozenV2BalanceForBandwidth();
  }

  public long getTotalAcquiredDelegatedFrozenBalanceForBandwidth() {
    return getAcquiredDelegatedFrozenBalanceForBandwidth() + getAcquiredDelegatedFrozenV2BalanceForBandwidth();
  }

  public void addFrozenBalanceForBandwidthV2(long balance) {
    this.addFrozenBalanceForResource(BANDWIDTH, balance);
  }

  public void setAcquiredDelegatedFrozenBalanceForBandwidth(long balance) {
    this.account = this.account.toBuilder().setAcquiredDelegatedFrozenBalanceForBandwidth(balance)
        .build();
  }

  public void setAcquiredDelegatedFrozenV2BalanceForBandwidth(long balance) {
    this.account = this.account.toBuilder().setAcquiredDelegatedFrozenV2BalanceForBandwidth(balance)
        .build();
  }

  public void addAcquiredDelegatedFrozenBalanceForBandwidth(long balance) {
    this.account = this.account.toBuilder().setAcquiredDelegatedFrozenBalanceForBandwidth(
        this.account.getAcquiredDelegatedFrozenBalanceForBandwidth() + balance)
        .build();
  }

  public void addAcquiredDelegatedFrozenV2BalanceForBandwidth(long balance) {
    this.account = this.account.toBuilder().setAcquiredDelegatedFrozenV2BalanceForBandwidth(
            this.account.getAcquiredDelegatedFrozenV2BalanceForBandwidth() + balance).build();
  }

  public void safeAddAcquiredDelegatedFrozenBalanceForBandwidth(long balance) {
    this.account = this.account.toBuilder().setAcquiredDelegatedFrozenBalanceForBandwidth(
        Math.max(0, this.account.getAcquiredDelegatedFrozenBalanceForBandwidth() + balance))
        .build();
  }

  public void safeAddAcquiredDelegatedFrozenV2BalanceForBandwidth(long balance) {
    this.account = this.account.toBuilder().setAcquiredDelegatedFrozenV2BalanceForBandwidth(
            Math.max(0, this.account.getAcquiredDelegatedFrozenV2BalanceForBandwidth() + balance))
            .build();
  }

  public long getAcquiredDelegatedFrozenBalanceForEnergy() {
    return getAccountResource().getAcquiredDelegatedFrozenBalanceForEnergy();
  }

  public long getAcquiredDelegatedFrozenV2BalanceForEnergy() {
    return getAccountResource().getAcquiredDelegatedFrozenV2BalanceForEnergy();
  }

  public long getTotalAcquiredDelegatedFrozenBalanceForEnergy() {
    return getAcquiredDelegatedFrozenBalanceForEnergy() + getAcquiredDelegatedFrozenV2BalanceForEnergy();
  }

  public void setAcquiredDelegatedFrozenBalanceForEnergy(long balance) {
    AccountResource newAccountResource = getAccountResource().toBuilder()
        .setAcquiredDelegatedFrozenBalanceForEnergy(balance).build();

    this.account = this.account.toBuilder()
        .setAccountResource(newAccountResource)
        .build();
  }

  public void setAcquiredDelegatedFrozenV2BalanceForEnergy(long balance) {
    AccountResource newAccountResource = getAccountResource().toBuilder()
            .setAcquiredDelegatedFrozenV2BalanceForEnergy(balance).build();
    this.account = this.account.toBuilder().setAccountResource(newAccountResource).build();
  }

  public long getDelegatedFrozenBalanceForEnergy() {
    return getAccountResource().getDelegatedFrozenBalanceForEnergy();
  }

  public long getDelegatedFrozenV2BalanceForEnergy() {
    return getAccountResource().getDelegatedFrozenV2BalanceForEnergy();
  }

  public long getTotalDelegatedFrozenBalanceForEnergy() {
    return getDelegatedFrozenBalanceForEnergy() + getDelegatedFrozenV2BalanceForEnergy();
  }

  public long getDelegatedFrozenBalanceForBandwidth() {
    return this.account.getDelegatedFrozenBalanceForBandwidth();
  }

  public long getDelegatedFrozenV2BalanceForBandwidth() {
    return this.account.getDelegatedFrozenV2BalanceForBandwidth();
  }

  public long getTotalDelegatedFrozenBalanceForBandwidth() {
    return getDelegatedFrozenBalanceForBandwidth() + getDelegatedFrozenV2BalanceForBandwidth();
  }

  public void setDelegatedFrozenBalanceForBandwidth(long balance) {
    this.account = this.account.toBuilder()
        .setDelegatedFrozenBalanceForBandwidth(balance)
        .build();
  }

  public void setDelegatedFrozenBalanceForEnergy(long balance){
    AccountResource newAccountResource = getAccountResource().toBuilder()
        .setDelegatedFrozenBalanceForEnergy(balance).build();

    this.account = this.account.toBuilder()
        .setAccountResource(newAccountResource)
        .build();
  }

  public void addAcquiredDelegatedFrozenBalanceForEnergy(long balance) {
    AccountResource newAccountResource = getAccountResource().toBuilder()
        .setAcquiredDelegatedFrozenBalanceForEnergy(
            getAccountResource().getAcquiredDelegatedFrozenBalanceForEnergy() + balance).build();

    this.account = this.account.toBuilder()
        .setAccountResource(newAccountResource)
        .build();
  }

  public void addAcquiredDelegatedFrozenV2BalanceForEnergy(long balance) {
    AccountResource newAccountResource = getAccountResource().toBuilder()
            .setAcquiredDelegatedFrozenV2BalanceForEnergy(getAccountResource()
                    .getAcquiredDelegatedFrozenV2BalanceForEnergy() + balance).build();
    this.account = this.account.toBuilder().setAccountResource(newAccountResource).build();
  }

  public void safeAddAcquiredDelegatedFrozenBalanceForEnergy(long balance) {
    AccountResource newAccountResource = getAccountResource().toBuilder()
        .setAcquiredDelegatedFrozenBalanceForEnergy(
            Math.max(0, getAccountResource().getAcquiredDelegatedFrozenBalanceForEnergy() + balance))
        .build();

    this.account = this.account.toBuilder()
        .setAccountResource(newAccountResource)
        .build();
  }

  public void safeAddAcquiredDelegatedFrozenV2BalanceForEnergy(long balance) {
    AccountResource newAccountResource = getAccountResource().toBuilder()
            .setAcquiredDelegatedFrozenV2BalanceForEnergy(Math.max(0, getAccountResource()
                    .getAcquiredDelegatedFrozenV2BalanceForEnergy() + balance)).build();
    this.account = this.account.toBuilder().setAccountResource(newAccountResource).build();
  }

  public void addDelegatedFrozenBalanceForEnergy(long balance) {
    AccountResource newAccountResource = getAccountResource().toBuilder()
        .setDelegatedFrozenBalanceForEnergy(
            getAccountResource().getDelegatedFrozenBalanceForEnergy() + balance).build();

    this.account = this.account.toBuilder()
        .setAccountResource(newAccountResource)
        .build();
  }

  public void addDelegatedFrozenV2BalanceForEnergy(long balance) {
    AccountResource newAccountResource = getAccountResource().toBuilder()
            .setDelegatedFrozenV2BalanceForEnergy(
                    getAccountResource().getDelegatedFrozenV2BalanceForEnergy() + balance).build();

    this.account = this.account.toBuilder().setAccountResource(newAccountResource).build();
  }

  public void addFrozenBalanceForEnergyV2(long balance) {
    this.addFrozenBalanceForResource(ENERGY, balance);
  }

  private void addFrozenBalanceForResource(ResourceCode type, long balance) {
    boolean doUpdate = false;
    for (int i = 0; i < this.account.getFrozenV2List().size(); i++) {
      if (this.account.getFrozenV2List().get(i).getType().equals(type)) {
        long newAmount = this.account.getFrozenV2(i).getAmount() + balance;
        FreezeV2 freezeV2 = FreezeV2.newBuilder()
                .setType(type)
                .setAmount(newAmount)
                .build();
        this.updateFrozenV2List(i, freezeV2);
        doUpdate = true;
        break;
      }
    }

    if (!doUpdate) {
      FreezeV2 freezeV2 = FreezeV2.newBuilder()
              .setType(type)
              .setAmount(balance)
              .build();
      this.addFrozenV2List(freezeV2);
    }
  }

  @Override
  public String toString() {
    return this.account.toString();
  }

  /**
   * set votes.
   */
  public void addVotes(ByteString voteAddress, long voteAdd) {
    this.account = this.account.toBuilder()
        .addVotes(Vote.newBuilder().setVoteAddress(voteAddress).setVoteCount(voteAdd).build())
        .build();
  }

  public void addAllVotes(List<Vote> votesToAdd) {
    this.account = this.account.toBuilder().addAllVotes(votesToAdd).build();
  }

  public void clearLatestAssetOperationTimeV2() {
    this.account = this.account.toBuilder()
        .clearLatestAssetOperationTimeV2()
        .build();
  }

  public void clearFreeAssetNetUsageV2() {
    this.account = this.account.toBuilder()
        .clearFreeAssetNetUsageV2()
        .build();
  }

  public void clearVotes() {
    this.account = this.account.toBuilder()
        .clearVotes()
        .build();
  }

  /**
   * get votes.
   */
  public List<Vote> getVotesList() {
    return this.account.getVotesList();
  }

  public long getTronPowerUsage() {
    if (getVotesList().isEmpty()) {
      return 0L;
    }
    return this.account.getVotesList().stream().mapToLong(Vote::getVoteCount).sum();
  }

  //tp:Tron_Power
  public long getTronPower() {
    long tp = 0;
    for (int i = 0; i < account.getFrozenCount(); ++i) {
      tp += account.getFrozen(i).getFrozenBalance();
    }

    tp += account.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance();
    tp += account.getDelegatedFrozenBalanceForBandwidth();
    tp += account.getAccountResource().getDelegatedFrozenBalanceForEnergy();

    tp += getFrozenV2List().stream().filter(o -> o.getType() != TRON_POWER)
            .mapToLong(FreezeV2::getAmount).sum();
    tp += account.getDelegatedFrozenV2BalanceForBandwidth();
    tp += account.getAccountResource().getDelegatedFrozenV2BalanceForEnergy();
    return tp;
  }

  public long getAllTronPower() {
    if (account.getOldTronPower() == -1) {
      return getTronPowerFrozenBalance() + getTronPowerFrozenV2Balance();
    } else if (account.getOldTronPower() == 0) {
      return getTronPower() + getTronPowerFrozenBalance() + getTronPowerFrozenV2Balance();
    } else {
      return account.getOldTronPower() + getTronPowerFrozenBalance()
          + getTronPowerFrozenV2Balance();
    }
  }



  public List<FreezeV2> getFrozenV2List() {
    return account.getFrozenV2List();
  }

  public List<UnFreezeV2> getUnfrozenV2List() {
    return account.getUnfrozenV2List();
  }

  public void updateFrozenV2List(int index, FreezeV2 frozenV2) {
    if (Objects.isNull(frozenV2)) {
      return;
    }
    this.account = this.account.toBuilder().setFrozenV2(index, frozenV2).build();
  }

  public void addFrozenV2List(FreezeV2 frozenV2) {
    this.account = this.account.toBuilder().addFrozenV2(frozenV2).build();
  }

  public void addUnfrozenV2List(ResourceCode type, long unfreezeAmount, long expireTime) {
    UnFreezeV2 unFreezeV2 = UnFreezeV2.newBuilder()
            .setType(type)
            .setUnfreezeAmount(unfreezeAmount)
            .setUnfreezeExpireTime(expireTime)
            .build();
    this.account = this.account.toBuilder().addUnfrozenV2(unFreezeV2).build();
  }


  public int getUnfreezingV2Count(long now) {
    int count = 0;
    List<UnFreezeV2> unFreezeV2List = account.getUnfrozenV2List();
    for (UnFreezeV2 item : unFreezeV2List) {
      if (item.getUnfreezeExpireTime() > now) {
        count++;
      }
    }
    return count;
  }


  /*************************** start asset ****************************************/

  public boolean getAssetOptimized() {
    return this.account.getAssetOptimized();
  }

  public void setAssetOptimized(boolean flag) {
    this.account = this.account.toBuilder().setAssetOptimized(flag).build();
  }

  public boolean assetBalanceEnoughV2(byte[] key, long amount,
      DynamicPropertiesStore dynamicPropertiesStore) {
    importAsset(key);
    Map<String, Long> assetMap;
    String nameKey;
    Long currentAmount;
    if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
      assetMap = this.account.getAssetMap();
      nameKey = ByteArray.toStr(key);
      currentAmount = assetMap.get(nameKey);
    } else {
      String tokenID = ByteArray.toStr(key);
      assetMap = this.account.getAssetV2Map();
      currentAmount = assetMap.get(tokenID);
    }

    return amount > 0 && null != currentAmount && amount <= currentAmount;
  }

  public boolean addAssetAmount(byte[] key, long amount) {
    Map<String, Long> assetMap = this.account.getAssetMap();
    String nameKey = ByteArray.toStr(key);
    Long currentAmount = assetMap.get(nameKey);
    if (currentAmount == null) {
      currentAmount = 0L;
    }
    this.account = this.account.toBuilder().putAsset(nameKey, Math.addExact(currentAmount, amount))
        .build();
    return true;
  }

  public boolean addAssetAmountV2(byte[] key, long amount,
      DynamicPropertiesStore dynamicPropertiesStore, AssetIssueStore assetIssueStore) {
    importAsset(key);
    //key is token name
    if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
      Map<String, Long> assetMap = this.account.getAssetMap();
      AssetIssueCapsule assetIssueCapsule = assetIssueStore.get(key);
      String tokenID = assetIssueCapsule.getId();
      String nameKey = ByteArray.toStr(key);
      Long currentAmount = assetMap.get(nameKey);
      if (currentAmount == null) {
        currentAmount = 0L;
      }
      this.account = this.account.toBuilder()
          .putAsset(nameKey, Math.addExact(currentAmount, amount))
          .putAssetV2(tokenID, Math.addExact(currentAmount, amount))
          .build();
    }
    //key is token id
    if (dynamicPropertiesStore.getAllowSameTokenName() == 1) {
      String tokenIDStr = ByteArray.toStr(key);
      Map<String, Long> assetMapV2 = this.account.getAssetV2Map();
      Long currentAmount = assetMapV2.get(tokenIDStr);
      if (currentAmount == null) {
        currentAmount = 0L;
      }
      this.account = this.account.toBuilder()
          .putAssetV2(tokenIDStr, Math.addExact(currentAmount, amount))
          .build();
    }
    return true;
  }

  public boolean reduceAssetAmount(byte[] key, long amount) {
    Map<String, Long> assetMap = this.account.getAssetMap();
    String nameKey = ByteArray.toStr(key);
    Long currentAmount = assetMap.get(nameKey);
    if (amount > 0 && null != currentAmount && amount <= currentAmount) {
      this.account = this.account.toBuilder()
              .putAsset(nameKey, Math.subtractExact(currentAmount, amount)).build();
      return true;
    }

    return false;
  }

  public boolean reduceAssetAmountV2(byte[] key, long amount,
                                     DynamicPropertiesStore dynamicPropertiesStore, AssetIssueStore assetIssueStore) {
    importAsset(key);
    //key is token name
    if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
      Map<String, Long> assetMap = this.account.getAssetMap();
      AssetIssueCapsule assetIssueCapsule = assetIssueStore.get(key);
      String tokenID = assetIssueCapsule.getId();
      String nameKey = ByteArray.toStr(key);
      Long currentAmount = assetMap.get(nameKey);
      if (amount > 0 && null != currentAmount && amount <= currentAmount) {
        this.account = this.account.toBuilder()
                .putAsset(nameKey, Math.subtractExact(currentAmount, amount))
                .putAssetV2(tokenID, Math.subtractExact(currentAmount, amount))
                .build();
        return true;
      }
    }
    //key is token id
    if (dynamicPropertiesStore.getAllowSameTokenName() == 1) {
      String tokenID = ByteArray.toStr(key);
      Map<String, Long> assetMapV2 = this.account.getAssetV2Map();
      Long currentAmount = assetMapV2.get(tokenID);
      if (amount > 0 && null != currentAmount && amount <= currentAmount) {
        this.account = this.account.toBuilder()
                .putAssetV2(tokenID, Math.subtractExact(currentAmount, amount))
                .build();
        return true;
      }
    }

    return false;
  }

  public void clearAssetV2() {
    this.account = this.account.toBuilder()
            .clearAssetV2()
            .build();
  }

  public void clearAsset() {
    this.account = this.account.toBuilder()
            .clearAsset()
            .clearAssetV2()
            .build();
  }

  public boolean addAsset(byte[] key, long value) {
    Map<String, Long> assetMap = this.account.getAssetMap();
    String nameKey = ByteArray.toStr(key);
    if (!assetMap.isEmpty() && assetMap.containsKey(nameKey)) {
      return false;
    }
    this.account = this.account.toBuilder().putAsset(nameKey, value).build();
    return true;
  }

  public boolean addAssetV2(byte[] key, long value) {
    if (AssetUtil.hasAssetV2(this.account, key)) {
      return false;
    }

    this.account = this.account.toBuilder()
        .putAssetV2(ByteArray.toStr(key), value)
        .build();
    return true;
  }

  public void addAssetMapV2(Map<String, Long> assetMap) {
    this.account = this.account.toBuilder().putAllAssetV2(assetMap).build();
  }

  public Long getAsset(DynamicPropertiesStore dynamicStore, String key) {
    Long balance;
    if (dynamicStore.getAllowSameTokenName() == 0) {
      balance = this.account.getAssetMap().get(key);
    } else {
      importAsset(key.getBytes());
      balance = this.account.getAssetV2Map().get(key);
    }
    return balance;
  }

  public long getAssetV2(String key) {
    importAsset(key.getBytes());
    Long balance = this.account.getAssetV2Map().get(key);
    return balance == null ? 0 : balance;
  }

  public Map<String, Long> getAssetMap() {
    Map<String, Long> assetMap = this.account.getAssetMap();
    if (assetMap.isEmpty()) {
      assetMap = Maps.newHashMap();
    }
    return assetMap;
  }

  public Map<String, Long> getAssetMapV2() {
    importAllAsset();
    Map<String, Long> assetMap = this.account.getAssetV2Map();
    if (assetMap.isEmpty()) {
      assetMap = Maps.newHashMap();
    }
    return assetMap;
  }

  public Map<String, Long> getAssetMapForTest() {
    return getAssetMap();
  }

  public Map<String, Long> getAssetV2MapForTest() {
    return getAssetMapV2();
  }

  /*************************** end asset ****************************************/

  public void addAllLatestAssetOperationTimeV2(Map<String, Long> map) {
    this.account = this.account.toBuilder().putAllLatestAssetOperationTimeV2(map).build();
  }

  public Map<String, Long> getLatestAssetOperationTimeMap() {
    return this.account.getLatestAssetOperationTimeMap();
  }

  public Map<String, Long> getLatestAssetOperationTimeMapV2() {
    return this.account.getLatestAssetOperationTimeV2Map();
  }

  public long getLatestAssetOperationTime(String assetName) {
    return this.account.getLatestAssetOperationTimeOrDefault(assetName, 0);
  }

  public long getLatestAssetOperationTimeV2(String assetName) {
    return this.account.getLatestAssetOperationTimeV2OrDefault(assetName, 0);
  }

  public void putLatestAssetOperationTimeMap(String key, Long value) {
    this.account = this.account.toBuilder().putLatestAssetOperationTime(key, value).build();
  }

  public void putLatestAssetOperationTimeMapV2(String key, Long value) {
    this.account = this.account.toBuilder().putLatestAssetOperationTimeV2(key, value).build();
  }

  public int getFrozenCount() {
    return getInstance().getFrozenCount();
  }

  public List<Frozen> getFrozenList() {
    return getInstance().getFrozenList();
  }

  public long getFrozenBalance() {
    List<Frozen> frozenList = getFrozenList();
    final long[] frozenBalance = {0};
    frozenList.forEach(frozen -> frozenBalance[0] = Long.sum(frozenBalance[0],
        frozen.getFrozenBalance()));
    return frozenBalance[0];
  }

  public long getFrozenV2BalanceForBandwidth() {
    List<FreezeV2> frozenList = getFrozenV2List();
    if (frozenList.isEmpty()) {
      return 0;
    }
    return frozenList.stream().filter(o -> o.getType() == BANDWIDTH)
            .mapToLong(FreezeV2::getAmount).sum();
  }

  public long getAllFrozenBalanceForBandwidth() {
    return getFrozenBalance() + getAcquiredDelegatedFrozenBalanceForBandwidth()
        + getFrozenV2BalanceForBandwidth() + getAcquiredDelegatedFrozenV2BalanceForBandwidth();
  }

  public int getFrozenSupplyCount() {
    return getInstance().getFrozenSupplyCount();
  }

  public List<Frozen> getFrozenSupplyList() {
    return getInstance().getFrozenSupplyList();
  }

  public long getFrozenSupplyBalance() {
    List<Frozen> frozenSupplyList = getFrozenSupplyList();
    final long[] frozenSupplyBalance = {0};
    frozenSupplyList.forEach(frozen -> frozenSupplyBalance[0] = Long.sum(frozenSupplyBalance[0],
        frozen.getFrozenBalance()));
    return frozenSupplyBalance[0];
  }

  public ByteString getAssetIssuedName() {
    return getInstance().getAssetIssuedName();
  }

  public void setAssetIssuedName(byte[] nameKey) {
    ByteString assetIssuedName = ByteString.copyFrom(nameKey);
    this.account = this.account.toBuilder().setAssetIssuedName(assetIssuedName).build();
  }

  public ByteString getAssetIssuedID() {
    return getInstance().getAssetIssuedID();
  }

  public void setAssetIssuedID(byte[] id) {
    ByteString assetIssuedID = ByteString.copyFrom(id);
    this.account = this.account.toBuilder().setAssetIssuedID(assetIssuedID).build();
  }

  public long getAllowance() {
    return getInstance().getAllowance();
  }

  public void setAllowance(long allowance) {
    this.account = this.account.toBuilder().setAllowance(allowance).build();
  }

  public long getLatestWithdrawTime() {
    return getInstance().getLatestWithdrawTime();
  }

  //for test only
  public void setLatestWithdrawTime(long latestWithdrawTime) {
    this.account = this.account.toBuilder()
        .setLatestWithdrawTime(latestWithdrawTime)
        .build();
  }

  public boolean getIsWitness() {
    return getInstance().getIsWitness();
  }

  public void setIsWitness(boolean isWitness) {
    this.account = this.account.toBuilder().setIsWitness(isWitness).build();
  }

  public boolean getIsCommittee() {
    return getInstance().getIsCommittee();
  }

  public void setIsCommittee(boolean isCommittee) {
    this.account = this.account.toBuilder().setIsCommittee(isCommittee).build();
  }

  public void setFrozenForBandwidth(long frozenBalance, long expireTime) {
    Frozen newFrozen = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(expireTime)
        .build();

    long frozenCount = getFrozenCount();
    if (frozenCount == 0) {
      setInstance(getInstance().toBuilder()
          .addFrozen(newFrozen)
          .build());
    } else {
      setInstance(getInstance().toBuilder()
          .setFrozen(0, newFrozen)
          .build()
      );
    }
  }

  //set FrozenBalanceForBandwidth
  //for test only
  public void setFrozen(long frozenBalance, long expireTime) {
    Frozen newFrozen = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(expireTime)
        .build();

    this.account = this.account.toBuilder()
        .addFrozen(newFrozen)
        .build();
  }

  public long getNetUsage() {
    return this.account.getNetUsage();
  }

  public long getUsage(ResourceCode resourceCode) {
    if (resourceCode == BANDWIDTH) {
      return this.account.getNetUsage();
    } else {
      return this.account.getAccountResource().getEnergyUsage();
    }
  }

  public void setNetUsage(long netUsage) {
    this.account = this.account.toBuilder()
        .setNetUsage(netUsage).build();
  }

  public AccountResource getAccountResource() {
    return this.account.getAccountResource();
  }

  public void setFrozenForEnergy(long newFrozenBalanceForEnergy, long time) {
    Frozen newFrozenForEnergy = Frozen.newBuilder()
        .setFrozenBalance(newFrozenBalanceForEnergy)
        .setExpireTime(time)
        .build();

    AccountResource newAccountResource = getAccountResource().toBuilder()
        .setFrozenBalanceForEnergy(newFrozenForEnergy).build();

    this.account = this.account.toBuilder()
        .setAccountResource(newAccountResource)
        .build();
  }

  public long getEnergyFrozenBalance() {
    return this.account.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance();
  }

  public long getFrozenV2BalanceForEnergy() {
    List<FreezeV2> frozenList = getFrozenV2List();
    if (frozenList.isEmpty()) {
      return 0;
    }
    return frozenList.stream().filter(o -> o.getType() == ENERGY)
            .mapToLong(FreezeV2::getAmount).sum();
  }

  public boolean oldTronPowerIsNotInitialized() {
    return this.account.getOldTronPower() == 0;
  }

  public boolean oldTronPowerIsInvalid() {
    return this.account.getOldTronPower() == -1;
  }

  public void initializeOldTronPower() {
    long value = getTronPower();
    if (value == 0) {
      value = -1;
    }
    setInstance(getInstance().toBuilder()
        .setOldTronPower(value)
        .build());
  }

  public void invalidateOldTronPower() {
    setInstance(getInstance().toBuilder()
        .setOldTronPower(-1)
        .build());
  }


  public void setOldTronPower(long value) {
    setInstance(getInstance().toBuilder()
        .setOldTronPower(value)
        .build());
  }

  public void setFrozenForTronPower(long frozenBalance, long expireTime) {
    Frozen newFrozen = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(expireTime)
        .build();

    setInstance(getInstance().toBuilder()
        .setTronPower(newFrozen)
        .build());
  }

  public void addFrozenForTronPowerV2(long balance) {
    this.addFrozenBalanceForResource(TRON_POWER, balance);
  }

  public long getTronPowerFrozenBalance() {
    return this.account.getTronPower().getFrozenBalance();
  }

  public long getTronPowerFrozenV2Balance() {
    return getFrozenV2List().stream().filter(o-> o.getType() == TRON_POWER)
            .mapToLong(FreezeV2::getAmount).sum();
  }

  public long getEnergyUsage() {
    return this.account.getAccountResource().getEnergyUsage();
  }

  public void setEnergyUsage(long energyUsage) {
    this.account = this.account.toBuilder()
        .setAccountResource(
            this.account.getAccountResource().toBuilder().setEnergyUsage(energyUsage).build())
        .build();
  }

  public long getAllFrozenBalanceForEnergy() {
    return getEnergyFrozenBalance() + getAcquiredDelegatedFrozenBalanceForEnergy()
        + getFrozenV2BalanceForEnergy() + getAcquiredDelegatedFrozenV2BalanceForEnergy();
  }

  public long getLatestConsumeTimeForEnergy() {
    return this.account.getAccountResource().getLatestConsumeTimeForEnergy();
  }

  public void setLatestConsumeTimeForEnergy(long latestTime) {
    this.account = this.account.toBuilder()
        .setAccountResource(
            this.account.getAccountResource().toBuilder().setLatestConsumeTimeForEnergy(latestTime)
                .build()).build();
  }

  public long getFreeNetUsage() {
    return this.account.getFreeNetUsage();
  }

  public void setFreeNetUsage(long freeNetUsage) {
    this.account = this.account.toBuilder().setFreeNetUsage(freeNetUsage).build();
  }

  public void addAllFreeAssetNetUsageV2(Map<String, Long> map) {
    this.account = this.account.toBuilder().putAllFreeAssetNetUsageV2(map).build();
  }

  public long getFreeAssetNetUsage(String assetName) {
    return this.account.getFreeAssetNetUsageOrDefault(assetName, 0);
  }

  public long getFreeAssetNetUsageV2(String assetName) {
    return this.account.getFreeAssetNetUsageV2OrDefault(assetName, 0);
  }

  public Map<String, Long> getAllFreeAssetNetUsage() {
    return this.account.getFreeAssetNetUsageMap();
  }

  public Map<String, Long> getAllFreeAssetNetUsageV2() {
    return this.account.getFreeAssetNetUsageV2Map();
  }

  public void putFreeAssetNetUsage(String s, long freeAssetNetUsage) {
    this.account = this.account.toBuilder()
        .putFreeAssetNetUsage(s, freeAssetNetUsage).build();
  }

  public void putFreeAssetNetUsageV2(String s, long freeAssetNetUsage) {
    this.account = this.account.toBuilder()
        .putFreeAssetNetUsageV2(s, freeAssetNetUsage).build();
  }

  public long getStorageLimit() {
    return this.account.getAccountResource().getStorageLimit();
  }

  public void setStorageLimit(long limit) {
    AccountResource accountResource = this.account.getAccountResource();
    accountResource = accountResource.toBuilder().setStorageLimit(limit).build();

    this.account = this.account.toBuilder()
        .setAccountResource(accountResource)
        .build();
  }

  public long getStorageUsage() {
    return this.account.getAccountResource().getStorageUsage();
  }

  public void setStorageUsage(long usage) {
    AccountResource accountResource = this.account.getAccountResource();
    accountResource = accountResource.toBuilder().setStorageUsage(usage).build();

    this.account = this.account.toBuilder()
        .setAccountResource(accountResource)
        .build();
  }

  public long getStorageLeft() {
    return getStorageLimit() - getStorageUsage();
  }

  public long getLatestExchangeStorageTime() {
    return this.account.getAccountResource().getLatestExchangeStorageTime();
  }

  public void setLatestExchangeStorageTime(long time) {
    AccountResource accountResource = this.account.getAccountResource();
    accountResource = accountResource.toBuilder().setLatestExchangeStorageTime(time).build();

    this.account = this.account.toBuilder()
        .setAccountResource(accountResource)
        .build();
  }

  public void addStorageUsage(long storageUsage) {
    if (storageUsage <= 0) {
      return;
    }
    AccountResource accountResource = this.account.getAccountResource();
    accountResource = accountResource.toBuilder()
        .setStorageUsage(accountResource.getStorageUsage() + storageUsage).build();

    this.account = this.account.toBuilder()
        .setAccountResource(accountResource)
        .build();
  }

  public Permission getPermissionById(int id) {
    if (id == 0) {
      if (this.account.hasOwnerPermission()) {
        return this.account.getOwnerPermission();
      }
      return getDefaultPermission(this.account.getAddress());
    }
    if (id == 1) {
      if (this.account.hasWitnessPermission()) {
        return this.account.getWitnessPermission();
      }
      return null;
    }
    for (Permission permission : this.account.getActivePermissionList()) {
      if (id == permission.getId()) {
        return permission;
      }
    }
    return null;
  }

  public void updatePermissions(Permission owner, Permission witness, List<Permission> actives) {
    Builder builder = this.account.toBuilder();

    owner = owner.toBuilder().setId(0).build();
    builder.setOwnerPermission(owner);
    if (witness != null && builder.getIsWitness()) {
      witness = witness.toBuilder().setId(1).build();
      builder.setWitnessPermission(witness);
    }

    builder.clearActivePermission();
    if (actives != null) {
      for (int i = 0; i < actives.size(); i++) {
        Permission permission = actives.get(i).toBuilder().setId(i + 2).build();
        builder.addActivePermission(permission);
      }
    }

    this.account = builder.build();
  }

  public void updateAccountType(AccountType accountType) {
    this.account = this.account.toBuilder().setType(accountType).build();
  }

  // just for vm create2 instruction
  public void clearDelegatedResource() {
    Builder builder = account.toBuilder();
    AccountResource newAccountResource = getAccountResource().toBuilder()
        .setAcquiredDelegatedFrozenBalanceForEnergy(0L)
            .setAcquiredDelegatedFrozenV2BalanceForEnergy(0L)
            .build();
    builder.setAccountResource(newAccountResource);
    builder.setAcquiredDelegatedFrozenBalanceForBandwidth(0L)
            .setAcquiredDelegatedFrozenV2BalanceForBandwidth(0L);
    this.account = builder.build();
  }

  public void importAsset(byte[] key) {
    this.account = AssetUtil.importAsset(this.account, key);
  }

  public void importAllAsset() {
    if (!flag) {
      this.account = AssetUtil.importAllAsset(this.account);
      flag = true;
    }
  }

  public void addUnfrozenV2(UnFreezeV2 unfrozenV2) {
    if (Objects.isNull(unfrozenV2)) {
      return;
    }
    this.account = this.account.toBuilder().addUnfrozenV2(unfrozenV2).build();
  }

  public void addAllUnfrozenV2(List<UnFreezeV2> unFreezeV2List) {
    if (CollectionUtils.isEmpty(unFreezeV2List)) {
      return;
    }
    this.account = this.account.toBuilder().addAllUnfrozenV2(unFreezeV2List).build();
  }

  public void clearUnfrozenV2() {
    this.account = this.account.toBuilder().clearUnfrozenV2().build();
  }

  public void clearFrozenV2() {
    this.account = this.account.toBuilder().clearFrozenV2().build();
  }

  public void setNewWindowSize(ResourceCode resourceCode, long newWindowSize) {
    if (resourceCode == BANDWIDTH) {
      this.account = this.account.toBuilder().setNetWindowSize(newWindowSize).build();
    } else {
      this.account = this.account.toBuilder().setAccountResource(this.account.getAccountResource()
              .toBuilder().setEnergyWindowSize(newWindowSize).build()).build();
    }
  }

  public long getWindowSize(ResourceCode resourceCode) {
    long windowSize;
    if (resourceCode == BANDWIDTH) {
      windowSize = this.account.getNetWindowSize();
    } else {
      windowSize = this.account.getAccountResource().getEnergyWindowSize();
    }
    return windowSize == 0 ? WINDOW_SIZE_MS / BLOCK_PRODUCED_INTERVAL : windowSize;
  }

  public long getLastConsumeTime(ResourceCode resourceCode) {
    if (resourceCode == BANDWIDTH) {
      return this.account.getLatestConsumeTime();
    } else {
      return this.account.getAccountResource().getLatestConsumeTimeForEnergy();
    }
  }

  public long getFrozenV2BalanceWithDelegated(ResourceCode resourceCode) {
    if (resourceCode == BANDWIDTH) {
      return getFrozenV2BalanceForBandwidth() + getDelegatedFrozenV2BalanceForBandwidth();
    } else {
      return getFrozenV2BalanceForEnergy() + getDelegatedFrozenV2BalanceForEnergy();
    }
  }

}
