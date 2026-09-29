package org.tron.p2p.stats;

import lombok.Data;

@Data
public class P2pStats {
  private long tcpOutSize;
  private long tcpInSize;
  private long tcpOutPackets;
  private long tcpInPackets;
  private long udpOutSize;
  private long udpInSize;
  private long udpOutPackets;
  private long udpInPackets;
}
