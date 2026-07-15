package org.tron.core.services.ratelimiter.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GlobalPreemptibleStrategy extends Strategy {

  public static final String STRATEGY_PARAM_PERMIT = "permit";
  public static final int DEFAULT_PERMIT_NUM = 1;
  public static final int DEFAULT_ACQUIRE_TIMEOUT = 2;
  private Semaphore sp;

  public GlobalPreemptibleStrategy(String paramString) {
    super(paramString);
    sp = new Semaphore((Integer) mapParams.get(STRATEGY_PARAM_PERMIT).value);
  }

  // define the default strategy params.
  @Override
  protected Map<String, ParamItem> defaultParam() {
    Map<String, ParamItem> map = new HashMap<>();
    map.put(STRATEGY_PARAM_PERMIT, new ParamItem(Integer.class, DEFAULT_PERMIT_NUM));
    return map;
  }

  // Non-blocking: immediately rejects if no permit is available. Used when the
  // apiNonBlocking switch is on, to shed overload instead of tying up Netty IO /
  // gRPC executor threads while waiting for a permit.
  public boolean tryAcquire() {
    return sp.tryAcquire();
  }

  public boolean acquire() {
    try {
      return sp.tryAcquire(DEFAULT_ACQUIRE_TIMEOUT, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      // Restore the interrupt flag and reject — caller must not release a permit
      // it never acquired.
      logger.error("acquire permit with error: {}", e.getMessage());
      Thread.currentThread().interrupt();
      return false;
    }
  }

  public void release() {
    sp.release();
  }

}