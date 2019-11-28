package org.tron.common.utils;

import static org.tron.common.utils.DecodeUtil.addressPreFixByte;
import static org.tron.core.Constant.ADD_PRE_FIX_BYTE_MAINNET;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.ExchangeStore;
import org.tron.core.store.ExchangeV2Store;

@Slf4j(topic = "Commons")
public class Commons {

  public static final int ADDRESS_SIZE = 42;
  public static final int ASSET_ISSUE_COUNT_LIMIT_MAX = 1000;

  public static byte[] clone(byte[] value) {
    byte[] clone = new byte[value.length];
    System.arraycopy(value, 0, clone, 0, value.length);
    return clone;
  }

  private static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(decodeData);
    byte[] hash1 = Sha256Hash.hash(hash0);
    if (hash1[0] == decodeCheck[decodeData.length] &&
        hash1[1] == decodeCheck[decodeData.length + 1] &&
        hash1[2] == decodeCheck[decodeData.length + 2] &&
        hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  public static boolean addressValid(byte[] address) {
    if (ArrayUtils.isEmpty(address)) {
      logger.warn("Warning: Address is empty !!");
      return false;
    }
    if (address.length != ADDRESS_SIZE / 2) {
      logger.warn(
          "Warning: Address length need " + ADDRESS_SIZE + " but " + address.length
              + " !!");
      return false;
    }

    if (address[0] != addressPreFixByte) {
      logger.warn("Warning: Address need prefix with " + addressPreFixByte + " but "
          + address[0] + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  public static byte[] decodeFromBase58Check(String addressBase58) {
    if (StringUtils.isEmpty(addressBase58)) {
      logger.warn("Warning: Address is empty !!");
      return null;
    }
    byte[] address = decode58Check(addressBase58);
    if (address == null) {
      return null;
    }

    if (!addressValid(address)) {
      return null;
    }

    return address;
  }

  public static void adjustBalance(AccountStore accountStore, byte[] accountAddress, long amount)
      throws BalanceInsufficientException {
    AccountCapsule account = accountStore.getUnchecked(accountAddress);
    adjustBalance(accountStore, account, amount);
  }

  public static String createReadableString(byte[] bytes) {
    return ByteArray.toHexString(bytes);
  }

  /**
   * judge balance.
   */
  public static void adjustBalance(AccountStore accountStore, AccountCapsule account, long amount)
      throws BalanceInsufficientException {

    long balance = account.getBalance();
    if (amount == 0) {
      return;
    }

    if (amount < 0 && balance < -amount) {
      throw new BalanceInsufficientException(
          createReadableString(account.createDbKey()) + " insufficient balance");
    }
    account.setBalance(Math.addExact(balance, amount));
    accountStore.put(account.getAddress().toByteArray(), account);
  }

  public static ExchangeStore getExchangeStoreFinal(DynamicPropertiesStore dynamicPropertiesStore,
      ExchangeStore exchangeStore,
      ExchangeV2Store exchangeV2Store) {
    if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
      return exchangeStore;
    } else {
      return exchangeV2Store;
    }
  }

  public static void putExchangeCapsule(ExchangeCapsule exchangeCapsule,
      DynamicPropertiesStore dynamicPropertiesStore, ExchangeStore exchangeStore,
      ExchangeV2Store exchangeV2Store, AssetIssueStore assetIssueStore) {
    if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
      exchangeStore.put(exchangeCapsule.createDbKey(), exchangeCapsule);
      ExchangeCapsule exchangeCapsuleV2 = new ExchangeCapsule(exchangeCapsule.getData());
      exchangeCapsuleV2.resetTokenWithID(assetIssueStore, dynamicPropertiesStore);
      exchangeV2Store.put(exchangeCapsuleV2.createDbKey(), exchangeCapsuleV2);
    } else {
      exchangeV2Store.put(exchangeCapsule.createDbKey(), exchangeCapsule);
    }
  }

  public static AssetIssueStore getAssetIssueStoreFinal(
      DynamicPropertiesStore dynamicPropertiesStore,
      AssetIssueStore assetIssueStore, AssetIssueV2Store assetIssueV2Store) {
    if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
      return assetIssueStore;
    } else {
      return assetIssueV2Store;
    }
  }

  public static void adjustAssetBalanceV2(AccountCapsule account, String AssetID, long amount,
      AccountStore accountStore, AssetIssueStore assetIssueStore,
      DynamicPropertiesStore dynamicPropertiesStore)
      throws BalanceInsufficientException {
    if (amount < 0) {
      if (!account.reduceAssetAmountV2(AssetID.getBytes(), -amount, dynamicPropertiesStore,
          assetIssueStore)) {
        throw new BalanceInsufficientException("reduceAssetAmount failed !");
      }
    } else if (amount > 0 &&
        !account.addAssetAmountV2(AssetID.getBytes(), amount, dynamicPropertiesStore,
            assetIssueStore)) {
      throw new BalanceInsufficientException("addAssetAmount failed !");
    }
    accountStore.put(account.getAddress().toByteArray(), account);
  }

  public static void adjustTotalShieldedPoolValue(long valueBalance,
      DynamicPropertiesStore dynamicPropertiesStore) throws BalanceInsufficientException {
    long totalShieldedPoolValue = Math
        .subtractExact(dynamicPropertiesStore.getTotalShieldedPoolValue(), valueBalance);
    if (totalShieldedPoolValue < 0) {
      throw new BalanceInsufficientException("Total shielded pool value can not below 0");
    }
    dynamicPropertiesStore.saveTotalShieldedPoolValue(totalShieldedPoolValue);
  }

  public static void adjustAssetBalanceV2(byte[] accountAddress, String AssetID, long amount
      , AccountStore accountStore, AssetIssueStore assetIssueStore,
      DynamicPropertiesStore dynamicPropertiesStore)
      throws BalanceInsufficientException {
    AccountCapsule account = accountStore.getUnchecked(accountAddress);
    adjustAssetBalanceV2(account, AssetID, amount, accountStore, assetIssueStore,
        dynamicPropertiesStore);
  }
}
