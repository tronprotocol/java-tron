package org.tron.core.metrics.net;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ApiInfo {
  private RateInfo qps;
  private RateInfo failQps;
  private RateInfo outTraffic;
  private List<ApiDetailInfo> detail = new ArrayList<>();
}
