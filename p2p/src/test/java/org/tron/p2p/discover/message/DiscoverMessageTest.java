package org.tron.p2p.discover.message;

import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.kad.FindNodeMessage;
import org.tron.p2p.discover.message.kad.NeighborsMessage;
import org.tron.p2p.discover.message.kad.PingMessage;
import org.tron.p2p.discover.message.kad.PongMessage;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.utils.NetUtil;

/**
 * Message.parse is the entry point for every inbound discovery datagram, so its
 * dispatch table and its two rejection paths are what a hostile packet meets
 * first.
 */
public class DiscoverMessageTest {

  private P2pConfig saved;
  private Node from;

  @Before
  public void setUp() {
    saved = Parameter.p2pConfig;
    P2pConfig config = new P2pConfig();
    config.setNetworkId(11111);
    Parameter.p2pConfig = config;
    from = new Node(NetUtil.getNodeId(), "127.0.0.1", null, 18888, 18888);
  }

  @After
  public void tearDown() {
    Parameter.p2pConfig = saved;
  }

  @Test
  public void parseDispatchesEachKadType() throws Exception {
    Node to = new Node(NetUtil.getNodeId(), "127.0.0.2", null, 18889, 18889);

    Assert.assertEquals(MessageType.KAD_PING,
        Message.parse(new PingMessage(from, to).getSendData()).getType());
    Assert.assertEquals(MessageType.KAD_PONG,
        Message.parse(new PongMessage(from).getSendData()).getType());
    Assert.assertEquals(MessageType.KAD_FIND_NODE,
        Message.parse(new FindNodeMessage(from, NetUtil.getNodeId()).getSendData()).getType());
    Assert.assertEquals(MessageType.KAD_NEIGHBORS,
        Message.parse(new NeighborsMessage(from, Collections.singletonList(to), 1L)
            .getSendData()).getType());
  }

  @Test
  public void sendDataPrefixesTheTypeByte() {
    PongMessage pong = new PongMessage(from);
    byte[] sendData = pong.getSendData();

    Assert.assertEquals(pong.getData().length + 1, sendData.length);
    Assert.assertEquals(MessageType.KAD_PONG.getType(), sendData[0]);
    for (int i = 0; i < pong.getData().length; i++) {
      Assert.assertEquals(pong.getData()[i], sendData[i + 1]);
    }
  }

  @Test
  public void unknownTypeByteIsRejected() {
    try {
      Message.parse(new byte[] {0x7F, 1, 2, 3});
      Assert.fail("expected a P2pException");
    } catch (Exception e) {
      Assert.assertTrue(e instanceof P2pException);
      Assert.assertEquals(P2pException.TypeEnum.NO_SUCH_MESSAGE,
          ((P2pException) e).getType());
    }
  }

  @Test
  public void unparseableBodyIsRejected() {
    try {
      Message.parse(new byte[] {MessageType.KAD_PING.getType(), (byte) 0xFF, (byte) 0xFF, 0x7F});
      Assert.fail("expected an exception");
    } catch (Exception expected) {
      Assert.assertNotNull(expected);
    }
  }

  /** The concrete kad messages all override toString, so reach the base one directly. */
  private static Message bare(final MessageType type, final byte[] data) {
    return new Message(type, data) {
      @Override
      public boolean valid() {
        return true;
      }
    };
  }

  @Test
  public void baseToStringReportsTypeAndLength() {
    Message message = bare(MessageType.KAD_PING, new byte[] {1, 2, 3});
    Assert.assertEquals("[Message Type: KAD_PING, len: 3]", message.toString());
    Assert.assertEquals(MessageType.KAD_PING, message.getType());
    Assert.assertArrayEquals(new byte[] {1, 2, 3}, message.getData());

    // A null payload reports zero rather than throwing.
    Assert.assertEquals("[Message Type: KAD_PONG, len: 0]",
        bare(MessageType.KAD_PONG, null).toString());
  }

  @Test
  public void concreteMessageToStringNamesItsKind() {
    Assert.assertTrue(new PongMessage(from).toString().contains("pongMessage"));
  }

  @Test
  public void messageTypeMapsBytesBothWays() {
    for (MessageType type : MessageType.values()) {
      if (type == MessageType.UNKNOWN) {
        continue;
      }
      Assert.assertEquals(type, MessageType.fromByte(type.getType()));
    }
    Assert.assertEquals(MessageType.UNKNOWN, MessageType.fromByte((byte) 0x7F));
  }
}
