package org.tron.core.services.ratelimiter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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

  /**
   * acquire() must drain the IP limiter before the global limiter, mirroring
   * tryAcquire(). A reversed order would let one chatty IP consume global
   * quota even when its own per-IP budget is exhausted.
   */
  @Test
  public void testAcquireOrdersIpBeforeGlobal() throws Exception {
    RateLimiter globalMock = Mockito.mock(RateLimiter.class);
    RateLimiter ipMock = Mockito.mock(RateLimiter.class);
    injectRateLimiter(globalMock);
    Cache<String, RateLimiter> seeded = CacheBuilder.newBuilder()
        .maximumSize(10).expireAfterWrite(1, TimeUnit.HOURS).build();
    seeded.put("10.0.0.1", ipMock);
    injectCache(seeded);

    Assert.assertTrue(GlobalRateLimiter.acquire(runtimeDataFor("10.0.0.1")));

    InOrder inOrder = Mockito.inOrder(ipMock, globalMock);
    inOrder.verify(ipMock).acquire();
    inOrder.verify(globalMock).acquire();
  }

  /**
   * If the IP limiter cannot be created (cache loader throws), acquire()
   * returns false without consuming a global token — same fail-closed
   * behaviour as tryAcquire().
   */
  @Test
  public void testAcquireDoesNotConsumeGlobalWhenIpLoaderFails() throws Exception {
    RateLimiter globalMock = Mockito.mock(RateLimiter.class);
    injectRateLimiter(globalMock);
    // RateLimiter.create(-1.0) throws IllegalArgumentException, so the
    // cache loader fails and loadIpLimiter() returns null.
    injectIpQps(-1.0);
    injectCache(CacheBuilder.newBuilder()
        .maximumSize(10).expireAfterWrite(1, TimeUnit.HOURS).build());

    Assert.assertFalse(GlobalRateLimiter.acquire(runtimeDataFor("10.0.0.1")));

    Mockito.verify(globalMock, never()).acquire();
  }

  /**
   * acquirePermit dispatches based on rate.limiter.apiNonBlocking:
   * switch on → only tryAcquire runs; switch off → only acquire runs.
   * These tests pin that contract on the static dispatcher; the matching
   * default-method contract for IRateLimiter is covered in AdaptorTest.
   */
  @Test
  public void testAcquirePermitDispatchesToTryAcquireWhenNonBlocking() throws Exception {
    Args.getInstance().setRateLimiterApiNonBlocking(true);
    RuntimeData rd = runtimeDataFor("10.0.0.1");

    try (MockedStatic<GlobalRateLimiter> mock = mockStatic(GlobalRateLimiter.class)) {
      mock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenCallRealMethod();
      mock.when(() -> GlobalRateLimiter.tryAcquire(any())).thenReturn(true);

      Assert.assertTrue(GlobalRateLimiter.acquirePermit(rd));

      mock.verify(() -> GlobalRateLimiter.tryAcquire(any()), times(1));
      mock.verify(() -> GlobalRateLimiter.acquire(any()), never());
    }
  }

  @Test
  public void testAcquirePermitDispatchesToAcquireWhenBlocking() throws Exception {
    Args.getInstance().setRateLimiterApiNonBlocking(false);
    RuntimeData rd = runtimeDataFor("10.0.0.1");

    try (MockedStatic<GlobalRateLimiter> mock = mockStatic(GlobalRateLimiter.class)) {
      mock.when(() -> GlobalRateLimiter.acquirePermit(any())).thenCallRealMethod();
      mock.when(() -> GlobalRateLimiter.acquire(any())).thenReturn(true);

      Assert.assertTrue(GlobalRateLimiter.acquirePermit(rd));

      mock.verify(() -> GlobalRateLimiter.acquire(any()), times(1));
      mock.verify(() -> GlobalRateLimiter.tryAcquire(any()), never());
    }
  }

  private static void injectRateLimiter(RateLimiter rl) throws Exception {
    Field f = GlobalRateLimiter.class.getDeclaredField("rateLimiter");
    f.setAccessible(true);
    f.set(null, rl);
  }

  private static void injectCache(Cache<String, RateLimiter> cache) throws Exception {
    Field f = GlobalRateLimiter.class.getDeclaredField("cache");
    f.setAccessible(true);
    f.set(null, cache);
  }

  private static void injectIpQps(double qps) throws Exception {
    Field f = GlobalRateLimiter.class.getDeclaredField("IP_QPS");
    f.setAccessible(true);
    f.set(null, qps);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }
}
