package org.tron.p2p.connection;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import java.lang.reflect.Field;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.socket.PeerClient;
import org.tron.p2p.connection.socket.PeerServer;

public class PeerServerTest {

  private static P2pConfig previousConfig;

  @BeforeClass
  public static void init() {
    previousConfig = Parameter.p2pConfig;
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp("127.0.0.1");
    Parameter.p2pConfig.setIpv6("");
  }

  @AfterClass
  public static void destroy() {
    Parameter.p2pConfig = previousConfig;
  }

  @Test(timeout = 15000)
  public void occupiedPortFailsSynchronouslyAndReleasesEventLoops() throws Exception {
    PeerServer server = new PeerServer();
    try (ServerSocket occupied = new ServerSocket(0)) {
      Parameter.p2pConfig.setPort(occupied.getLocalPort());
      IllegalStateException error = Assert.assertThrows(IllegalStateException.class, server::init);
      Assert.assertTrue(error.getCause() instanceof BindException);
      Assert.assertTrue(error.getMessage().contains(Integer.toString(occupied.getLocalPort())));
      assertEventLoopsTerminated(server);
    } finally {
      server.close();
    }
  }

  @Test(timeout = 15000)
  public void successfulBindReturnsBeforeCloseAndReleasesPort() throws Exception {
    PeerServer server = new PeerServer();
    try {
      server.start(0);
      ChannelFuture bind = (ChannelFuture) field(server, "channelFuture");
      Assert.assertTrue(bind.isSuccess());
      Assert.assertTrue(bind.channel().isActive());
      int port = ((InetSocketAddress) bind.channel().localAddress()).getPort();
      Assert.assertThrows(BindException.class, () -> {
        try (ServerSocket unexpected = new ServerSocket(port)) {
          Assert.fail("The listener must hold its port until close");
        }
      });
      server.close();
      server.close();
      assertEventLoopsTerminated(server);
      Assert.assertFalse(bind.channel().isOpen());
      try (ServerSocket rebound = new ServerSocket(port)) {
        Assert.assertTrue(rebound.isBound());
      }
    } finally {
      server.close();
    }
  }

  @Test(timeout = 15000)
  public void startupFailurePreservesInterruptStatus() throws Exception {
    PeerServer server = new PeerServer();
    try (ServerSocket occupied = new ServerSocket(0)) {
      Thread.currentThread().interrupt();
      Assert.assertThrows(IllegalStateException.class, () -> server.start(occupied.getLocalPort()));
      Assert.assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
      server.close();
      assertEventLoopsTerminated(server);
    }
  }

  @Test
  public void disabledListenerAndUninitializedClientCanBeClosed() throws Exception {
    Parameter.p2pConfig.setPort(0);
    PeerServer server = new PeerServer();
    server.init();
    server.close();
    Assert.assertNull(field(server, "channelFuture"));
    Assert.assertNull(field(server, "bossGroup"));
    Assert.assertNull(field(server, "workerGroup"));
    new PeerClient().close();
  }

  private void assertEventLoopsTerminated(PeerServer server) throws Exception {
    for (String name : new String[]{"bossGroup", "workerGroup"}) {
      EventLoopGroup group = (EventLoopGroup) field(server, name);
      Assert.assertNotNull(group);
      Assert.assertTrue(group.terminationFuture().awaitUninterruptibly(5, TimeUnit.SECONDS));
      Assert.assertTrue(group.isTerminated());
    }
  }

  private Object field(PeerServer server, String name) throws Exception {
    Field field = PeerServer.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(server);
  }
}
