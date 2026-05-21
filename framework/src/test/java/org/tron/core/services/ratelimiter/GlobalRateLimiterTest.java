package org.tron.core.services.ratelimiter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;

public class GlobalRateLimiterTest {

  /**
   * Reset GlobalRateLimiter's static state to known rates before each test.
   * Static fields are initialized at class-load time from Args, so we must
   * override them via reflection to guarantee test isolation.
   */
  @Before
  public void setUp() throws Exception {
    String[] a = new String[0];
    Args.setParam(a, TestConstants.TEST_CONF);
    resetGlobalRateLimiter(2.0, 1.0);
  }

  private static void resetGlobalRateLimiter(double globalQps, double ipQps) throws Exception {
    // Reset per-IP QPS value
    Field ipQpsField = GlobalRateLimiter.class.getDeclaredField("IP_QPS");
    ipQpsField.setAccessible(true);
    ipQpsField.set(null, ipQps);

    // Create a fresh rate limiter, then sleep one stable interval (1000/qps ms) so
    // Guava's SmoothBursty accumulates exactly 1 stored permit.  With 1 stored permit
    // the first tryAcquire() consumes it (no advance of nextFreeTicket), and the second
    // call pre-bills the next slot and still returns true — giving exactly floor(qps)=2
    // consecutive successes without touching Guava-internal fields.
    RateLimiter rl = RateLimiter.create(globalQps);
    Thread.sleep((long) (1000.0 / globalQps));

    Field rateLimiterField = GlobalRateLimiter.class.getDeclaredField("rateLimiter");
    rateLimiterField.setAccessible(true);
    rateLimiterField.set(null, rl);

    // Clear the per-IP cache so each test starts fresh
    Field cacheField = GlobalRateLimiter.class.getDeclaredField("cache");
    cacheField.setAccessible(true);
    Cache<String, RateLimiter> freshCache = CacheBuilder.newBuilder()
        .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).build();
    cacheField.set(null, freshCache);
  }

  private static RuntimeData runtimeDataFor(String ip) throws Exception {
    RuntimeData runtimeData = new RuntimeData(null);
    Field field = runtimeData.getClass().getDeclaredField("address");
    field.setAccessible(true);
    field.set(runtimeData, ip == null ? "" : ip);
    return runtimeData;
  }

  /**
   * Normal request: passes both IP and global limits.
   */
  @Test
  public void testNormalRequestPasses() throws Exception {
    RuntimeData runtimeData = runtimeDataFor("10.0.0.1");
    Assert.assertTrue(GlobalRateLimiter.tryAcquire(runtimeData));
  }

  /**
   * IP limit exhausted: second request from same IP is rejected without
   * consuming a global token. A third request from a different IP must still
   * pass because the global budget was not wasted.
   * globalQps=2, ipQps=1
   */
  @Test
  public void testIpLimitDoesNotWasteGlobalToken() throws Exception {
    RuntimeData ip1 = runtimeDataFor("10.0.0.1");
    RuntimeData ip2 = runtimeDataFor("10.0.0.2");

    // First request from 10.0.0.1: IP passes (1/1), global passes (1/2)
    Assert.assertTrue(GlobalRateLimiter.tryAcquire(ip1));

    // Second request from 10.0.0.1: IP exhausted → rejected, global NOT consumed
    Assert.assertFalse(GlobalRateLimiter.tryAcquire(ip1));

    // First request from 10.0.0.2: IP passes (1/1), global passes (2/2)
    Assert.assertTrue(GlobalRateLimiter.tryAcquire(ip2));

    // Any further request: global exhausted
    Assert.assertFalse(GlobalRateLimiter.tryAcquire(runtimeDataFor("10.0.0.3")));
  }

  /**
   * Multiple IPs each consume one global token and then hit their own IP limit.
   * globalQps=2, ipQps=1: exactly 2 distinct IPs can succeed.
   */
  @Test
  public void testGlobalCapAcrossMultipleIps() throws Exception {
    Assert.assertTrue(GlobalRateLimiter.tryAcquire(runtimeDataFor("1.1.1.1")));
    Assert.assertTrue(GlobalRateLimiter.tryAcquire(runtimeDataFor("1.1.1.2")));

    // Global budget exhausted; a fresh IP is also rejected
    Assert.assertFalse(GlobalRateLimiter.tryAcquire(runtimeDataFor("1.1.1.3")));
  }

  /**
   * Request with no IP address bypasses the IP-level check and goes straight
   * to the global limiter.
   * globalQps=2: two no-IP requests succeed, third fails.
   */
  @Test
  public void testNoIpAddressFallsBackToGlobalOnly() throws Exception {
    RuntimeData noIp = runtimeDataFor("");

    Assert.assertTrue(GlobalRateLimiter.tryAcquire(noIp));
    Assert.assertTrue(GlobalRateLimiter.tryAcquire(noIp));
    Assert.assertFalse(GlobalRateLimiter.tryAcquire(noIp));
  }

  /**
   * Per-IP limit is independent between different IPs.
   * globalQps=10 (high), ipQps=1: each IP gets exactly one successful request.
   */
  @Test
  public void testPerIpLimitsAreIndependent() throws Exception {
    resetGlobalRateLimiter(10.0, 1.0);

    Assert.assertTrue(GlobalRateLimiter.tryAcquire(runtimeDataFor("2.2.2.1")));
    Assert.assertFalse(GlobalRateLimiter.tryAcquire(runtimeDataFor("2.2.2.1")));

    Assert.assertTrue(GlobalRateLimiter.tryAcquire(runtimeDataFor("2.2.2.2")));
    Assert.assertFalse(GlobalRateLimiter.tryAcquire(runtimeDataFor("2.2.2.2")));
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }
}
