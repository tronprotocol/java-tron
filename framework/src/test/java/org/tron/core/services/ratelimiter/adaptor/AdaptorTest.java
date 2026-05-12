package org.tron.core.services.ratelimiter.adaptor;

import com.google.common.cache.Cache;
import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.TestConstants;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.config.args.Args;
import org.tron.core.services.ratelimiter.RuntimeData;
import org.tron.core.services.ratelimiter.adapter.GlobalPreemptibleAdapter;
import org.tron.core.services.ratelimiter.adapter.IPQPSRateLimiterAdapter;
import org.tron.core.services.ratelimiter.adapter.IRateLimiter;
import org.tron.core.services.ratelimiter.adapter.QpsRateLimiterAdapter;
import org.tron.core.services.ratelimiter.strategy.GlobalPreemptibleStrategy;
import org.tron.core.services.ratelimiter.strategy.IPQpsStrategy;
import org.tron.core.services.ratelimiter.strategy.QpsStrategy;

public class AdaptorTest {

  @Before
  public void setUp() {
    Args.setParam(new String[0], TestConstants.TEST_CONF);
  }

  @AfterClass
  public static void tearDown() {
    Args.clearParam();
  }

  /**
   * IRateLimiter.acquirePermit is a default method that dispatches based on
   * rate.limiter.apiNonBlocking. The two cases below pin that contract: with
   * the switch on, only tryAcquire is invoked; with the switch off, only
   * acquire is invoked. Breaking either direction is a behavioural regression.
   */
  @Test
  public void testAcquirePermitDispatchesToTryAcquireWhenNonBlocking() {
    Args.getInstance().setRateLimiterApiNonBlocking(true);
    CountingRateLimiter limiter = new CountingRateLimiter();

    Assert.assertTrue(limiter.acquirePermit(null));

    Assert.assertEquals(1, limiter.tryAcquireCount);
    Assert.assertEquals(0, limiter.acquireCount);
  }

  @Test
  public void testAcquirePermitDispatchesToAcquireWhenBlocking() {
    Args.getInstance().setRateLimiterApiNonBlocking(false);
    CountingRateLimiter limiter = new CountingRateLimiter();

    Assert.assertTrue(limiter.acquirePermit(null));

    Assert.assertEquals(0, limiter.tryAcquireCount);
    Assert.assertEquals(1, limiter.acquireCount);
  }

  private static final class CountingRateLimiter implements IRateLimiter {
    int tryAcquireCount;
    int acquireCount;

    @Override
    public boolean tryAcquire(RuntimeData data) {
      tryAcquireCount++;
      return true;
    }

    @Override
    public boolean acquire(RuntimeData data) {
      acquireCount++;
      return true;
    }
  }

  @Test
  public void testStrategy() {
    String paramString1 = "qps=5 notExist=6";
    IPQPSRateLimiterAdapter adapter1 = new IPQPSRateLimiterAdapter(paramString1);
    IPQpsStrategy strategy1 = (IPQpsStrategy) ReflectUtils.getFieldObject(adapter1,
        "strategy");

    Assert.assertEquals(5.0d, Double.parseDouble(
        ReflectUtils.getFieldValue(strategy1.getMapParams().get("qps"),
            "value").toString()), 0.0);
    Assert.assertNull(strategy1.getMapParams().get("notExist"));

    String paramString2 = "qps=5xyz";
    IPQPSRateLimiterAdapter adapter2 = new IPQPSRateLimiterAdapter(paramString2);
    IPQpsStrategy strategy2 = (IPQpsStrategy) ReflectUtils.getFieldObject(adapter2,
        "strategy");

    Assert.assertEquals(IPQpsStrategy.DEFAULT_IPQPS, Double.valueOf(
        ReflectUtils.getFieldValue(strategy2.getMapParams().get("qps"),
            "value").toString()));
  }

