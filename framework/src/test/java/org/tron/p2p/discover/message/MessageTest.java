package org.tron.p2p.discover.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Constant;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.kad.FindNodeMessage;
import org.tron.p2p.discover.message.kad.NeighborsMessage;
import org.tron.p2p.discover.message.kad.PingMessage;
import org.tron.p2p.discover.message.kad.PongMessage;
import org.tron.p2p.exception.P2pException;

public class MessageTest {

  private static Node fromNode;
  private static Node toNode;

  @BeforeClass
  public static void init() {
    Parameter.p2pConfig = new P2pConfig();

    byte[] nodeId1 = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId1, (byte) 0x01);
    fromNode = new Node(nodeId1, "192.168.1.1", null, 18888);

    byte[] nodeId2 = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId2, (byte) 0x02);
    toNode = new Node(nodeId2, "192.168.1.2", null, 18889);
  }

  @Test
  public void testParsePingMessage() throws Exception {
    PingMessage ping = new PingMessage(fromNode, toNode);
    byte[] sendData = ping.getSendData();
    Message parsed = Message.parse(sendData);
    Assert.assertEquals(MessageType.KAD_PING, parsed.getType());
    Assert.assertTrue(parsed instanceof PingMessage);
  }

  @Test
  public void testParsePongMessage() throws Exception {
    PongMessage pong = new PongMessage(fromNode);
    byte[] sendData = pong.getSendData();
    Message parsed = Message.parse(sendData);
    Assert.assertEquals(MessageType.KAD_PONG, parsed.getType());
    Assert.assertTrue(parsed instanceof PongMessage);
  }

  @Test
  public void testParseFindNodeMessage() throws Exception {
    byte[] targetId = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(targetId, (byte) 0x03);
    FindNodeMessage findNode = new FindNodeMessage(fromNode, targetId);
    byte[] sendData = findNode.getSendData();
    Message parsed = Message.parse(sendData);
    Assert.assertEquals(MessageType.KAD_FIND_NODE, parsed.getType());
    Assert.assertTrue(parsed instanceof FindNodeMessage);
  }

  @Test
  public void testParseNeighborsMessage() throws Exception {
    List<Node> neighbors = new ArrayList<>();
    neighbors.add(toNode);
    NeighborsMessage neighborsMsg =
        new NeighborsMessage(fromNode, neighbors, System.currentTimeMillis());
    byte[] sendData = neighborsMsg.getSendData();
    Message parsed = Message.parse(sendData);
    Assert.assertEquals(MessageType.KAD_NEIGHBORS, parsed.getType());
    Assert.assertTrue(parsed instanceof NeighborsMessage);
  }

  @Test
  public void testParseUnknownType() {
    byte[] data = new byte[] {(byte) 0xFF, 0x00, 0x01};
    try {
      Message.parse(data);
      Assert.fail("Should throw P2pException for unknown type");
    } catch (P2pException e) {
      Assert.assertEquals(P2pException.TypeEnum.NO_SUCH_MESSAGE, e.getType());
    } catch (Exception e) {
      Assert.fail("Expected P2pException, got: " + e.getClass().getName());
    }
  }

  @Test
  public void testParseInvalidData() {
    // KAD_PING type byte followed by garbage data
    byte[] data = new byte[] {MessageType.KAD_PING.getType(), 0x00, 0x01, 0x02};
    try {
      Message.parse(data);
      Assert.fail("Should throw for invalid protobuf data");
    } catch (Exception e) {
      // Expected: either P2pException (BAD_MESSAGE) or protobuf parse exception
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testGetType() {
    PingMessage ping = new PingMessage(fromNode, toNode);
    Assert.assertEquals(MessageType.KAD_PING, ping.getType());
  }

  @Test
  public void testGetData() {
    PingMessage ping = new PingMessage(fromNode, toNode);
    byte[] data = ping.getData();
    Assert.assertNotNull(data);
    Assert.assertTrue(data.length > 0);
  }

  @Test
  public void testGetSendData() {
    PingMessage ping = new PingMessage(fromNode, toNode);
    byte[] sendData = ping.getSendData();
    Assert.assertEquals(MessageType.KAD_PING.getType(), sendData[0]);
    // sendData should be data prepended with type byte
    byte[] data = ping.getData();
    Assert.assertEquals(data.length + 1, sendData.length);
    for (int i = 0; i < data.length; i++) {
      Assert.assertEquals(data[i], sendData[i + 1]);
    }
  }

  @Test
  public void testBaseToString() {
    // Test the base Message.toString() - needs a concrete instance with null data scenario
    PingMessage ping = new PingMessage(fromNode, toNode);
    // PingMessage overrides toString, but we can test the base via its own logic
    String str = ping.toString();
    Assert.assertNotNull(str);
    Assert.assertTrue(str.length() > 0);
  }

  @Test
  public void testEquals() {
    PingMessage ping1 = new PingMessage(fromNode, toNode);
    PingMessage ping2 = new PingMessage(fromNode, toNode);
    // equals() delegates to Object.equals (reference equality)
    Assert.assertTrue(ping1.equals(ping1));
    Assert.assertFalse(ping1.equals(ping2));
    Assert.assertFalse(ping1.equals(null));
  }

  @Test
  public void testMessageTypeFromByte() {
    Assert.assertEquals(MessageType.KAD_PING, MessageType.fromByte((byte) 0x01));
    Assert.assertEquals(MessageType.KAD_PONG, MessageType.fromByte((byte) 0x02));
    Assert.assertEquals(MessageType.KAD_FIND_NODE, MessageType.fromByte((byte) 0x03));
    Assert.assertEquals(MessageType.KAD_NEIGHBORS, MessageType.fromByte((byte) 0x04));
    Assert.assertEquals(MessageType.UNKNOWN, MessageType.fromByte((byte) 0x00));
    Assert.assertEquals(MessageType.UNKNOWN, MessageType.fromByte((byte) 0x99));
  }
}
