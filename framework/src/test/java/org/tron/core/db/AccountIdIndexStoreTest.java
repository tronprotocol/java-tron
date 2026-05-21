package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.api.MigrateTurkishKeyHelper;
import org.tron.core.store.AccountIdIndexStore;
import org.tron.protos.Protocol.AccountType;

public class AccountIdIndexStoreTest extends BaseTest {

  private static final byte[] ACCOUNT_ADDRESS_ONE = randomBytes(16);
  private static final byte[] ACCOUNT_ADDRESS_TWO = randomBytes(16);
  private static final byte[] ACCOUNT_ADDRESS_THREE = randomBytes(16);
  private static final byte[] ACCOUNT_ADDRESS_FOUR = randomBytes(16);
  private static final byte[] ACCOUNT_NAME_ONE = randomBytes(6);
  private static final byte[] ACCOUNT_NAME_TWO = randomBytes(6);
  private static final byte[] ACCOUNT_NAME_THREE = randomBytes(6);
  private static final byte[] ACCOUNT_NAME_FOUR = randomBytes(6);
  private static final byte[] ACCOUNT_NAME_FIVE = randomBytes(6);
  private static final Locale TURKISH = Locale.forLanguageTag("tr");
  @Resource
  private AccountIdIndexStore accountIdIndexStore;
  private static AccountCapsule accountCapsule1;
  private static AccountCapsule accountCapsule2;
  private static AccountCapsule accountCapsule3;
  private static AccountCapsule accountCapsule4;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()},
        TestConstants.TEST_CONF);
  }

  @BeforeClass
  public static void init() {
    accountCapsule1 = new AccountCapsule(ByteString.copyFrom(ACCOUNT_ADDRESS_ONE),
        ByteString.copyFrom(ACCOUNT_NAME_ONE), AccountType.Normal);
    accountCapsule1.setAccountId(ByteString.copyFrom(ACCOUNT_NAME_ONE).toByteArray());
    accountCapsule2 = new AccountCapsule(ByteString.copyFrom(ACCOUNT_ADDRESS_TWO),
        ByteString.copyFrom(ACCOUNT_NAME_TWO), AccountType.Normal);
    accountCapsule2.setAccountId(ByteString.copyFrom(ACCOUNT_NAME_TWO).toByteArray());
    accountCapsule3 = new AccountCapsule(ByteString.copyFrom(ACCOUNT_ADDRESS_THREE),
        ByteString.copyFrom(ACCOUNT_NAME_THREE), AccountType.Normal);
    accountCapsule3.setAccountId(ByteString.copyFrom(ACCOUNT_NAME_THREE).toByteArray());
    accountCapsule4 = new AccountCapsule(ByteString.copyFrom(ACCOUNT_ADDRESS_FOUR),
        ByteString.copyFrom(ACCOUNT_NAME_FOUR), AccountType.Normal);
    accountCapsule4.setAccountId(ByteString.copyFrom(ACCOUNT_NAME_FOUR).toByteArray());

  }

  @Before
  public void before() {
    accountIdIndexStore.put(accountCapsule1);
    accountIdIndexStore.put(accountCapsule2);
    accountIdIndexStore.put(accountCapsule3);
    accountIdIndexStore.put(accountCapsule4);
  }

  public static byte[] randomBytes(int length) {
    // generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    result[0] = Wallet.getAddressPreFixByte();
    return result;
  }

  @Test
  public void putAndGet() {
    byte[] address = accountIdIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_ONE));
    Assert.assertArrayEquals("putAndGet1", address, ACCOUNT_ADDRESS_ONE);
    address = accountIdIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_TWO));
    Assert.assertArrayEquals("putAndGet2", address, ACCOUNT_ADDRESS_TWO);
    address = accountIdIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_THREE));
    Assert.assertArrayEquals("putAndGet3", address, ACCOUNT_ADDRESS_THREE);
    address = accountIdIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_FOUR));
    Assert.assertArrayEquals("putAndGet4", address, ACCOUNT_ADDRESS_FOUR);
    address = accountIdIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_FIVE));
    Assert.assertNull("putAndGet4", address);

  }

  @Test
  public void putAndHas() {
    Boolean result = accountIdIndexStore.has(ACCOUNT_NAME_ONE);
    Assert.assertTrue("putAndGet1", result);
    result = accountIdIndexStore.has(ACCOUNT_NAME_TWO);
    Assert.assertTrue("putAndGet2", result);
    result = accountIdIndexStore.has(ACCOUNT_NAME_THREE);
    Assert.assertTrue("putAndGet3", result);
    result = accountIdIndexStore.has(ACCOUNT_NAME_FOUR);
    Assert.assertTrue("putAndGet4", result);
    result = accountIdIndexStore.has(ACCOUNT_NAME_FIVE);
    Assert.assertFalse("putAndGet4", result);
  }

  @Test
  @SuppressWarnings("StringCaseLocaleUsage")
  public void testCaseInsensitive() {
    byte[] ACCOUNT_NAME = "aABbCcDd_ssd1234".getBytes();
    byte[] ACCOUNT_ADDRESS = randomBytes(16);

    AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom(ACCOUNT_ADDRESS),
        ByteString.copyFrom(ACCOUNT_NAME), AccountType.Normal);
    accountCapsule.setAccountId(ByteString.copyFrom(ACCOUNT_NAME).toByteArray());
    accountIdIndexStore.put(accountCapsule);

    Boolean result = accountIdIndexStore.has(ACCOUNT_NAME);
    Assert.assertTrue("fail", result);

    byte[] lowerCase = ByteString
        .copyFromUtf8(ByteString.copyFrom(ACCOUNT_NAME).toStringUtf8().toLowerCase())
        .toByteArray();
    result = accountIdIndexStore.has(lowerCase);
    Assert.assertTrue("lowerCase fail", result);

    byte[] upperCase = ByteString
        .copyFromUtf8(ByteString.copyFrom(ACCOUNT_NAME).toStringUtf8().toUpperCase())
        .toByteArray();
    result = accountIdIndexStore.has(upperCase);
    Assert.assertTrue("upperCase fail", result);

    Assert.assertNotNull("getLowerCase fail", accountIdIndexStore.get(upperCase));

  }

  @Test
  @SuppressWarnings("StringCaseLocaleUsage")
  public void testKeysMigration() {
    String[]accountIds = {"", "12345678", "543838383", "BitTorrent",
        "Converse", "HelloWorld", "InfStonesSSRWallet", "ISSRWallet", "JustDoIt",
        "JustinSun", "JustinSunTron", "RtytIturtet", "TronBetFestival", "vena_family"
    };

    byte[][] addresses = new byte[accountIds.length][];
    byte[][] turkishKeys = new byte[accountIds.length][];

    for (int i = 0; i < accountIds.length; i++) {
      addresses[i] = randomBytes(21);
      String turkishLower = accountIds[i].toLowerCase(TURKISH);
      turkishKeys[i] = turkishLower.getBytes(StandardCharsets.UTF_8);
      accountIdIndexStore.put(turkishKeys[i], new BytesCapsule(addresses[i]));
    }

    for (int i = 0; i < accountIds.length; i++) {
      String rootLower = accountIds[i].toLowerCase(Locale.ROOT);
      String turkishLower = accountIds[i].toLowerCase(TURKISH);
      boolean shouldMiss = !rootLower.equals(turkishLower);
      if (shouldMiss) {
        Assert.assertNull(
            "pre-migrate: ROOT query should miss for " + accountIds[i],
            accountIdIndexStore.get(ByteString.copyFrom(
                accountIds[i].getBytes(StandardCharsets.UTF_8))));
      } else {
        Assert.assertArrayEquals(
            "pre-migrate: ROOT query should hit for " + accountIds[i],
            addresses[i],
            accountIdIndexStore.get(ByteString.copyFrom(
                accountIds[i].getBytes(StandardCharsets.UTF_8))));
      }
    }

    new MigrateTurkishKeyHelper(chainBaseManager).doWork();

    for (int i = 0; i < accountIds.length; i++) {
      Assert.assertArrayEquals(
          "post-migrate: get(" + accountIds[i] + ")",
          addresses[i],
          accountIdIndexStore.get(ByteString.copyFrom(
              accountIds[i].getBytes(StandardCharsets.UTF_8))));
      String lower = accountIds[i].toLowerCase(Locale.ROOT);
      Assert.assertTrue(
          "post-migrate: has(" + lower + ")",
          accountIdIndexStore.has(lower.getBytes(StandardCharsets.UTF_8)));
      String upper = accountIds[i].toUpperCase(Locale.ROOT);
      Assert.assertTrue(
          "post-migrate: has(" + upper + ")",
          accountIdIndexStore.has(upper.getBytes(StandardCharsets.UTF_8)));
    }
  }
}
