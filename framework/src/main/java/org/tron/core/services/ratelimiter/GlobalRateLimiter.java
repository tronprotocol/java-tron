package org.tron.core.services.ratelimiter;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.TimeUnit;
import org.tron.core.config.args.Args;

public class GlobalRateLimiter {

  private static double QPS = Args.getInstance().getRateLimiterGlobalQps();

  private static double IP_QPS = Args.getInstance().getRateLimiterGlobalIpQps();

  private static Cache<String, RateLimiter> cache = CacheBuilder.newBuilder()
      .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).build();

  private static RateLimiter rateLimiter = RateLimiter.create(QPS);

  public static void acquire(RuntimeData runtimeData) {
    rateLimiter.acquire();
    String ip = runtimeData.getRemoteAddr();
    if (Strings.isNullOrEmpty(ip)) {
      return;
    }
    RateLimiter r = cache.getIfPresent(ip);
    if (r == null) {
      r = RateLimiter.create(IP_QPS);
      cache.put(ip, r);
    }
    r.acquire();
  }

}
