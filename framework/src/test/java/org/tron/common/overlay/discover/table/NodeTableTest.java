package org.tron.common.overlay.discover.table;

import org.junit.Assert;
import org.junit.Test;
import org.tron.common.overlay.discover.node.Node;

import java.util.List;

public class NodeTableTest {
    @Test
    public void test() {
        Node node1 = Node.instanceOf("127.0.0.1:10002");
        Node node2 = Node.instanceOf("127.0.0.1:10003");
        Node node3 = Node.instanceOf("127.0.0.2:10004");
        NodeTable table = new NodeTable(node1);
        Node nodeTemp = table.getNode();
        Assert.assertEquals(10002, nodeTemp.getPort());
        Assert.assertEquals(0, table.getNodesCount());
        Assert.assertEquals(0, table.getBucketsCount());

        table.addNode(node2);
        table.addNode(node3);
        int bucketsCount = table.getBucketsCount();
        int nodeCount = table.getNodesCount();
        Assert.assertEquals(2, nodeCount);
        Assert.assertEquals(2, bucketsCount);

        boolean isExist = table.contains(node2);
        table.touchNode(node2);
        Assert.assertTrue(isExist);

        byte[] targetId = Node.getNodeId();
        List<Node> nodeList = table.getClosestNodes(targetId);
        Assert.assertTrue(nodeList.isEmpty());
        //Assert.assertTrue(true);
    }
}
