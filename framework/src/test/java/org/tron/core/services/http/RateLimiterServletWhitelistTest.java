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
 * Security test: verifies that RateLimiterServlet uses a strict whitelist
 * instead of Class.forName(), preventing arbitrary class loading (RCE)
 * via a tampered config file.
 */
public class RateLimiterServletWhitelistTest {

  // Derive names from the classes themselves — stays in sync if classes are renamed.
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

  // Verifies all 4 legitimate adapters are present and map to the correct classes.
  @Test
  public void testWhitelistContents() {
    assertNotNull(allowedAdapters.get(GLOBAL_PREEMPTIBLE));
    assertTrue(allowedAdapters.get(GLOBAL_PREEMPTIBLE)
        .isAssignableFrom(GlobalPreemptibleAdapter.class));

    assertNotNull(allowedAdapters.get(QPS_RATE_LIMITER));
    assertTrue(allowedAdapters.get(QPS_RATE_LIMITER)
        .isAssignableFrom(QpsRateLimiterAdapter.class));

    assertNotNull(allowedAdapters.get(IP_QPS_RATE_LIMITER));
    assertTrue(allowedAdapters.get(IP_QPS_RATE_LIMITER)
        .isAssignableFrom(IPQPSRateLimiterAdapter.class));

    assertNotNull(allowedAdapters.get(DEFAULT_BASE_QPS));
    assertTrue(allowedAdapters.get(DEFAULT_BASE_QPS)
        .isAssignableFrom(DefaultBaseQqsAdapter.class));
  }

  // Verifies that arbitrary / malicious class names are rejected by the whitelist.
  @Test
  public void testInvalidClassNameIsRejected() {
    assertNull(allowedAdapters.get("com.evil.MaliciousAdapter"));
    assertNull(allowedAdapters.get("../../../../evil.Payload"));
    assertNull(allowedAdapters.get(Runtime.class.getName()));
    assertNull(allowedAdapters.get(ProcessBuilder.class.getName()));
    assertNull(allowedAdapters.get(""));
    assertNull(allowedAdapters.get(null));
  }

  // Verifies the whitelist cannot be modified at runtime (unmodifiable map).
  @Test(expected = UnsupportedOperationException.class)
  public void testWhitelistIsImmutable() {
    allowedAdapters.put("Injected", DefaultBaseQqsAdapter.class);
  }

  // Verifies structural invariants: exact size and all entries implement IRateLimiter.
  @Test
  public void testWhitelistStructure() {
    assertFalse("Whitelist must not be empty", allowedAdapters.isEmpty());
    assertTrue("Whitelist must contain exactly 4 adapters", allowedAdapters.size() == 4);
    for (Map.Entry<String, Class<? extends IRateLimiter>> entry : allowedAdapters.entrySet()) {
      assertTrue(entry.getKey() + " must implement IRateLimiter",
          IRateLimiter.class.isAssignableFrom(entry.getValue()));
    }
  }
}
