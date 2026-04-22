package org.tron.p2p.connection;

import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.socket.P2pChannelInitializer;

public class P2pChannelInitializerTest {

  @Before
  public void setUp() {
    Parameter.p2pConfig = new P2pConfig();
  }

  @Test
  public void testConstructor() {
    P2pChannelInitializer initializer = new P2pChannelInitializer("remoteId", false, true);
    Assert.assertNotNull(initializer);
  }

  @Test
  public void testConstructorDiscoveryMode() {
    P2pChannelInitializer initializer = new P2pChannelInitializer("remoteId", true, false);
    Assert.assertNotNull(initializer);
  }

  @Test
  public void testInitChannelFields() throws Exception {
    P2pChannelInitializer initializer = new P2pChannelInitializer("remoteId", true, true);

    // Verify internal fields
    Field remoteIdField = P2pChannelInitializer.class.getDeclaredField("remoteId");
    remoteIdField.setAccessible(true);
    Assert.assertEquals("remoteId", remoteIdField.get(initializer));

    Field discoveryField = P2pChannelInitializer.class.getDeclaredField("peerDiscoveryMode");
    discoveryField.setAccessible(true);
    Assert.assertTrue((Boolean) discoveryField.get(initializer));

    Field triggerField = P2pChannelInitializer.class.getDeclaredField("trigger");
    triggerField.setAccessible(true);
    Assert.assertTrue((Boolean) triggerField.get(initializer));
  }
}
