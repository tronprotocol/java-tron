package org.tron.core.metrics.net;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;

public class ApiInfo {
  private RateInfo totalCount;
  private RateInfo totalFailCount;
  private RateInfo totalOutTraffic;
  private List<ApiDetailInfo> detail = new ArrayList<>();

  public RateInfo getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(RateInfo totalCount) {
    this.totalCount = totalCount;
  }

  public RateInfo getTotalFailCount() {
    return totalFailCount;
  }

  public void setTotalFailCount(RateInfo totalFailCount) {
    this.totalFailCount = totalFailCount;
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
