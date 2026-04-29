package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;
import org.tron.core.exception.TronError;
import org.tron.core.services.ratelimiter.GlobalRateLimiter;
import org.tron.core.services.ratelimiter.RateLimiterContainer;
import org.tron.core.services.ratelimiter.RuntimeData;
import org.tron.core.services.ratelimiter.adapter.DefaultBaseQqsAdapter;
import org.tron.core.services.ratelimiter.adapter.GlobalPreemptibleAdapter;
import org.tron.core.services.ratelimiter.adapter.IPQPSRateLimiterAdapter;
import org.tron.core.services.ratelimiter.adapter.IPreemptibleRateLimiter;
import org.tron.core.services.ratelimiter.adapter.IRateLimiter;
import org.tron.core.services.ratelimiter.adapter.QpsRateLimiterAdapter;

/**
 * Verifies RateLimiterServlet's adapter resolution: strict whitelist
 * (no Class.forName arbitrary class loading), fail-fast on unknown or
 * empty names, and successful construction of every whitelisted adapter.
 *
 * <p>Also covers the rate-limiting logic in {@link RateLimiterServlet#service}:
 * <ol>
 *   <li>Per-endpoint check runs <em>before</em> the global check, so a per-endpoint
 *       rejection never consumes a global IP/QPS token.</li>
 *   <li>A {@link IPreemptibleRateLimiter} permit is always released — whether the
 *       global limiter rejects the request or the request handler completes normally.</li>
 * </ol>
 */
public class RateLimiterServletTest {

  private static final Map<String, Class<? extends IRateLimiter>> allowedAdapters =
      RateLimiterServlet.ALLOWED_ADAPTERS;

  private static final String KEY_HTTP = "http_";

