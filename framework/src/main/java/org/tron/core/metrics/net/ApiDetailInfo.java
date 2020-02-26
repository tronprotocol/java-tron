package org.tron.core.metrics.net;

public class ApiDetailInfo {
  private String name;
  private Common count;
  private Common failCount;
  private Common outTraffic;

  public String getName() {
    return this.name;
  }

  public ApiDetailInfo setName(String name) {
    this.name = name;
    return this;
  }

  public Common getCount() {
    return this.count;
  }

  public ApiDetailInfo setCount(Common count) {
    this.count = count;
    return this;
  }

  public Common getFailCount() {
    return this.failCount;
  }

  public ApiDetailInfo setFailCount(Common failCount) {
    this.failCount = failCount;
    return this;
  }

  public Common getOutTraffic() {
    return this.outTraffic;
  }

  public ApiDetailInfo setOutTraffic(Common outTraffic) {
    this.outTraffic = outTraffic;
    return this;
  }
}
