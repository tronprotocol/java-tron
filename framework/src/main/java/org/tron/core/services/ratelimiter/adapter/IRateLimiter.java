package org.tron.core.services.ratelimiter.adapter;

import org.tron.core.config.args.Args;
import org.tron.core.services.ratelimiter.RuntimeData;

public interface IRateLimiter {

  boolean tryAcquire(RuntimeData data);

  boolean acquire(RuntimeData data);

  default boolean acquirePermit(RuntimeData data) {
    return Args.getInstance().isRateLimiterApiNonBlocking()
        ? tryAcquire(data)
        : acquire(data);
  }
}
