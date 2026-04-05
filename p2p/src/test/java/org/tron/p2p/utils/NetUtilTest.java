package org.tron.p2p.utils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.base.Constant;
import org.tron.p2p.discover.Node;
import org.tron.p2p.protos.Discover;

public class NetUtilTest {

  @Test
  public void testValidIp() {
    boolean flag = NetUtil.validIpV4(null);
    Assert.assertFalse(flag);
    flag = NetUtil.validIpV4("a.1.1.1");
    Assert.assertFalse(flag);
    flag = NetUtil.validIpV4("1.1.1");
    Assert.assertFalse(flag);
    flag = NetUtil.validIpV4("0.0.0.0");
    Assert.assertFalse(flag);
    flag = NetUtil.validIpV4("256.1.2.3");
    Assert.assertFalse(flag);
    flag = NetUtil.validIpV4("1.1.1.1");
    Assert.assertTrue(flag);
  }

  @Test
  public void testValidNode() {
    boolean flag = NetUtil.validNode(null);
    Assert.assertFalse(flag);

    InetSocketAddress address = new InetSocketAddress("1.1.1.1", 1000);
    Node node = new Node(address);
    flag = NetUtil.validNode(node);
    Assert.assertTrue(flag);

    node.setId(new byte[10]);
    flag = NetUtil.validNode(node);
    Assert.assertFalse(flag);

    node = new Node(NetUtil.getNodeId(), "1.1.1", null, 1000);
    flag = NetUtil.validNode(node);
    Assert.assertFalse(flag);
  }

  @Test
  public void testGetNode() {
    Discover.Endpoint endpoint = Discover.Endpoint.newBuilder()
        .setPort(100).build();
    Node node = NetUtil.getNode(endpoint);
    Assert.assertEquals(100, node.getPort());
  }

  @Test
  public void testExternalIp() {
    String ip = NetUtil.getExternalIpV4();
    Assert.assertFalse(ip.startsWith("10."));
    Assert.assertFalse(ip.startsWith("192.168."));
    Assert.assertFalse(ip.startsWith("172.16."));
    Assert.assertFalse(ip.startsWith("172.17."));
    Assert.assertFalse(ip.startsWith("172.18."));
    Assert.assertFalse(ip.startsWith("172.19."));
    Assert.assertFalse(ip.startsWith("172.20."));
    Assert.assertFalse(ip.startsWith("172.21."));
    Assert.assertFalse(ip.startsWith("172.22."));
    Assert.assertFalse(ip.startsWith("172.23."));
    Assert.assertFalse(ip.startsWith("172.24."));
    Assert.assertFalse(ip.startsWith("172.25."));
    Assert.assertFalse(ip.startsWith("172.26."));
    Assert.assertFalse(ip.startsWith("172.27."));
    Assert.assertFalse(ip.startsWith("172.28."));
    Assert.assertFalse(ip.startsWith("172.29."));
    Assert.assertFalse(ip.startsWith("172.30."));
    Assert.assertFalse(ip.startsWith("172.31."));
  }

  @Test
  public void testGetIP() {
    //notice: please check that you only have one externalIP
    String ip1 = null, ip2 = null, ip3 = null;
    try {
      Method method = NetUtil.class.getDeclaredMethod("getExternalIp", String.class, boolean.class);
      method.setAccessible(true);
      ip1 = (String) method.invoke(NetUtil.class, Constant.ipV4Urls.get(0), true);
      ip2 = (String) method.invoke(NetUtil.class, Constant.ipV4Urls.get(1), true);
      ip3 = (String) method.invoke(NetUtil.class, Constant.ipV4Urls.get(2), true);
    } catch (Exception e) {
      Assert.fail();
    }
    String ip4 = NetUtil.getExternalIpV4();
    Assert.assertEquals(ip1, ip4);
    Assert.assertEquals(ip2, ip4);
    Assert.assertEquals(ip3, ip4);
  }