  @Test
  public void testIPQPSRateLimiterAdapter() {
    String paramString = "qps=1";
    IPQPSRateLimiterAdapter adapter = new IPQPSRateLimiterAdapter(paramString);

    IPQpsStrategy strategy = (IPQpsStrategy) ReflectUtils.getFieldObject(adapter,
        "strategy");
    Assert.assertEquals(1.0d, Double
        .parseDouble(ReflectUtils.getFieldValue(strategy.getMapParams().get("qps"),
            "value").toString()), 0.0);

    boolean flag = strategy.tryAcquire("1.2.3.4");
    Assert.assertTrue(flag);

    flag = strategy.tryAcquire("1.2.3.4");
    Assert.assertFalse(flag);

    flag = strategy.tryAcquire("1.2.3.5");
    Assert.assertTrue(flag);

    Cache<String, RateLimiter> ipLimiter = (Cache<String, RateLimiter>) ReflectUtils
        .getFieldObject(strategy, "ipLimiter");
    Assert.assertEquals(2, ipLimiter.size());
  }

  @Test
  public void testGlobalPreemptibleAdapter() {
    String paramString1 = "permit=1";
    GlobalPreemptibleAdapter adapter1 = new GlobalPreemptibleAdapter(paramString1);
    GlobalPreemptibleStrategy strategy1 = (GlobalPreemptibleStrategy) ReflectUtils
        .getFieldObject(adapter1, "strategy");
    Assert.assertEquals(1, Integer.parseInt(
        ReflectUtils.getFieldValue(strategy1.getMapParams().get("permit"),
            "value").toString()));
    boolean first = strategy1.tryAcquire();
    Assert.assertTrue(first);

    boolean second = strategy1.tryAcquire();
    Assert.assertFalse(second);

    strategy1.release();
    boolean secondAfterOneRelease = strategy1.tryAcquire();
    Assert.assertTrue(secondAfterOneRelease);

    String paramString2 = "permit=3";
    GlobalPreemptibleAdapter adapter2 = new GlobalPreemptibleAdapter(paramString2);
    GlobalPreemptibleStrategy strategy2 = (GlobalPreemptibleStrategy) ReflectUtils
        .getFieldObject(adapter2, "strategy");
    Assert.assertEquals(3, Integer.parseInt(
        ReflectUtils.getFieldValue(strategy2.getMapParams().get("permit"),
            "value").toString()));

    first = strategy2.tryAcquire();
    Assert.assertTrue(first);
    second = strategy2.tryAcquire();
    Assert.assertTrue(second);
    boolean third = strategy2.tryAcquire();
    Assert.assertTrue(third);

    boolean four = strategy2.tryAcquire();
    Assert.assertFalse(four);

    strategy2.release();
    boolean fourAfterOneRelease = strategy2.tryAcquire();
    Assert.assertTrue(fourAfterOneRelease);

    Semaphore sp = (Semaphore) ReflectUtils.getFieldObject(strategy2, "sp");
    Assert.assertEquals(0, sp.availablePermits());
    strategy2.release();
    strategy2.release();
    strategy2.release();
    Assert.assertEquals(3, sp.availablePermits());
  }

  @Test
  public void testQpsRateLimiterAdapter() throws Exception {
    String paramString = "qps=1";
    QpsRateLimiterAdapter adapter = new QpsRateLimiterAdapter(paramString);

    QpsStrategy strategy = (QpsStrategy) ReflectUtils.getFieldObject(adapter, "strategy");
    Assert.assertEquals(1, Double
        .parseDouble(ReflectUtils.getFieldValue(strategy.getMapParams().get("qps"),
            "value").toString()), 0.0);

    Thread.sleep(1000);

    boolean flag = strategy.tryAcquire();
    Assert.assertTrue(flag);

    // Guava SmoothBursty "pre-bills" the next slot when stored permits are
    // consumed without cost: nextFreeTicketMicros stays at the resync time,
    // so the immediately following call still passes (waitLength = 0) while
    // advancing the ticket to 1 s in the future.
    flag = strategy.tryAcquire();
    Assert.assertTrue(flag);

    flag = strategy.tryAcquire();
    Assert.assertFalse(flag);
  }
}


