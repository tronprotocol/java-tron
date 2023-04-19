package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j
public class ExchangeCapsuleTest extends BaseTest {

  static {
    dbPath = "output_exchange_capsule_test_test";
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
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
  public void testExchange() {
    long sellBalance = 100000000L;
    long buyBalance = 100000000L;

    byte[] key = ByteArray.fromLong(1);

    ExchangeCapsule exchangeCapsule;
    try {
      exchangeCapsule = chainBaseManager.getExchangeStore().get(key);
      exchangeCapsule.setBalance(sellBalance, buyBalance);

      long sellQuant = 1_000_000L;
      byte[] sellID = "abc".getBytes();

      long result = exchangeCapsule.transaction(sellID, sellQuant);
      Assert.assertEquals(990_099L, result);
      sellBalance += sellQuant;
      Assert.assertEquals(sellBalance, exchangeCapsule.getFirstTokenBalance());
      buyBalance -= result;
      Assert.assertEquals(buyBalance, exchangeCapsule.getSecondTokenBalance());

      sellQuant = 9_000_000L;
      long result2 = exchangeCapsule.transaction(sellID, sellQuant);
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
