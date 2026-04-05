package org.tron.p2p.discover.protocol.kad;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.kad.PingMessage;
import org.tron.p2p.discover.socket.UdpEvent;

import java.net.InetSocketAddress;

public class KadServiceTest {

  private static KadService kadService;
  private static Node node1;
  private static Node node2;

  @BeforeClass
  public static void init() {
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setDiscoverEnable(false);
    kadService = new KadService();
    kadService.init();
    KadService.setPingTimeout(300);
    node1 = new Node(new InetSocketAddress("127.0.0.1", 22222));
    node2 = new Node(new InetSocketAddress("127.0.0.2", 22222));
  }

  @Test
  public void test() {
    Assert.assertNotNull(kadService.getPongTimer());
    Assert.assertNotNull(kadService.getPublicHomeNode());
    Assert.assertEquals(0, kadService.getAllNodes().size());

    NodeHandler nodeHandler = kadService.getNodeHandler(node1);
    Assert.assertNotNull(nodeHandler);
    Assert.assertEquals(1, kadService.getAllNodes().size());

    UdpEvent event = new UdpEvent(new PingMessage(node2, kadService.getPublicHomeNode()),
        new InetSocketAddress(node2.getHostV4(), node2.getPort()));
    kadService.handleEvent(event);
    Assert.assertEquals(2, kadService.getAllNodes().size());

  }


  @AfterClass
  public static void destroy() {
    kadService.close();
  }
}
