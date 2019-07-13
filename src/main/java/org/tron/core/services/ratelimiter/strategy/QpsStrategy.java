package org.tron.core.services.ratelimiter.strategy;


import com.google.common.util.concurrent.RateLimiter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QpsStrategy extends Strategy {

  private RateLimiter rateLimiter;

  public static final String STRATEGY_PARAM_QPS = "qps";
  public static final int DEFAULT_QPS = 100;

  // define the default strategy params
  @Override
  protected Map<String, ParamItem> defaultParam() {
    Map<String, ParamItem> map = new HashMap<>();
    map.put(STRATEGY_PARAM_QPS, new ParamItem(Integer.class, DEFAULT_QPS));
    return map;
  }

  public QpsStrategy(String paramString) {
    super(paramString);
    rateLimiter = RateLimiter.create((Integer) mapParams.get(STRATEGY_PARAM_QPS).value);
  }

  public boolean acquire() {
    rateLimiter.acquire();
    return true;
  }
}