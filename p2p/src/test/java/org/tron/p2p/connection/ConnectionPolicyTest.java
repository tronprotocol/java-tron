package org.tron.p2p.connection;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.socket.MessageHandler;
import org.tron.p2p.connection.socket.PeerClient;
import org.tron.p2p.discover.Node;

public class ConnectionPolicyTest {

  @After
  public void clearPolicy() {
    ConnectionPolicy.replaceBlockedIps(Collections.emptySet());
  }

  @Test
  public void replaceBlockedIpsUsesDefensiveSnapshot() throws Exception {
    InetAddress blocked = InetAddress.getByName("192.0.2.1");
    Set<InetAddress> input = new HashSet<>();
    input.add(blocked);

    ConnectionPolicy.replaceBlockedIps(input);
    input.clear();

    Assert.assertTrue(ConnectionPolicy.isBlocked(blocked));
    Assert.assertTrue(ConnectionPolicy.isBlocked(new InetSocketAddress(blocked, 18888)));

    InetAddress replacement = InetAddress.getByName("192.0.2.2");
    ConnectionPolicy.replaceBlockedIps(Collections.singleton(replacement));
    Assert.assertFalse(ConnectionPolicy.isBlocked(blocked));
    Assert.assertTrue(ConnectionPolicy.isBlocked(replacement));

    ConnectionPolicy.replaceBlockedIps(Collections.emptySet());
    Assert.assertFalse(ConnectionPolicy.isBlocked(replacement));
  }

  @Test
  public void ipv4MappedIpv6MatchesIpv4() throws Exception {
    byte[] mappedBytes = new byte[16];
    mappedBytes[10] = (byte) 0xff;
    mappedBytes[11] = (byte) 0xff;
    mappedBytes[12] = (byte) 192;
    mappedBytes[14] = 2;
    mappedBytes[15] = 1;
    InetAddress mapped = Inet6Address.getByAddress(null, mappedBytes, -1);
    InetAddress ipv4 = InetAddress.getByName("192.0.2.1");
    InetAddress other = InetAddress.getByName("192.0.2.2");

    ConnectionPolicy.replaceBlockedIps(Collections.singleton(mapped));

    Assert.assertTrue(ConnectionPolicy.isBlocked(ipv4));
    Assert.assertFalse(ConnectionPolicy.isBlocked(other));

    ConnectionPolicy.replaceBlockedIps(Collections.singleton(ipv4));

    Assert.assertTrue(ConnectionPolicy.isBlocked(mapped));
    Assert.assertFalse(ConnectionPolicy.isBlocked(other));
  }

  @Test
  public void addressMetadataDoesNotChangeIpMatching() throws Exception {
    InetAddress ipv4 = InetAddress.getByName("192.0.2.1");
    InetAddress namedIpv4 = InetAddress.getByAddress("peer.invalid", ipv4.getAddress());
    InetAddress ipv6 = InetAddress.getByName("2001:db8::1");
    InetAddress scopedIpv6 = Inet6Address.getByAddress("peer.invalid", ipv6.getAddress(), 3);
    InetAddress otherScope = Inet6Address.getByAddress(null, ipv6.getAddress(), 4);
    Set<InetAddress> input = new HashSet<>();
    input.add(namedIpv4);
    input.add(scopedIpv6);

    ConnectionPolicy.replaceBlockedIps(input);

    Assert.assertTrue(ConnectionPolicy.isBlocked(ipv4));
    Assert.assertTrue(ConnectionPolicy.isBlocked(ipv6));
    Assert.assertTrue(ConnectionPolicy.isBlocked(otherScope));
    Assert.assertTrue(ConnectionPolicy.isBlocked(new InetSocketAddress(ipv6, 18888)));
    Assert.assertTrue(ConnectionPolicy.isBlocked(new InetSocketAddress(ipv6, 18889)));
    Assert.assertFalse(ConnectionPolicy.isBlocked(InetAddress.getByName("2001:db8::2")));
    Assert.assertFalse(ConnectionPolicy.isBlocked((InetAddress) null));
    Assert.assertFalse(ConnectionPolicy.isBlocked((InetSocketAddress) null));
    Assert.assertFalse(ConnectionPolicy.isBlocked(
        InetSocketAddress.createUnresolved("peer.invalid", 18888)));

    ConnectionPolicy.replaceBlockedIps(Collections.singleton(ipv6));
    Assert.assertTrue(ConnectionPolicy.isBlocked(scopedIpv6));
  }

  @Test
  public void rejectInvalidReplacement() throws Exception {
    InetAddress blocked = InetAddress.getByName("192.0.2.2");
    ConnectionPolicy.replaceBlockedIps(Collections.singleton(blocked));
    try {
      ConnectionPolicy.replaceBlockedIps(null);
      Assert.fail("Expected null set to be rejected");
    } catch (IllegalArgumentException expected) {
      Assert.assertTrue(expected.getMessage().contains("must not be null"));
    }
    Assert.assertTrue(ConnectionPolicy.isBlocked(blocked));

    InetAddress replacement = InetAddress.getByName("192.0.2.1");
    // Visit the valid entry before null to catch premature publication of a partial replacement.
    Set<InetAddress> withNull = new LinkedHashSet<>();
    withNull.add(replacement);
    withNull.add(null);
    try {
      ConnectionPolicy.replaceBlockedIps(withNull);
      Assert.fail("Expected null element to be rejected");
    } catch (IllegalArgumentException expected) {
      Assert.assertTrue(expected.getMessage().contains("must not contain null"));
    }
    Assert.assertTrue(ConnectionPolicy.isBlocked(blocked));
    Assert.assertFalse(ConnectionPolicy.isBlocked(replacement));
  }

  @Test(timeout = 5_000)
  public void peerClientRejectsBlockedAddressBeforeBootstrap() throws Exception {
    P2pConfig previousConfig = Parameter.p2pConfig;
    boolean previousShutdown = ChannelManager.isShutdown;
    InetSocketAddress address = new InetSocketAddress("192.0.2.40", 18888);
    try {
      Parameter.p2pConfig = new P2pConfig();
      ChannelManager.isShutdown = false;
      ConnectionPolicy.replaceBlockedIps(Collections.singleton(address.getAddress()));

      PeerClient client = new PeerClient();
      Node node = new Node(address);
      ChannelFutureListener listener = Mockito.mock(ChannelFutureListener.class);
      Assert.assertNull(client.connectAsync(node, false));
      Assert.assertNull(client.connectAsync(node, true));
      Assert.assertNull(client.connect(node, listener));
      Mockito.verifyNoInteractions(listener);
    } finally {
      Parameter.p2pConfig = previousConfig;
      ChannelManager.isShutdown = previousShutdown;
    }
  }

  @Test
  public void messageHandlerClosesBlockedTcpChannelBeforeHandshake() {
    P2pConfig previousConfig = Parameter.p2pConfig;
    try {
      Parameter.p2pConfig = new P2pConfig();
      final InetSocketAddress address = new InetSocketAddress("192.0.2.41", 18888);
      ConnectionPolicy.replaceBlockedIps(Collections.singleton(address.getAddress()));
      Channel channel = new Channel();
      EmbeddedChannel embeddedChannel = new EmbeddedChannel() {
        @Override
        protected SocketAddress remoteAddress0() {
          return address;
        }
      };

      Assert.assertTrue(embeddedChannel.isOpen());
      embeddedChannel.pipeline().addLast(new MessageHandler(channel));
      embeddedChannel.pipeline().fireChannelActive();

      Assert.assertFalse(embeddedChannel.isOpen());
    } finally {
      Parameter.p2pConfig = previousConfig;
    }
  }
}
