package org.tron.core.actuator.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.tron.core.capsule.utils.TransactionUtil.isNumber;
import static org.tron.core.config.Parameter.ChainConstant.DELEGATE_PERIOD;
import static org.tron.core.utils.TransactionUtil.validAccountId;
import static org.tron.core.utils.TransactionUtil.validAccountName;
import static org.tron.core.utils.TransactionUtil.validAssetName;
import static org.tron.core.utils.TransactionUtil.validTokenAbbrName;

import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.utils.TransactionUtil;
import org.tron.protos.Protocol.AccountType;


@Slf4j(topic = "capsule")
public class TransactionUtilTest extends BaseTest {

  private static String OWNER_ADDRESS;

  /**
   * Init .
   */
  @BeforeClass
  public static void init() {
    dbPath = "output_transactionUtil_test";
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
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
    long estimateConsumeBandWidthSize = TransactionUtil.estimateConsumeBandWidthSize(ownerCapsule,
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
    long estimateConsumeBandWidthSize = TransactionUtil.estimateConsumeBandWidthSize(ownerCapsule,
        dbManager.getChainBaseManager());
    assertEquals(277L, estimateConsumeBandWidthSize);
    chainBaseManager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(DELEGATE_PERIOD / 3000);
  }

}
