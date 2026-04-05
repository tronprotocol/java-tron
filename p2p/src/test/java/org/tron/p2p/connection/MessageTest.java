package org.tron.p2p.connection;


import static org.tron.p2p.base.Parameter.NETWORK_TIME_DIFF;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.connection.message.keepalive.PingMessage;
import org.tron.p2p.connection.message.keepalive.PongMessage;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.exception.P2pException.TypeEnum;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Connect.KeepAliveMessage;

public class MessageTest {

  @Before
  public void init() {
    Parameter.p2pConfig = new P2pConfig();
  }

  @Test
  public void testPing() {
    PingMessage pingMessage = new PingMessage();
    byte[] messageData = pingMessage.getSendData();
    try {
      Message message = Message.parse(messageData);
      Assert.assertEquals(MessageType.KEEP_ALIVE_PING, message.getType());
    } catch (P2pException e) {
      Assert.fail();
    }
  }

  @Test
  public void testPong() {
    PongMessage pongMessage = new PongMessage();
    byte[] messageData = pongMessage.getSendData();
    try {
      Message message = Message.parse(messageData);
      Assert.assertEquals(MessageType.KEEP_ALIVE_PONG, message.getType());
    } catch (P2pException e) {
      Assert.fail();
    }
  }

  @Test
  public void testHandShakeHello() {
    HelloMessage helloMessage = new HelloMessage(DisconnectCode.NORMAL, 0);
    byte[] messageData = helloMessage.getSendData();
    try {
      Message message = Message.parse(messageData);
      Assert.assertEquals(MessageType.HANDSHAKE_HELLO, message.getType());
    } catch (P2pException e) {
      Assert.fail();
    }
  }

  @Test
  public void testUnKnownType() {
    PingMessage pingMessage = new PingMessage();
    byte[] messageData = pingMessage.getSendData();
    messageData[0] = (byte) 0x00;
    try {
      Message.parse(messageData);
    } catch (P2pException e) {
      Assert.assertEquals(TypeEnum.NO_SUCH_MESSAGE, e.getType());
    }
  }

  @Test
  public void testInvalidTime() {
    KeepAliveMessage keepAliveMessage = Connect.KeepAliveMessage.newBuilder()
        .setTimestamp(System.currentTimeMillis() + NETWORK_TIME_DIFF * 2).build();
    try {
      PingMessage message = new PingMessage(keepAliveMessage.toByteArray());
      Assert.assertFalse(message.valid());
    } catch (Exception e) {
      Assert.fail();
    }
  }
}
