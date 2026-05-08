package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Test;
import org.tron.core.exception.TronError;
import org.tron.core.services.ratelimiter.adapter.DefaultBaseQqsAdapter;
import org.tron.core.services.ratelimiter.adapter.GlobalPreemptibleAdapter;
import org.tron.core.services.ratelimiter.adapter.IPQPSRateLimiterAdapter;
import org.tron.core.services.ratelimiter.adapter.IRateLimiter;
import org.tron.core.services.ratelimiter.adapter.QpsRateLimiterAdapter;

/**
 * Verifies RateLimiterServlet's adapter resolution: strict whitelist
 * (no Class.forName arbitrary class loading), fail-fast on unknown or
 * empty names, and successful construction of every whitelisted adapter.
 */
public class RateLimiterServletTest {

  private static final Map<String, Class<? extends IRateLimiter>> allowedAdapters =
      RateLimiterServlet.ALLOWED_ADAPTERS;

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
}
