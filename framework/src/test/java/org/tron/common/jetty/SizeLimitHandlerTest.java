package org.tron.common.jetty;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
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
import org.junit.Test;
import org.tron.common.TestConstants;
import org.tron.common.application.HttpService;
import org.tron.common.utils.PublicMethod;
import org.tron.core.config.args.Args;

/**
 * Tests the {@link org.eclipse.jetty.server.handler.SizeLimitHandler} body-size
 * enforcement configured in {@link HttpService initContextHandler()}.
 *
 * <p>Unlike a standalone Jetty micro-server, this test creates a concrete
 * {@link HttpService} subclass and calls {@link HttpService#start()}, so the
 * production code paths of {@code initServer()} and {@code initContextHandler()}
 * are exercised directly and reflected in code-coverage reports.</p>
 *
 * <p>Key behaviours proven:</p>
 * <ul>
 *   <li>Bodies within the limit are accepted ({@code 200}).</li>
 *   <li>Bodies exceeding the limit are rejected ({@code 413}).</li>
 *   <li>The limit counts raw UTF-8 <em>bytes</em>, not Java {@code char}s.</li>
 * </ul>
 */
@Slf4j
public class SizeLimitHandlerTest {

  private static final int MAX_BODY_SIZE = 1024;

  private static TestHttpService     httpService;
  private static URI                 serverUri;
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

  /** Minimal concrete {@link HttpService} that registers only an {@link EchoServlet}. */
  static class TestHttpService extends HttpService {
    TestHttpService(int port) {
      this.port = port;
      this.contextPath = "/";
    }

    @Override
    protected void addServlet(ServletContextHandler context) {
      context.addServlet(new ServletHolder(new EchoServlet()), "/*");
    }
  }

  /**
   * Initialises {@link Args} and starts a real {@link HttpService} whose
   * {@code initServer()} and {@code initContextHandler()} are the production
   * implementations — guaranteeing test coverage of the new
   * {@code SizeLimitHandler} wiring.
   */
  @BeforeClass
  public static void setup() throws Exception {
    String dbPath = Files.createTempDirectory("sizelimit-test").toString();
    Args.setParam(new String[]{"--output-directory", dbPath}, TestConstants.TEST_CONF);
    Args.getInstance().setMaxMessageSize(MAX_BODY_SIZE);

    int port = PublicMethod.chooseRandomPort();
    httpService = new TestHttpService(port);
    httpService.start().get(10, TimeUnit.SECONDS);

    serverUri = new URI(String.format("http://localhost:%d/", port));
    client = HttpClients.createDefault();
  }

  @AfterClass
  public static void teardown() throws Exception {
    try {
      if (client != null) {
        client.close();
      }
    } finally {
      if (httpService != null) {
        httpService.stop();
      }
      Args.clearParam();
    }
  }

  // -- body-size tests (covers HttpService.initContextHandler) ---------------

  @Test
  public void testBodyWithinLimit() throws Exception {
    Assert.assertEquals(200, post(new StringEntity("small body")));
  }

  @Test
  public void testBodyExceedsLimit() throws Exception {
    Assert.assertEquals(413, post(new StringEntity(repeat('a', MAX_BODY_SIZE + 1))));
  }

  // -- helpers ---------------------------------------------------------------

  /** POSTs with the given entity and returns the HTTP status code. */
  private int post(HttpEntity entity) throws Exception {
    HttpPost req = new HttpPost(serverUri);
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
