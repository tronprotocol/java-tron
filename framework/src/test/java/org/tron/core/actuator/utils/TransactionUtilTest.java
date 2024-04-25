package org.tron.core.actuator.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.tron.core.capsule.utils.TransactionUtil.isNumber;
import static org.tron.core.config.Parameter.ChainConstant.DELEGATE_COST_BASE_SIZE;
import static org.tron.core.config.Parameter.ChainConstant.DELEGATE_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.core.utils.TransactionUtil.validAccountId;
import static org.tron.core.utils.TransactionUtil.validAccountName;
import static org.tron.core.utils.TransactionUtil.validAssetName;
import static org.tron.core.utils.TransactionUtil.validTokenAbbrName;

import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.utils.TransactionUtil;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.DelegateResourceContract;

@Slf4j(topic = "capsule")
public class TransactionUtilTest extends BaseTest {

  private static String OWNER_ADDRESS;

  /**
   * Init .
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath()}, Constant.TEST_CONF);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
  }

  @Before
  public void setUp() {
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(owner),
            AccountType.Normal,
            10_000_000_000L);
    ownerCapsule.setFrozenForBandwidth(1000000L, 1000000L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  // only for testing
  public static long consumeBandWidthSize(
      final TransactionCapsule transactionCapsule,
      ChainBaseManager chainBaseManager) {
    long bs;

    boolean supportVM = chainBaseManager.getDynamicPropertiesStore().supportVM();
    if (supportVM) {
      bs = transactionCapsule.getInstance().toBuilder().clearRet().build().getSerializedSize();
    } else {
      bs = transactionCapsule.getSerializedSize();
    }

    List<Transaction.Contract> contracts = transactionCapsule.getInstance().getRawData()
        .getContractList();
    for (Transaction.Contract contract : contracts) {
      if (contract.getType() == Transaction.Contract.ContractType.ShieldedTransferContract) {
        continue;
      }
      if (supportVM) {
        bs += Constant.MAX_RESULT_SIZE_IN_TX;
      }
    }

    return bs;
  }

  // only for testing
  public static long estimateConsumeBandWidthSize(final AccountCapsule ownerCapsule,
                                                  ChainBaseManager chainBaseManager) {
    DelegateResourceContract.Builder builder;
    if (chainBaseManager.getDynamicPropertiesStore().supportMaxDelegateLockPeriod()) {
      builder = DelegateResourceContract.newBuilder()
          .setLock(true)
          .setLockPeriod(chainBaseManager.getDynamicPropertiesStore().getMaxDelegateLockPeriod())
          .setBalance(ownerCapsule.getFrozenV2BalanceForBandwidth());
    } else {
      builder = DelegateResourceContract.newBuilder()
          .setLock(true)
          .setBalance(ownerCapsule.getFrozenV2BalanceForBandwidth());
    }
    TransactionCapsule fakeTransactionCapsule = new TransactionCapsule(builder.build(),
        ContractType.DelegateResourceContract);
    long size1 = consumeBandWidthSize(fakeTransactionCapsule, chainBaseManager);

    DelegateResourceContract.Builder builder2 = DelegateResourceContract.newBuilder()
        .setBalance(TRX_PRECISION);
    TransactionCapsule fakeTransactionCapsule2 = new TransactionCapsule(builder2.build(),
        ContractType.DelegateResourceContract);
    long size2 = consumeBandWidthSize(fakeTransactionCapsule2, chainBaseManager);
    long addSize = Math.max(size1 - size2, 0L);

    return DELEGATE_COST_BASE_SIZE + addSize;
  }

  // only for testing
  public static long estimateConsumeBandWidthSizeOld(
      final AccountCapsule ownerCapsule,
      ChainBaseManager chainBaseManager) {
    DelegateResourceContract.Builder builder = DelegateResourceContract.newBuilder()
        .setLock(true)
        .setBalance(ownerCapsule.getFrozenV2BalanceForBandwidth());
    TransactionCapsule fakeTransactionCapsule = new TransactionCapsule(builder.build(),
        ContractType.DelegateResourceContract);
    long size1 = consumeBandWidthSize(fakeTransactionCapsule, chainBaseManager);

    DelegateResourceContract.Builder builder2 = DelegateResourceContract.newBuilder()
        .setBalance(TRX_PRECISION);
    TransactionCapsule fakeTransactionCapsule2 = new TransactionCapsule(builder2.build(),
        ContractType.DelegateResourceContract);
    long size2 = consumeBandWidthSize(fakeTransactionCapsule2, chainBaseManager);
    long addSize = Math.max(size1 - size2, 0L);

    return DELEGATE_COST_BASE_SIZE + addSize;
  }

  @Test
  public void validAccountNameCheck() {
    StringBuilder account = new StringBuilder();
    assertTrue(validAccountName(account.toString().getBytes(StandardCharsets.UTF_8)));
    for (int i = 0; i < 200; i++) {
      account.append((char) ('a' + (i % 26)));
    }
    assertTrue(validAccountName(account.toString().getBytes(StandardCharsets.UTF_8)));
    account.append('z');
    assertFalse(validAccountName(account.toString().getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void validAccountIdCheck() {
    StringBuilder accountId = new StringBuilder();
    assertFalse(validAccountId(accountId.toString().getBytes(StandardCharsets.UTF_8)));
    for (int i = 0; i < 7; i++) {
      accountId.append((char) ('a' + (i % 26)));
    }
    assertFalse(validAccountId(accountId.toString().getBytes(StandardCharsets.UTF_8)));
    for (int i = 0; i < 26; i++) {
      accountId.append((char) ('a' + (i % 26)));
    }
    assertFalse(validAccountId(accountId.toString().getBytes(StandardCharsets.UTF_8)));
    accountId = new StringBuilder("ab  cdefghij");
    assertFalse(validAccountId(accountId.toString().getBytes(StandardCharsets.UTF_8)));
    accountId = new StringBuilder((char) 128 + "abcdefjijk" + (char) 129);
    assertFalse(validAccountId(accountId.toString().getBytes(StandardCharsets.UTF_8)));
    accountId = new StringBuilder();
    for (int i = 0; i < 30; i++) {
      accountId.append((char) ('a' + (i % 26)));
    }
    assertTrue(validAccountId(accountId.toString().getBytes(StandardCharsets.UTF_8)));

  }

  @Test
  public void validAssetNameCheck() {
    StringBuilder assetName = new StringBuilder();
    assertFalse(validAssetName(assetName.toString().getBytes(StandardCharsets.UTF_8)));
    for (int i = 0; i < 33; i++) {
      assetName.append((char) ('a' + (i % 26)));
    }
    assertFalse(validAssetName(assetName.toString().getBytes(StandardCharsets.UTF_8)));
    assetName = new StringBuilder("ab  cdefghij");
    assertFalse(validAssetName(assetName.toString().getBytes(StandardCharsets.UTF_8)));
    assetName = new StringBuilder((char) 128 + "abcdefjijk" + (char) 129);
    assertFalse(validAssetName(assetName.toString().getBytes(StandardCharsets.UTF_8)));
    assetName = new StringBuilder();
    for (int i = 0; i < 20; i++) {
      assetName.append((char) ('a' + (i % 26)));
    }
    assertTrue(validAssetName(assetName.toString().getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void validTokenAbbrNameCheck() {
    StringBuilder abbrName = new StringBuilder();
    assertFalse(validTokenAbbrName(abbrName.toString().getBytes(StandardCharsets.UTF_8)));
    for (int i = 0; i < 6; i++) {
      abbrName.append((char) ('a' + (i % 26)));
    }
    assertFalse(validTokenAbbrName(abbrName.toString().getBytes(StandardCharsets.UTF_8)));
    abbrName = new StringBuilder("a bd");
    assertFalse(validTokenAbbrName(abbrName.toString().getBytes(StandardCharsets.UTF_8)));
    abbrName = new StringBuilder("a" + (char) 129 + 'f');
    assertFalse(validTokenAbbrName(abbrName.toString().getBytes(StandardCharsets.UTF_8)));
    abbrName = new StringBuilder();
    for (int i = 0; i < 5; i++) {
      abbrName.append((char) ('a' + (i % 26)));
    }
    assertTrue(validTokenAbbrName(abbrName.toString().getBytes(StandardCharsets.UTF_8)));
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
  public void testEstimateConsumeBandWidthSize() {
    AccountCapsule ownerCapsule =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    long estimateConsumeBandWidthSize = estimateConsumeBandWidthSize(ownerCapsule,
        dbManager.getChainBaseManager());
    assertEquals(275L, estimateConsumeBandWidthSize);
    chainBaseManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(DELEGATE_PERIOD / 3000);
  }

  @Test
  public void testEstimateConsumeBandWidthSize2() {
    chainBaseManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(14);
    chainBaseManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(864000L);
    AccountCapsule ownerCapsule =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    long estimateConsumeBandWidthSize = estimateConsumeBandWidthSize(ownerCapsule,
        dbManager.getChainBaseManager());
    assertEquals(277L, estimateConsumeBandWidthSize);
    chainBaseManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(DELEGATE_PERIOD / 3000);
  }


  @Test
  public void testEstimateConsumeBandWidthSizeOld() {
    dbManager.getDynamicPropertiesStore().saveAllowCreationOfContracts(1L);
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    long balance = 1000_000L;

    AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), Protocol.AccountType.Normal,
        balance);
    ownerCapsule.addFrozenBalanceForBandwidthV2(balance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
    ownerCapsule = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    long estimateConsumeBandWidthSize1 = estimateConsumeBandWidthSizeOld(
        ownerCapsule, chainBaseManager);
    Assert.assertEquals(277, estimateConsumeBandWidthSize1);

    balance = 1000_000_000L;
    ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), Protocol.AccountType.Normal,
        balance);
    ownerCapsule.addFrozenBalanceForBandwidthV2(balance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
    long estimateConsumeBandWidthSize2 = estimateConsumeBandWidthSizeOld(
        ownerCapsule, chainBaseManager);
    Assert.assertEquals(279, estimateConsumeBandWidthSize2);

    balance = 1000_000_000_000L;
    ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), Protocol.AccountType.Normal,
        balance);
    ownerCapsule.addFrozenBalanceForBandwidthV2(balance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
    long estimateConsumeBandWidthSize3 = estimateConsumeBandWidthSizeOld(
        ownerCapsule, chainBaseManager);
    Assert.assertEquals(280, estimateConsumeBandWidthSize3);

    balance = 1000_000_000_000_000L;
    ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), Protocol.AccountType.Normal,
        balance);
    ownerCapsule.addFrozenBalanceForBandwidthV2(balance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
    long estimateConsumeBandWidthSize4 = estimateConsumeBandWidthSizeOld(
        ownerCapsule, chainBaseManager);
    Assert.assertEquals(282, estimateConsumeBandWidthSize4);
  }


  @Test
  public void testEstimateConsumeBandWidthSizeNew() {
    long balance = 1000_000L;
    DynamicPropertiesStore dps = chainBaseManager.getDynamicPropertiesStore();
    long estimateConsumeBandWidthSize1 = TransactionUtil.estimateConsumeBandWidthSize(dps, balance);
    Assert.assertEquals(277, estimateConsumeBandWidthSize1);

    balance = 1000_000_000L;
    long estimateConsumeBandWidthSize2 = TransactionUtil.estimateConsumeBandWidthSize(dps, balance);
    Assert.assertEquals(279, estimateConsumeBandWidthSize2);

    balance = 1000_000_000_000L;
    long estimateConsumeBandWidthSize3 = TransactionUtil.estimateConsumeBandWidthSize(dps, balance);
    Assert.assertEquals(280, estimateConsumeBandWidthSize3);

    balance = 1000_000_000_000_000L;
    long estimateConsumeBandWidthSize4 = TransactionUtil.estimateConsumeBandWidthSize(dps, balance);
    Assert.assertEquals(282, estimateConsumeBandWidthSize4);
  }


  @Test
  public void testEstimateConsumeBandWidthSize3() {
    dbManager.getDynamicPropertiesStore().saveAllowCreationOfContracts(1L);
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    DynamicPropertiesStore dps = chainBaseManager.getDynamicPropertiesStore();
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
      estimateConsumeBandWidthSizeOld = estimateConsumeBandWidthSizeOld(
          ownerCapsule, chainBaseManager);

      // new value is
      estimateConsumeBandWidthSizeNew = TransactionUtil.estimateConsumeBandWidthSize(dps, balance);

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
    DynamicPropertiesStore dps = chainBaseManager.getDynamicPropertiesStore();
    long balance = 100;
    DelegateResourceContract.Builder builder =
        DelegateResourceContract.newBuilder()
            .setLock(true)
            .setBalance(balance);
    DelegateResourceContract.Builder builder2 =
        DelegateResourceContract.newBuilder()
            .setBalance(TRX_PRECISION);

    long expected = DELEGATE_COST_BASE_SIZE + Math.max(
        builder.build().getSerializedSize() - builder2.build().getSerializedSize(), 0L);
    long actual = TransactionUtil.estimateConsumeBandWidthSize(dps, balance);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void estimateConsumeBandWidthSizeBoundary() {
    DynamicPropertiesStore dps = chainBaseManager.getDynamicPropertiesStore();
    long balance = TRX_PRECISION;
    DelegateResourceContract.Builder builder =
        DelegateResourceContract.newBuilder()
            .setLock(true)
            .setBalance(balance);
    DelegateResourceContract.Builder builder2 =
        DelegateResourceContract.newBuilder()
            .setBalance(TRX_PRECISION);

    long expected = DELEGATE_COST_BASE_SIZE + Math.max(
        builder.build().getSerializedSize() - builder2.build().getSerializedSize(), 0L);
    long actual = TransactionUtil.estimateConsumeBandWidthSize(dps, balance);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void estimateConsumeBandWidthSizeEdge() {
    DynamicPropertiesStore dps = chainBaseManager.getDynamicPropertiesStore();
    long balance = TRX_PRECISION + 1;
    DelegateResourceContract.Builder builder =
        DelegateResourceContract.newBuilder()
            .setLock(true)
            .setBalance(balance);
    DelegateResourceContract.Builder builder2 =
        DelegateResourceContract.newBuilder()
            .setBalance(TRX_PRECISION);

    long expected = DELEGATE_COST_BASE_SIZE + Math.max(
        builder.build().getSerializedSize() - builder2.build().getSerializedSize(), 0L);
    long actual = TransactionUtil.estimateConsumeBandWidthSize(dps, balance);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void estimateConsumeBandWidthSizeCorner() {
    DynamicPropertiesStore dps = chainBaseManager.getDynamicPropertiesStore();
    long balance = Long.MAX_VALUE;
    DelegateResourceContract.Builder builder =
        DelegateResourceContract.newBuilder()
            .setLock(true)
            .setBalance(balance);
    DelegateResourceContract.Builder builder2 =
        DelegateResourceContract.newBuilder()
            .setBalance(TRX_PRECISION);

    long expected = DELEGATE_COST_BASE_SIZE + Math.max(
        builder.build().getSerializedSize() - builder2.build().getSerializedSize(), 0L);
    long actual = TransactionUtil.estimateConsumeBandWidthSize(dps, balance);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testConcurrentToString() throws InterruptedException {
    Transaction.Builder builder = Transaction.newBuilder();
    TransactionCapsule trx = new TransactionCapsule(builder.build());
    List<Thread> threadList = new ArrayList<>();
    int n = 10;
    for (int i = 0; i < n; i++) {
      threadList.add(new Thread(() -> trx.toString()));
    }
    for (int i = 0; i < n; i++) {
      threadList.get(i).start();
    }
    for (int i = 0; i < n; i++) {
      threadList.get(i).join();
    }
    Assert.assertTrue(true);
  }
}
