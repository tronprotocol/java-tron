package org.tron.p2p.discover.protocol.kad.table;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.utils.NetUtil;

public class TimeComparatorTest {
  private P2pConfig previousConfig;

  @Before
  public void setUp() {
    previousConfig = Parameter.p2pConfig;
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp("127.0.0.1");
  }

  @After
  public void tearDown() {
    Parameter.p2pConfig = previousConfig;
  }

  @Test
  public void test() throws Exception {
    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 10001));
    NodeEntry ne1 = new NodeEntry(NetUtil.getNodeId(), node1);
    Node node2 = new Node(new InetSocketAddress("127.0.0.1", 10002));
    NodeEntry ne2 = new NodeEntry(NetUtil.getNodeId(), node2);
    Field modified = NodeEntry.class.getDeclaredField("modified");
    modified.setAccessible(true);
    modified.setLong(ne1, 1);
    modified.setLong(ne2, 2);
    TimeComparator tc = new TimeComparator();
    Assert.assertTrue(tc.compare(ne1, ne2) < 0);
    Assert.assertTrue(tc.compare(ne2, ne1) > 0);
    modified.setLong(ne2, 1);
    Assert.assertEquals(0, tc.compare(ne1, ne2));
  }
}
