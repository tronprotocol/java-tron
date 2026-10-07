package org.tron.p2p.stats;

import org.junit.Assert;
import org.junit.Test;

public class StatsManagerTest {

  @Test
  public void snapshotMirrorsTheLiveCounters() {
    P2pStats stats = new StatsManager().getP2pStats();

    Assert.assertEquals(TrafficStats.tcp.getInPackets().get(), stats.getTcpInPackets());
    Assert.assertEquals(TrafficStats.tcp.getOutPackets().get(), stats.getTcpOutPackets());
    Assert.assertEquals(TrafficStats.tcp.getInSize().get(), stats.getTcpInSize());
    Assert.assertEquals(TrafficStats.tcp.getOutSize().get(), stats.getTcpOutSize());
    Assert.assertEquals(TrafficStats.udp.getInPackets().get(), stats.getUdpInPackets());
    Assert.assertEquals(TrafficStats.udp.getOutPackets().get(), stats.getUdpOutPackets());
    Assert.assertEquals(TrafficStats.udp.getInSize().get(), stats.getUdpInSize());
    Assert.assertEquals(TrafficStats.udp.getOutSize().get(), stats.getUdpOutSize());
  }

  @Test
  public void snapshotIsDetachedFromLaterTraffic() {
    P2pStats before = new StatsManager().getP2pStats();
    long recorded = before.getTcpInPackets();
    TrafficStats.tcp.getInPackets().incrementAndGet();

    // The old snapshot must not move with the counter.
    Assert.assertEquals(recorded, before.getTcpInPackets());
    Assert.assertEquals(recorded + 1, new StatsManager().getP2pStats().getTcpInPackets());
  }
}
