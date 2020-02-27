package org.tron.core.metrics.net;

public class ApiDetailInfo {
  private String name;
  private RateInfo count;
  private RateInfo failCount;
  private RateInfo outTraffic;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public RateInfo getCount() {
    return count;
  }

  public void setCount(RateInfo count) {
    this.count = count;
  }

  public RateInfo getFailCount() {
    return failCount;
  }

  public void setFailCount(RateInfo failCount) {
    this.failCount = failCount;
  }

  public RateInfo getOutTraffic() {
    return outTraffic;
  }

  public void setOutTraffic(RateInfo outTraffic) {
    this.outTraffic = outTraffic;
  }
}
