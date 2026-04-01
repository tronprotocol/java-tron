package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class GenesisConfigTest {

  private static Config withRef(String hocon) {
    return ConfigFactory.parseString(hocon).withFallback(ConfigFactory.defaultReference());
  }

  private static Config withRef() {
    return ConfigFactory.defaultReference();
  }

  @Test
  public void testDefaults() {
    Config empty = withRef();
    GenesisConfig gc = GenesisConfig.fromConfig(empty);
    // reference.conf has genesis.block with timestamp, parentHash, assets, witnesses
    assertEquals("0", gc.getTimestamp());
    assertFalse(gc.getAssets().isEmpty());  // reference.conf has seed accounts
    assertFalse(gc.getWitnesses().isEmpty()); // reference.conf has seed witnesses
  }

  @Test
  public void testWithAssets() {
    Config config = withRef(
        "genesis.block { timestamp = \"12345\", parentHash = \"0x00\","
            + " assets = [{ accountName = Zion, accountType = AssetIssue,"
            + " address = \"TAddr1\", balance = \"99000\" }],"
            + " witnesses = [{ address = \"TWitness1\", url = \"http://test.com\","
            + " voteCount = 100 }] }");
    GenesisConfig gc = GenesisConfig.fromConfig(config);
    assertEquals("12345", gc.getTimestamp());
    assertEquals("0x00", gc.getParentHash());
    assertEquals(1, gc.getAssets().size());
    assertEquals("Zion", gc.getAssets().get(0).getAccountName());
    assertEquals("TAddr1", gc.getAssets().get(0).getAddress());
    assertEquals(1, gc.getWitnesses().size());
    assertEquals("TWitness1", gc.getWitnesses().get(0).getAddress());
    assertEquals(100, gc.getWitnesses().get(0).getVoteCount());
  }

  @Test
  public void testEmptyLists() {
    Config config = withRef(
        "genesis.block { timestamp = \"0\", parentHash = \"0x00\","
            + " assets = [], witnesses = [] }");
    GenesisConfig gc = GenesisConfig.fromConfig(config);
    assertTrue(gc.getAssets().isEmpty());
    assertTrue(gc.getWitnesses().isEmpty());
  }
}
