package org.tron.core.actuator.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.tron.core.capsule.utils.TransactionUtil.isNumber;
import static org.tron.core.config.Parameter.ChainConstant.DELEGATE_COST_BASE_SIZE;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.utils.TransactionUtil;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;


@Slf4j(topic = "capsule")
public class TransactionUtilTest extends BaseTest {

  private static final String dbPath = "output_transactionUtil_test";
  private static final String OWNER_ADDRESS;

  static {
    OWNER_ADDRESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
  }


  @Test
  public void validAccountNameCheck() {
    String account = "";
    assertTrue(TransactionUtil.validAccountName(account.getBytes(StandardCharsets.UTF_8)));
    for (int i = 0; i < 200; i++) {
      account += (char) ('a' + (i % 26));
    }
    assertTrue(TransactionUtil.validAccountName(account.getBytes(StandardCharsets.UTF_8)));
    account += 'z';
    assertFalse(TransactionUtil.validAccountName(account.getBytes(StandardCharsets.UTF_8)));

  }

  @Test
  public void validAccountIdCheck() {
    String accountId = "";
    assertFalse(TransactionUtil.validAccountId(accountId.getBytes(StandardCharsets.UTF_8)));
    for (int i = 0; i < 7; i++) {
      accountId += (char) ('a' + (i % 26));
    }
    assertFalse(TransactionUtil.validAccountId(accountId.getBytes(StandardCharsets.UTF_8)));
    for (int i = 0; i < 26; i++) {
      accountId += (char) ('a' + (i % 26));
    }
    assertFalse(TransactionUtil.validAccountId(accountId.getBytes(StandardCharsets.UTF_8)));
    accountId = "ab  cdefghij";
    assertFalse(TransactionUtil.validAccountId(accountId.getBytes(StandardCharsets.UTF_8)));
    accountId = (char) 128 + "abcdefjijk" + (char) 129;
    assertFalse(TransactionUtil.validAccountId(accountId.getBytes(StandardCharsets.UTF_8)));
    accountId = "";
    for (int i = 0; i < 30; i++) {
      accountId += (char) ('a' + (i % 26));
    }
    assertTrue(TransactionUtil.validAccountId(accountId.getBytes(StandardCharsets.UTF_8)));

  }