  private String getLanIP2() {
    String lanIP;
    try (Socket s = new Socket("www.baidu.com", 80)) {
      lanIP = s.getLocalAddress().getHostAddress();
    } catch (IOException e) {
      lanIP = "127.0.0.1";
    }
    return lanIP;
  }

  @Test
  public void testGetLanIP() {
    String lanIpv4 = NetUtil.getLanIP();
    Assert.assertNotNull(lanIpv4);
    String lanIpv4Old = getLanIP2();
    Assert.assertEquals(lanIpv4, lanIpv4Old);
  }

  @Test
  public void testIPv6Format() {
    String std = "fe80:0:0:0:204:61ff:fe9d:f156";
    int randomPort = 10001;
    String ip1 = new InetSocketAddress("fe80:0000:0000:0000:0204:61ff:fe9d:f156",
        randomPort).getAddress().getHostAddress();
    Assert.assertEquals(ip1, std);

    String ip2 = new InetSocketAddress("fe80::204:61ff:fe9d:f156",
        randomPort).getAddress().getHostAddress();
    Assert.assertEquals(ip2, std);

    String ip3 = new InetSocketAddress("fe80:0000:0000:0000:0204:61ff:254.157.241.86",
        randomPort).getAddress().getHostAddress();
    Assert.assertEquals(ip3, std);

    String ip4 = new InetSocketAddress("fe80:0:0:0:0204:61ff:254.157.241.86",
        randomPort).getAddress().getHostAddress();
    Assert.assertEquals(ip4, std);

    String ip5 = new InetSocketAddress("fe80::204:61ff:254.157.241.86",
        randomPort).getAddress().getHostAddress();
    Assert.assertEquals(ip5, std);

    String ip6 = new InetSocketAddress("FE80::204:61ff:254.157.241.86",
        randomPort).getAddress().getHostAddress();
    Assert.assertEquals(ip6, std);

    String ip7 = new InetSocketAddress("[fe80:0:0:0:204:61ff:fe9d:f156]",
        randomPort).getAddress().getHostAddress();
    Assert.assertEquals(ip7, std);
  }

  @Test
  public void testParseIpv6() {
    InetSocketAddress address1 = NetUtil.parseInetSocketAddress(
        "[2600:1f13:908:1b00:e1fd:5a84:251c:a32a]:18888");
    Assert.assertNotNull(address1);
    Assert.assertEquals(18888, address1.getPort());
    Assert.assertEquals("2600:1f13:908:1b00:e1fd:5a84:251c:a32a",
        address1.getAddress().getHostAddress());

    try {
      NetUtil.parseInetSocketAddress(
          "[2600:1f13:908:1b00:e1fd:5a84:251c:a32a]:abcd");
      Assert.fail();
    } catch (RuntimeException e) {
      Assert.assertTrue(true);
    }

    try {
      NetUtil.parseInetSocketAddress(
          "2600:1f13:908:1b00:e1fd:5a84:251c:a32a:18888");
      Assert.fail();
    } catch (RuntimeException e) {
      Assert.assertTrue(true);
    }

    try {
      NetUtil.parseInetSocketAddress(
          "[2600:1f13:908:1b00:e1fd:5a84:251c:a32a:18888");
      Assert.fail();
    } catch (RuntimeException e) {
      Assert.assertTrue(true);
    }

    try {
      NetUtil.parseInetSocketAddress(
          "2600:1f13:908:1b00:e1fd:5a84:251c:a32a]:18888");
      Assert.fail();
    } catch (RuntimeException e) {
      Assert.assertTrue(true);
    }

    try {
      NetUtil.parseInetSocketAddress(
          "2600:1f13:908:1b00:e1fd:5a84:251c:a32a");
      Assert.fail();
    } catch (RuntimeException e) {
      Assert.assertTrue(true);
    }

    InetSocketAddress address5 = NetUtil.parseInetSocketAddress(
        "192.168.0.1:18888");
    Assert.assertNotNull(address5);
  }

}
