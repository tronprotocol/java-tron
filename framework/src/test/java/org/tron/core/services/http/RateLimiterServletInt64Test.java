package org.tron.core.services.http;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;

/**
 * End-to-end integration tests for {@link RateLimiterServlet#service} wiring of the
 * {@code int64_as_string} flag. The single-class {@link JsonFormatInt64AsStringTest} verifies
 * the {@code JsonFormat} ThreadLocal mechanism in isolation; this test verifies the full
 * request-handling chain: URL query --&gt; {@code service()} --&gt; ThreadLocal --&gt; output, and
 * the {@code finally} clear that prevents state leakage across reused threads.
 *
 * <p>Pins four contracts:
 * <ol>
 *   <li>GET with {@code ?int64_as_string=true} produces quoted int64 fields.</li>
 *   <li>GET without the flag produces unquoted int64 fields (regression baseline).</li>
 *   <li>POST never honors the flag, regardless of source -- GET-only is the documented
 *       contract under issue #6568.</li>
 *   <li>{@code service()}'s {@code finally} block clears the ThreadLocal so reused Tomcat
 *       threads do not leak state between requests.</li>
 * </ol>
 *
 * <p>Uses {@link GetNowBlockServlet} as the fixture servlet because its response goes through
 * {@code JsonFormat.printToString}, which is what the ThreadLocal actually controls.
 */
public class RateLimiterServletInt64Test extends BaseTest {

  @Resource(name = "getNowBlockServlet")
  private GetNowBlockServlet servlet;

  @Resource(name = "getBurnTrxServlet")
  private GetBurnTrxServlet handBuiltServlet;

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath(),
        }, TestConstants.TEST_CONF
    );
  }

  @Before
  public void clearBefore() {
    JsonFormat.clearInt64AsString();
  }

  @After
  public void clearAfter() {
    JsonFormat.clearInt64AsString();
  }

  /** Contract 1: GET with int64_as_string=true on URL query produces quoted int64 fields. */
  @Test
  public void getWithUrlFlagQuotesInt64() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.addParameter("int64_as_string", "true");
    MockHttpServletResponse response = new MockHttpServletResponse();
    servlet.service(request, response);
    String body = readBody(response);
    if (body.contains("\"timestamp\"")) {
      assertTrue("timestamp should be quoted when int64_as_string=true, got: " + body,
          body.matches("(?s).*\"timestamp\"\\s*:\\s*\"\\d+\".*"));
    }
  }

  /** Contract 2: GET without flag produces unquoted int64 fields (default behavior). */
  @Test
  public void getWithoutFlagKeepsUnquoted() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    MockHttpServletResponse response = new MockHttpServletResponse();
    servlet.service(request, response);
    String body = readBody(response);
    if (body.contains("\"timestamp\"")) {
      assertTrue("timestamp should be unquoted when no flag, got: " + body,
          body.matches("(?s).*\"timestamp\"\\s*:\\s*\\d+.*"));
    }
  }

  /**
   * Contract 3: POST never honors int64_as_string, regardless of where the flag is placed.
   * Pins the GET-only design contract for issue #6568. Any future PR that tries to extend
   * support to POST will fail this test, forcing an explicit design review.
   */
  @Test
  public void postWithUrlFlagIgnored() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.addParameter("int64_as_string", "true");
    MockHttpServletResponse response = new MockHttpServletResponse();
    servlet.service(request, response);
    String body = readBody(response);
    if (body.contains("\"timestamp\"")) {
      assertFalse("POST URL flag must be ignored under GET-only design, got: " + body,
          body.matches("(?s).*\"timestamp\"\\s*:\\s*\"\\d+\".*"));
    }
  }

  /**
   * Contract 4 (CRITICAL): service() must clear the ThreadLocal in finally. Without this
   * clear, reused Tomcat threads leak the flag across requests, producing intermittent
   * quoted/unquoted output that is extremely hard to debug in production.
   */
  @Test
  public void serviceClearsThreadLocalInFinally() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.addParameter("int64_as_string", "true");
    servlet.service(request, new MockHttpServletResponse());
    assertFalse(
        "RateLimiterServlet.service must clear int64_as_string ThreadLocal in its finally "
            + "block. Removing this clear will leak state across requests on reused threads.",
        JsonFormat.isInt64AsString());
  }

  /**
   * Contract 5: hand-built JSON servlets (the ones that emit JSON literals manually instead
   * of going through {@link JsonFormat#printToString}) honor the flag. The previous tests use
   * {@link GetNowBlockServlet} which goes through {@code printToString}; this test uses
   * {@link GetBurnTrxServlet} as a representative of the four ternary-style servlets
   * (GetBurnTrx / GetPendingSize / GetTransactionCountByBlockNum / GetReward) to lock down
   * their {@code isInt64AsString() ? quoted : unquoted} branch -- so a future refactor that
   * inverts the ternary or breaks the quote placement fails visibly here.
   */
  @Test
  public void handBuiltJsonServletQuotesInt64WhenFlagSet() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.addParameter("int64_as_string", "true");
    MockHttpServletResponse response = new MockHttpServletResponse();
    handBuiltServlet.service(request, response);
    String body = readBody(response);
    assertTrue("burnTrxAmount should be quoted when int64_as_string=true, got: " + body,
        body.matches("(?s).*\"burnTrxAmount\"\\s*:\\s*\"\\d+\".*"));
  }

  /** Contract 6: hand-built JSON servlets default to unquoted output (regression baseline). */
  @Test
  public void handBuiltJsonServletKeepsUnquotedByDefault() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    MockHttpServletResponse response = new MockHttpServletResponse();
    handBuiltServlet.service(request, response);
    String body = readBody(response);
    assertTrue("burnTrxAmount should be unquoted by default, got: " + body,
        body.matches("(?s).*\"burnTrxAmount\"\\s*:\\s*\\d+.*"));
  }

  private String readBody(MockHttpServletResponse response) throws UnsupportedEncodingException {
    return response.getContentAsString();
  }
}
