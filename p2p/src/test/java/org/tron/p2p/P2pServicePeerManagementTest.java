package org.tron.p2p;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.ConnectionPolicy;
import org.tron.p2p.discover.NodeManager;
import org.tron.p2p.dns.DnsManager;

public class P2pServicePeerManagementTest {

  private P2pService p2pService;
  private P2pConfig savedConfig;

  @Before
  public void setUp() {
    savedConfig = Parameter.p2pConfig;
    Parameter.p2pConfig = new P2pConfig();
    p2pService = new P2pService();
    ConnectionPolicy.replaceBlockedIps(Collections.emptySet());
  }

  @After
  public void tearDown() {
    Parameter.p2pConfig = savedConfig;
    ConnectionPolicy.replaceBlockedIps(Collections.emptySet());
  }

  @Test
  public void startupInstallsPolicyBeforeInitializingNetwork() {
    InetAddress blocked = new InetSocketAddress("192.0.2.24", 18888).getAddress();
    P2pConfig config = new P2pConfig();
    config.setBlockedIps(Collections.singleton(blocked));
    try (MockedStatic<NodeManager> nodes = Mockito.mockStatic(NodeManager.class);
        MockedStatic<ChannelManager> channels = Mockito.mockStatic(ChannelManager.class);
        MockedStatic<DnsManager> dns = Mockito.mockStatic(DnsManager.class)) {
      nodes.when(NodeManager::init).thenAnswer(invocation -> {
        Assert.assertTrue(ConnectionPolicy.isBlocked(blocked));
        return null;
      });
      channels.when(ChannelManager::init).thenAnswer(invocation -> {
        Assert.assertTrue(ConnectionPolicy.isBlocked(blocked));
        return null;
      });
      dns.when(DnsManager::init).thenAnswer(invocation -> {
        Assert.assertTrue(ConnectionPolicy.isBlocked(blocked));
        return null;
      });
      try {
        p2pService.start(config);
        nodes.verify(NodeManager::init);
        channels.verify(ChannelManager::init);
        dns.verify(DnsManager::init);
      } finally {
        p2pService.close();
      }
    }
  }

  @Test
  public void blockedActiveNodeIsRejected() {
    InetSocketAddress address = new InetSocketAddress("192.0.2.20", 18888);
    ConnectionPolicy.replaceBlockedIps(Collections.singleton(address.getAddress()));

    Assert.assertFalse(p2pService.addActiveNode(address));
    Assert.assertFalse(Parameter.p2pConfig.getActiveNodes().contains(address));
  }

  @Test
  public void removeAndDisconnectDoNotChangeEachOthersState() {
    InetSocketAddress address = new InetSocketAddress("192.0.2.21", 18888);
    Assert.assertTrue(p2pService.addActiveNode(address));
    Assert.assertFalse(p2pService.addActiveNode(address));

    Assert.assertEquals(0, p2pService.disconnect(address));
    Assert.assertTrue(Parameter.p2pConfig.getActiveNodes().contains(address));
    Assert.assertTrue(p2pService.removeActiveNode(address));
    Assert.assertFalse(p2pService.removeActiveNode(address));
  }

  @Test
  public void invalidAddressIsRejectedBeforeNetworkAccess() {
    try {
      p2pService.addActiveNode(InetSocketAddress.createUnresolved("peer.example", 18888));
      Assert.fail("Expected unresolved address to be rejected");
    } catch (IllegalArgumentException expected) {
      Assert.assertTrue(expected.getMessage().contains("must be resolved"));
    }
  }

  @Test
  public void activeNodeAddressPolicyAllowsLoopback() {
    Assert.assertTrue(p2pService.addActiveNode(new InetSocketAddress("127.0.0.1", 18888)));
    Assert.assertTrue(p2pService.addActiveNode(new InetSocketAddress("::1", 18888)));
  }

  @Test
  public void activeNodeAddressPolicyRejectsNonDialableAddresses() {
    String[] invalidAddresses = {
        "0.0.0.0", "::", "224.0.0.1", "ff02::1", "255.255.255.255"
    };
    for (String invalidAddress : invalidAddresses) {
      try {
        p2pService.addActiveNode(new InetSocketAddress(invalidAddress, 18888));
        Assert.fail("Expected address to be rejected: " + invalidAddress);
      } catch (IllegalArgumentException expected) {
        Assert.assertTrue(expected.getMessage().contains("must not use"));
      }
    }
  }

  @Test
  public void activeNodeSetterKeepsCollectionSafeForRuntimeUpdates() {
    P2pConfig config = new P2pConfig();
    config.setActiveNodes(new ArrayList<InetSocketAddress>());

    java.util.Iterator<InetSocketAddress> iterator = config.getActiveNodes().iterator();
    config.getActiveNodes().add(new InetSocketAddress("192.0.2.22", 18888));

    Assert.assertFalse(iterator.hasNext());
    Assert.assertEquals(1, config.getActiveNodes().size());
  }

  @Test
  public void replaceBlockedIpsUpdatesOnlyRuntimePolicy() {
    InetAddress initialAddress = new InetSocketAddress("192.0.2.24", 18888).getAddress();
    Set<InetAddress> initialPolicy = Collections.singleton(initialAddress);
    Parameter.p2pConfig.setBlockedIps(initialPolicy);
    ConnectionPolicy.replaceBlockedIps(initialPolicy);
    InetAddress address = new InetSocketAddress("192.0.2.23", 18888).getAddress();
    Set<InetAddress> blockedIps = new HashSet<>();
    blockedIps.add(address);

    p2pService.replaceBlockedIps(blockedIps);

    Assert.assertSame(initialPolicy, Parameter.p2pConfig.getBlockedIps());
    Assert.assertFalse(ConnectionPolicy.isBlocked(initialAddress));
    Assert.assertTrue(ConnectionPolicy.isBlocked(address));
    blockedIps.clear();
    Assert.assertTrue(ConnectionPolicy.isBlocked(address));

    p2pService.replaceBlockedIps(Collections.emptySet());
    Assert.assertFalse(ConnectionPolicy.isBlocked(address));
    Assert.assertFalse(ConnectionPolicy.isBlocked(initialAddress));
    Assert.assertSame(initialPolicy, Parameter.p2pConfig.getBlockedIps());
  }
}
