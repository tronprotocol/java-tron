package org.tron.core.net.message.handshake;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.read.ListAppender;
import com.google.protobuf.ByteString;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Constant;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.utils.NetUtil;
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
  public void testAcceptIpLiteralsAndPortBoundaries() throws Exception {
    for (String[] hosts : new String[][] {
        {"192.0.2.1", ""}, {"", "2001:db8::1"}, {"192.0.2.1", "2001:db8::1"},
        {"", "::1"}, {"", "::ffff:192.0.2.1"}}) {
      for (int port : new int[] {1, 18888, 65535}) {
        HelloMessage message = spy(hello(hosts[0], hosts[1], port));
        Assert.assertTrue(message.validEndPoint());
        Assert.assertTrue(message.valid());
        verify(message, never()).getFrom();
      }
    }
  }

  @Test(timeout = 5000)
  public void testNodeIdLengthBoundaries() throws Exception {
    for (int length : new int[] {0, 63, 64, 65}) {
      HelloMessage message = hello("192.0.2.1", "", 18888);
      Assert.assertTrue(message.valid());
      message.setHelloMessage(message.getInstance().toBuilder()
          .setFrom(message.getInstance().getFrom().toBuilder()
              .setNodeId(ByteString.copyFrom(new byte[length])))
          .build());
      if (length == 64) {
        HelloMessage checked = spy(message);
        Assert.assertTrue(checked.validEndPoint());
        Assert.assertTrue(checked.valid());
        verify(checked, never()).getFrom();
      } else {
        assertRejectedBeforeNodeConstruction(message);
      }
    }
  }

  @Test(timeout = 5000)
  public void testRejectHostnamesAndMalformedAddresses() throws Exception {
    for (String[] hosts : new String[][] {
        {"hello.invalid", ""}, {"", "hello.invalid"},
        {"hello.invalid", "2001:db8::1"}, {"192.0.2.1", "hello.invalid"},
        {"256.0.0.1", ""}, {"192.0.2.1\n", ""}, {" 192.0.2.1", ""},
        {"2001:db8::1", ""}, {"", "192.0.2.1"}, {"", "2001:db8:::1"}}) {
      assertRejectedBeforeNodeConstruction(hello(hosts[0], hosts[1], 18888));
    }
  }

  @Test(timeout = 5000)
  public void testRejectMissingAddresses() throws Exception {
    assertRejectedBeforeNodeConstruction(hello("", "", 18888));
    HelloMessage missingFrom = hello("192.0.2.1", "", 18888);
    missingFrom.setHelloMessage(missingFrom.getInstance().toBuilder().clearFrom().build());
    assertRejectedBeforeNodeConstruction(missingFrom);
  }

  @Test(timeout = 5000)
  public void testRejectInvalidPorts() throws Exception {
    for (int port : new int[] {Integer.MIN_VALUE, -1, 0, 65536, Integer.MAX_VALUE}) {
      assertRejectedBeforeNodeConstruction(hello("192.0.2.1", "2001:db8::1", port));
    }
  }

  @Test(timeout = 5000)
  public void testRejectOversizedAddressesBeforeLiteralValidation() throws Exception {
    String oversized = StringUtils.repeat('a', 201);
    try (MockedStatic<NetUtil> netUtil = Mockito.mockStatic(NetUtil.class)) {
      assertRejectedBeforeNodeConstruction(hello(oversized, "", 18888));
      assertRejectedBeforeNodeConstruction(hello("192.0.2.1", oversized, 18888));
      netUtil.verifyNoInteractions();
    }
  }

  @Test(timeout = 5000)
  public void testIpv6AddressLengthBoundary() throws Exception {
    String scoped = "fe80::1%" + StringUtils.repeat('a', 192);
    Assert.assertEquals(200, scoped.length());
    Assert.assertTrue(NetUtil.validIpV6(scoped));
    HelloMessage message = spy(hello("", scoped, 18888));
    Assert.assertTrue(message.valid());
    verify(message, never()).getFrom();
    // The syntax remains valid, so rejection must come from the byte limit.
    Assert.assertTrue(NetUtil.validIpV6(scoped + "a"));
    assertRejectedBeforeNodeConstruction(hello("", scoped + "a", 18888));
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

  @Test(timeout = 5000)
  public void testAsyncLoggingRejectsHostnameBeforeNodeConstruction() throws Exception {
    HelloMessage message = spy(hello("hello.invalid", "", 18888));
    doThrow(new AssertionError("Invalid endpoint reached Node construction"))
        .when(message).getFrom();
    LoggerContext context = new LoggerContext();
    context.setMDCAdapter(new LogbackMDCAdapter());
    ListAppender<ILoggingEvent> sink = new ListAppender<>();
    sink.setContext(context);
    sink.start();
    AsyncAppender async = new AsyncAppender();
    async.setContext(context);
    async.addAppender(sink);
    async.setDiscardingThreshold(0);
    async.start();
    ch.qos.logback.classic.Logger logger = context.getLogger("hello.validation.test");
    logger.setLevel(Level.INFO);
    logger.setAdditive(false);
    logger.addAppender(async);
    try {
      logger.info("Receive HELLO {}", message);
    } finally {
      async.stop();
      context.stop();
    }
    Assert.assertEquals(1, sink.list.size());
    Assert.assertEquals("Receive HELLO P2P_HELLO: invalid hello message",
        sink.list.get(0).getFormattedMessage());
    verify(message, never()).getFrom();
  }

  private static void assertRejectedBeforeNodeConstruction(HelloMessage message) {
    HelloMessage checked = spy(message);
    doThrow(new AssertionError("Invalid endpoint reached Node construction"))
        .when(checked).getFrom();
    Assert.assertFalse(checked.validEndPoint());
    Assert.assertFalse(checked.valid());
    Assert.assertEquals("P2P_HELLO: invalid hello message", checked.toString());
    verify(checked, never()).getFrom();
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
