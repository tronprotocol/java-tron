package org.tron.core.metrics.net;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;

public class ApiInfo {
  private Common totalCount;
  private Common totalFailCount;
  private Common totalOutTraffic;
  @JSONField(name = "detail")
  private List<ApiDetailInfo> detail = new ArrayList<>();

  public Common getTotalCount() {
    return this.totalCount;
  }

  public ApiInfo setTotalCount(Common totalCount) {
    this.totalCount = totalCount;
    return this;
  }

  public Common getTotalFailCount() {
    return this.totalFailCount;
  }

  public ApiInfo setTotalFailCount(Common totalFailCount) {
    this.totalFailCount = totalFailCount;
    return this;
  }

  public Common getTotalOutTraffic() {
    return this.totalOutTraffic;
  }

  public ApiInfo setTotalOutTraffic(Common totaloutTraffic) {
    this.totalOutTraffic = totaloutTraffic;
    return this;
  }

  @JSONField(name = "detail")
  public List<ApiDetailInfo> getApiDetailInfo() {
    return this.detail;
  }

  public ApiInfo setApiDetailInfo(List<ApiDetailInfo> detail) {
    this.detail = detail;
    return this;
  }
}
