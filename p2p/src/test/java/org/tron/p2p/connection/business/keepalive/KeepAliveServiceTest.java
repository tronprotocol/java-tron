package org.tron.p2p.connection.business.keepalive;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.base.P2pDisconnectMessage;
import org.tron.p2p.connection.message.keepalive.PingMessage;
import org.tron.p2p.connection.message.keepalive.PongMessage;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Connect.DisconnectReason;

/**
 * The keep-alive loop is what disconnects a silent peer, so both halves matter:
 * answering a ping, and clearing the wait flag when a pong lands.
 */
public class KeepAliveServiceTest {

  private static final InetSocketAddress ADDRESS =
      new InetSocketAddress("127.0.0.1", 18888);

  private static class RecordingChannel extends Channel {
    final List<Message> sent = new ArrayList<>();
    private int closeCount;

    @Override
    public void send(Message message) {
      sent.add(message);
    }

    @Override
    public void close() {
      closeCount++;
      markDisconnected();
    }
  }

  private P2pConfig saved;

  private static RecordingChannel channelAt(InetSocketAddress address) throws Exception {
    RecordingChannel channel = new RecordingChannel();
    Field field = Channel.class.getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(channel, address);
    return channel;
  }

  @Before
  public void setUp() {
    saved = Parameter.p2pConfig;
    Parameter.p2pConfig = new P2pConfig();
    ChannelManager.getAllChannels().forEach(ChannelManager::notifyDisconnect);
  }

  @After
  public void tearDown() {
    ChannelManager.getAllChannels().forEach(ChannelManager::notifyDisconnect);
    Parameter.p2pConfig = saved;
  }

  @Test
  public void pingIsAnsweredWithAPong() throws Exception {
    RecordingChannel channel = channelAt(ADDRESS);
    new KeepAliveService().processMessage(channel, new PingMessage());

    Assert.assertEquals(1, channel.sent.size());
    Assert.assertEquals(MessageType.KEEP_ALIVE_PONG, channel.sent.get(0).getType());
    Assert.assertFalse(channel.isDisconnect());
  }

  @Test
  public void inboundPingBurstDisconnectsWithoutPongForRejectedPing() throws Exception {
    RecordingChannel channel = channelAt(ADDRESS);
    KeepAliveService service = new KeepAliveService();
    try {
      // A Guava limiter may have one saved permit after idle, in addition to a fresh permit.
      for (int i = 0; i < 3 && !channel.isDisconnect(); i++) {
        service.processMessage(channel, new PingMessage());
      }

      Assert.assertTrue("Excess PING must disconnect the peer", channel.isDisconnect());
      Assert.assertEquals(1, channel.closeCount);
      int last = channel.sent.size() - 1;
      Assert.assertTrue(last >= 1 && last <= 2);
      for (int i = 0; i < last; i++) {
        Assert.assertEquals(MessageType.KEEP_ALIVE_PONG, channel.sent.get(i).getType());
      }
      Assert.assertEquals(MessageType.DISCONNECT, channel.sent.get(last).getType());
      Assert.assertEquals(DisconnectReason.BAD_PROTOCOL,
          Connect.P2pDisconnectMessage.parseFrom(channel.sent.get(last).getData()).getReason());
    } finally {
      service.close();
    }
  }

  @Test
  public void inboundPingAllowanceIsIndependentForEachConnection() throws Exception {
    RecordingChannel channel = channelAt(ADDRESS);
    RecordingChannel other = channelAt(ADDRESS);
    KeepAliveService service = new KeepAliveService();
    try {
      service.processMessage(channel, new PingMessage());
      service.processMessage(other, new PingMessage());

      Assert.assertEquals(1, channel.sent.size());
      Assert.assertEquals(1, other.sent.size());
      Assert.assertEquals(MessageType.KEEP_ALIVE_PONG, other.sent.get(0).getType());
      Assert.assertFalse(channel.isDisconnect());
      Assert.assertFalse(other.isDisconnect());
    } finally {
      service.close();
    }
  }

  @Test
  public void pongClearsTheWaitFlagAndRecordsLatency() throws Exception {
    RecordingChannel channel = channelAt(ADDRESS);
    channel.waitForPong = true;
    channel.pingSent = System.currentTimeMillis() - 25;

    new KeepAliveService().processMessage(channel, new PongMessage());

    Assert.assertFalse(channel.waitForPong);
    Assert.assertTrue("latency should have been recorded", channel.getAvgLatency() >= 0);
    // A pong is not itself answered.
    Assert.assertTrue(channel.sent.isEmpty());
  }

  @Test
  public void otherMessageTypesAreIgnored() throws Exception {
    RecordingChannel channel = channelAt(ADDRESS);
    channel.waitForPong = true;

    new KeepAliveService().processMessage(channel,
        new P2pDisconnectMessage(DisconnectReason.PEER_QUITING));

    Assert.assertTrue(channel.sent.isEmpty());
    Assert.assertTrue("unrelated messages must not clear the wait flag", channel.waitForPong);
  }

  @Test
  public void closeShutsDownTheScheduler() {
    KeepAliveService service = new KeepAliveService();
    service.close();
    // A second close must not throw.
    service.close();
  }
}
