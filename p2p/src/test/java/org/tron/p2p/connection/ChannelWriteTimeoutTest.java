package org.tron.p2p.connection;

import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.connection.message.keepalive.PingMessage;

public class ChannelWriteTimeoutTest {

  @Test
  public void stalledWriteClosesConnectionAfterTenSeconds() throws Exception {
    EmbeddedChannel netty = new EmbeddedChannel() {
      @Override
      protected void doWrite(ChannelOutboundBuffer buffer) {
        // A socket whose remote peer does not read: leave writes pending in Netty.
      }
    };
    netty.freezeTime();
    Channel channel = new Channel();
    channel.init(netty.pipeline(), "", false);
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", 18888);
    set(channel, "ctx", netty.pipeline().lastContext());
    set(channel, "inetSocketAddress", address);
    set(channel, "inetAddress", address.getAddress());
    try {
      channel.send(new PingMessage());
      netty.advanceTimeBy(9, TimeUnit.SECONDS);
      netty.runScheduledPendingTasks();
      Assert.assertTrue(netty.isOpen());

      netty.advanceTimeBy(1, TimeUnit.SECONDS);
      netty.runScheduledPendingTasks();
      Assert.assertFalse("a pending write must time out even without a business queue",
          netty.isOpen());
      Assert.assertTrue(channel.isDisconnect());
    } finally {
      netty.finishAndReleaseAll();
      ChannelManager.getBannedNodes().invalidate(address.getAddress());
    }
  }

  private static void set(Channel channel, String name, Object value) throws Exception {
    Field field = Channel.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(channel, value);
  }
}
