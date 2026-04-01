package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class GenesisConfigTest {

  @Test
  public void testDefaults() {
    Config empty = ConfigFactory.empty();
    GenesisConfig gc = GenesisConfig.fromConfig(empty);
    assertEquals("", gc.getTimestamp());
    assertEquals("", gc.getParentHash());
    assertTrue(gc.getAssets().isEmpty());
    assertTrue(gc.getWitnesses().isEmpty());
  }

  @Test
  public void testWithAssets() {
    Config config = ConfigFactory.parseString(
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
    Config config = ConfigFactory.parseString(
        "genesis.block { timestamp = \"0\", parentHash = \"0x00\","
            + " assets = [], witnesses = [] }");
    GenesisConfig gc = GenesisConfig.fromConfig(config);
    assertTrue(gc.getAssets().isEmpty());
    assertTrue(gc.getWitnesses().isEmpty());
  }
}
