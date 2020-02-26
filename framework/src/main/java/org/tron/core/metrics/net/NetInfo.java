package org.tron.core.metrics.net;

import com.alibaba.fastjson.annotation.JSONField;

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
    return this.errorProtoCount;
  }

  public NetInfo setErrorProtoCount(int errorProtoCount) {
    this.errorProtoCount = errorProtoCount;
    return this;
  }

  public ApiInfo getApi() {
    return this.api;
  }

  public NetInfo setApi(ApiInfo api) {
    this.api = api;
    return this;
  }

  public int getConnectionCount() {
    return this.connectionCount;
  }

  public NetInfo setConnectionCount(int connectionCount) {
    this.connectionCount = connectionCount;
    return this;
  }

  public int getValidConnectionCount() {
    return this.validConnectionCount;
  }

  public NetInfo setValidConnectionCount(int validConnectionCount) {
    this.validConnectionCount = validConnectionCount;
    return this;
  }

  public RateInfo getTcpInTraffic() {
    return this.tcpInTraffic;
  }

  @JSONField(name = "TCPInTraffic")
  public NetInfo setTcpInTraffic(RateInfo tcpInTraffic) {
    this.tcpInTraffic = tcpInTraffic;
    return this;
  }

  public RateInfo getTcpOutTraffic() {
    return this.tcpOutTraffic;
  }

  @JSONField(name = "TCPOutTraffic")
  public NetInfo setTcpOutTraffic(RateInfo tcpOutTraffic) {
    this.tcpOutTraffic = tcpOutTraffic;
    return this;
  }

  public int getDisconnectionCount() {
    return this.disconnectionCount;
  }

  public NetInfo setDisconnectionCount(int disconnectionCount) {
    this.disconnectionCount = disconnectionCount;
    return this;
  }

  public List<DisconnectionDetailInfo> getDisconnectionDetail() {
    return this.disconnectionDetail;
  }

  public NetInfo setDisconnectionDetail(List<DisconnectionDetailInfo> disconnectionDetail) {
    this.disconnectionDetail = disconnectionDetail;
    return this;
  }

  public RateInfo getUdpInTraffic() {
    return this.udpInTraffic;
  }

  @JSONField(name = "UDPInTraffic")
  public NetInfo setUdpInTraffic(RateInfo udpInTraffic) {
    this.udpInTraffic = udpInTraffic;
    return this;
  }

  public RateInfo getUdpOutTraffic() {
    return this.udpOutTraffic;
  }

  @JSONField(name = "UDPOutTraffic")
  public NetInfo setUdpOutTraffic(RateInfo udpOutTraffic) {
    this.udpOutTraffic = udpOutTraffic;
    return this;
  }

  public LatencyInfo getLatency() {
    return this.latency;
  }

  public NetInfo setLatency(LatencyInfo latency) {
    this.latency = latency;
    return this;
  }


}
