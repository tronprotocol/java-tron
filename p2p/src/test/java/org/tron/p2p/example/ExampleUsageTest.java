package org.tron.p2p.example;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.P2pService;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.dns.update.DnsType;
import org.tron.p2p.dns.update.PublishConfig;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.utils.TestPort;

/**
 * Replaces the former `example` sourceSet.
 *
 * <p>`DnsExample1`, `DnsExample2` and `ImportUsing` documented how an embedder
 * configures and drives this module. They only ever compiled — each one ended in
 * a `while (true)` loop, bound a fixed port and pointed at live seed nodes, so
 * nothing they demonstrated was actually checked.
 *
 * <p>What is worth checking is the contract they advertised: that those exact
 * configuration shapes are still accepted and still mean what the comments said.
 * External embedders copy them, so a renamed setter or tightened validation is a
 * breaking change even though nothing in this repo calls them.
 */
public class ExampleUsageTest {

  private P2pConfig saved;
  private List<P2pEventHandler> savedHandlerList;
  private java.util.Map<Byte, P2pEventHandler> savedHandlerMap;

  private static final byte TEST_MESSAGE_TYPE = (byte) 0x01;

  @Before
  public void setUp() {
    saved = Parameter.p2pConfig;
    savedHandlerList = new ArrayList<>(Parameter.handlerList);
    savedHandlerMap = new java.util.HashMap<>(Parameter.handlerMap);
  }

  @After
  public void tearDown() {
    Parameter.handlerList = savedHandlerList;
    Parameter.handlerMap = savedHandlerMap;
    ChannelManager.isShutdown = false;
    Parameter.p2pConfig = saved;
  }

  /** The connection-tuning surface `ImportUsing` documented. */
  @Test
  public void importUsingConfigurationShapeIsStillAccepted() {
    P2pConfig config = new P2pConfig();
    config.setNetworkId(11111);
    config.setPort(18888);
    config.setDiscoverEnable(true);

    config.setSeedNodes(Arrays.asList(
        new InetSocketAddress("13.124.62.58", 18888),
        new InetSocketAddress("2600:1f13:908:1b00:e1fd:5a84:251c:a32a", 18888),
        new InetSocketAddress("127.0.0.4", 18888)));
    config.setActiveNodes(Arrays.asList(
        new InetSocketAddress("127.0.0.2", 18888),
        new InetSocketAddress("127.0.0.3", 18888)));
    config.setTrustNodes(Arrays.asList(
        new InetSocketAddress("127.0.0.2", 18888).getAddress()));

    config.setMinConnections(8);
    config.setMinActiveConnections(2);
    config.setMaxConnections(30);
    config.setMaxConnectionsWithSameIp(2);

    Assert.assertEquals(11111, config.getNetworkId());
    Assert.assertEquals(18888, config.getPort());
    Assert.assertTrue(config.isDiscoverEnable());
    Assert.assertEquals(3, config.getSeedNodes().size());
    Assert.assertEquals(2, config.getActiveNodes().size());
    Assert.assertEquals(1, config.getTrustNodes().size());
    Assert.assertEquals(8, config.getMinConnections());
    Assert.assertEquals(2, config.getMinActiveConnections());
    Assert.assertEquals(30, config.getMaxConnections());
    Assert.assertEquals(2, config.getMaxConnectionsWithSameIp());
  }

  /** The register / start / query / close lifecycle `ImportUsing` walked through. */
  @Test
  public void importUsingLifecycleRunsEndToEnd() throws P2pException {
    P2pConfig config = new P2pConfig();
    config.setNetworkId(11111);
    // The example hard-codes 18888; a test has to take a free port instead.
    config.setPort(TestPort.choose());
    config.setDiscoverEnable(false);
    config.setDisconnectionPolicyEnable(false);

    // messageTypes is protected, so it is set from inside the subclass — which
    // is exactly how the examples did it, in their constructor.
    P2pEventHandler handler = new P2pEventHandler() {
      {
        this.messageTypes = new HashSet<>(Arrays.asList(TEST_MESSAGE_TYPE));
      }

      @Override
      public void onMessage(Channel channel, byte[] data) {
      }
    };

    P2pService service = new P2pService();
    try {
      service.register(handler);
      service.start(config);

      Assert.assertNotNull(service.getP2pStats());
      Assert.assertNotNull(service.getAllNodes());
      Assert.assertNotNull(service.getTableNodes());
      Assert.assertNotNull(service.getConnectableNodes());
    } finally {
      service.close();
    }
  }

