package org.tron.core.net;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;

public class P2pRateLimiter {
  private final Cache<Byte, RateLimiter> rateLimiters = CacheBuilder.newBuilder()
      .maximumSize(256).build();

  public void register (Byte type, double rate) {
    rateLimiters.put(type, RateLimiter.create(rate));
  }

  public void acquire (Byte type) {
    RateLimiter rateLimiter = rateLimiters.getIfPresent(type);
    if (rateLimiter == null) {
      return;
    }
    rateLimiter.acquire();
  }

  public boolean tryAcquire (Byte type) {
    RateLimiter rateLimiter = rateLimiters.getIfPresent(type);
    if (rateLimiter == null) {
      return true;
    }
    return rateLimiter.tryAcquire();
  }
}
