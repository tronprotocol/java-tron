package org.tron.core.services.ratelimiter.strategy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class IPQpsStrategy extends Strategy {

  public static final String STRATEGY_PARAM_IPQPS = "qps";
  public static final Double DEFAULT_IPQPS = 2D;

  private Cache<String, RateLimiter> ipLimiter = CacheBuilder.newBuilder().maximumSize(10000)
      .expireAfterWrite(600, TimeUnit.SECONDS).recordStats().build();

  public IPQpsStrategy(String paramString) {
    super(paramString);
  }

  public boolean acquire(String ip) {
    RateLimiter limiter = ipLimiter.getIfPresent(ip);
    if (limiter == null) {
      limiter = newRateLimiter();
      ipLimiter.put(ip, limiter);
    }
    limiter.acquire();
    return true;
  }

  private RateLimiter newRateLimiter() {
    return RateLimiter.create((Double) mapParams.get(STRATEGY_PARAM_IPQPS).value);
  }

  // define the default strategy params.
  @Override
  protected Map<String, ParamItem> defaultParam() {
    Map<String, ParamItem> map = new HashMap<>();
    map.put(STRATEGY_PARAM_IPQPS, new ParamItem(Double.class, DEFAULT_IPQPS));
    return map;
  }
}