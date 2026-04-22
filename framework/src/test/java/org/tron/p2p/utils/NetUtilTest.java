package org.tron.p2p.utils;

import static org.mockito.Mockito.mockStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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
    Discover.Endpoint endpoint =
        Discover.Endpoint.newBuilder().setPort(100).build();
    Node node = NetUtil.getNode(endpoint);
    Assert.assertEquals(100, node.getPort());
  }

  @Test
  public void testGetExternalIpWithMock() throws Exception {
    String fakeIp = "203.0.113.42";
    URLConnection mockConn = Mockito.mock(URLConnection.class);
    Mockito.when(mockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(
            fakeIp.getBytes(StandardCharsets.UTF_8)));

    try (MockedConstruction<URL> urlMock = Mockito.mockConstruction(URL.class,
        (mock, context) -> Mockito.when(mock.openConnection())
            .thenReturn(mockConn))) {

      Method method = NetUtil.class.getDeclaredMethod(
          "getExternalIp", String.class, boolean.class);
      method.setAccessible(true);

      String ip = (String) method.invoke(null,
          "http://mock-service.test", true);
      Assert.assertEquals(fakeIp, ip);
    }
  }

  @Test
  public void testGetExternalIpReturnsNullOnFailure() throws Exception {
    URLConnection mockConn = Mockito.mock(URLConnection.class);
    Mockito.when(mockConn.getInputStream())
        .thenThrow(new IOException("Connection refused"));

    try (MockedConstruction<URL> urlMock = Mockito.mockConstruction(URL.class,
        (mock, context) -> Mockito.when(mock.openConnection())
            .thenReturn(mockConn))) {

      Method method = NetUtil.class.getDeclaredMethod(
          "getExternalIp", String.class, boolean.class);
      method.setAccessible(true);

      String ip = (String) method.invoke(null,
          "http://unreachable.test", true);
      Assert.assertNull(ip);
    }
  }

  @Test
  public void testGetExternalIpRejectsInvalidIp() throws Exception {
    String invalidIp = "not-an-ip";
    URLConnection mockConn = Mockito.mock(URLConnection.class);
    Mockito.when(mockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(
            invalidIp.getBytes(StandardCharsets.UTF_8)));

    try (MockedConstruction<URL> urlMock = Mockito.mockConstruction(URL.class,
        (mock, context) -> Mockito.when(mock.openConnection())
            .thenReturn(mockConn))) {

      Method method = NetUtil.class.getDeclaredMethod(
          "getExternalIp", String.class, boolean.class);
      method.setAccessible(true);

      String ip = (String) method.invoke(null,
          "http://bad-service.test", true);
      Assert.assertNull(ip);
    }
  }

  @Test
  public void testGetExternalIpRejectsEmptyResponse() throws Exception {
    URLConnection mockConn = Mockito.mock(URLConnection.class);
    Mockito.when(mockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(
            "".getBytes(StandardCharsets.UTF_8)));

    try (MockedConstruction<URL> urlMock = Mockito.mockConstruction(URL.class,
        (mock, context) -> Mockito.when(mock.openConnection())
            .thenReturn(mockConn))) {

      Method method = NetUtil.class.getDeclaredMethod(
          "getExternalIp", String.class, boolean.class);
      method.setAccessible(true);

      String ip = (String) method.invoke(null,
          "http://empty-service.test", true);
      Assert.assertNull(ip);
    }
  }

  @Test
  public void testGetLanIP() {
    String lanIpv4 = NetUtil.getLanIP();
    Assert.assertNotNull(lanIpv4);
    // verify it's a valid IPv4 format (not relying on external network)
    Assert.assertTrue(
        "LAN IP should be valid IPv4 or loopback",
        NetUtil.validIpV4(lanIpv4) || "127.0.0.1".equals(lanIpv4));
  }

  @Test
  public void testIPv6Format() {
    String std = "fe80:0:0:0:204:61ff:fe9d:f156";
    int randomPort = 10001;
    String ip1 =
        new InetSocketAddress(
                "fe80:0000:0000:0000:0204:61ff:fe9d:f156", randomPort)
            .getAddress()
            .getHostAddress();
    Assert.assertEquals(ip1, std);

    String ip2 =
        new InetSocketAddress("fe80::204:61ff:fe9d:f156", randomPort)
            .getAddress()
            .getHostAddress();
    Assert.assertEquals(ip2, std);

    String ip3 =
        new InetSocketAddress(
                "fe80:0000:0000:0000:0204:61ff:254.157.241.86", randomPort)
            .getAddress()
            .getHostAddress();
    Assert.assertEquals(ip3, std);

    String ip4 =
        new InetSocketAddress(
                "fe80:0:0:0:0204:61ff:254.157.241.86", randomPort)
            .getAddress()
            .getHostAddress();
    Assert.assertEquals(ip4, std);

    String ip5 =
        new InetSocketAddress(
                "fe80::204:61ff:254.157.241.86", randomPort)
            .getAddress()
            .getHostAddress();
    Assert.assertEquals(ip5, std);

    String ip6 =
        new InetSocketAddress(
                "FE80::204:61ff:254.157.241.86", randomPort)
            .getAddress()
            .getHostAddress();
    Assert.assertEquals(ip6, std);

    String ip7 =
        new InetSocketAddress(
                "[fe80:0:0:0:204:61ff:fe9d:f156]", randomPort)
            .getAddress()
            .getHostAddress();
    Assert.assertEquals(ip7, std);
  }

  @Test
  public void testParseIpv6() {
    InetSocketAddress address1 =
        NetUtil.parseInetSocketAddress(
            "[2600:1f13:908:1b00:e1fd:5a84:251c:a32a]:18888");
    Assert.assertNotNull(address1);
    Assert.assertEquals(18888, address1.getPort());
    Assert.assertEquals(
        "2600:1f13:908:1b00:e1fd:5a84:251c:a32a",
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

    InetSocketAddress address5 =
        NetUtil.parseInetSocketAddress("192.168.0.1:18888");
    Assert.assertNotNull(address5);
  }
}
