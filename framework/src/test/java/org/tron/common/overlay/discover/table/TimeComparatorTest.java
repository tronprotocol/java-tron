package org.tron.common.overlay.discover.table;

import org.junit.Assert;
import org.junit.Test;
import org.tron.common.overlay.discover.node.Node;

public class TimeComparatorTest {

  @Test
  public void test() throws InterruptedException {
    Node node1 = Node.instanceOf("127.0.0.1:10001");
    NodeEntry ne1 = new NodeEntry(node1);
    Thread.sleep(1);
    Node node2 = Node.instanceOf("127.0.0.1:10002");
    NodeEntry ne2 = new NodeEntry(node2);
    TimeComparator tc = new TimeComparator();
    int result = tc.compare(ne1, ne2);
    Assert.assertEquals(1, result);

  }
}
