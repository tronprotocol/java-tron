package org.tron.p2p.discover;

import java.net.InetSocketAddress;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.utils.NetUtil;

public class NodeTest {

  @Before
  public void init() {
    Parameter.p2pConfig = new P2pConfig();
  }

  @Test
  public void nodeTest() throws InterruptedException {
    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 10001));
    Assert.assertEquals(64, node1.getId().length);

    Node node2 = new Node(NetUtil.getNodeId(), "127.0.0.1", null, 10002);
    boolean isDif = node1.equals(node2);
    Assert.assertFalse(isDif);

    long lastModifyTime = node1.getUpdateTime();
    Thread.sleep(1);
    node1.touch();
    Assert.assertNotEquals(lastModifyTime, node1.getUpdateTime());

    node1.setP2pVersion(11111);
    Assert.assertTrue(node1.isConnectible(11111));
    Assert.assertFalse(node1.isConnectible(11112));
    Node node3 = new Node(NetUtil.getNodeId(), "127.0.0.1", null, 10003, 10004);
    node3.setP2pVersion(11111);
    Assert.assertFalse(node3.isConnectible(11111));
  }

  @Test
  public void ipV4CompatibleTest() {
    Parameter.p2pConfig.setIp("127.0.0.1");
    Parameter.p2pConfig.setIpv6(null);

    Node node1 = new Node(NetUtil.getNodeId(), "127.0.0.1", null, 10002);
    Assert.assertNotNull(node1.getPreferInetSocketAddress());

    Node node2 = new Node(NetUtil.getNodeId(), null, "fe80:0:0:0:204:61ff:fe9d:f156", 10002);
    Assert.assertNull(node2.getPreferInetSocketAddress());

    Node node3 = new Node(NetUtil.getNodeId(), "127.0.0.1", "fe80:0:0:0:204:61ff:fe9d:f156", 10002);
    Assert.assertNotNull(node3.getPreferInetSocketAddress());
  }

  @Test
  public void ipV6CompatibleTest() {
    Parameter.p2pConfig.setIp(null);
    Parameter.p2pConfig.setIpv6("fe80:0:0:0:204:61ff:fe9d:f157");

    Node node1 = new Node(NetUtil.getNodeId(), "127.0.0.1", null, 10002);
    Assert.assertNull(node1.getPreferInetSocketAddress());

    Node node2 = new Node(NetUtil.getNodeId(), null, "fe80:0:0:0:204:61ff:fe9d:f156", 10002);
    Assert.assertNotNull(node2.getPreferInetSocketAddress());

    Node node3 = new Node(NetUtil.getNodeId(), "127.0.0.1", "fe80:0:0:0:204:61ff:fe9d:f156", 10002);
    Assert.assertNotNull(node3.getPreferInetSocketAddress());
  }

  @Test
  public void ipCompatibleTest() {
    Parameter.p2pConfig.setIp("127.0.0.1");
    Parameter.p2pConfig.setIpv6("fe80:0:0:0:204:61ff:fe9d:f157");

    Node node1 = new Node(NetUtil.getNodeId(), "127.0.0.1", null, 10002);
    Assert.assertNotNull(node1.getPreferInetSocketAddress());

    Node node2 = new Node(NetUtil.getNodeId(), null, "fe80:0:0:0:204:61ff:fe9d:f156", 10002);
    Assert.assertNotNull(node2.getPreferInetSocketAddress());

    Node node3 = new Node(NetUtil.getNodeId(), "127.0.0.1", "fe80:0:0:0:204:61ff:fe9d:f156", 10002);
    Assert.assertNotNull(node3.getPreferInetSocketAddress());

    Node node4 = new Node(NetUtil.getNodeId(), null, null, 10002);
    Assert.assertNull(node4.getPreferInetSocketAddress());
  }
}
