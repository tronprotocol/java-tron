package org.tron.p2p.connection;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import org.junit.Assert;
import org.junit.Test;

/**
 * Covers Channel's value semantics and latency accounting — the parts that do not
 * need a live connection. Identity matters because channels are held in sets and
 * maps keyed by peer, so equals/hashCode drive connection de-duplication.
 */
public class ChannelValueTest {

  private static Channel channelAt(String host, int port) throws Exception {
    Channel channel = new Channel();
    Field field = Channel.class.getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(channel, new InetSocketAddress(host, port));
    return channel;
  }

  @Test
  public void updateAvgLatencyKeepsARunningMean() {
    Channel channel = new Channel();
    // running mean: 10 -> 10, then (10+20)/2 = 15, then (15*2+30)/3 = 20
    channel.updateAvgLatency(10);
    Assert.assertEquals(10, channel.getAvgLatency());
    channel.updateAvgLatency(20);
    Assert.assertEquals(15, channel.getAvgLatency());
    channel.updateAvgLatency(30);
    Assert.assertEquals(20, channel.getAvgLatency());
  }

  @Test
  public void updateAvgLatencyFromZero() {
    Channel channel = new Channel();
    // the first sample defines the mean, with no division-by-zero on count
    channel.updateAvgLatency(0);
    Assert.assertEquals(0, channel.getAvgLatency());
    channel.updateAvgLatency(100);
    Assert.assertEquals(50, channel.getAvgLatency());
  }

  @Test
  public void equalityIsByRemoteAddress() throws Exception {
    Channel a = channelAt("127.0.0.1", 10000);
    Channel sameAddress = channelAt("127.0.0.1", 10000);
    Channel otherPort = channelAt("127.0.0.1", 10001);
    Channel otherHost = channelAt("127.0.0.2", 10000);

    Assert.assertEquals(a, a);
    Assert.assertEquals(a, sameAddress);
    Assert.assertNotEquals(a, otherPort);
    Assert.assertNotEquals(a, otherHost);

    Assert.assertNotEquals(a, null);
    Assert.assertNotEquals(a, "not a channel");
  }

  @Test
  public void hashCodeAgreesWithEquals() throws Exception {
    Channel a = channelAt("127.0.0.1", 10000);
    Channel sameAddress = channelAt("127.0.0.1", 10000);
    // equal channels must hash equally, or set/map de-duplication breaks
    Assert.assertEquals(a.hashCode(), sameAddress.hashCode());
    Assert.assertEquals(new InetSocketAddress("127.0.0.1", 10000).hashCode(), a.hashCode());
  }
}
