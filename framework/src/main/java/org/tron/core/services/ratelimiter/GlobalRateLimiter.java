package org.tron.core.services.ratelimiter;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.config.args.Args;

@Slf4j
public class GlobalRateLimiter {

  private static double QPS = Args.getInstance().getRateLimiterGlobalQps();

  private static double IP_QPS = Args.getInstance().getRateLimiterGlobalIpQps();

  private static Cache<String, RateLimiter> cache = CacheBuilder.newBuilder()
      .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).build();

  private static RateLimiter rateLimiter = RateLimiter.create(QPS);

  public static boolean tryAcquire(RuntimeData runtimeData) {
    String ip = runtimeData.getRemoteAddr();
    if (!Strings.isNullOrEmpty(ip)) {
      RateLimiter r;
      try {
        // cache.get is atomic: only one loader executes per key under concurrent requests,
        // preventing multiple RateLimiter instances from being created for the same IP.
        r = cache.get(ip, () -> RateLimiter.create(IP_QPS));
      } catch (Exception e) {
        logger.warn("Failed to load IP rate limiter for {}, denying request: {}",
            ip, e.getMessage());
        return false;
      }
      if (!r.tryAcquire()) {
        return false;
      }
    }
    return rateLimiter.tryAcquire();
  }

}
