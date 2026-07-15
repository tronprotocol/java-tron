package org.tron.core.services.ratelimiter.strategy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IPQpsStrategy extends Strategy {

  public static final String STRATEGY_PARAM_IPQPS = "qps";
  public static final Double DEFAULT_IPQPS = 2D;

  private Cache<String, RateLimiter> ipLimiter = CacheBuilder.newBuilder().maximumSize(10000)
      .expireAfterWrite(600, TimeUnit.SECONDS).recordStats().build();

  public IPQpsStrategy(String paramString) {
    super(paramString);
  }

  public boolean tryAcquire(String ip) {
    RateLimiter limiter = loadLimiter(ip);
    return limiter != null && limiter.tryAcquire();
  }

  public boolean acquire(String ip) {
    RateLimiter limiter = loadLimiter(ip);
    if (limiter == null) {
      return false;
    }
    limiter.acquire();
    return true;
  }

  private RateLimiter loadLimiter(String ip) {
    try {
      // cache.get is atomic: only one loader executes per key under concurrent requests,
      // preventing multiple RateLimiter instances from being created for the same IP.
      return ipLimiter.get(ip, this::newRateLimiter);
    } catch (Exception e) {
      logger.warn("Failed to load IP rate limiter for {}, denying request: {}",
          ip, e.getMessage());
      return null;
    }
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