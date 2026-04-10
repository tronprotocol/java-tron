package org.tron.p2p;

import org.junit.Assert;
import org.junit.Test;

public class P2pConfigTest {

  @Test
  public void testDefaultValues() {
    P2pConfig config = new P2pConfig();
    Assert.assertNotNull(config.getSeedNodes());
    Assert.assertTrue(config.getSeedNodes().isEmpty());
    Assert.assertNotNull(config.getActiveNodes());
    Assert.assertTrue(config.getActiveNodes().isEmpty());
    Assert.assertNotNull(config.getTrustNodes());
    Assert.assertTrue(config.getTrustNodes().isEmpty());
    Assert.assertNotNull(config.getNodeID());
    Assert.assertEquals(64, config.getNodeID().length);
    Assert.assertEquals(18888, config.getPort());
    Assert.assertEquals(1, config.getNetworkId());
    Assert.assertEquals(8, config.getMinConnections());
    Assert.assertEquals(50, config.getMaxConnections());
    Assert.assertEquals(2, config.getMinActiveConnections());
    Assert.assertEquals(2, config.getMaxConnectionsWithSameIp());
    Assert.assertTrue(config.isDiscoverEnable());
    Assert.assertFalse(config.isDisconnectionPolicyEnable());
    Assert.assertFalse(config.isNodeDetectEnable());
    Assert.assertNotNull(config.getTreeUrls());
    Assert.assertTrue(config.getTreeUrls().isEmpty());
    Assert.assertNotNull(config.getPublishConfig());
  }

  @Test
  public void testSettersAndGetters() {
    P2pConfig config = new P2pConfig();

    config.setPort(19999);
    Assert.assertEquals(19999, config.getPort());

    config.setNetworkId(42);
    Assert.assertEquals(42, config.getNetworkId());

    config.setMinConnections(10);
    Assert.assertEquals(10, config.getMinConnections());

    config.setMaxConnections(100);
    Assert.assertEquals(100, config.getMaxConnections());

    config.setMinActiveConnections(5);
    Assert.assertEquals(5, config.getMinActiveConnections());

    config.setMaxConnectionsWithSameIp(3);
    Assert.assertEquals(3, config.getMaxConnectionsWithSameIp());

    config.setDiscoverEnable(false);
    Assert.assertFalse(config.isDiscoverEnable());

    config.setDisconnectionPolicyEnable(true);
    Assert.assertTrue(config.isDisconnectionPolicyEnable());

    config.setNodeDetectEnable(true);
    Assert.assertTrue(config.isNodeDetectEnable());

    byte[] customId = new byte[64];
    config.setNodeID(customId);
    Assert.assertArrayEquals(customId, config.getNodeID());

    config.setIp("10.0.0.1");
    Assert.assertEquals("10.0.0.1", config.getIp());

    config.setLanIp("192.168.0.1");
    Assert.assertEquals("192.168.0.1", config.getLanIp());

    config.setIpv6("::1");
    Assert.assertEquals("::1", config.getIpv6());
  }
}
