package org.tron.core.capsule.utils;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;

@Slf4j
public class ExchangeProcessorTest {

  private static ExchangeProcessor processor;
  private static final String dbPath = "output_buy_exchange_processor_test";
  private static AnnotationConfigApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000_000_000L;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    long supply = 1_000_000_000_000_000_000L;
    processor = new ExchangeProcessor(supply);
    //    Args.setParam(new String[]{"--output-directory", dbPath},
    //        "config-junit.conf");
    //    dbManager = new Manager();
    //    dbManager.init();
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
  }

  @Test
  public void testExchange() {
    long sellBalance = 100_000_000_000000L;
    long buyBalance = 128L * 1024 * 1024 * 1024;
    long sellQuant = 2_000_000_000_000L; // 2 million trx

    long result = processor.exchange(sellBalance, buyBalance, sellQuant);

    Assert.assertEquals(2694881440L, result);
  }

  @Test
  public void testExchange2() {
    long sellBalance = 100_000_000_000000L;
    long buyBalance = 128L * 1024 * 1024 * 1024;
    long sellQuant = 1_000_000_000_000L; // 2 million trx

    long result = processor.exchange(sellBalance, buyBalance, sellQuant);
    Assert.assertEquals(1360781717L, result);

    sellBalance += sellQuant;
    buyBalance -= result;

    long result2 = processor.exchange(sellBalance, buyBalance, sellQuant);
    Assert.assertEquals(2694881440L - 1360781717L, result2);

  }


  @Test
  public void testSellAndBuy() {
    long sellBalance = 100_000_000_000000L;
    long buyBalance = 128L * 1024 * 1024 * 1024;
    long sellQuant = 2_000_000_000_000L; // 2 million trx

    long result = processor.exchange(sellBalance, buyBalance, sellQuant);
    Assert.assertEquals(2694881440L, result);

    sellBalance += sellQuant;
    buyBalance -= result;

    long result2 = processor.exchange(buyBalance, sellBalance, result);
    Assert.assertEquals(1999999999542L, result2);

  }

  @Test
  public void testSellAndBuy2() {
    long sellBalance = 100_000_000_000000L;
    long buyBalance = 128L * 1024 * 1024 * 1024;
    long sellQuant = 2_000_000_000_000L; // 2 million trx

    long result = processor.exchange(sellBalance, buyBalance, sellQuant);
    Assert.assertEquals(2694881440L, result);

    sellBalance += sellQuant;
    buyBalance -= result;

    long quant1 = 2694881440L - 1360781717L;
    long quant2 = 1360781717L;

    long result1 = processor.exchange(buyBalance, sellBalance, quant1);
    Assert.assertEquals(999999999927L, result1);

    buyBalance += quant1;
    sellBalance -= result1;

    long result2 = processor.exchange(buyBalance, sellBalance, quant2);
    Assert.assertEquals(999999999587L, result2);

  }


}