  private TestServlet servlet;
  private RateLimiterContainer container;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  /** Minimal concrete subclass — only {@code doGet} is needed for the happy-path test. */
  static class TestServlet extends RateLimiterServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
      // intentional no-op
    }
  }

  /**
   * GlobalRateLimiter's static initializer calls Args.getInstance().getRateLimiterGlobalQps().
   * Without Args being initialized the default QPS is 0, causing RateLimiter.create(0) to throw.
   * Initializing Args here (before the class is first loaded inside each test method) prevents
   * the static initialization failure that would otherwise break mockStatic().
   */
  @Before
  public void setUp() throws Exception {
    Args.setParam(new String[0], TestConstants.TEST_CONF);
    servlet = new TestServlet();
    container = new RateLimiterContainer();
    Field f = RateLimiterServlet.class.getDeclaredField("container");
    f.setAccessible(true);
    f.set(servlet, container);

    request = new MockHttpServletRequest("GET", "/test");
    request.setRemoteAddr("10.0.0.1");
    response = new MockHttpServletResponse();
  }

  @AfterClass
  public static void tearDown() {
    Args.clearParam();
  }

  @Test
  public void testWhitelistContents() {
    assertEquals(GlobalPreemptibleAdapter.class,
        allowedAdapters.get(GlobalPreemptibleAdapter.class.getSimpleName()));
    assertEquals(QpsRateLimiterAdapter.class,
        allowedAdapters.get(QpsRateLimiterAdapter.class.getSimpleName()));
    assertEquals(IPQPSRateLimiterAdapter.class,
        allowedAdapters.get(IPQPSRateLimiterAdapter.class.getSimpleName()));
    assertEquals(DefaultBaseQqsAdapter.class,
        allowedAdapters.get(DefaultBaseQqsAdapter.class.getSimpleName()));
  }

  @Test
  public void testWhitelistRejectsUnknownAdapter() {
    assertNull(allowedAdapters.get("EvilAdapter"));
    assertNull(allowedAdapters.get("java.lang.Runtime"));
  }

  @Test
  public void testUnknownAdapterThrowsTronError() {
    // Fail-fast parity with the pre-whitelist Class.forName behavior: an unknown
    // adapter name raises TronError from @PostConstruct so Spring startup aborts
    // rather than silently masking a misconfigured node.
    TronError e = assertThrows(TronError.class,
        () -> RateLimiterServlet.buildAdapter("UnknownAdapter", "qps=100", "TestServlet"));
    assertEquals(TronError.ErrCode.RATE_LIMITER_INIT, e.getErrCode());
    assertTrue(e.getMessage().contains("UnknownAdapter"));
    assertTrue(e.getMessage().contains("TestServlet"));
  }

  @Test
  public void testDefaultAdapterNameBuildsDefaultBaseQqsAdapter() {
    // When no config entry exists for a servlet, addRateContainer passes
    // DEFAULT_ADAPTER_NAME to buildAdapter; verify it resolves to
    // DefaultBaseQqsAdapter.
    IRateLimiter limiter = RateLimiterServlet.buildAdapter(
        RateLimiterServlet.DEFAULT_ADAPTER_NAME, "qps=100", "TestServlet");
    assertNotNull(limiter);
    assertTrue(limiter instanceof DefaultBaseQqsAdapter);
  }

  @Test
  public void testEmptyAdapterNameThrowsTronError() {
    // Fail-fast parity with original: a configured-but-empty strategy name is
    // a configuration bug and must not be silently replaced by the default.
    TronError e = assertThrows(TronError.class,
        () -> RateLimiterServlet.buildAdapter("", "qps=100", "TestServlet"));
    assertEquals(TronError.ErrCode.RATE_LIMITER_INIT, e.getErrCode());
  }

  @Test
  public void testBuildsEachWhitelistedAdapter() {
    // Exercises the newInstance(String) constructor path for every whitelisted
    // adapter so a signature/strategy-class break on any entry fails here
    // instead of at node startup.
    assertTrue(RateLimiterServlet.buildAdapter(
        QpsRateLimiterAdapter.class.getSimpleName(), "qps=100", "TestServlet")
        instanceof QpsRateLimiterAdapter);
    assertTrue(RateLimiterServlet.buildAdapter(
        IPQPSRateLimiterAdapter.class.getSimpleName(), "qps=100", "TestServlet")
        instanceof IPQPSRateLimiterAdapter);
    assertTrue(RateLimiterServlet.buildAdapter(
        GlobalPreemptibleAdapter.class.getSimpleName(), "permit=1", "TestServlet")
        instanceof GlobalPreemptibleAdapter);
  }

  /**
   * Per-endpoint rejects → GlobalRateLimiter must NOT be invoked.
   * The global IP/QPS quota is fully preserved for other clients.
   */
  @Test
  public void testPerEndpointRejectedDoesNotConsumeGlobalQuota() throws Exception {
    IPreemptibleRateLimiter perEndpoint = Mockito.mock(IPreemptibleRateLimiter.class);
    when(perEndpoint.tryAcquire(any(RuntimeData.class))).thenReturn(false);
    container.add(KEY_HTTP, "TestServlet", perEndpoint);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      servlet.service(request, response);

      globalMock.verify(() -> GlobalRateLimiter.tryAcquire(any()), never());
      // tryAcquire returned false — no permit was taken, nothing to release
      verify(perEndpoint, never()).release();
    }
  }

  /**
   * Per-endpoint (QPS-only, non-preemptible) rejects → global not called,
   * and no release() attempt on a non-IPreemptibleRateLimiter.
   */
  @Test
  public void testNonPreemptiblePerEndpointRejectedDoesNotConsumeGlobal() throws Exception {
    IRateLimiter perEndpoint = Mockito.mock(IRateLimiter.class);
    when(perEndpoint.tryAcquire(any(RuntimeData.class))).thenReturn(false);
    container.add(KEY_HTTP, "TestServlet", perEndpoint);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      servlet.service(request, response);

      globalMock.verify(() -> GlobalRateLimiter.tryAcquire(any()), never());
    }
  }

  /**
   * Per-endpoint (IPreemptibleRateLimiter) acquires the permit, but the global limiter
   * then rejects. The finally block must release the permit to avoid a semaphore leak.
   */
  @Test
  public void testGlobalRejectedReleasesPreemptiblePermit() throws Exception {
    IPreemptibleRateLimiter perEndpoint = Mockito.mock(IPreemptibleRateLimiter.class);
    when(perEndpoint.tryAcquire(any(RuntimeData.class))).thenReturn(true);
    container.add(KEY_HTTP, "TestServlet", perEndpoint);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.tryAcquire(any())).thenReturn(false);

      servlet.service(request, response);

      // Permit was acquired but request blocked — must be returned
      verify(perEndpoint, times(1)).release();
    }
  }

  /**
   * Both limiters pass → request executes and the permit is released exactly once
   * in the finally block after the handler returns.
   */
  @Test
  public void testBothPassPermitReleasedAfterRequest() throws Exception {
    IPreemptibleRateLimiter perEndpoint = Mockito.mock(IPreemptibleRateLimiter.class);
    when(perEndpoint.tryAcquire(any(RuntimeData.class))).thenReturn(true);
    container.add(KEY_HTTP, "TestServlet", perEndpoint);

    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.tryAcquire(any())).thenReturn(true);

      servlet.service(request, response);

      verify(perEndpoint, times(1)).release();
    }
  }

  /**
   * No per-endpoint limiter configured (null) → only GlobalRateLimiter is consulted,
   * and nothing is released (no permit to hold).
   */
  @Test
  public void testNullRateLimiterConsultsOnlyGlobal() throws Exception {
    // No entry added to container — container.get() returns null
    try (MockedStatic<GlobalRateLimiter> globalMock = mockStatic(GlobalRateLimiter.class)) {
      globalMock.when(() -> GlobalRateLimiter.tryAcquire(any())).thenReturn(true);

      servlet.service(request, response);

      globalMock.verify(() -> GlobalRateLimiter.tryAcquire(any()), times(1));
    }
  }
}
