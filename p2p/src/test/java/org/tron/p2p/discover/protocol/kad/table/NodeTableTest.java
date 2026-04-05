package org.tron.p2p.discover.protocol.kad.table;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.discover.Node;
import org.tron.p2p.utils.NetUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NodeTableTest {

  private Node homeNode;
  private NodeTable nodeTable;
  private String[] ips;
  private List<byte[]> ids;

  @Test
  public void test() {
    Node node1 = new Node(new InetSocketAddress("127.0.0.1", 10002));

    NodeTable table = new NodeTable(node1);
    Node nodeTemp = table.getNode();
    Assert.assertEquals(10002, nodeTemp.getPort());
    Assert.assertEquals(0, table.getNodesCount());
    Assert.assertEquals(0, table.getBucketsCount());

    Node node2 = new Node(new InetSocketAddress("127.0.0.2", 10003));
    Node node3 = new Node(new InetSocketAddress("127.0.0.3", 10004));
    table.addNode(node2);
    table.addNode(node3);
    int bucketsCount = table.getBucketsCount();
    int nodeCount = table.getNodesCount();
    Assert.assertEquals(2, nodeCount);
    Assert.assertTrue(bucketsCount > 0);

    boolean isExist = table.contains(node2);
    table.touchNode(node2);
    Assert.assertTrue(isExist);

    byte[] targetId = NetUtil.getNodeId();
    List<Node> nodeList = table.getClosestNodes(targetId);
    Assert.assertFalse(nodeList.isEmpty());
  }

  /**
   * init nodes for test.
   */
  @Before
  public void init() {
    ids = new ArrayList<>();
    for (int i = 0; i < KademliaOptions.BUCKET_SIZE + 1; i++) {
      byte[] id = new byte[64];
      id[0] = 17;
      id[1] = 16;
      if (i < 10) {
        id[63] = (byte) i;
      } else {
        id[62] = 1;
        id[63] = (byte) (i - 10);
      }
      ids.add(id);
    }

    ips = new String[KademliaOptions.BUCKET_SIZE + 1];
    byte[] homeId = new byte[64];
    homeNode = new Node(homeId, "127.0.0.1", null, 18888, 18888);
    nodeTable = new NodeTable(homeNode);
    ips[0] = "127.0.0.2";
    ips[1] = "127.0.0.3";
    ips[2] = "127.0.0.4";
    ips[3] = "127.0.0.5";
    ips[4] = "127.0.0.6";
    ips[5] = "127.0.0.7";
    ips[6] = "127.0.0.8";
    ips[7] = "127.0.0.9";
    ips[8] = "127.0.0.10";
    ips[9] = "127.0.0.11";
    ips[10] = "127.0.0.12";
    ips[11] = "127.0.0.13";
    ips[12] = "127.0.0.14";
    ips[13] = "127.0.0.15";
    ips[14] = "127.0.0.16";
    ips[15] = "127.0.0.17";
    ips[16] = "127.0.0.18";
  }

  @Test
  public void addNodeTest() {
    Node node = new Node(ids.get(0), ips[0], null, 18888, 18888);
    Assert.assertEquals(0, nodeTable.getNodesCount());
    nodeTable.addNode(node);
    Assert.assertEquals(1, nodeTable.getNodesCount());
    Assert.assertTrue(nodeTable.contains(node));
  }

  @Test
  public void addDupNodeTest() throws Exception {
    Node node = new Node(ids.get(0), ips[0], null, 18888, 18888);
    nodeTable.addNode(node);
    long firstTouchTime = nodeTable.getAllNodes().get(0).getModified();
    TimeUnit.MILLISECONDS.sleep(20);
    nodeTable.addNode(node);
    long lastTouchTime = nodeTable.getAllNodes().get(0).getModified();
    Assert.assertTrue(lastTouchTime > firstTouchTime);
    Assert.assertEquals(1, nodeTable.getNodesCount());
  }

  @Test
  public void addNode_bucketFullTest() throws Exception {
    for (int i = 0; i < KademliaOptions.BUCKET_SIZE; i++) {
      TimeUnit.MILLISECONDS.sleep(10);
      addNode(new Node(ids.get(i), ips[i], null, 18888, 18888));
    }
    Node lastSeen = nodeTable.addNode(new Node(ids.get(16), ips[16], null, 18888, 18888));
    Assert.assertTrue(null != lastSeen);
    Assert.assertEquals(ips[15], lastSeen.getHostV4());
  }

  public void addNode(Node n) {
    nodeTable.addNode(n);
  }

  @Test
  public void dropNodeTest() {
    Node node = new Node(ids.get(0), ips[0], null, 18888, 18888);
    nodeTable.addNode(node);
    Assert.assertTrue(nodeTable.contains(node));
    nodeTable.dropNode(node);
    Assert.assertTrue(!nodeTable.contains(node));
    nodeTable.addNode(node);
    nodeTable.dropNode(new Node(ids.get(1), ips[0], null, 10000, 10000));
    Assert.assertTrue(!nodeTable.contains(node));
  }

  @Test
  public void getBucketsCountTest() {
    Assert.assertEquals(0, nodeTable.getBucketsCount());
    Node node = new Node(ids.get(0), ips[0], null, 18888, 18888);
    nodeTable.addNode(node);
    Assert.assertEquals(1, nodeTable.getBucketsCount());
  }

  @Test
  public void touchNodeTest() throws Exception {
    Node node = new Node(ids.get(0), ips[0], null, 18888, 18888);
    nodeTable.addNode(node);
    long firstTouchTime = nodeTable.getAllNodes().get(0).getModified();
    TimeUnit.MILLISECONDS.sleep(10);
    nodeTable.touchNode(node);
    long lastTouchTime = nodeTable.getAllNodes().get(0).getModified();
    Assert.assertTrue(firstTouchTime < lastTouchTime);
  }

  @Test
  public void containsTest() {
    Node node = new Node(ids.get(0), ips[0], null, 18888, 18888);
    Assert.assertTrue(!nodeTable.contains(node));
    nodeTable.addNode(node);
    Assert.assertTrue(nodeTable.contains(node));
  }

  @Test
  public void getBuckIdTest() {
    Node node = new Node(ids.get(0), ips[0], null, 18888, 18888);  //id: 11100...000
    nodeTable.addNode(node);
    NodeEntry nodeEntry = new NodeEntry(homeNode.getId(), node);
    Assert.assertEquals(13, nodeTable.getBucketId(nodeEntry));
  }

  @Test
  public void getClosestNodes_nodesMoreThanBucketCapacity() throws Exception {
    byte[] bytes = new byte[64];
    bytes[0] = 15;
    Node nearNode = new Node(bytes, "127.0.0.19", null, 18888, 18888);
    bytes[0] = 70;
    Node farNode = new Node(bytes, "127.0.0.20", null, 18888, 18888);
    nodeTable.addNode(nearNode);
    nodeTable.addNode(farNode);
    for (int i = 0; i < KademliaOptions.BUCKET_SIZE - 1; i++) {
      //To control totally 17 nodes, however closest's capacity is 16
      nodeTable.addNode(new Node(ids.get(i), ips[i], null, 18888, 18888));
      TimeUnit.MILLISECONDS.sleep(10);
    }
    Assert.assertTrue(nodeTable.getBucketsCount() > 1);
    //3 buckets, nearnode's distance is 252, far's is 255, others' are 253
    List<Node> closest = nodeTable.getClosestNodes(homeNode.getId());
    Assert.assertTrue(closest.contains(nearNode));
    //the farest node should be excluded
  }

  @Test
  public void getClosestNodes_isDiscoverNode() {
    Node node = new Node(ids.get(0), ips[0], null, 18888);
    nodeTable.addNode(node);
    List<Node> closest = nodeTable.getClosestNodes(homeNode.getId());
    Assert.assertFalse(closest.isEmpty());
  }
}
