package org.tron.core.metrics.net;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class NetInfo {
  private int errorProtoCount;
  private ApiInfo api;
  private int connectionCount;
  private int validConnectionCount;
  private RateInfo tcpInTraffic;
  private RateInfo tcpOutTraffic;
  private int disconnectionCount;
  private List<DisconnectionDetailInfo> disconnectionDetail = new ArrayList<>();
  private RateInfo udpInTraffic;
  private RateInfo udpOutTraffic;
  private LatencyInfo latency;
}
