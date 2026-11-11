package org.tron.p2p.connection.message.detect;

import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;

public class StatusMessageValidationTest {

  @Test
  public void statusParsedThroughMessageDiscardsUnknownFields() throws Exception {
    Connect.StatusMessage expected = Connect.StatusMessage.newBuilder().setFrom(endpoint())
        .setNetworkId(11111).setVersion(1).setMaxConnections(50).setCurrentConnections(3)
        .setTimestamp(123).build();
    Connect.StatusMessage padded = expected.toBuilder()
        .setUnknownFields(UnknownFieldSet.newBuilder()
            .addField(1000, UnknownFieldSet.Field.newBuilder()
                .addLengthDelimited(ByteString.copyFrom(new byte[8192])).build())
            .build())
        .build();

    StatusMessage parsed = (StatusMessage) Message.parse(wire(padded));

    Assert.assertEquals(expected, Connect.StatusMessage.parseFrom(parsed.getData()));
    Assert.assertArrayEquals(expected.toByteArray(), parsed.getData());
    Assert.assertArrayEquals(wire(expected), parsed.getSendData());
    Assert.assertEquals(11111, parsed.getNetworkId());
    Assert.assertEquals(47, parsed.getRemainConnections());
    Assert.assertEquals(123, parsed.getTimestamp());
  }

  @Test
  public void invalidStatusIpv6IsRejected() throws Exception {
    byte[] padding = new byte[8192];
    Arrays.fill(padding, (byte) 'a');
    ByteString scoped = ByteString.copyFromUtf8("fe80::1%")
        .concat(ByteString.copyFrom(padding));
    ByteString scoped46 = ByteString.copyFromUtf8("fe80::1%")
        .concat(ByteString.copyFrom(padding, 0, 38));
    for (ByteString ipv6 : new ByteString[]{ByteString.copyFromUtf8("not-an-ip"),
        ByteString.copyFrom(padding), scoped, scoped46}) {
      Connect.StatusMessage status = Connect.StatusMessage.newBuilder()
          .setFrom(endpoint().setAddressIpv6(ipv6)).build();

      P2pException error = Assert.assertThrows(P2pException.class,
          () -> Message.parse(wire(status)));

      Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, error.getType());
    }
  }

  @Test
  public void validStatusIpv6IsAccepted() throws Exception {
    for (String ipv6 : new String[]{"", "::1", "2001:db8::1",
        "ffff:ffff:ffff:ffff:ffff:ffff:255.255.255.255"}) {
      Connect.StatusMessage status = Connect.StatusMessage.newBuilder()
          .setFrom(endpoint().setAddressIpv6(ByteString.copyFromUtf8(ipv6))).build();

      Assert.assertTrue(Message.parse(wire(status)).valid());
    }
    Connect.StatusMessage ipv6Only = Connect.StatusMessage.newBuilder()
        .setFrom(endpoint().clearAddress()
            .setAddressIpv6(ByteString.copyFromUtf8("2001:db8::1")))
        .build();
    Assert.assertTrue(Message.parse(wire(ipv6Only)).valid());
  }

  @Test
  public void oversizedStatusIpv4IsRejected() throws Exception {
    byte[] padding = new byte[1024 * 1024];
    Arrays.fill(padding, (byte) 'a');
    for (int length : new int[]{16, padding.length}) {
      Connect.StatusMessage status = Connect.StatusMessage.newBuilder()
          .setFrom(endpoint().setAddress(ByteString.copyFrom(padding, 0, length))
              .setAddressIpv6(ByteString.copyFromUtf8("2001:db8::1")))
          .build();

      P2pException error = Assert.assertThrows(P2pException.class,
          () -> Message.parse(wire(status)));

      Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, error.getType());
    }
  }

  @Test
  public void statusIpv4BoundaryAndIpv6Fallback() throws Exception {
    Connect.StatusMessage status = Connect.StatusMessage.newBuilder()
        .setFrom(endpoint().setAddress(ByteString.copyFromUtf8("255.255.255.255"))).build();
    StatusMessage parsed = (StatusMessage) Message.parse(wire(status));
    Assert.assertEquals("255.255.255.255", parsed.getFrom().getHostV4());

    for (String ipv4 : new String[]{"", "not-an-ip", "999.999.999.999"}) {
      status = Connect.StatusMessage.newBuilder()
          .setFrom(endpoint().setAddress(ByteString.copyFromUtf8(ipv4))
              .setAddressIpv6(ByteString.copyFromUtf8("2001:db8::1")))
          .build();
      parsed = (StatusMessage) Message.parse(wire(status));
      Assert.assertNull(parsed.getFrom().getHostV4());
      Assert.assertNotNull(parsed.getFrom().getHostV6());
    }
  }

  private Discover.Endpoint.Builder endpoint() {
    return Discover.Endpoint.newBuilder().setNodeId(ByteString.copyFrom(new byte[64]))
        .setAddress(ByteString.copyFromUtf8("127.0.0.1")).setPort(18888);
  }

  private byte[] wire(Connect.StatusMessage status) {
    return ArrayUtils.add(status.toByteArray(), 0, MessageType.STATUS.getType());
  }
}
