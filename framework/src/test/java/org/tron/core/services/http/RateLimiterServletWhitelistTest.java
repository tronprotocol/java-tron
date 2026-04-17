package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Test;
import org.tron.core.services.ratelimiter.adapter.DefaultBaseQqsAdapter;
import org.tron.core.services.ratelimiter.adapter.GlobalPreemptibleAdapter;
import org.tron.core.services.ratelimiter.adapter.IPQPSRateLimiterAdapter;
import org.tron.core.services.ratelimiter.adapter.IRateLimiter;
import org.tron.core.services.ratelimiter.adapter.QpsRateLimiterAdapter;

/**
 * Verifies that RateLimiterServlet uses a strict whitelist
 * instead of Class.forName(), preventing arbitrary class loading
 * via a tampered config file.
 */
public class RateLimiterServletWhitelistTest {

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
  public void testUnknownAdapterFallsBackToDefault() throws Exception {
    IRateLimiter limiter = RateLimiterServlet.buildAdapter(
        "UnknownAdapter", "qps=100", "TestServlet");
    assertNotNull(limiter);
    assertTrue(limiter instanceof DefaultBaseQqsAdapter);
  }

  @Test
  public void testEmptyStrategyResolvesToDefaultAdapter() throws Exception {
    // When strategy is empty in config, addRateContainer resolves to DEFAULT_ADAPTER_NAME.
    // Verify buildAdapter creates a DefaultBaseQqsAdapter for that resolved name.
    IRateLimiter limiter = RateLimiterServlet.buildAdapter(
        RateLimiterServlet.DEFAULT_ADAPTER_NAME, "qps=100", "TestServlet");
    assertNotNull(limiter);
    assertTrue(limiter instanceof DefaultBaseQqsAdapter);
  }
}
