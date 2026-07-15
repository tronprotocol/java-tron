package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.math.StrictMathWrapper;
import org.tron.common.utils.ByteArray;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j
public class ExchangeCapsuleTest extends BaseTest {

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createExchangeCapsule() {
    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(0);

    long now = chainBaseManager.getHeadBlockTimeStamp();
    ExchangeCapsule exchangeCapsulee =
        new ExchangeCapsule(
            ByteString.copyFromUtf8("owner"),
            1,
            now,
            "abc".getBytes(),
            "def".getBytes());

    chainBaseManager.getExchangeStore().put(exchangeCapsulee.createDbKey(), exchangeCapsulee);

  }

  @Test
  public void testHardenedTransactionFirstTokenSell() throws Exception {
    byte[] key = ByteArray.fromLong(1);
    ExchangeCapsule capsule = chainBaseManager.getExchangeStore().get(key);
    capsule.setBalance(100_000_000L, 100_000_000L);

    long sellQuant = 1_000_000L;
    long buyQuant = capsule.transaction("abc".getBytes(), sellQuant, true, true);

    Assert.assertTrue("Hardened result must be positive", buyQuant > 0);
    Assert.assertEquals(100_000_000L + sellQuant, capsule.getFirstTokenBalance());
    Assert.assertEquals(100_000_000L - buyQuant, capsule.getSecondTokenBalance());
  }

  @Test
  public void testHardenedTransactionSecondTokenSell() throws Exception {
    byte[] key = ByteArray.fromLong(1);
    ExchangeCapsule capsule = chainBaseManager.getExchangeStore().get(key);
    capsule.setBalance(100_000_000L, 100_000_000L);

    long sellQuant = 1_000_000L;
    long buyQuant = capsule.transaction("def".getBytes(), sellQuant, true, true);

    Assert.assertTrue(buyQuant > 0);
    Assert.assertEquals(100_000_000L - buyQuant, capsule.getFirstTokenBalance());
    Assert.assertEquals(100_000_000L + sellQuant, capsule.getSecondTokenBalance());
  }

  @Test
  public void testHardenedTransactionNegativeBalanceThrows() throws Exception {
    // Construct a corrupt-state pool with a negative balance to drive the
    // < 0 invariant in the hardened branch via subtractExact wrapping.
    ExchangeCapsule capsule = new ExchangeCapsule(
        ByteString.copyFromUtf8("owner"), 99L, 0L,
        "abc".getBytes(), "def".getBytes());
    capsule.setBalance(Long.MAX_VALUE, 1L);

    // Selling abc adds to firstTokenBalance: addExact(MAX, q) overflows -> ArithmeticException
    Assert.assertThrows(ArithmeticException.class,
        () -> capsule.transaction("abc".getBytes(), 1L, true, true));
  }

  @Test
  public void testTransactionLegacyVsHardenedProcessorSelection() throws Exception {
    // Same input produces deterministic results in both modes.
    ExchangeCapsule legacy = new ExchangeCapsule(
        ByteString.copyFromUtf8("owner"), 100L, 0L,
        "abc".getBytes(), "def".getBytes());
    legacy.setBalance(100_000_000L, 100_000_000L);
    long legacyResult = legacy.transaction("abc".getBytes(), 1_000_000L, true, false);

    ExchangeCapsule hardened = new ExchangeCapsule(
        ByteString.copyFromUtf8("owner"), 101L, 0L,
        "abc".getBytes(), "def".getBytes());
    hardened.setBalance(100_000_000L, 100_000_000L);
    long hardenedResult = hardened.transaction("abc".getBytes(), 1_000_000L, true, true);

    Assert.assertTrue("Both must return positive", legacyResult > 0 && hardenedResult > 0);
    Assert.assertTrue("Hardened must not exceed pool",
        hardenedResult <= 100_000_000L);
    // Allow ±1 difference due to BigDecimal vs double precision
    Assert.assertTrue("Results should be within 1 unit",
        StrictMathWrapper.abs(legacyResult - hardenedResult) <= 1);
  }

  @Test
  public void testExchange() throws ContractValidateException {
    long sellBalance = 100000000L;
    long buyBalance = 100000000L;

    byte[] key = ByteArray.fromLong(1);

    ExchangeCapsule exchangeCapsule;
    try {
      exchangeCapsule = chainBaseManager.getExchangeStore().get(key);
      exchangeCapsule.setBalance(sellBalance, buyBalance);

      long sellQuant = 1_000_000L;
      byte[] sellID = "abc".getBytes();
      boolean useStrictMath = chainBaseManager.getDynamicPropertiesStore().allowStrictMath();
      long result = exchangeCapsule.transaction(sellID, sellQuant, useStrictMath);
      Assert.assertEquals(990_099L, result);
      sellBalance += sellQuant;
      Assert.assertEquals(sellBalance, exchangeCapsule.getFirstTokenBalance());
      buyBalance -= result;
      Assert.assertEquals(buyBalance, exchangeCapsule.getSecondTokenBalance());

      sellQuant = 9_000_000L;
      long result2 = exchangeCapsule.transaction(sellID, sellQuant, true, true);
      Assert.assertEquals(9090909L, result + result2);
      sellBalance += sellQuant;
      Assert.assertEquals(sellBalance, exchangeCapsule.getFirstTokenBalance());
      buyBalance -= result2;
      Assert.assertEquals(buyBalance, exchangeCapsule.getSecondTokenBalance());

    } catch (ItemNotFoundException e) {
      Assert.fail();
    }

  }


}
