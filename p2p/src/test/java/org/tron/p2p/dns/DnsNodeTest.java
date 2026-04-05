package org.tron.p2p.dns;


import com.google.protobuf.InvalidProtocolBufferException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class DnsNodeTest {

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
