package org.tron.core.metrics.net;

public class ApiDetailInfo {

  private String name;
  private RateInfo qps;
  private RateInfo failQps;
  private RateInfo outTraffic;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public RateInfo getQps() {
    return qps;
  }

  public void setQps(RateInfo qps) {
    this.qps = qps;
  }

  public RateInfo getFailQps() {
    return failQps;
  }

  public void setFailQps(RateInfo failQps) {
    this.failQps = failQps;
  }

  public RateInfo getOutTraffic() {
    return outTraffic;
  }

  public void setOutTraffic(RateInfo outTraffic) {
    this.outTraffic = outTraffic;
  }
}
