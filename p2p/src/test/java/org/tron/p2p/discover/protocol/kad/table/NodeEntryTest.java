package org.tron.p2p.discover.protocol.kad.table;

import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.discover.Node;
import org.tron.p2p.utils.ByteArray;
import org.tron.p2p.utils.NetUtil;

import java.net.InetSocketAddress;

public class NodeEntryTest {
  @Test
  public void test() throws InterruptedException {
    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 10001));
    NodeEntry nodeEntry = new NodeEntry(NetUtil.getNodeId(), node1);

    long lastModified = nodeEntry.getModified();
    Thread.sleep(1);
    nodeEntry.touch();
    long nowModified = nodeEntry.getModified();
    Assert.assertNotEquals(lastModified, nowModified);

    Node node2 = new Node(new InetSocketAddress("127.0.0.1", 10002));
    NodeEntry nodeEntry2 = new NodeEntry(NetUtil.getNodeId(), node2);
    boolean isDif = nodeEntry.equals(nodeEntry2);
    Assert.assertTrue(isDif);
  }

  @Test
  public void testDistance() {
    byte[] randomId = NetUtil.getNodeId();
    String hexRandomIdStr = ByteArray.toHexString(randomId);
    Assert.assertEquals(128, hexRandomIdStr.length());

    byte[] nodeId1 = ByteArray.fromHexString(
        "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000");
    byte[] nodeId2 = ByteArray.fromHexString(
        "a000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000");
    Assert.assertEquals(17, NodeEntry.distance(nodeId1, nodeId2));

    byte[] nodeId3 = ByteArray.fromHexString(
        "0000800000000000000000000000000000000000000000000000000000000001"
            + "0000000000000000000000000000000000000000000000000000000000000000");
    Assert.assertEquals(1, NodeEntry.distance(nodeId1, nodeId3));

    byte[] nodeId4 = ByteArray.fromHexString(
        "0000400000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000");
    Assert.assertEquals(0, NodeEntry.distance(nodeId1, nodeId4)); // => 0

    byte[] nodeId5 = ByteArray.fromHexString(
        "0000200000000000000000000000000000000000000000000000000000000000"
            + "4000000000000000000000000000000000000000000000000000000000000000");
    Assert.assertEquals(-1, NodeEntry.distance(nodeId1, nodeId5)); // => 0

    byte[] nodeId6 = ByteArray.fromHexString(
        "0000100000000000000000000000000000000000000000000000000000000000"
            + "2000000000000000000000000000000000000000000000000000000000000000");
    Assert.assertEquals(-2, NodeEntry.distance(nodeId1, nodeId6)); // => 0

    byte[] nodeId7 = ByteArray.fromHexString(
        "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000001");
    Assert.assertEquals(-494, NodeEntry.distance(nodeId1, nodeId7)); // => 0

    Assert.assertEquals(-495, NodeEntry.distance(nodeId1, nodeId1)); // => 0
  }
}
