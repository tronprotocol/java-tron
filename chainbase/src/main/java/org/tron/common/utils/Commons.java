package org.tron.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.parameter.CommonParameter;
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

  public static final int ASSET_ISSUE_COUNT_LIMIT_MAX = 1000;

  public static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
        decodeData);
    byte[] hash1 = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
        hash0);
    if (hash1[0] == decodeCheck[decodeData.length] &&
        hash1[1] == decodeCheck[decodeData.length + 1] &&
        hash1[2] == decodeCheck[decodeData.length + 2] &&
        hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
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

    if (!DecodeUtil.addressValid(address)) {
      return null;
    }

    return address;
  }

  public static void adjustBalance(AccountStore accountStore, byte[] accountAddress, long amount)
      throws BalanceInsufficientException {
    AccountCapsule account = accountStore.getUnchecked(accountAddress);
    adjustBalance(accountStore, account, amount);
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
          StringUtil.createReadableString(account.createDbKey()) + " insufficient balance");
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
