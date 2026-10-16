package org.tron.core.net.message.handshake;

import com.google.protobuf.ByteString;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Constant;
import org.tron.p2p.base.Parameter;
import org.tron.protos.Discover.Endpoint;
import org.tron.protos.Protocol;

public class HelloMessageTest {

  private P2pConfig savedConfig;

  @Before
  public void setUp() {
    savedConfig = Parameter.p2pConfig;
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp("127.0.0.1");
    Parameter.p2pConfig.setIpv6("::1");
  }

  @After
  public void tearDown() {
    Parameter.p2pConfig = savedConfig;
  }

  @Test(timeout = 5000)
  public void testValidAddressLogFormat() throws Exception {
    for (String[] hosts : new String[][] {
        {"192.0.2.1", "", "192.0.2.1"},
        {"", "2001:db8::1", "2001:db8::1"},
        {"192.0.2.1", "2001:db8::1", "192.0.2.1"}}) {
      HelloMessage message = hello(hosts[0], hosts[1], 18888);
      Assert.assertTrue(message.valid());
      // InetSocketAddress adds IPv6 brackets on JDK 17, but not on JDK 8.
      InetSocketAddress expectedEndpoint = new InetSocketAddress(hosts[2], 18888);
      Assert.assertFalse(expectedEndpoint.isUnresolved());
      String formatted = message.toString();
      Assert.assertTrue(formatted, formatted.contains("from: " + expectedEndpoint + "\n"));
      Assert.assertTrue(formatted.contains("timestamp: 123\n"));
      Assert.assertTrue(formatted.contains("headBlockId: "
          + message.getHeadBlockId().getString() + "\n"));
      Assert.assertTrue(formatted.contains("nodeType: 0\nlowestBlockNum: 0\n"));
    }
  }

  private static HelloMessage hello(String ipv4, String ipv6, int port) throws Exception {
    Protocol.HelloMessage.BlockId block = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(ByteString.copyFrom(new byte[32])).build();
    Endpoint endpoint = Endpoint.newBuilder().setAddress(ByteString.copyFromUtf8(ipv4))
        .setAddressIpv6(ByteString.copyFromUtf8(ipv6))
        .setNodeId(ByteString.copyFrom(new byte[Constant.NODE_ID_LEN])).setPort(port).build();
    return new HelloMessage(Protocol.HelloMessage.newBuilder().setFrom(endpoint)
        .setGenesisBlockId(block).setSolidBlockId(block).setHeadBlockId(block)
        .setTimestamp(123).build().toByteArray());
  }
}
