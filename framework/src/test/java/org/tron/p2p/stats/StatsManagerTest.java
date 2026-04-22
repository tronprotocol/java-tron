package org.tron.p2p.stats;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StatsManagerTest {

  private StatsManager statsManager;

  @Before
  public void setUp() {
    statsManager = new StatsManager();
    // Reset static counters before each test
    resetTrafficHandler(TrafficStats.tcp);
    resetTrafficHandler(TrafficStats.udp);
  }

  @After
  public void tearDown() {
    resetTrafficHandler(TrafficStats.tcp);
    resetTrafficHandler(TrafficStats.udp);
  }

  private void resetTrafficHandler(Object handler) {
    try {
      for (String fieldName : new String[] {"outSize", "inSize", "outPackets", "inPackets"}) {
        Field f = handler.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        ((AtomicLong) f.get(handler)).set(0);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testGetP2pStatsInitiallyZero() {
    P2pStats stats = statsManager.getP2pStats();
    assertEquals(0, stats.getTcpInPackets());
    assertEquals(0, stats.getTcpOutPackets());
    assertEquals(0, stats.getTcpInSize());
    assertEquals(0, stats.getTcpOutSize());
    assertEquals(0, stats.getUdpInPackets());
    assertEquals(0, stats.getUdpOutPackets());
    assertEquals(0, stats.getUdpInSize());
    assertEquals(0, stats.getUdpOutSize());
  }

  @Test
  public void testGetP2pStatsWithTcpTraffic() {
    TrafficStats.tcp.getInPackets().set(10);
    TrafficStats.tcp.getOutPackets().set(5);
    TrafficStats.tcp.getInSize().set(1024);
    TrafficStats.tcp.getOutSize().set(512);

    P2pStats stats = statsManager.getP2pStats();
    assertEquals(10, stats.getTcpInPackets());
    assertEquals(5, stats.getTcpOutPackets());
    assertEquals(1024, stats.getTcpInSize());
    assertEquals(512, stats.getTcpOutSize());
  }

  @Test
  public void testGetP2pStatsWithUdpTraffic() {
    TrafficStats.udp.getInPackets().set(20);
    TrafficStats.udp.getOutPackets().set(15);
    TrafficStats.udp.getInSize().set(2048);
    TrafficStats.udp.getOutSize().set(1024);

    P2pStats stats = statsManager.getP2pStats();
    assertEquals(20, stats.getUdpInPackets());
    assertEquals(15, stats.getUdpOutPackets());
    assertEquals(2048, stats.getUdpInSize());
    assertEquals(1024, stats.getUdpOutSize());
  }

  @Test
  public void testGetP2pStatsWithMixedTraffic() {
    TrafficStats.tcp.getInPackets().set(100);
    TrafficStats.tcp.getOutPackets().set(50);
    TrafficStats.tcp.getInSize().set(10000);
    TrafficStats.tcp.getOutSize().set(5000);
    TrafficStats.udp.getInPackets().set(200);
    TrafficStats.udp.getOutPackets().set(150);
    TrafficStats.udp.getInSize().set(20000);
    TrafficStats.udp.getOutSize().set(15000);

    P2pStats stats = statsManager.getP2pStats();
    assertEquals(100, stats.getTcpInPackets());
    assertEquals(50, stats.getTcpOutPackets());
    assertEquals(10000, stats.getTcpInSize());
    assertEquals(5000, stats.getTcpOutSize());
    assertEquals(200, stats.getUdpInPackets());
    assertEquals(150, stats.getUdpOutPackets());
    assertEquals(20000, stats.getUdpInSize());
    assertEquals(15000, stats.getUdpOutSize());
  }
}
