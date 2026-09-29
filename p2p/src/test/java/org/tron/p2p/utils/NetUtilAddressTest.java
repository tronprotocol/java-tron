package org.tron.p2p.utils;

import com.google.protobuf.ByteString;
import java.net.InetSocketAddress;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.discover.Node;
import org.tron.p2p.protos.Discover.Endpoint;

/**
 * Address parsing and endpoint conversion. parseInetSocketAddress reads operator
 * config, and getNode converts the peer-supplied Endpoint of every discovery
 * message, so both shapes of malformed input matter.
 */
public class NetUtilAddressTest {

  @Test
  public void parsesIpv4WithPort() {
    InetSocketAddress address = NetUtil.parseInetSocketAddress("127.0.0.1:18888");
    Assert.assertEquals("127.0.0.1", address.getAddress().getHostAddress());
    Assert.assertEquals(18888, address.getPort());
  }

  @Test
  public void parsesBracketedIpv6WithPort() {
    InetSocketAddress address = NetUtil.parseInetSocketAddress("[::1]:18888");
    Assert.assertEquals(18888, address.getPort());
    Assert.assertNotNull(address.getAddress());
  }

  @Test
  public void tolerantOfSurroundingWhitespace() {
    Assert.assertEquals(18888,
        NetUtil.parseInetSocketAddress(" 127.0.0.1:18888 ".trim()).getPort());
  }

  @Test(expected = RuntimeException.class)
  public void rejectsBareIpv6WithoutBrackets() {
    // Ambiguous: every colon looks like a port separator.
    NetUtil.parseInetSocketAddress("::1:18888");
  }

  @Test(expected = RuntimeException.class)
  public void rejectsAnAddressWithNoPort() {
    NetUtil.parseInetSocketAddress("127.0.0.1");
  }

  @Test(expected = NumberFormatException.class)
  public void rejectsANonNumericPort() {
    NetUtil.parseInetSocketAddress("127.0.0.1:notaport");
  }

  @Test
  public void getNodeReadsBothAddressFamilies() {
    byte[] id = NetUtil.getNodeId();
    Endpoint endpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(id))
        .setAddress(ByteString.copyFrom(ByteArray.fromString("127.0.0.1")))
        .setAddressIpv6(ByteString.copyFrom(ByteArray.fromString("::1")))
        .setPort(18888)
        .build();

    Node node = NetUtil.getNode(endpoint);
    Assert.assertArrayEquals(id, node.getId());
    Assert.assertEquals("127.0.0.1", node.getHostV4());
    Assert.assertEquals(18888, node.getPort());
  }

  @Test
  public void getNodeIdIsRandomAndSixtyFourBytes() {
    byte[] first = NetUtil.getNodeId();
    Assert.assertEquals(64, first.length);
    Assert.assertFalse(java.util.Arrays.equals(first, NetUtil.getNodeId()));
  }

  @Test
  public void localAddressesAlwaysIncludeLoopback() {
    Set<String> local = NetUtil.getAllLocalAddress();
    Assert.assertNotNull(local);
    Assert.assertTrue("loopback should always be present", local.contains("127.0.0.1"));
    for (String ip : local) {
      Assert.assertFalse("zone suffixes must be stripped", ip.contains("%"));
    }
  }

  @Test
  public void ipVersionCheckersAgreeWithTheirPatterns() {
    Assert.assertTrue(NetUtil.validIpV4("127.0.0.1"));
    Assert.assertTrue(NetUtil.validIpV4("255.255.255.255"));
    Assert.assertFalse(NetUtil.validIpV4("256.0.0.1"));
    Assert.assertFalse(NetUtil.validIpV4("127.0.0"));
    Assert.assertFalse(NetUtil.validIpV4(""));
    Assert.assertFalse(NetUtil.validIpV4(null));
    Assert.assertFalse(NetUtil.validIpV4("example.org"));

    Assert.assertTrue(NetUtil.validIpV6("::1"));
    Assert.assertTrue(NetUtil.validIpV6("2001:db8::1"));
    Assert.assertFalse(NetUtil.validIpV6("127.0.0.1"));
    Assert.assertFalse(NetUtil.validIpV6(""));
    Assert.assertFalse(NetUtil.validIpV6(null));
  }
}
