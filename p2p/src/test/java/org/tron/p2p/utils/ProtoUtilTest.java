package org.tron.p2p.utils;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.google.protobuf.UnknownFieldSet;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.connection.message.keepalive.PingMessage;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;

public class ProtoUtilTest {

  // A field number not used by any of the messages under test, so it always parses as unknown.
  private static final int UNKNOWN_FIELD_NUMBER = 1000;

  private static Discover.Endpoint endpoint() {
    return Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(new byte[64]))
        .setAddress(ByteString.copyFromUtf8("127.0.0.1"))
        .setPort(18888)
        .build();
  }

  /**
   * Append a large unknown field to a valid message, parse the result through
   * {@link ProtoUtil#parseFrom}, and assert the unknown bytes were discarded while every
   * known field is preserved.
   */
  private static <T extends Message> void assertUnknownDiscarded(T valid, Parser<T> parser)
      throws Exception {
    UnknownFieldSet unknown = UnknownFieldSet.newBuilder()
        .addField(UNKNOWN_FIELD_NUMBER, UnknownFieldSet.Field.newBuilder()
            .addLengthDelimited(ByteString.copyFrom(new byte[8192]))
            .build())
        .build();
    byte[] withUnknown = valid.toBuilder().setUnknownFields(unknown).build().toByteArray();
    // Sanity: the padded bytes really are larger than the clean message.
    Assert.assertTrue(withUnknown.length > valid.toByteArray().length + 8000);

    T parsed = ProtoUtil.parseFrom(parser, withUnknown);
    Assert.assertTrue("unknown fields must be discarded",
        parsed.getUnknownFields().asMap().isEmpty());
    // Known fields untouched -> equal to the clean message, and serialized size back to normal.
    Assert.assertEquals(valid, parsed);
    Assert.assertEquals(valid.getSerializedSize(), parsed.getSerializedSize());
  }

  @Test
  public void statusMessageDiscardsUnknown() throws Exception {
    Connect.StatusMessage msg = Connect.StatusMessage.newBuilder()
        .setFrom(endpoint()).setVersion(1).setNetworkId(1)
        .setMaxConnections(30).setCurrentConnections(1).setTimestamp(123).build();
    assertUnknownDiscarded(msg, Connect.StatusMessage.parser());
  }

  @Test
  public void p2pDisconnectMessageDiscardsUnknown() throws Exception {
    Connect.P2pDisconnectMessage msg = Connect.P2pDisconnectMessage.newBuilder()
        .setReason(Connect.DisconnectReason.TOO_MANY_PEERS).build();
    assertUnknownDiscarded(msg, Connect.P2pDisconnectMessage.parser());
  }

  @Test
  public void keepAliveMessageDiscardsUnknown() throws Exception {
    Connect.KeepAliveMessage msg = Connect.KeepAliveMessage.newBuilder()
        .setTimestamp(123).build();
    assertUnknownDiscarded(msg, Connect.KeepAliveMessage.parser());
  }

  @Test
  public void compressMessageDiscardsUnknown() throws Exception {
    Connect.CompressMessage msg = Connect.CompressMessage.newBuilder()
        .setType(Connect.CompressMessage.CompressType.uncompress)
        .setData(ByteString.copyFromUtf8("payload")).build();
    assertUnknownDiscarded(msg, Connect.CompressMessage.parser());
  }

  @Test
  public void discoverPingMessageDiscardsUnknown() throws Exception {
    Discover.PingMessage msg = Discover.PingMessage.newBuilder()
        .setVersion(1).setFrom(endpoint()).setTo(endpoint()).setTimestamp(123).build();
    assertUnknownDiscarded(msg, Discover.PingMessage.parser());
  }

  @Test
  public void discoverPongMessageDiscardsUnknown() throws Exception {
    Discover.PongMessage msg = Discover.PongMessage.newBuilder()
        .setFrom(endpoint()).setEcho(1).setTimestamp(123).build();
    assertUnknownDiscarded(msg, Discover.PongMessage.parser());
  }

  @Test
  public void findNeighboursDiscardsUnknown() throws Exception {
    Discover.FindNeighbours msg = Discover.FindNeighbours.newBuilder()
        .setFrom(endpoint()).setTargetId(ByteString.copyFrom(new byte[64]))
        .setTimestamp(123).build();
    assertUnknownDiscarded(msg, Discover.FindNeighbours.parser());
  }

  @Test
  public void neighboursDiscardsUnknown() throws Exception {
    Discover.Neighbours msg = Discover.Neighbours.newBuilder()
        .setFrom(endpoint()).addNeighbours(endpoint()).setTimestamp(123).build();
    assertUnknownDiscarded(msg, Discover.Neighbours.parser());
  }

  @Test
  public void testCompressMessage() throws Exception {
    PingMessage p1 = new PingMessage();

    Connect.CompressMessage message = ProtoUtil.compressMessage(p1.getData());

    byte[] d1 = ProtoUtil.uncompressMessage(message);

    PingMessage p2 = new PingMessage(d1);

    Assert.assertTrue(p1.getTimeStamp() == p2.getTimeStamp());


    Connect.CompressMessage m2 = ProtoUtil.compressMessage(new byte[1000]);

    byte[] d2 = ProtoUtil.uncompressMessage(m2);

    Assert.assertTrue(d2.length == 1000);
    Assert.assertTrue(d2[0] == 0);
  }
}
