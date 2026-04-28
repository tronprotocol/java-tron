package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class LocalWitnessConfigTest {

  private static Config withRef(String hocon) {
    return ConfigFactory.parseString(hocon).withFallback(ConfigFactory.defaultReference());
  }

  private static Config withRef() {
    return ConfigFactory.defaultReference();
  }

  @Test
  public void testDefaults() {
    Config empty = withRef();
    LocalWitnessConfig lw = LocalWitnessConfig.fromConfig(empty);
    assertTrue(lw.getPrivateKeys().isEmpty());
    assertNull(lw.getAccountAddress());
    assertTrue(lw.getKeystores().isEmpty());
  }

  @Test
  public void testWithPrivateKeys() {
    Config config = withRef(
        "localwitness = [\"key1\", \"key2\"]\n"
            + "localWitnessAccountAddress = \"TAddr123\"");
    LocalWitnessConfig lw = LocalWitnessConfig.fromConfig(config);
    assertEquals(2, lw.getPrivateKeys().size());
    assertEquals("key1", lw.getPrivateKeys().get(0));
    assertEquals("TAddr123", lw.getAccountAddress());
  }

  @Test
  public void testWithKeystores() {
    Config config = withRef(
        "localwitnesskeystore = [\"/path/to/keystore1\"]");
    LocalWitnessConfig lw = LocalWitnessConfig.fromConfig(config);
    assertEquals(1, lw.getKeystores().size());
  }
}
