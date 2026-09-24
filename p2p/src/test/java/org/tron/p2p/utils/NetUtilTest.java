package org.tron.p2p.utils;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
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
    // a trailing line terminator must not be accepted (matches() vs find())
    flag = NetUtil.validIpV4("1.1.1.1\n");
    Assert.assertFalse(flag);

    flag = NetUtil.validIpV6(null);
    Assert.assertFalse(flag);
    flag = NetUtil.validIpV6("evil.example.com");
    Assert.assertFalse(flag);
    flag = NetUtil.validIpV6("2001:db8::1");
    Assert.assertTrue(flag);
    // a trailing line terminator must not be accepted (matches() vs find())
    flag = NetUtil.validIpV6("2001:db8::1\n");
    Assert.assertFalse(flag);
    // a scope id may not contain whitespace (%\\S+, not %.+)
    flag = NetUtil.validIpV6("fe80::1%eth0");
    Assert.assertTrue(flag);
    flag = NetUtil.validIpV6("fe80::1%eth0 ");
    Assert.assertFalse(flag);
    flag = NetUtil.validIpV6("fe80::1%e t h0");
    Assert.assertFalse(flag);
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

  /**
   * getAllLocalAddress feeds the filter that stops a node dialling itself, so it must
   * include loopback and must strip the %scope suffix that link-local IPv6 addresses
   * carry — an unstripped suffix would never match the address seen on the wire.
   * Enumerating local interfaces needs no external network.
   */
  @Test
  public void testGetAllLocalAddress() {
    Set<String> addresses = NetUtil.getAllLocalAddress();

    Assert.assertNotNull(addresses);
    Assert.assertFalse("every host has at least a loopback address", addresses.isEmpty());
    Assert.assertTrue("loopback must be present", addresses.contains("127.0.0.1"));

    for (String address : addresses) {
      Assert.assertFalse("scope id must be stripped, got " + address, address.contains("%"));
      Assert.assertFalse(address.isEmpty());
    }
  }

  @Test
  public void testGetNode() {
    Discover.Endpoint endpoint = Discover.Endpoint.newBuilder()
        .setPort(100).build();
    Node node = NetUtil.getNode(endpoint);
    Assert.assertEquals(100, node.getPort());
  }

  /**
   * getExternalIpV4 queries public IP-echo services and returns null when every one of
   * them fails, so the assertions below are guarded by an assumption rather than left to
   * NPE on a host with no egress. When the lookup does succeed the result must be a
   * routable address: a private one would be advertised to peers that cannot reach it.
   */
  @Test
  public void testExternalIp() {
    String ip = NetUtil.getExternalIpV4();
    Assume.assumeNotNull(ip);

    Assert.assertTrue("not a valid IPv4: " + ip, NetUtil.validIpV4(ip));
    Assert.assertFalse(ip.startsWith("10."));
    Assert.assertFalse(ip.startsWith("192.168."));
    // 172.16.0.0/12 is 172.16 through 172.31
    for (int second = 16; second <= 31; second++) {
      Assert.assertFalse("private address returned: " + ip,
          ip.startsWith("172." + second + "."));
    }
  }

  /**
   * Upstream's version of this test called three public IP-echo services and asserted
   * all three returned the same string. That makes the test depend on the network and
   * on the host having exactly one egress address, which is why it was unreliable
   * (libp2p's own CI never ran it). It is replaced here by a loopback HTTP server, so
   * the same code path — fetch, read a line, parse, validate — runs deterministically
   * and the rejection branches get covered too.
   */
  @Test
  public void testGetIP() throws Exception {
    Method method = NetUtil.class.getDeclaredMethod("getExternalIp", String.class, boolean.class);
    method.setAccessible(true);

    // a well-formed IPv4 body is returned verbatim
    assertExternalIp(method, "1.2.3.4\n", true, "1.2.3.4");
    // an IPv6 literal is rejected when an IPv4 address was requested
    assertExternalIp(method, "2001:db8::1\n", true, null);
    // an IPv4 literal is rejected when an IPv6 address was requested
    assertExternalIp(method, "1.2.3.4\n", false, null);
    // an empty body is rejected
    assertExternalIp(method, "\n", true, null);
  }

  /**
   * Serves {@code body} once over loopback and asserts getExternalIp returns
   * {@code expected}. Bodies are IP literals only, so no DNS lookup is triggered
   * and the test stays hermetic.
   */
  private void assertExternalIp(Method method, String body, boolean askIpv4, String expected)
      throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, bytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
      }
    });
    server.start();
    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
      Assert.assertEquals(expected, method.invoke(NetUtil.class, url, askIpv4));
    } finally {
      server.stop(0);
    }
  }

  /**
   * Upstream compared getLanIP() against the source address the kernel picks for a socket
   * to www.baidu.com. Those are two different definitions of "the LAN IP": getLanIP()
   * walks NetworkInterface.getNetworkInterfaces() and returns the first non-reserved IPv4
   * on an interface that is up, non-loopback and non-virtual, whereas the socket reflects
   * the routing table. They disagree on any multi-homed host — a VPN or Docker bridge is
   * enough — and offline the socket path falls back to 127.0.0.1 while the enumeration
   * still finds the real address. This asserts the contract getLanIP() actually has, and
   * needs no network.
   */
  @Test
  public void testGetLanIP() {
    String lanIpv4 = NetUtil.getLanIP();

    Assert.assertNotNull(lanIpv4);
    Assert.assertTrue("not a valid IPv4: " + lanIpv4, NetUtil.validIpV4(lanIpv4));
    // either a usable LAN address, or the documented loopback fallback when no
    // interface qualifies
    if (!"127.0.0.1".equals(lanIpv4)) {
      Assert.assertFalse("must not return a multicast or broadcast address",
          lanIpv4.startsWith("224.") || lanIpv4.startsWith("255."));
    }
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
