package org.tron.p2p.connection;

import static org.tron.p2p.base.Parameter.NETWORK_TIME_DIFF;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.detect.StatusMessage;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.connection.message.keepalive.PingMessage;
import org.tron.p2p.connection.message.keepalive.PongMessage;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.exception.P2pException.TypeEnum;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Connect.KeepAliveMessage;
import org.tron.p2p.protos.Discover;

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
  public void testInvalidEndpointPorts() throws Exception {
    for (int port : new int[]{0, -1, 65536, 70000, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
      for (String ipv6 : new String[]{"", "2001:db8::1"}) {
        Discover.Endpoint endpoint = endpoint(port, ipv6);
        HelloMessage hello = new HelloMessage(Connect.HelloMessage.newBuilder()
            .setFrom(endpoint).setNetworkId(Parameter.p2pConfig.getNetworkId())
            .build().toByteArray());
        StatusMessage status = new StatusMessage(Connect.StatusMessage.newBuilder()
            .setFrom(endpoint).build().toByteArray());
        for (Message message : new Message[]{hello, status}) {
          P2pException error = Assert.assertThrows(P2pException.class,
              () -> Message.parse(message.getSendData()));
          Assert.assertEquals(TypeEnum.BAD_MESSAGE, error.getType());
        }
      }
    }
  }

  @Test
  public void testValidEndpointBoundaryPorts() throws Exception {
    for (int port : new int[]{1, 65535}) {
      for (String ipv6 : new String[]{"", "2001:db8::1"}) {
        Discover.Endpoint endpoint = endpoint(port, ipv6);
        HelloMessage hello = new HelloMessage(Connect.HelloMessage.newBuilder()
            .setFrom(endpoint).build().toByteArray());
        StatusMessage status = new StatusMessage(Connect.StatusMessage.newBuilder()
            .setFrom(endpoint).build().toByteArray());
        Assert.assertEquals(MessageType.HANDSHAKE_HELLO,
            Message.parse(hello.getSendData()).getType());
        Assert.assertEquals(MessageType.STATUS, Message.parse(status.getSendData()).getType());
      }
    }
  }

  private Discover.Endpoint endpoint(int port, String ipv6) {
    return Discover.Endpoint.newBuilder().setNodeId(ByteString.copyFrom(new byte[64]))
        .setAddress(ByteString.copyFromUtf8("1.2.3.4"))
        .setAddressIpv6(ByteString.copyFromUtf8(ipv6)).setPort(port).build();
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
