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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Contract.AccountUpdateContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.AccountResource;
import org.tron.protos.Protocol.Account.Frozen;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Vote;

@Slf4j
public class AccountCapsule implements ProtoCapsule<Account>, Comparable<AccountCapsule> {

  private Account account;


  @Override
  public int compareTo(AccountCapsule otherObject) {
    return Long.compare(otherObject.getBalance(), this.getBalance());
  }

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
  public AccountCapsule(final AccountCreateContract contract, long createTime) {
    this.account = Account.newBuilder()
        .setType(contract.getType())
        .setAddress(contract.getAccountAddress())
        .setTypeValue(contract.getTypeValue())
        .setCreateTime(createTime)
        .build();
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
      AccountType accountType, long createTime) {
    this.account = Account.newBuilder()
        .setType(accountType)
        .setAddress(address)
        .setCreateTime(createTime)
        .build();
  }

  public AccountCapsule(Account account) {
    this.account = account;
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

  public ByteString getAccountId() {
    return this.account.getAccountId();
  }

  public long getBalance() {
    return this.account.getBalance();
  }

  public long getLatestOperationTime() {
    return this.account.getLatestOprationTime();
  }

  public void setLatestOperationTime(long latest_time) {
    this.account = this.account.toBuilder().setLatestOprationTime(latest_time).build();
  }

  public long getLatestConsumeTime() {
    return this.account.getLatestConsumeTime();
  }

  public void setLatestConsumeTime(long latest_time) {
    this.account = this.account.toBuilder().setLatestConsumeTime(latest_time).build();
  }

  public long getLatestConsumeFreeTime() {
    return this.account.getLatestConsumeFreeTime();
  }

  public void setLatestConsumeFreeTime(long latest_time) {
    this.account = this.account.toBuilder().setLatestConsumeFreeTime(latest_time).build();
  }

  public void setBalance(long balance) {
    this.account = this.account.toBuilder().setBalance(balance).build();
  }

  public void setAllowance(long allowance) {
    this.account = this.account.toBuilder().setAllowance(allowance).build();
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

  public void clearVotes() {
    this.account = this.account.toBuilder()
        .clearVotes()
        .build();
  }

  /**
   * get votes.
   */
  public List<Vote> getVotesList() {
    if (this.account.getVotesList() != null) {
      return this.account.getVotesList();
    } else {
      return Lists.newArrayList();
    }
  }

  //tp:Tron_Power
  public long getTronPower() {
    long tp = 0;
    //long now = Time.getCurrentMillis();
    for (int i = 0; i < account.getFrozenCount(); ++i) {
      tp += account.getFrozen(i).getFrozenBalance();
    }

    tp += account.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance();
    return tp;
  }

  /**
   * asset balance enough
   */
  public boolean assetBalanceEnough(byte[] key, long amount) {
    Map<String, Long> assetMap = this.account.getAssetMap();
    String nameKey = ByteArray.toStr(key);
    Long currentAmount = assetMap.get(nameKey);

    return amount > 0 && null != currentAmount && amount <= currentAmount;
  }


  /**
   * reduce asset amount.
   */
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

  /**
   * add asset amount.
   */
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

  /**
   * set account name
   */
  public void setAccountName(byte[] name) {
    this.account = this.account.toBuilder().setAccountName(ByteString.copyFrom(name)).build();
  }

  /**
   * set account id
   */
  public void setAccountId(byte[] id) {
    this.account = this.account.toBuilder().setAccountId(ByteString.copyFrom(id)).build();
  }

  /**
   * add asset.
   */
  public boolean addAsset(byte[] key, long value) {
    Map<String, Long> assetMap = this.account.getAssetMap();
    String nameKey = ByteArray.toStr(key);
    if (!assetMap.isEmpty()) {
      if (assetMap.containsKey(nameKey)) {
        return false;
      }
    }

    this.account = this.account.toBuilder().putAsset(nameKey, value).build();

    return true;
  }

  /**
   * add asset.
   */
  public Map<String, Long> getAssetMap() {
    Map<String, Long> assetMap = this.account.getAssetMap();
    if (assetMap.isEmpty()) {
      assetMap = Maps.newHashMap();
    }

    return assetMap;
  }


  public long getLatestAssetOperationTime(String assetName) {
    return this.account.getLatestAssetOperationTimeOrDefault(assetName, 0);
  }

  public void putLatestAssetOperationTimeMap(String key, Long value) {
    this.account = this.account.toBuilder().putLatestAssetOperationTime(key, value).build();
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

  public long getAllowance() {
    return getInstance().getAllowance();
  }

  public long getLatestWithdrawTime() {
    return getInstance().getLatestWithdrawTime();
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

  //for test only
  public void setLatestWithdrawTime(long latestWithdrawTime) {
    this.account = this.account.toBuilder()
        .setLatestWithdrawTime(latestWithdrawTime)
        .build();
  }

  public long getNetUsage() {
    return this.account.getNetUsage();
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

  public long getEnergyUsage() {
    return this.account.getAccountResource().getEnergyUsage();
  }

  public void setEnergyUsage(long energyUsage) {
    this.account = this.account.toBuilder()
        .setAccountResource(
            this.account.getAccountResource().toBuilder().setEnergyUsage(energyUsage).build())
        .build();
  }

  public void setLatestConsumeTimeForEnergy(long latest_time) {
    this.account = this.account.toBuilder()
        .setAccountResource(
            this.account.getAccountResource().toBuilder().setLatestConsumeTimeForEnergy(latest_time)
                .build()).build();
  }

  public long getLatestConsumeTimeForEnergy() {
    return this.account.getAccountResource().getLatestConsumeTimeForEnergy();
  }

  public long getFreeNetUsage() {
    return this.account.getFreeNetUsage();
  }

  public void setFreeNetUsage(long freeNetUsage) {
    this.account = this.account.toBuilder()
        .setFreeNetUsage(freeNetUsage).build();
  }

  public long getFreeAssetNetUsage(String assetName) {
    return this.account.getFreeAssetNetUsageOrDefault(assetName, 0);
  }

  public Map<String, Long> getAllFreeAssetNetUsage() {
    return this.account.getFreeAssetNetUsageMap();
  }

  public void putFreeAssetNetUsage(String s, long freeAssetNetUsage) {
    this.account = this.account.toBuilder()
        .putFreeAssetNetUsage(s, freeAssetNetUsage).build();
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

  public long getStorageLeft() {
    return getStorageLimit() - getStorageUsage();
  }

  public void setStorageUsage(long usage) {
    AccountResource accountResource = this.account.getAccountResource();
    accountResource = accountResource.toBuilder().setStorageUsage(usage).build();

    this.account = this.account.toBuilder()
        .setAccountResource(accountResource)
        .build();
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
}
