package org.tron.common.jetty;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
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

  /** Echoes the raw request-body bytes back so tests can inspect what arrived. */
  public static class EchoServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      byte[] body = ByteStreams.toByteArray(req.getInputStream());
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("application/octet-stream");
      resp.getOutputStream().write(body);
    }
  }

  /** Minimal concrete {@link HttpService} wired with a given size limit. */
  static class TestHttpService extends HttpService {
    TestHttpService(int port, int maxRequestSize) {
      this.port = port;
      this.contextPath = "/";
      this.maxRequestSize = maxRequestSize;
    }

    @Override
    protected void addServlet(ServletContextHandler context) {
      context.addServlet(new ServletHolder(new EchoServlet()), "/*");
    }
  }

  /** Minimal concrete {@link HttpService} simulating a JSON-RPC service. */
  static class TestJsonRpcService extends HttpService {
    TestJsonRpcService(int port, int maxRequestSize) {
      this.port = port;
      this.contextPath = "/";
      this.maxRequestSize = maxRequestSize;
    }

    @Override
    protected void addServlet(ServletContextHandler context) {
      context.addServlet(new ServletHolder(new EchoServlet()), "/jsonrpc");
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

  // -- helpers ----------------------------------------------------------------

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
