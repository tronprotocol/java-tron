package org.tron.p2p.stats;

public class StatsManager {

  public P2pStats getP2pStats() {
    P2pStats stats = new P2pStats();
    stats.setTcpInPackets(TrafficStats.tcp.getInPackets().get());
    stats.setTcpOutPackets(TrafficStats.tcp.getOutPackets().get());
    stats.setTcpInSize(TrafficStats.tcp.getInSize().get());
    stats.setTcpOutSize(TrafficStats.tcp.getOutSize().get());
    stats.setUdpInPackets(TrafficStats.udp.getInPackets().get());
    stats.setUdpOutPackets(TrafficStats.udp.getOutPackets().get());
    stats.setUdpInSize(TrafficStats.udp.getInSize().get());
    stats.setUdpOutSize(TrafficStats.udp.getOutSize().get());
    return stats;
  }
}
