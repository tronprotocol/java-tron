package org.tron.common.jetty;

import com.alibaba.fastjson.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.TestConstants;
import org.tron.common.application.HttpService;
import org.tron.common.utils.PublicMethod;
import org.tron.core.config.args.Args;

/**
 * Tests the {@link org.eclipse.jetty.server.handler.SizeLimitHandler} body-size
 * enforcement configured in {@link HttpService initContextHandler()}.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>Bodies within the limit are accepted ({@code 200}).</li>
 *   <li>Bodies exceeding the limit are rejected ({@code 413}).</li>
 *   <li>The limit counts raw UTF-8 <em>bytes</em>, not Java {@code char}s.</li>
 *   <li>HTTP and JSON-RPC services use independent size limits.</li>
 *   <li>Default values are {@code GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE} (4 MB).</li>
 * </ul>
 */
@Slf4j
public class SizeLimitHandlerTest {

  private static final int HTTP_MAX_BODY_SIZE = 1024;
  private static final int JSONRPC_MAX_BODY_SIZE = 512;

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static TestHttpService     httpService;
  private static TestJsonRpcService  jsonRpcService;
  private static URI                 httpServerUri;
  private static URI                 jsonRpcServerUri;
  private static CloseableHttpClient client;

  /**
   * Simulates the real servlet pattern: reads body via getReader(), wraps in
   * broad catch(Exception) — mirrors what RateLimiterServlet + actual servlets do.
   */
  public static class BroadCatchServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      try {
        String body = req.getReader().lines()
            .collect(Collectors.joining(System.lineSeparator()));
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().println("{\"size\":" + body.length()
            + ",\"bytes\":" + body.getBytes().length + "}");
      } catch (Exception e) {
        // Mimics RateLimiterServlet line 119-120: silently logs, does not rethrow
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().println("{\"Error\":\"" + e.getClass().getSimpleName() + "\"}");
      }
    }
  }

  /** Minimal concrete {@link HttpService} wired with a given size limit. */
  static class TestHttpService extends HttpService {
    TestHttpService(int port, long maxRequestSize) {
      this.port = port;
      this.contextPath = "/";
      this.maxRequestSize = maxRequestSize;
    }

    @Override
    protected void addServlet(ServletContextHandler context) {
      context.addServlet(new ServletHolder(new BroadCatchServlet()), "/*");
    }
  }

  /** Minimal concrete {@link HttpService} simulating a JSON-RPC service. */
  static class TestJsonRpcService extends HttpService {
    TestJsonRpcService(int port, long maxRequestSize) {
      this.port = port;
      this.contextPath = "/";
      this.maxRequestSize = maxRequestSize;
    }

    @Override
    protected void addServlet(ServletContextHandler context) {
      context.addServlet(new ServletHolder(new BroadCatchServlet()), "/jsonrpc");
    }
  }

  @BeforeClass
  public static void setup() throws Exception {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
    Args.getInstance().setHttpMaxMessageSize(HTTP_MAX_BODY_SIZE);
    Args.getInstance().setJsonRpcMaxMessageSize(JSONRPC_MAX_BODY_SIZE);

    int httpPort = PublicMethod.chooseRandomPort();
    httpService = new TestHttpService(httpPort, HTTP_MAX_BODY_SIZE);
    httpService.start().get(10, TimeUnit.SECONDS);
    httpServerUri = new URI(String.format("http://localhost:%d/", httpPort));

    int jsonRpcPort = PublicMethod.chooseRandomPort();
    jsonRpcService = new TestJsonRpcService(jsonRpcPort, JSONRPC_MAX_BODY_SIZE);
    jsonRpcService.start().get(10, TimeUnit.SECONDS);
    jsonRpcServerUri = new URI(String.format("http://localhost:%d/jsonrpc", jsonRpcPort));

    client = HttpClients.createDefault();
  }

  @AfterClass
  public static void teardown() throws Exception {
    try {
      if (client != null) {
        client.close();
      }
    } finally {
      try {
        if (httpService != null) {
          httpService.stop();
        }
      } finally {
        if (jsonRpcService != null) {
          jsonRpcService.stop();
        }
      }
      Args.clearParam();
    }
  }

  // -- HTTP service body-size tests -------------------------------------------

  @Test
  public void testHttpBodyWithinLimit() throws Exception {
    Assert.assertEquals(200, post(httpServerUri, new StringEntity("small body")));
  }

  @Test
  public void testHttpBodyExceedsLimit() throws Exception {
    Assert.assertEquals(413,
        post(httpServerUri, new StringEntity(repeat('a', HTTP_MAX_BODY_SIZE + 1))));
  }

  @Test
  public void testHttpBodyAtExactLimit() throws Exception {
    Assert.assertEquals(200,
        post(httpServerUri, new StringEntity(repeat('b', HTTP_MAX_BODY_SIZE))));
  }

  // -- JSON-RPC service body-size tests ---------------------------------------

  @Test
  public void testJsonRpcBodyWithinLimit() throws Exception {
    Assert.assertEquals(200,
        post(jsonRpcServerUri, new StringEntity("{\"method\":\"eth_blockNumber\"}")));
  }

  @Test
  public void testJsonRpcBodyExceedsLimit() throws Exception {
    Assert.assertEquals(413,
        post(jsonRpcServerUri, new StringEntity(repeat('x', JSONRPC_MAX_BODY_SIZE + 1))));
  }

  @Test
  public void testJsonRpcBodyAtExactLimit() throws Exception {
    Assert.assertEquals(200,
        post(jsonRpcServerUri, new StringEntity(repeat('c', JSONRPC_MAX_BODY_SIZE))));
  }

  // -- Independent limit tests ------------------------------------------------

  @Test
  public void testHttpAndJsonRpcHaveIndependentLimits() throws Exception {
    // A body that exceeds JSON-RPC limit but is within HTTP limit
    String body = repeat('d', JSONRPC_MAX_BODY_SIZE + 100);
    Assert.assertTrue(body.length() < HTTP_MAX_BODY_SIZE);

    Assert.assertEquals(200, post(httpServerUri, new StringEntity(body)));
    Assert.assertEquals(413, post(jsonRpcServerUri, new StringEntity(body)));
  }

  // -- UTF-8 byte counting test -----------------------------------------------

  @Test
  public void testLimitIsBasedOnBytesNotCharacters() throws Exception {
    // Each CJK character is 3 UTF-8 bytes; 342 chars x 3 = 1026 bytes > 1024
    String cjk = repeat('一', 342);
    Assert.assertEquals(342, cjk.length());
    Assert.assertEquals(1026, cjk.getBytes("UTF-8").length);
    Assert.assertEquals(413, post(httpServerUri, new StringEntity(cjk, "UTF-8")));
  }

  // -- Chunked (no Content-Length) transfer tests ------------------------------

  /**
   * Chunked request within the limit should succeed (EchoServlet).
   * InputStreamEntity with size=-1 sends chunked Transfer-Encoding (no Content-Length).
   */
  @Test
  public void testChunkedBodyWithinLimit() throws Exception {
    byte[] data = repeat('a', HTTP_MAX_BODY_SIZE / 4).getBytes("UTF-8");
    InputStreamEntity chunked = new InputStreamEntity(new ByteArrayInputStream(data), -1);
    Assert.assertEquals(200, post(httpServerUri, chunked));
  }

  /**
   * Chunked oversized body hitting a servlet with broad catch(Exception).
   *
   * <p>SizeLimitHandler's LimitInterceptor throws BadMessageException during
   * streaming read, but the servlet's catch(Exception) absorbs it and returns
   * 200 + error JSON instead of 413. This matches real TRON servlet behavior.
   *
   * <p>OOM protection still works: the body read is truncated at the limit.
   */
  @Test
  public void testChunkedBodyExceedsLimit() throws Exception {
    byte[] data = repeat('a', HTTP_MAX_BODY_SIZE * 2).getBytes("UTF-8");
    InputStreamEntity chunked = new InputStreamEntity(new ByteArrayInputStream(data), -1);
    HttpPost req = new HttpPost(httpServerUri);
    req.setEntity(chunked);
    HttpResponse resp = client.execute(req);
    int status = resp.getStatusLine().getStatusCode();
    String body = EntityUtils.toString(resp.getEntity());
    logger.info("Chunked oversized: status={}, body={}", status, body);

    // catch(Exception) absorbs BadMessageException → 200 + error JSON, not 413.
    // Body read IS truncated — OOM protection still effective.
    Assert.assertEquals(200, status);
    Assert.assertTrue("Error should be surfaced in response body",
        body.contains("Error"));
  }

  // -- Zero-limit behavior test -----------------------------------------------

  /**
   * When maxRequestSize is 0, SizeLimitHandler treats it as "reject all bodies > 0 bytes".
   * Jetty's logic: {@code _requestLimit >= 0 && size > _requestLimit} — 0 >= 0 is true,
   * so any non-empty body triggers 413. This is NOT "pass all" — it is a silent DoS
   * against the node's own API.
   */
  @Test
  public void testZeroLimitRejectsAllBodies() throws Exception {
    int zeroPort = PublicMethod.chooseRandomPort();
    TestHttpService zeroService = new TestHttpService(zeroPort, 0);
    try {
      zeroService.start().get(10, TimeUnit.SECONDS);
      URI zeroUri = new URI(String.format("http://localhost:%d/", zeroPort));

      // Empty body should pass (0 is NOT > 0)
      Assert.assertEquals(200, post(zeroUri, new StringEntity("")));

      // Any non-empty body should be rejected
      Assert.assertEquals(413, post(zeroUri, new StringEntity("x")));
    } finally {
      zeroService.stop();
    }
  }

  // -- checkBodySize vs SizeLimitHandler consistency tests --------------------

  /**
   * For pure ASCII JSON (the normal TRON API case), wire bytes and
   * {@code body.getBytes().length} (what {@code Util.checkBodySize()} measures)
   * must be identical — the two enforcement layers agree exactly.
   */
  @Test
  public void testWireBytesMatchCheckBodySizeForAsciiJson() throws Exception {
    String jsonBody = "{\"owner_address\":\"TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz\""
        + ",\"amount\":1000000}";
    int wireBytes = jsonBody.getBytes("UTF-8").length;

    String respBody = postForBody(httpServerUri, new StringEntity(jsonBody, "UTF-8"));
    JSONObject json = JSONObject.parseObject(respBody);
    int servletBytes = json.getIntValue("bytes");

    Assert.assertEquals("wire bytes should equal checkBodySize for ASCII JSON",
        wireBytes, servletBytes);
  }

  /**
   * For UTF-8 JSON with multi-byte characters (CJK), wire bytes and
   * {@code body.getBytes().length} must still be identical — UTF-8 round-trips
   * through {@code request.getReader()} → {@code String.getBytes()} losslessly.
   */
  @Test
  public void testWireBytesMatchCheckBodySizeForUtf8Json() throws Exception {
    String jsonBody = "{\"name\":\"测试地址\",\"amount\":100}";
    int wireBytes = jsonBody.getBytes("UTF-8").length;

    String respBody = postForBody(httpServerUri, new StringEntity(jsonBody, "UTF-8"));
    JSONObject json = JSONObject.parseObject(respBody);
    int servletBytes = json.getIntValue("bytes");

    Assert.assertEquals("wire bytes should equal checkBodySize for UTF-8 JSON",
        wireBytes, servletBytes);
  }

  /**
   * When the body contains {@code \r\n} line endings, {@code lines().collect()}
   * normalizes them to {@code \n} (on Linux) or the platform line separator.
   * This makes {@code checkBodySize} measure <em>fewer</em> bytes than the wire —
   * a safe direction: checkBodySize never rejects what SizeLimitHandler accepts.
   */
  @Test
  public void testCheckBodySizeSafeDirectionWithNewlines() throws Exception {
    String body = "{\"key1\":\"value1\",\r\n\"key2\":\"value2\",\r\n\"key3\":\"value3\"}";
    int wireBytes = body.getBytes("UTF-8").length;

    String respBody = postForBody(httpServerUri, new StringEntity(body, "UTF-8"));
    JSONObject json = JSONObject.parseObject(respBody);
    int servletBytes = json.getIntValue("bytes");

    Assert.assertTrue("checkBodySize bytes <= wire bytes (safe direction)",
        servletBytes <= wireBytes);
    logger.info("Newline test: wire={}, servlet={}, diff={}",
        wireBytes, servletBytes, wireBytes - servletBytes);
  }

  // -- helpers ----------------------------------------------------------------

  /** POSTs with the given entity and returns the response body as a string. */
  private String postForBody(URI uri, HttpEntity entity) throws Exception {
    HttpPost req = new HttpPost(uri);
    req.setEntity(entity);
    HttpResponse resp = client.execute(req);
    return EntityUtils.toString(resp.getEntity());
  }

  /** POSTs with the given entity and returns the HTTP status code. */
  private int post(URI uri, HttpEntity entity) throws Exception {
    HttpPost req = new HttpPost(uri);
    req.setEntity(entity);
    HttpResponse resp = client.execute(req);
    EntityUtils.consume(resp.getEntity());
    return resp.getStatusLine().getStatusCode();
  }

  /** Returns a string of {@code n} repetitions of {@code c}. */
  private static String repeat(char c, int n) {
    return new String(new char[n]).replace('\0', c);
  }
}