  @Test
  public void validAssetNameCheck() {
    String assetName = "";
    assertFalse(TransactionUtil.validAssetName(assetName.getBytes(StandardCharsets.UTF_8)));
    for (int i = 0; i < 33; i++) {
      assetName += (char) ('a' + (i % 26));
    }
    assertFalse(TransactionUtil.validAssetName(assetName.getBytes(StandardCharsets.UTF_8)));
    assetName = "ab  cdefghij";
    assertFalse(TransactionUtil.validAssetName(assetName.getBytes(StandardCharsets.UTF_8)));
    assetName = (char) 128 + "abcdefjijk" + (char) 129;
    assertFalse(TransactionUtil.validAssetName(assetName.getBytes(StandardCharsets.UTF_8)));
    assetName = "";
    for (int i = 0; i < 20; i++) {
      assetName += (char) ('a' + (i % 26));
    }
    assertTrue(TransactionUtil.validAssetName(assetName.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void validTokenAbbrNameCheck() {
    String abbrName = "";
    assertFalse(TransactionUtil.validTokenAbbrName(abbrName.getBytes(StandardCharsets.UTF_8)));
    for (int i = 0; i < 6; i++) {
      abbrName += (char) ('a' + (i % 26));
    }
    assertFalse(TransactionUtil.validTokenAbbrName(abbrName.getBytes(StandardCharsets.UTF_8)));
    abbrName = "a bd";
    assertFalse(TransactionUtil.validTokenAbbrName(abbrName.getBytes(StandardCharsets.UTF_8)));
    abbrName = "a" + (char) 129 + 'f';
    assertFalse(TransactionUtil.validTokenAbbrName(abbrName.getBytes(StandardCharsets.UTF_8)));
    abbrName = "";
    for (int i = 0; i < 5; i++) {
      abbrName += (char) ('a' + (i % 26));
    }
    assertTrue(TransactionUtil.validTokenAbbrName(abbrName.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void isNumberCheck() {
    String number = "";
    assertFalse(isNumber(number.getBytes(StandardCharsets.UTF_8)));

    number = "123df34";
    assertFalse(isNumber(number.getBytes(StandardCharsets.UTF_8)));
    number = "013";
    assertFalse(isNumber(number.getBytes(StandardCharsets.UTF_8)));
    number = "24";
    assertTrue(isNumber(number.getBytes(StandardCharsets.UTF_8)));
  }



  @Test
  public void testEstimateConsumeBandWidthSizeOld() {
    dbManager.getDynamicPropertiesStore().saveAllowCreationOfContracts(1L);
    ChainBaseManager chainBaseManager =  dbManager.getChainBaseManager();
    long balance = 1000_000L;

    AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), Protocol.AccountType.Normal,
        balance);
    ownerCapsule.addFrozenBalanceForBandwidthV2(balance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
    ownerCapsule = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    long estimateConsumeBandWidthSize1 =  TransactionUtil.estimateConsumeBandWidthSizeOld(
        ownerCapsule, chainBaseManager);
    Assert.assertEquals(277, estimateConsumeBandWidthSize1);

    balance = 1000_000_000L;
    ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), Protocol.AccountType.Normal,
        balance);
    ownerCapsule.addFrozenBalanceForBandwidthV2(balance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
    long estimateConsumeBandWidthSize2 =  TransactionUtil.estimateConsumeBandWidthSizeOld(
        ownerCapsule, chainBaseManager);
    Assert.assertEquals(279, estimateConsumeBandWidthSize2);

    balance = 1000_000_000_000L;
    ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), Protocol.AccountType.Normal,
        balance);
    ownerCapsule.addFrozenBalanceForBandwidthV2(balance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
    long estimateConsumeBandWidthSize3 =  TransactionUtil.estimateConsumeBandWidthSizeOld(
        ownerCapsule, chainBaseManager);
    Assert.assertEquals(280, estimateConsumeBandWidthSize3);

    balance = 1000_000_000_000_000L;
    ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), Protocol.AccountType.Normal,
        balance);
    ownerCapsule.addFrozenBalanceForBandwidthV2(balance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
    long estimateConsumeBandWidthSize4 =  TransactionUtil.estimateConsumeBandWidthSizeOld(
        ownerCapsule, chainBaseManager);
    Assert.assertEquals(282, estimateConsumeBandWidthSize4);
  }


  @Test
  public void testEstimateConsumeBandWidthSizeNew() {
    long balance = 1000_000L;

    long estimateConsumeBandWidthSize1 =  TransactionUtil.estimateConsumeBandWidthSize(balance);
    Assert.assertEquals(277, estimateConsumeBandWidthSize1);

    balance = 1000_000_000L;
    long estimateConsumeBandWidthSize2 =  TransactionUtil.estimateConsumeBandWidthSize(balance);
    Assert.assertEquals(279, estimateConsumeBandWidthSize2);

    balance = 1000_000_000_000L;
    long estimateConsumeBandWidthSize3 =  TransactionUtil.estimateConsumeBandWidthSize(balance);
    Assert.assertEquals(280, estimateConsumeBandWidthSize3);

    balance = 1000_000_000_000_000L;
    long estimateConsumeBandWidthSize4 =  TransactionUtil.estimateConsumeBandWidthSize(balance);
    Assert.assertEquals(282, estimateConsumeBandWidthSize4);
  }



  @Test
  public void testEstimateConsumeBandWidthSize() {
    dbManager.getDynamicPropertiesStore().saveAllowCreationOfContracts(1L);
    ChainBaseManager chainBaseManager =  dbManager.getChainBaseManager();
    long balance = 1000_000L;

    AccountCapsule ownerCapsule;
    long estimateConsumeBandWidthSizeOld;
    long estimateConsumeBandWidthSizeNew;

    for (int i = 0; i < 100; i++) {
      // old value is
      ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
          ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), Protocol.AccountType.Normal,
          balance);
      ownerCapsule.addFrozenBalanceForBandwidthV2(balance);
      dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
      estimateConsumeBandWidthSizeOld =  TransactionUtil.estimateConsumeBandWidthSizeOld(
          ownerCapsule, chainBaseManager);

      // new value is
      estimateConsumeBandWidthSizeNew =  TransactionUtil.estimateConsumeBandWidthSize(balance);

      System.out.println("balance:"
          + balance
          + ", estimateConsumeBandWidthSizeOld:"
          + estimateConsumeBandWidthSizeOld
          + ", estimateConsumeBandWidthSizeNew:"
          + estimateConsumeBandWidthSizeNew);
      // new value assert equal to old value
      Assert.assertEquals(estimateConsumeBandWidthSizeOld, estimateConsumeBandWidthSizeNew);

      // balance accumulated
      balance = balance * 10;
      if (balance < 0) {
        break;
      }
    }

  }

