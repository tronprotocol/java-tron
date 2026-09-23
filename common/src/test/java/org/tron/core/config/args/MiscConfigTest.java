package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.tron.core.Constant;

public class MiscConfigTest {

  private final Logger logger = (Logger) LoggerFactory.getLogger(MiscConfig.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private Level originalLevel;

  @Before
  public void setUp() {
    originalLevel = logger.getLevel();
    logger.setLevel(Level.WARN);
    appender.start();
    logger.addAppender(appender);
  }

  @After
  public void tearDown() {
    logger.detachAppender(appender);
    appender.stop();
    logger.setLevel(originalLevel);
  }

  private static Config withRef(String hocon) {
    return ConfigFactory.parseString(hocon).withFallback(ConfigFactory.defaultReference());
  }

  private static Config withRef() {
    return ConfigFactory.defaultReference();
  }

  @Test
  public void testDefaults() {
    Config empty = withRef();
    MiscConfig mc = MiscConfig.fromConfig(empty);
    assertTrue(mc.isNeedToUpdateAsset());
    assertFalse(mc.isHistoryBalanceLookup());
    assertEquals("solid", mc.getTrxReferenceBlock());
    assertEquals(Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME,
        mc.getTrxExpirationTimeInMilliseconds());
    // reference.conf has seed.node.ip.list with actual IPs
    assertFalse(mc.getSeedNodeIpList().isEmpty());
    assertTrue(appender.list.isEmpty());
  }

  @Test
  public void testFromConfig() {
    Config config = withRef(
        "storage { needToUpdateAsset = false,"
            + " balance { history { lookup = true } } }\n"
            + "trx { reference { block = head } }\n"
            + "seed.node { ip.list = [\"1.2.3.4:18888\"] }");
    MiscConfig mc = MiscConfig.fromConfig(config);
    assertFalse(mc.isNeedToUpdateAsset());
    assertTrue(mc.isHistoryBalanceLookup());
    assertEquals("head", mc.getTrxReferenceBlock());
    assertEquals(1, mc.getSeedNodeIpList().size());
  }

  @Test
  public void testLegacyEckeyWarning() {
    for (String engine : new String[]{"eckey", "ECKey"}) {
      appender.list.clear();
      MiscConfig.fromConfig(withRef("crypto.engine = " + engine));
      assertEquals(1, appender.list.size());
      assertEquals(Level.WARN, appender.list.get(0).getLevel());
      assertEquals("crypto.engine is deprecated and ignored; ECKey and SHA-256 are always used",
          appender.list.get(0).getFormattedMessage());
    }
  }

  @Test
  public void testUnsupportedCryptoEngine() {
    for (String engine : new String[]{"sm2", "SM2", "unknown", "\"\"", "null", "true",
        "42", "[]", "{}"}) {
      Config config = withRef("crypto.engine = " + engine);
      IllegalArgumentException exception = assertThrows(engine, IllegalArgumentException.class,
          () -> MiscConfig.fromConfig(config));
      assertTrue(exception.getMessage().contains("SM2/SM3 support has been removed"));
      assertTrue(exception.getMessage().contains("crypto.engine only accepts eckey"));
      assertTrue(exception.getMessage().contains("remain on a compatible release"));
      assertTrue(exception.getMessage().contains("do not reuse the chain database"));
      assertTrue(appender.list.isEmpty());
    }
  }
}
