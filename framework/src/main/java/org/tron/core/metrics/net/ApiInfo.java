package org.tron.core.metrics.net;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;

public class ApiInfo {
  private int totalCount;
  private int totalFailCount;
  private RateInfo totalOutTraffic;
  private List<ApiDetailInfo> detail = new ArrayList<>();

  public int getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }

  public int getTotalFailCount() {
    return totalFailCount;
  }

  public void setTotalFailCount(int totalFailCount) {
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