  @Test
  public void estimateConsumeBandWidthSizePositive() {
    long balance = 100;

    BalanceContract.DelegateResourceContract.Builder builder =
        BalanceContract.DelegateResourceContract.newBuilder()
        .setLock(true)
        .setBalance(balance);
    BalanceContract.DelegateResourceContract.Builder builder2 =
        BalanceContract.DelegateResourceContract.newBuilder()
        .setBalance(TRX_PRECISION);

    long expected = DELEGATE_COST_BASE_SIZE + Math.max(
        builder.build().getSerializedSize() - builder2.build().getSerializedSize(), 0L);

    long actual = TransactionUtil.estimateConsumeBandWidthSize(balance);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void estimateConsumeBandWidthSizeBoundary() {
    long balance = TRX_PRECISION;

    BalanceContract.DelegateResourceContract.Builder builder =
        BalanceContract.DelegateResourceContract.newBuilder()
        .setLock(true)
        .setBalance(balance);
    BalanceContract.DelegateResourceContract.Builder builder2 =
        BalanceContract.DelegateResourceContract.newBuilder()
        .setBalance(TRX_PRECISION);

    long expected = DELEGATE_COST_BASE_SIZE + Math.max(
        builder.build().getSerializedSize() - builder2.build().getSerializedSize(), 0L);

    long actual = TransactionUtil.estimateConsumeBandWidthSize(balance);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void estimateConsumeBandWidthSizeEdge() {
    long balance = TRX_PRECISION + 1;

    BalanceContract.DelegateResourceContract.Builder builder =
        BalanceContract.DelegateResourceContract.newBuilder()
        .setLock(true)
        .setBalance(balance);
    BalanceContract.DelegateResourceContract.Builder builder2 =
        BalanceContract.DelegateResourceContract.newBuilder()
        .setBalance(TRX_PRECISION);

    long expected = DELEGATE_COST_BASE_SIZE + Math.max(
        builder.build().getSerializedSize() - builder2.build().getSerializedSize(), 0L);

    long actual = TransactionUtil.estimateConsumeBandWidthSize(balance);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void estimateConsumeBandWidthSizeCorner() {
    long balance = Long.MAX_VALUE;

    BalanceContract.DelegateResourceContract.Builder builder =
        BalanceContract.DelegateResourceContract.newBuilder()
        .setLock(true)
        .setBalance(balance);
    BalanceContract.DelegateResourceContract.Builder builder2 =
        BalanceContract.DelegateResourceContract.newBuilder()
        .setBalance(TRX_PRECISION);

    long expected = DELEGATE_COST_BASE_SIZE + Math.max(
        builder.build().getSerializedSize() - builder2.build().getSerializedSize(), 0L);

    long actual = TransactionUtil.estimateConsumeBandWidthSize(balance);
    Assert.assertEquals(expected, actual);
  }

}
