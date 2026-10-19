package org.tron.p2p.discover.message;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.discover.message.kad.FindNodeMessage;
import org.tron.p2p.discover.message.kad.NeighborsMessage;
import org.tron.p2p.discover.message.kad.PingMessage;
import org.tron.p2p.discover.message.kad.PongMessage;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.exception.P2pException.TypeEnum;
import org.tron.p2p.protos.Discover;

public class PortValidationTest {

  @Test
  public void invalidSenderPortsAreRejected() throws Exception {
    for (int port : new int[]{0, -1, 65536, 70000, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
      for (String ipv6 : new String[]{"", "2001:db8::1"}) {
        for (Message message : messages(endpoint(port, ipv6), endpoint(18888, ""))) {
          assertBadMessage(message);
        }
      }
    }
  }

  @Test
  public void invalidNeighborPortsAreRejected() throws Exception {
    NeighborsMessage message = new NeighborsMessage(Discover.Neighbours.newBuilder()
        .setFrom(endpoint(18888, "")).addNeighbours(endpoint(70000, ""))
        .build().toByteArray());
    assertBadMessage(message);
  }

  @Test
  public void validBoundaryPortsAreAccepted() throws Exception {
    for (int port : new int[]{1, 65535}) {
      for (String ipv6 : new String[]{"", "2001:db8::1"}) {
        Discover.Endpoint endpoint = endpoint(port, ipv6);
        for (Message message : messages(endpoint, endpoint)) {
          Assert.assertEquals(message.getType(), Message.parse(message.getSendData()).getType());
        }
      }
    }
  }

  private void assertBadMessage(Message message) {
    P2pException error = Assert.assertThrows(P2pException.class,
        () -> Message.parse(message.getSendData()));
    Assert.assertEquals(TypeEnum.BAD_MESSAGE, error.getType());
  }

  private Discover.Endpoint endpoint(int port, String ipv6) {
    return Discover.Endpoint.newBuilder().setNodeId(ByteString.copyFrom(new byte[64]))
        .setAddress(ByteString.copyFromUtf8("1.2.3.4"))
        .setAddressIpv6(ByteString.copyFromUtf8(ipv6)).setPort(port).build();
  }

  private Message[] messages(Discover.Endpoint from, Discover.Endpoint neighbor) throws Exception {
    return new Message[]{
        new PingMessage(Discover.PingMessage.newBuilder().setFrom(from).build().toByteArray()),
        new PongMessage(Discover.PongMessage.newBuilder().setFrom(from).build().toByteArray()),
        new FindNodeMessage(Discover.FindNeighbours.newBuilder().setFrom(from)
            .setTargetId(ByteString.copyFrom(new byte[64])).build().toByteArray()),
        new NeighborsMessage(Discover.Neighbours.newBuilder().setFrom(from)
            .addNeighbours(neighbor).build().toByteArray())};
  }
}
