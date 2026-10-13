package org.tron.p2p.connection;

import com.google.protobuf.ByteString;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.business.pool.ConnPoolService;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.connection.socket.PeerClient;
import org.tron.p2p.discover.Node;
import org.tron.p2p.dns.DnsNode;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;

public class ConnPoolPortValidationTest {

  private final InetSocketAddress candidate = new InetSocketAddress("127.0.0.3", 18888);
  private final List<Node> dialed = new ArrayList<>();
  private final List<EmbeddedChannel> sockets = new ArrayList<>();
  private P2pConfig previousConfig;
  private List<P2pEventHandler> previousHandlers;
  private Map<InetSocketAddress, Channel> previousChannels;
  private Map<InetAddress, Long> previousBans;
  private ConnPoolService pool;

  @Before
  public void init() throws Exception {
    previousConfig = Parameter.p2pConfig;
    previousHandlers = Parameter.handlerList;
    previousChannels = new HashMap<>(ChannelManager.getChannels());
    previousBans = new HashMap<>(ChannelManager.getBannedNodes().asMap());
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp("127.0.0.1");
    Parameter.p2pConfig.setIpv6("");
    Parameter.p2pConfig.setMinConnections(0);
    Parameter.p2pConfig.setMinActiveConnections(0);
    Parameter.p2pConfig.getActiveNodes().add(candidate);
    Parameter.handlerList = new ArrayList<>();
    ChannelManager.getChannels().clear();
    ChannelManager.getBannedNodes().invalidateAll();
    pool = new ConnPoolService();
    Field client = ConnPoolService.class.getDeclaredField("peerClient");
    client.setAccessible(true);
    client.set(pool, new PeerClient() {
      @Override
      public ChannelFuture connectAsync(Node node, boolean discoveryMode) {
        dialed.add(node);
        return null;
      }
    });
  }

  @After
  public void destroy() {
    sockets.forEach(EmbeddedChannel::finishAndReleaseAll);
    if (pool != null) {
      pool.close();
    }
    ChannelManager.getChannels().clear();
    ChannelManager.getChannels().putAll(previousChannels);
    ChannelManager.getBannedNodes().invalidateAll();
    ChannelManager.getBannedNodes().putAll(previousBans);
    Parameter.handlerList = previousHandlers;
    Parameter.p2pConfig = previousConfig;
  }

  @Test
  public void poisonedChannelDoesNotPreventDialing() throws Exception {
    Channel healthy = registerChannel(18888, 20000);
    for (int port : new int[]{70000, 0, -1, 65536, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
      Channel poisoned = registerChannel(port, 20001);
      Method connect = ConnPoolService.class.getDeclaredMethod("connect", boolean.class);
      connect.setAccessible(true);
      connect.invoke(pool, false);

      Assert.assertTrue(poisoned.isDisconnect());
      Assert.assertFalse(poisoned.getCtx().channel().isOpen());
      Assert.assertFalse(ChannelManager.getChannels().containsKey(poisoned.getInetSocketAddress()));
      Assert.assertTrue(healthy.getCtx().channel().isOpen());
      Assert.assertSame(healthy, ChannelManager.getChannels().get(healthy.getInetSocketAddress()));
      Assert.assertEquals(candidate, dialed.get(dialed.size() - 1).getPreferInetSocketAddress());
      ChannelManager.getBannedNodes().invalidateAll();
    }
    Assert.assertEquals(6, dialed.size());
  }

  @Test
  public void invalidCandidatesAreSkippedAndDnsNodesRemainEligible() throws Exception {
    Node good = new Node(new byte[64], "127.0.0.4", "", 65535);
    DnsNode dns = new DnsNode(null, "127.0.0.5", "", 1);
    List<Node> candidates = new ArrayList<>(Arrays.asList(good, dns, null,
        new Node(new byte[64], "", "2001:db8::1", 18888)));
    for (int port : new int[]{0, -1, 65536, 70000, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
      candidates.add(new Node(new byte[64], "127.0.0.6", "", port));
    }
    List<Node> selected = pool.getNodes(new HashSet<>(), new HashSet<>(), candidates, 10);
    Assert.assertEquals(2, selected.size());
    Assert.assertTrue(selected.contains(good));
    Assert.assertTrue(selected.contains(dns));
    Assert.assertNull(dns.getId());
  }

  private Channel registerChannel(int port, int sourcePort) throws Exception {
    EmbeddedChannel socket = new EmbeddedChannel(new ChannelInboundHandlerAdapter()) {
      @Override
      protected SocketAddress remoteAddress0() {
        return new InetSocketAddress("127.0.0.2", sourcePort);
      }
    };
    sockets.add(socket);
    Channel channel = new Channel();
    channel.setChannelHandlerContext(socket.pipeline().firstContext());
    byte[] id = new byte[64];
    id[0] = (byte) sourcePort;
    Discover.Endpoint endpoint = Discover.Endpoint.newBuilder().setNodeId(ByteString.copyFrom(id))
        .setAddress(ByteString.copyFromUtf8("1.2.3.4")).setPort(port).build();
    // Deliberately bypass wire validation to exercise defense against existing bad state.
    channel.setHelloMessage(new HelloMessage(Connect.HelloMessage.newBuilder()
        .setFrom(endpoint).build().toByteArray()));
    socket.closeFuture().addListener(future -> ChannelManager.notifyDisconnect(channel));
    synchronized (ChannelManager.class) {
      Assert.assertEquals(DisconnectCode.NORMAL, ChannelManager.checkPeer(channel));
      ChannelManager.addPeer(channel);
    }
    pool.onConnect(channel);
    return channel;
  }
}
