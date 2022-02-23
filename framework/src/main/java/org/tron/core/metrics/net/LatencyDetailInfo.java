package org.tron.core.metrics.net;

import lombok.Data;

@Data
public class LatencyDetailInfo {
  private String witness;
  private int top99;
  private int top95;
  private int top75;
  private int count;
  private int delay1S;
  private int delay2S;
  private int delay3S;
}
