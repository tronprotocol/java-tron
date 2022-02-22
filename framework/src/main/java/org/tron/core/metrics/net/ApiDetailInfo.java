package org.tron.core.metrics.net;

import lombok.Data;

@Data
public class ApiDetailInfo {
  private String name;
  private RateInfo qps;
  private RateInfo failQps;
  private RateInfo outTraffic;
}
