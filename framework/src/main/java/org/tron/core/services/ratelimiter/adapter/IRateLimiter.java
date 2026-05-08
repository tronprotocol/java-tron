package org.tron.core.services.ratelimiter.adapter;

import org.tron.core.services.ratelimiter.RuntimeData;

public interface IRateLimiter {

  boolean tryAcquire(RuntimeData data);

}