  /** Registering the same message type twice is still rejected. */
  @Test
  public void duplicateMessageTypeRegistrationIsRejected() throws P2pException {
    P2pEventHandler first = new P2pEventHandler() {
      {
        this.messageTypes = new HashSet<>(Arrays.asList(TEST_MESSAGE_TYPE));
      }

      @Override
      public void onMessage(Channel channel, byte[] data) {
      }
    };
    Parameter.addP2pEventHandle(first);

    P2pEventHandler clash = new P2pEventHandler() {
      {
        this.messageTypes = new HashSet<>(Arrays.asList(TEST_MESSAGE_TYPE));
      }

      @Override
      public void onMessage(Channel channel, byte[] data) {
      }
    };
    try {
      Parameter.addP2pEventHandle(clash);
      Assert.fail("expected the duplicate type to be rejected");
    } catch (P2pException expected) {
      Assert.assertEquals(P2pException.TypeEnum.TYPE_ALREADY_REGISTERED, expected.getType());
    }
  }

  /** The DNS *publish* shape `DnsExample1` documented. */
  @Test
  public void dnsPublishConfigurationShapeIsStillAccepted() {
    PublishConfig publishConfig = new PublishConfig();
    // Upstream's well-known test key, also used by AlgorithmTest. Keeping it in
    // a test rather than in copyable example code is part of the point: an
    // embedder must supply their own.
    publishConfig.setDnsPrivate(
        "b71c71a67e1177ad4e901695e1b4b9ee17ae16c6668d313eac2f96dbcda3f291");
    publishConfig.setDnsDomain("nodes.example.org");
    publishConfig.setKnownTreeUrls(Arrays.asList(
        "tree://APFGGTFOBVE2ZNAB3CSMNNX6RRK3ODIRLP2AA5U4YFAA6MSYZUYTQ@nodes.example1.org",
        "tree://APFGGTFOBVE2ZNAB3CSMNNX6RRK3ODIRLP2AA5U4YFAA6MSYZUYTQ@nodes.example2.org"));
    publishConfig.setDnsType(DnsType.AwsRoute53);
    publishConfig.setAccessKeyId("access-key");
    publishConfig.setAccessKeySecret("access-key-secret");
    publishConfig.setAwsHostZoneId("host-zone-id");
    publishConfig.setAwsRegion("us-east-1");
    publishConfig.setDnsPublishEnable(true);

    P2pConfig config = new P2pConfig();
    config.setNetworkId(11111);
    config.setPort(18888);
    config.setDiscoverEnable(true);
    config.setPublishConfig(publishConfig);

    Assert.assertTrue(config.getPublishConfig().isDnsPublishEnable());
    Assert.assertEquals(DnsType.AwsRoute53, config.getPublishConfig().getDnsType());
    Assert.assertEquals("nodes.example.org", config.getPublishConfig().getDnsDomain());
    Assert.assertEquals(2, config.getPublishConfig().getKnownTreeUrls().size());
    Assert.assertEquals("us-east-1", config.getPublishConfig().getAwsRegion());
  }

  /** The DNS *sync* shape `DnsExample2` documented: discovery off, tree urls on. */
  @Test
  public void dnsSyncConfigurationShapeIsStillAccepted() {
    P2pConfig config = new P2pConfig();
    config.setDiscoverEnable(false);
    config.setTreeUrls(Arrays.asList(
        "tree://APFGGTFOBVE2ZNAB3CSMNNX6RRK3ODIRLP2AA5U4YFAA6MSYZUYTQ@nodes.example.org"));

    Assert.assertFalse(config.isDiscoverEnable());
    Assert.assertEquals(1, config.getTreeUrls().size());
    // Publishing stays off unless a PublishConfig says otherwise.
    Assert.assertFalse(config.getPublishConfig() != null
        && config.getPublishConfig().isDnsPublishEnable());
  }

  /** Trust nodes come in as InetAddress, which is what StartApp's -t builds. */
  @Test
  public void trustNodesAreInetAddresses() {
    List<InetAddress> trustNodes = new ArrayList<>();
    for (String ip : "127.0.0.2,127.0.0.3".split(",")) {
      trustNodes.add(new InetSocketAddress(ip, 0).getAddress());
    }
    P2pConfig config = new P2pConfig();
    config.setTrustNodes(trustNodes);

    Assert.assertEquals(2, config.getTrustNodes().size());
    Assert.assertEquals("127.0.0.2", config.getTrustNodes().get(0).getHostAddress());
    Assert.assertEquals("127.0.0.3", config.getTrustNodes().get(1).getHostAddress());
  }
}
