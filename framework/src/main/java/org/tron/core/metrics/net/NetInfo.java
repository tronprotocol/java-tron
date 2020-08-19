package org.tron.core.metrics.net;

import java.util.ArrayList;
import java.util.List;

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

  public int getErrorProtoCount() {
    return errorProtoCount;
  }

  public void setErrorProtoCount(int errorProtoCount) {
    this.errorProtoCount = errorProtoCount;
  }

  public ApiInfo getApi() {
    return api;
  }

  public void setApi(ApiInfo api) {
    this.api = api;
  }

  public int getConnectionCount() {
    return connectionCount;
  }

  public void setConnectionCount(int connectionCount) {
    this.connectionCount = connectionCount;
  }

  public int getValidConnectionCount() {
    return validConnectionCount;
  }

  public void setValidConnectionCount(int validConnectionCount) {
    this.validConnectionCount = validConnectionCount;
  }

  public RateInfo getTcpInTraffic() {
    return tcpInTraffic;
  }

  public void setTcpInTraffic(RateInfo tcpInTraffic) {
    this.tcpInTraffic = tcpInTraffic;
  }

  public RateInfo getTcpOutTraffic() {
    return tcpOutTraffic;
  }

  public void setTcpOutTraffic(RateInfo tcpOutTraffic) {
    this.tcpOutTraffic = tcpOutTraffic;
  }

  public int getDisconnectionCount() {
    return disconnectionCount;
  }

  public void setDisconnectionCount(int disconnectionCount) {
    this.disconnectionCount = disconnectionCount;
  }

  public List<DisconnectionDetailInfo> getDisconnectionDetail() {
    return disconnectionDetail;
  }

  public void setDisconnectionDetail(List<DisconnectionDetailInfo> disconnectionDetail) {
    this.disconnectionDetail = disconnectionDetail;
  }

  public RateInfo getUdpInTraffic() {
    return udpInTraffic;
  }

  public void setUdpInTraffic(RateInfo udpInTraffic) {
    this.udpInTraffic = udpInTraffic;
  }

  public RateInfo getUdpOutTraffic() {
    return udpOutTraffic;
  }

  public void setUdpOutTraffic(RateInfo udpOutTraffic) {
    this.udpOutTraffic = udpOutTraffic;
  }

  public LatencyInfo getLatency() {
    return latency;
  }

  public void setLatency(LatencyInfo latency) {
    this.latency = latency;
  }
}
