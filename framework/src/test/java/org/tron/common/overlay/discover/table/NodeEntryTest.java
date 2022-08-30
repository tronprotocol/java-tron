package org.tron.common.overlay.discover.table;

import org.junit.Assert;
import org.junit.Test;
import org.tron.common.overlay.discover.node.Node;

public class NodeEntryTest {

  @Test
  public void test() throws InterruptedException {
    Node node1 = Node.instanceOf("127.0.0.1:10001");
    NodeEntry nodeEntry = new NodeEntry(Node.getNodeId(), node1);

    long lastModified = nodeEntry.getModified();
    Thread.sleep(1);
    nodeEntry.touch();
    long nowModified = nodeEntry.getModified();
    Assert.assertNotEquals(lastModified, nowModified);

    Node node2 = Node.instanceOf("127.0.0.1:10002");
    NodeEntry nodeEntry2 = new NodeEntry(Node.getNodeId(), node2);
    boolean isDif = nodeEntry.equals(nodeEntry2);
    Assert.assertTrue(isDif);
  }

}
