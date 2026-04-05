package org.tron.p2p.discover.protocol.kad.table;

import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.discover.Node;
import org.tron.p2p.utils.NetUtil;

import java.net.InetSocketAddress;

public class TimeComparatorTest {
  @Test
  public void test() throws InterruptedException {
    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 10001));
    NodeEntry ne1 = new NodeEntry(NetUtil.getNodeId(), node1);
    Thread.sleep(1);
    Node node2 = new Node(new InetSocketAddress("127.0.0.1", 10002));
    NodeEntry ne2 = new NodeEntry(NetUtil.getNodeId(), node2);
    TimeComparator tc = new TimeComparator();
    int result = tc.compare(ne1, ne2);
    Assert.assertEquals(1, result);

  }
}
