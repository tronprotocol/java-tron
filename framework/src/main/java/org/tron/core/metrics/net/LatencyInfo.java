package org.tron.core.metrics.net;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;

public class LatencyInfo {
  private int top99;
  private int top95;
  private int totalCount;
  private int delay1S;
  private int delay2S;
  private int delay3S;
  @JSONField(name = "detail")
  private List<LatencyDetailInfo> detail = new ArrayList<>();

  public int getTop99() {
    return this.top99;
  }

  public LatencyInfo setTop99(int top99) {
    this.top99 = top99;
    return this;
  }

  public int getTop95() {
    return this.top95;
  }

  public LatencyInfo setTop95(int top95) {
    this.top95 = top95;
    return this;
  }

  public int getTotalCount() {
    return this.totalCount;
  }

  public LatencyInfo setTotalCount(int totalCount) {
    this.totalCount = totalCount;
    return this;
  }

  public int getDelay1S() {
    return this.delay1S;
  }

  public LatencyInfo setDelay1S(int delay1S) {
    this.delay1S = delay1S;
    return this;
  }

  public int getDelay2S() {
    return this.delay2S;
  }

  public LatencyInfo setDelay2S(int delay2S) {
    this.delay2S = delay2S;
    return this;
  }

  public int getDelay3S() {
    return this.delay3S;
  }

  public LatencyInfo setDelay3S(int delay3S) {
    this.delay3S = delay3S;
    return this;
  }

  @JSONField(name = "detail")
  public List<LatencyDetailInfo> getLatencyDetailInfo() {
    return this.detail;
  }

  public LatencyInfo setLatencyDetailInfo(List<LatencyDetailInfo> detail) {
    this.detail = detail;
    return this;
  }
}
