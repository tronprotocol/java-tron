package org.tron.core.services.ratelimiter.strategy;

import com.google.common.util.concurrent.RateLimiter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QpsStrategy extends Strategy {

  public static final String STRATEGY_PARAM_QPS = "qps";
  public static final Double DEFAULT_QPS = 100D;
  private RateLimiter rateLimiter;

  public QpsStrategy(String paramString) {
    super(paramString);
    rateLimiter = RateLimiter.create((Double) mapParams.get(STRATEGY_PARAM_QPS).value);
  }

  // define the default strategy params
  @Override
  protected Map<String, ParamItem> defaultParam() {
    Map<String, ParamItem> map = new HashMap<>();
    map.put(STRATEGY_PARAM_QPS, new ParamItem(Double.class, DEFAULT_QPS));
    return map;
  }

  public boolean acquire() {
    rateLimiter.acquire();
    return true;
  }
}