package org.tron.p2p.discover.message.kad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Constant;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.MessageType;
import org.tron.p2p.discover.protocol.kad.table.KademliaOptions;

public class NeighborsMessageTest {

  private static Node fromNode;
  private static List<Node> neighborNodes;

  @BeforeClass
  public static void init() {
    Parameter.p2pConfig = new P2pConfig();

    byte[] nodeId = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId, (byte) 0x01);
    fromNode = new Node(nodeId, "192.168.1.1", null, 18888);

    neighborNodes = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      byte[] id = new byte[Constant.NODE_ID_LEN];
      Arrays.fill(id, (byte) (0x10 + i));
      neighborNodes.add(new Node(id, "192.168.1." + (10 + i), null, 18888 + i));
    }
  }

  @Test
  public void testConstructorFromNodeList() {
    long sequence = System.currentTimeMillis();
    NeighborsMessage msg = new NeighborsMessage(fromNode, neighborNodes, sequence);
    Assert.assertEquals(MessageType.KAD_NEIGHBORS, msg.getType());
    Assert.assertNotNull(msg.getData());
    Assert.assertTrue(msg.getData().length > 0);
  }

  @Test
  public void testGetNodes() {
    long sequence = System.currentTimeMillis();
    NeighborsMessage msg = new NeighborsMessage(fromNode, neighborNodes, sequence);
    List<Node> nodes = msg.getNodes();
    Assert.assertEquals(3, nodes.size());
    for (int i = 0; i < 3; i++) {
      Assert.assertArrayEquals(neighborNodes.get(i).getId(), nodes.get(i).getId());
      Assert.assertEquals(neighborNodes.get(i).getHostV4(), nodes.get(i).getHostV4());
      Assert.assertEquals(neighborNodes.get(i).getPort(), nodes.get(i).getPort());
    }
  }

  @Test
  public void testGetTimestamp() {
    long sequence = 123456789L;
    NeighborsMessage msg = new NeighborsMessage(fromNode, neighborNodes, sequence);
    Assert.assertEquals(sequence, msg.getTimestamp());
  }

  @Test
  public void testGetFrom() {
    long sequence = System.currentTimeMillis();
    NeighborsMessage msg = new NeighborsMessage(fromNode, neighborNodes, sequence);
    Node from = msg.getFrom();
    Assert.assertNotNull(from);
    Assert.assertArrayEquals(fromNode.getId(), from.getId());
    Assert.assertEquals(fromNode.getHostV4(), from.getHostV4());
    Assert.assertEquals(fromNode.getPort(), from.getPort());
  }

  @Test
  public void testToString() {
    long sequence = System.currentTimeMillis();
    NeighborsMessage msg = new NeighborsMessage(fromNode, neighborNodes, sequence);
    String str = msg.toString();
    Assert.assertTrue(str.startsWith("[neighbours: "));
  }

  @Test
  public void testValid() {
    long sequence = System.currentTimeMillis();
    NeighborsMessage msg = new NeighborsMessage(fromNode, neighborNodes, sequence);
    Assert.assertTrue(msg.valid());
  }

  @Test
  public void testValidWithEmptyNeighbors() {
    long sequence = System.currentTimeMillis();
    NeighborsMessage msg =
        new NeighborsMessage(fromNode, Collections.<Node>emptyList(), sequence);
    Assert.assertTrue(msg.valid());
  }

  @Test
  public void testValidWithTooManyNeighbors() {
    List<Node> tooMany = new ArrayList<>();
    for (int i = 0; i < KademliaOptions.BUCKET_SIZE + 1; i++) {
      byte[] id = new byte[Constant.NODE_ID_LEN];
      Arrays.fill(id, (byte) (0x20 + i));
      tooMany.add(new Node(id, "10.0.0." + (i + 1), null, 18888));
    }
    long sequence = System.currentTimeMillis();
    NeighborsMessage msg = new NeighborsMessage(fromNode, tooMany, sequence);
    Assert.assertFalse(msg.valid());
  }

  @Test
  public void testValidWithInvalidNeighborNode() {
    // Create a neighbor with null id (invalid node)
    List<Node> badNeighbors = new ArrayList<>();
    badNeighbors.add(new Node(new byte[0], "192.168.1.10", null, 18888));

    long sequence = System.currentTimeMillis();
    NeighborsMessage msg = new NeighborsMessage(fromNode, badNeighbors, sequence);
    Assert.assertFalse(msg.valid());
  }

  @Test
  public void testRoundTripEncodeDecode() throws Exception {
    long sequence = 999888777L;
    NeighborsMessage original = new NeighborsMessage(fromNode, neighborNodes, sequence);
    byte[] data = original.getData();

    NeighborsMessage decoded = new NeighborsMessage(data);
    Assert.assertEquals(MessageType.KAD_NEIGHBORS, decoded.getType());
    Assert.assertEquals(sequence, decoded.getTimestamp());

    List<Node> decodedNodes = decoded.getNodes();
    Assert.assertEquals(neighborNodes.size(), decodedNodes.size());

    Node decodedFrom = decoded.getFrom();
    Assert.assertArrayEquals(fromNode.getId(), decodedFrom.getId());
  }

  @Test
  public void testGetSendData() {
    long sequence = System.currentTimeMillis();
    NeighborsMessage msg = new NeighborsMessage(fromNode, neighborNodes, sequence);
    byte[] sendData = msg.getSendData();
    Assert.assertNotNull(sendData);
    Assert.assertEquals(MessageType.KAD_NEIGHBORS.getType(), sendData[0]);
    Assert.assertEquals(msg.getData().length + 1, sendData.length);
  }
}
