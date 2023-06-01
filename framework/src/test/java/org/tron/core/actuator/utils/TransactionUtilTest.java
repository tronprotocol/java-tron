package org.tron.core.actuator.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.tron.core.capsule.utils.TransactionUtil.isNumber;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.utils.TransactionUtil;


@Slf4j(topic = "capsule")
public class TransactionUtilTest extends BaseTest {

  /**
   * Init .
   */
  @BeforeClass
  public static void init() {
    dbPath = "output_transactionUtil_test";
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

}
