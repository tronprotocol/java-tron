package org.tron.core.services.http;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.BeforeClass;
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

  private static final String GLOBAL_PREEMPTIBLE  = GlobalPreemptibleAdapter.class.getSimpleName();
  private static final String QPS_RATE_LIMITER    = QpsRateLimiterAdapter.class.getSimpleName();
  private static final String IP_QPS_RATE_LIMITER = IPQPSRateLimiterAdapter.class.getSimpleName();
  private static final String DEFAULT_BASE_QPS    = DefaultBaseQqsAdapter.class.getSimpleName();

  private static Map<String, Class<? extends IRateLimiter>> allowedAdapters;

  @SuppressWarnings("unchecked")
  @BeforeClass
  public static void loadWhitelist() throws Exception {
    Field f = RateLimiterServlet.class.getDeclaredField("ALLOWED_ADAPTERS");
    f.setAccessible(true);
    allowedAdapters = (Map<String, Class<? extends IRateLimiter>>) f.get(null);
  }

  @Test
  public void testWhitelistContents() {
    assertNotNull(allowedAdapters.get(GLOBAL_PREEMPTIBLE));
    assertNotNull(allowedAdapters.get(QPS_RATE_LIMITER));
    assertNotNull(allowedAdapters.get(IP_QPS_RATE_LIMITER));
    assertNotNull(allowedAdapters.get(DEFAULT_BASE_QPS));

    assertTrue(GlobalPreemptibleAdapter.class
        .isAssignableFrom(allowedAdapters.get(GLOBAL_PREEMPTIBLE)));
    assertTrue(QpsRateLimiterAdapter.class
        .isAssignableFrom(allowedAdapters.get(QPS_RATE_LIMITER)));
    assertTrue(IPQPSRateLimiterAdapter.class
        .isAssignableFrom(allowedAdapters.get(IP_QPS_RATE_LIMITER)));
    assertTrue(DefaultBaseQqsAdapter.class
        .isAssignableFrom(allowedAdapters.get(DEFAULT_BASE_QPS)));
  }

  @Test
  public void testWhitelistRejectsUnknownAdapter() {
    assertNull(allowedAdapters.get("EvilAdapter"));
    assertNull(allowedAdapters.get("java.lang.Runtime"));
  }

  @Test
  public void testWhitelistIsUnmodifiable() {
    try {
      allowedAdapters.put("EvilAdapter", DefaultBaseQqsAdapter.class);
      assertFalse("Whitelist should be unmodifiable", true);
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  @Test
  public void testWhitelistSize() {
    assertTrue("Whitelist should contain exactly 4 adapters",
        allowedAdapters.size() == 4);
  }
}
