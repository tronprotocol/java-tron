package org.tron.p2p.dns;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.dns.tree.Algorithm;
import org.tron.p2p.dns.tree.Entry;
import org.tron.p2p.dns.tree.NodesEntry;
import org.tron.p2p.protos.Discover.EndPoints;
import org.tron.p2p.protos.Discover.Endpoint;
import org.tron.p2p.utils.NetUtil;

public class DnsNodeTest {

  @Test
  public void invalidIpv4IsNeverPassedToAddressParsing() throws Exception {
    DnsNode node = new DnsNode(null, "localhost", "2001:db8::1", 18888) {
      @Override
      public String ipToString(String ip) throws UnknownHostException {
        Assert.assertTrue("Only IP literals may reach address parsing",
            NetUtil.validIpV4(ip) || NetUtil.validIpV6(ip));
        return super.ipToString(ip);
      }
    };

    Assert.assertNull(node.getHostV4());
    Assert.assertEquals(new DnsNode(null, null, "2001:db8::1", 18888), node);
  }

  @Test
  public void invalidIpv4DoesNotDiscardValidIpv6InNodesRecord() throws Exception {
    for (String hostV4 : new String[]{"localhost", "peer.example.invalid", "999.1.1.1",
        "::1", ""}) {
      EndPoints endpoints = EndPoints.newBuilder()
          .addNodes(Endpoint.newBuilder()
              .setAddress(ByteString.copyFromUtf8(hostV4))
              .setAddressIpv6(ByteString.copyFromUtf8("2001:DB8::1"))
              .setPort(18888))
          .addNodes(Endpoint.newBuilder()
              .setAddress(ByteString.copyFromUtf8("192.0.2.1"))
              .setPort(18889))
          .build();
      String txt = Entry.nodesPrefix + Algorithm.encode64(endpoints.toByteArray());

      List<DnsNode> nodes = NodesEntry.parseEntry(txt).getNodes();

      Assert.assertEquals(2, nodes.size());
      Assert.assertNull(nodes.get(0).getHostV4());
      Assert.assertEquals("2001:db8:0:0:0:0:0:1", nodes.get(0).getHostV6());
      Assert.assertEquals(new DnsNode(null, null, "2001:db8::1", 18888), nodes.get(0));
      Assert.assertEquals(new DnsNode(null, "192.0.2.1", null, 18889), nodes.get(1));

      EndPoints republished = EndPoints.parseFrom(Algorithm.decode64(DnsNode.compress(nodes)));
      Assert.assertTrue(republished.getNodes(0).getAddress().isEmpty());
      Assert.assertEquals(nodes.get(0).getHostV6(),
          republished.getNodes(0).getAddressIpv6().toStringUtf8());
    }
  }

  @Test
  public void unusableEndpointDoesNotAbortTheNodesRecord() throws Exception {
    for (String hostV4 : new String[]{"localhost", "999.1.1.1", "::1", ""}) {
      EndPoints endpoints = EndPoints.newBuilder()
          .addNodes(Endpoint.newBuilder()
              .setAddress(ByteString.copyFromUtf8("192.0.2.1"))
              .setPort(18888))
          .addNodes(Endpoint.newBuilder()
              .setAddress(ByteString.copyFromUtf8(hostV4))
              .setAddressIpv6(ByteString.copyFromUtf8("not-an-ipv6-address"))
              .setPort(18889))
          .build();
      String txt = Entry.nodesPrefix + Algorithm.encode64(endpoints.toByteArray());

      List<DnsNode> nodes = NodesEntry.parseEntry(txt).getNodes();

      Assert.assertEquals(2, nodes.size());
      Assert.assertEquals(new DnsNode(null, "192.0.2.1", null, 18888), nodes.get(0));
      Assert.assertNotNull(nodes.get(0).getInetSocketAddressV4());
      Assert.assertNull(nodes.get(1).getHostV4());
      Assert.assertNull(nodes.get(1).getHostV6());
      Assert.assertNull(nodes.get(1).getPreferInetSocketAddress());
    }
  }

  @Test
  public void unavailableIpv6ZoneDoesNotAbortTheNodesRecord() throws Exception {
    EndPoints endpoints = EndPoints.newBuilder()
        .addNodes(Endpoint.newBuilder()
            .setAddress(ByteString.copyFromUtf8("192.0.2.1"))
            .setAddressIpv6(ByteString.copyFromUtf8(
                "2001:db8::1%libp2p-missing-interface"))
            .setPort(18888))
        .addNodes(Endpoint.newBuilder()
            .setAddressIpv6(ByteString.copyFromUtf8("2001:DB8::2"))
            .setPort(18889))
        .build();
    String txt = Entry.nodesPrefix + Algorithm.encode64(endpoints.toByteArray());

    List<DnsNode> nodes = NodesEntry.parseEntry(txt).getNodes();

    Assert.assertEquals(2, nodes.size());
    Assert.assertEquals("192.0.2.1", nodes.get(0).getHostV4());
    Assert.assertNull(nodes.get(0).getHostV6());
    Assert.assertEquals(18888, nodes.get(0).getPort());
    Assert.assertEquals(new DnsNode(null, "192.0.2.1", null, 18888), nodes.get(0));
    Assert.assertEquals("2001:db8:0:0:0:0:0:2", nodes.get(1).getHostV6());
    Assert.assertEquals(18889, nodes.get(1).getPort());
  }

  @Test
  public void testCompressDnsNode() throws UnknownHostException, InvalidProtocolBufferException {
    DnsNode[] nodes = new DnsNode[] {
        new DnsNode(null, "192.168.0.1", null, 10000),
    };
    List<DnsNode> nodeList = Arrays.asList(nodes);
    String enrContent = DnsNode.compress(nodeList);

    List<DnsNode> dnsNodes = DnsNode.decompress(enrContent);
    Assert.assertEquals(1, dnsNodes.size());
    Assert.assertTrue(nodes[0].equals(dnsNodes.get(0)));
  }

  @Test
  public void testSortDnsNode() throws UnknownHostException {
    DnsNode[] nodes = new DnsNode[] {
        new DnsNode(null, "192.168.0.1", null, 10000),
        new DnsNode(null, "192.168.0.2", null, 10000),
        new DnsNode(null, "192.168.0.3", null, 10000),
        new DnsNode(null, "192.168.0.4", null, 10000),
        new DnsNode(null, "192.168.0.5", null, 10000),
        new DnsNode(null, "192.168.0.6", null, 10001),
        new DnsNode(null, "192.168.0.6", null, 10002),
        new DnsNode(null, "192.168.0.6", null, 10003),
        new DnsNode(null, "192.168.0.6", null, 10004),
        new DnsNode(null, "192.168.0.6", null, 10005),
        new DnsNode(null, "192.168.0.10", "fe80::0001", 10005),
        new DnsNode(null, "192.168.0.10", "fe80::0002", 10005),
        new DnsNode(null, null, "fe80::0001", 10000),
        new DnsNode(null, null, "fe80::0002", 10000),
        new DnsNode(null, null, "fe80::0002", 10001),
    };
    List<DnsNode> nodeList = Arrays.asList(nodes);
    Collections.shuffle(nodeList); //random order
    Collections.sort(nodeList);
    for (int i = 0; i < nodeList.size(); i++) {
      Assert.assertTrue(nodes[i].equals(nodeList.get(i)));
    }
  }
}
