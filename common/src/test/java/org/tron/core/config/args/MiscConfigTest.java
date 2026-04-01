package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.tron.core.Constant;

public class MiscConfigTest {

  @Test
  public void testDefaults() {
    Config empty = ConfigFactory.empty();
    MiscConfig mc = MiscConfig.fromConfig(empty);
    assertTrue(mc.isNeedToUpdateAsset());
    assertFalse(mc.isHistoryBalanceLookup());
    assertEquals("solid", mc.getTrxReferenceBlock());
    assertEquals(Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME,
        mc.getTrxExpirationTimeInMilliseconds());
    assertEquals(Constant.ECKey_ENGINE, mc.getCryptoEngine());
    assertTrue(mc.getSeedNodeIpList().isEmpty());
    assertTrue(mc.getActuatorWhitelist().isEmpty());
  }

  @Test
  public void testFromConfig() {
    Config config = ConfigFactory.parseString(
        "storage { needToUpdateAsset = false,"
            + " balance { history { lookup = true } } }\n"
            + "trx { reference { block = head } }\n"
            + "crypto { engine = sm2 }\n"
            + "seed.node { ip.list = [\"1.2.3.4:18888\"] }\n"
            + "actuator { whitelist = [\"CreateSmartContract\"] }");
    MiscConfig mc = MiscConfig.fromConfig(config);
    assertFalse(mc.isNeedToUpdateAsset());
    assertTrue(mc.isHistoryBalanceLookup());
    assertEquals("head", mc.getTrxReferenceBlock());
    assertEquals("sm2", mc.getCryptoEngine());
    assertEquals(1, mc.getSeedNodeIpList().size());
    assertEquals(1, mc.getActuatorWhitelist().size());
    assertTrue(mc.getActuatorWhitelist().contains("CreateSmartContract"));
  }
}
