package org.tron.p2p.discover.message.kad;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Constant;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.MessageType;
import org.tron.p2p.protos.Discover;

public class FindNodeMessageTest {

  private static Node fromNode;
  private static byte[] targetId;

  @BeforeClass
  public static void init() {
    Parameter.p2pConfig = new P2pConfig();
    byte[] nodeId = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId, (byte) 0x01);
    fromNode = new Node(nodeId, "192.168.1.1", null, 18888);
    targetId = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(targetId, (byte) 0x02);
  }

  @Test
  public void testConstructorFromNode() {
    FindNodeMessage msg = new FindNodeMessage(fromNode, targetId);
    Assert.assertEquals(MessageType.KAD_FIND_NODE, msg.getType());
    Assert.assertNotNull(msg.getData());
    Assert.assertTrue(msg.getData().length > 0);
  }

  @Test
  public void testGetTargetId() {
    FindNodeMessage msg = new FindNodeMessage(fromNode, targetId);
    byte[] result = msg.getTargetId();
    Assert.assertArrayEquals(targetId, result);
  }

  @Test
  public void testGetTimestamp() {
    long before = System.currentTimeMillis();
    FindNodeMessage msg = new FindNodeMessage(fromNode, targetId);
    long after = System.currentTimeMillis();
    Assert.assertTrue(msg.getTimestamp() >= before);
    Assert.assertTrue(msg.getTimestamp() <= after);
  }

  @Test
  public void testGetFrom() {
    FindNodeMessage msg = new FindNodeMessage(fromNode, targetId);
    Node from = msg.getFrom();
    Assert.assertNotNull(from);
    Assert.assertArrayEquals(fromNode.getId(), from.getId());
    Assert.assertEquals(fromNode.getHostV4(), from.getHostV4());
    Assert.assertEquals(fromNode.getPort(), from.getPort());
  }

  @Test
  public void testToString() {
    FindNodeMessage msg = new FindNodeMessage(fromNode, targetId);
    String str = msg.toString();
    Assert.assertTrue(str.startsWith("[findNeighbours: "));
  }

  @Test
  public void testValid() {
    FindNodeMessage msg = new FindNodeMessage(fromNode, targetId);
    Assert.assertTrue(msg.valid());
  }

  @Test
  public void testValidWithWrongTargetIdLength() throws Exception {
    // Build a FindNodeMessage with a short targetId via protobuf bytes
    byte[] shortTargetId = new byte[32]; // wrong length, should be 64
    Arrays.fill(shortTargetId, (byte) 0x03);

    // Create a valid message, then rebuild with wrong targetId via protobuf
    FindNodeMessage original = new FindNodeMessage(fromNode, targetId);
    byte[] data = original.getData();

    // Parse the protobuf and rebuild with wrong target
    Discover.FindNeighbours parsed = Discover.FindNeighbours.parseFrom(data);
    byte[] badData = parsed.toBuilder()
        .setTargetId(ByteString.copyFrom(shortTargetId))
        .build()
        .toByteArray();

    FindNodeMessage badMsg = new FindNodeMessage(badData);
    Assert.assertFalse(badMsg.valid());
  }

  @Test
  public void testRoundTripEncodeDecode() throws Exception {
    FindNodeMessage original = new FindNodeMessage(fromNode, targetId);
    byte[] data = original.getData();

    FindNodeMessage decoded = new FindNodeMessage(data);
    Assert.assertEquals(MessageType.KAD_FIND_NODE, decoded.getType());
    Assert.assertArrayEquals(targetId, decoded.getTargetId());
    Assert.assertEquals(original.getTimestamp(), decoded.getTimestamp());

    Node decodedFrom = decoded.getFrom();
    Assert.assertArrayEquals(fromNode.getId(), decodedFrom.getId());
    Assert.assertEquals(fromNode.getHostV4(), decodedFrom.getHostV4());
    Assert.assertEquals(fromNode.getPort(), decodedFrom.getPort());
  }

  @Test
  public void testGetSendData() {
    FindNodeMessage msg = new FindNodeMessage(fromNode, targetId);
    byte[] sendData = msg.getSendData();
    Assert.assertNotNull(sendData);
    Assert.assertEquals(MessageType.KAD_FIND_NODE.getType(), sendData[0]);
    Assert.assertEquals(msg.getData().length + 1, sendData.length);
  }
}
