package org.tron.core.metrics.net;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class LatencyInfo {
  private int top99;
  private int top95;
  private int top75;
  private int totalCount;
  private int delay1S;
  private int delay2S;
  private int delay3S;
  private List<LatencyDetailInfo> detail = new ArrayList<>();
}
