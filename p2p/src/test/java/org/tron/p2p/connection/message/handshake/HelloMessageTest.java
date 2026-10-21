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
        .setUnknownFields(UnknownFieldSet.newBuilder()
            .addField(1000, UnknownFieldSet.Field.newBuilder()
                .addLengthDelimited(ByteString.copyFrom(new byte[8192]))
                .build())
            .build())
        .build();
    byte[] wire = ArrayUtils.add(hello.toByteArray(), 0, MessageType.HANDSHAKE_HELLO.getType());

    HelloMessage parsed = (HelloMessage) Message.parse(wire);
    Field field = HelloMessage.class.getDeclaredField("helloMessage");
    field.setAccessible(true);
    Connect.HelloMessage parsedProto = (Connect.HelloMessage) field.get(parsed);

    Assert.assertTrue(parsedProto.getUnknownFields().asMap().isEmpty());
    Assert.assertEquals(11111, parsed.getNetworkId());
    Assert.assertEquals(18888, parsed.getFrom().getPort());
    Assert.assertArrayEquals(hello.toByteArray(), parsed.getData());
    Assert.assertArrayEquals(wire, parsed.getSendData());
  }
}
