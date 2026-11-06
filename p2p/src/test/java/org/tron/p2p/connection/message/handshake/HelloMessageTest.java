package org.tron.p2p.connection.message.handshake;

import static org.tron.p2p.base.Parameter.p2pConfig;

import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;

public class HelloMessageTest {

  @Test
  public void testHelloMessage() throws Exception {
    p2pConfig = new P2pConfig();
    HelloMessage m1 = new HelloMessage(DisconnectCode.NORMAL, 0);
    Assert.assertEquals(0, m1.getCode());

    Assert.assertTrue(Arrays.equals(p2pConfig.getNodeID(), m1.getFrom().getId()));
    Assert.assertEquals(p2pConfig.getPort(), m1.getFrom().getPort());
    Assert.assertEquals(p2pConfig.getIp(), m1.getFrom().getHostV4());
    Assert.assertEquals(p2pConfig.getNetworkId(), m1.getNetworkId());
    Assert.assertEquals(MessageType.HANDSHAKE_HELLO, m1.getType());

    HelloMessage m2 = new HelloMessage(m1.getData());
    Assert.assertTrue(Arrays.equals(p2pConfig.getNodeID(), m2.getFrom().getId()));
    Assert.assertEquals(p2pConfig.getPort(), m2.getFrom().getPort());
    Assert.assertEquals(p2pConfig.getIp(), m2.getFrom().getHostV4());
    Assert.assertEquals(p2pConfig.getNetworkId(), m2.getNetworkId());
    Assert.assertEquals(MessageType.HANDSHAKE_HELLO, m2.getType());
  }

  @Test
  public void testInvalidHelloIpv6() throws Exception {
    Discover.Endpoint.Builder from = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(new byte[64]))
        .setAddress(ByteString.copyFromUtf8("127.0.0.1"))
        .setPort(18888);
    byte[] padding = new byte[8192];
    Arrays.fill(padding, (byte) 'a');
    ByteString scoped = ByteString.copyFromUtf8("fe80::1%")
        .concat(ByteString.copyFrom(padding));

    for (ByteString ipv6 : new ByteString[]{ByteString.copyFromUtf8("not-an-ip"),
        ByteString.copyFrom(padding), scoped}) {
      Connect.HelloMessage hello = Connect.HelloMessage.newBuilder()
          .setFrom(from.setAddressIpv6(ipv6)).build();
      byte[] wire = ArrayUtils.add(hello.toByteArray(), 0, MessageType.HANDSHAKE_HELLO.getType());
      org.tron.p2p.exception.P2pException exception = Assert.assertThrows(
          org.tron.p2p.exception.P2pException.class, () -> Message.parse(wire));
      Assert.assertEquals(org.tron.p2p.exception.P2pException.TypeEnum.BAD_MESSAGE,
          exception.getType());
    }
  }

  @Test
  public void testValidHelloIpv6() throws Exception {
    Discover.Endpoint.Builder from = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(new byte[64]))
        .setAddress(ByteString.copyFromUtf8("127.0.0.1"))
        .setPort(18888);
    for (String ipv6 : new String[]{"", "::1", "2001:db8::1",
        "ffff:ffff:ffff:ffff:ffff:ffff:255.255.255.255"}) {
      Connect.HelloMessage hello = Connect.HelloMessage.newBuilder()
          .setFrom(from.setAddressIpv6(ByteString.copyFromUtf8(ipv6))).build();
      byte[] wire = ArrayUtils.add(hello.toByteArray(), 0, MessageType.HANDSHAKE_HELLO.getType());
      Assert.assertTrue(Message.parse(wire).valid());
    }
    Connect.HelloMessage hello = Connect.HelloMessage.newBuilder()
        .setFrom(from.clearAddress().setAddressIpv6(ByteString.copyFromUtf8("2001:db8::1")))
        .build();
    Assert.assertTrue(new HelloMessage(hello.toByteArray()).valid());
  }

  @Test
  public void helloParsedThroughMessageDiscardsUnknownFields() throws Exception {
    Discover.Endpoint endpoint = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(new byte[64]))
        .setAddress(ByteString.copyFromUtf8("127.0.0.1"))
        .setPort(18888)
        .build();
    Connect.HelloMessage hello = Connect.HelloMessage.newBuilder()
        .setFrom(endpoint)
        .setNetworkId(11111)
        .setVersion(1)
        .setTimestamp(123)
        .build();
    Connect.HelloMessage padded = hello.toBuilder()
        .setUnknownFields(UnknownFieldSet.newBuilder()
            .addField(1000, UnknownFieldSet.Field.newBuilder()
                .addLengthDelimited(ByteString.copyFrom(new byte[8192]))
                .build())
            .build())
        .build();
    Assert.assertTrue(padded.getSerializedSize() > hello.getSerializedSize() + 8192);
    byte[] wire = ArrayUtils.add(padded.toByteArray(), 0, MessageType.HANDSHAKE_HELLO.getType());

    HelloMessage parsed = (HelloMessage) Message.parse(wire);
    Field field = HelloMessage.class.getDeclaredField("helloMessage");
    field.setAccessible(true);
    Connect.HelloMessage parsedProto = (Connect.HelloMessage) field.get(parsed);

    Assert.assertTrue(parsedProto.getUnknownFields().asMap().isEmpty());
    Assert.assertEquals(hello, parsedProto);
    Assert.assertEquals(11111, parsed.getNetworkId());
    Assert.assertEquals(18888, parsed.getFrom().getPort());
    Assert.assertArrayEquals(hello.toByteArray(), parsed.getData());
    Assert.assertArrayEquals(
        ArrayUtils.add(hello.toByteArray(), 0, MessageType.HANDSHAKE_HELLO.getType()),
        parsed.getSendData());
  }
}
