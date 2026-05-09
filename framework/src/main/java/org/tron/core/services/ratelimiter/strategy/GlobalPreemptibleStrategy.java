package org.tron.core.services.ratelimiter.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class GlobalPreemptibleStrategy extends Strategy {

  public static final String STRATEGY_PARAM_PERMIT = "permit";
  public static final int DEFAULT_PERMIT_NUM = 1;
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

  // Non-blocking: immediately rejects if no permit is available.
  // Intentional change from the previous tryAcquire(2, TimeUnit.SECONDS) behaviour:
  // blocking the caller for up to 2 s ties up Netty IO / gRPC executor threads and
  // masks overload rather than shedding it. All rate-limiting in this stack is now
  // non-blocking to keep the thread model consistent with GlobalRateLimiter.
  public boolean tryAcquire() {
    return sp.tryAcquire();
  }

  public void release() {
    sp.release();
  }

}