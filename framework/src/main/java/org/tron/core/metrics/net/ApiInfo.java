package org.tron.core.metrics.net;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;

public class ApiInfo {
  private RateInfo qps;
  private RateInfo failQps;
  private RateInfo totalOutTraffic;
  private List<ApiDetailInfo> detail = new ArrayList<>();

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

  public RateInfo getTotalOutTraffic() {
    return totalOutTraffic;
  }

  public void setTotalOutTraffic(RateInfo totalOutTraffic) {
    this.totalOutTraffic = totalOutTraffic;
  }

  public List<ApiDetailInfo> getDetail() {
    return detail;
  }

  public void setDetail(List<ApiDetailInfo> detail) {
    this.detail = detail;
  }
}
