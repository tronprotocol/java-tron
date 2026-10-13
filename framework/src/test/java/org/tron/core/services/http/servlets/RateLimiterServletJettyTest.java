package org.tron.core.services.http.servlets;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;
import org.tron.core.services.WalletOnCursor;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.core.services.interfaceOnPBFT.WalletOnPBFT;
import org.tron.core.services.ratelimiter.RateLimiterContainer;
import org.tron.core.services.ratelimiter.RuntimeData;
import org.tron.core.services.ratelimiter.adapter.IRateLimiter;

/**
 * The surface tag must reach {@link RateLimiterServlet} through a real jetty request: one servlet
 * instance is mounted in several contexts, so the surface has to come from the request's context,
 * not from the servlet's own config. Mock requests cannot prove that.
 */
public class RateLimiterServletJettyTest {

  private static final String KEY_HTTP = "http_";

  private Server server;
  private int port;
  private RecordingServlet servlet;
  private RateLimiterContainer container;
  private WalletOnPBFT walletOnPBFT;
  private IRateLimiter fullLimiter;
  private IRateLimiter pbftLimiter;

  static class RecordingServlet extends RateLimiterServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      resp.getWriter().print("ok");
    }
  }

  @Before
  public void setUp() throws Exception {
    Args.setParam(new String[0], TestConstants.TEST_CONF);
    servlet = new RecordingServlet();
    container = new RateLimiterContainer();
    fullLimiter = admitting();
    pbftLimiter = admitting();
    container.add(KEY_HTTP, "RecordingServlet", fullLimiter);
    container.add(KEY_HTTP, "RecordingOnPBFTServlet", pbftLimiter);
    walletOnPBFT = mock(WalletOnPBFT.class);
    when(walletOnPBFT.selectCursor()).thenReturn((WalletOnCursor.CursorScope) () -> { });
    inject("container", container);
    inject("walletOnPBFT", walletOnPBFT);

    // the same servlet instance in two contexts, as the fullnode and PBFT services mount it
    ServletContextHandler full = new ServletContextHandler();
    full.setContextPath("/");
    full.addServlet(new ServletHolder(servlet), "/wallet/ping");
    ServletContextHandler pbft = new ServletContextHandler();
    pbft.setContextPath("/walletpbft");
    pbft.setAttribute(RateLimiterServlet.SURFACE_ATTRIBUTE, Surface.PBFT);
    pbft.addServlet(new ServletHolder(servlet), "/ping");

    server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setHost("127.0.0.1");
    connector.setPort(0);
    server.addConnector(connector);
    server.setHandler(new HandlerList(pbft, full));
    server.start();
    port = connector.getLocalPort();
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @AfterClass
  public static void clearArgs() {
    Args.clearParam();
  }

  @Test(timeout = 60_000)
  public void testPbftContextUsesPbftLimiterAndSelectsCursor() throws Exception {
    assertEquals(200, get("/walletpbft/ping"));

    verify(pbftLimiter, times(1)).acquirePermit(any(RuntimeData.class));
    verify(fullLimiter, never()).acquirePermit(any(RuntimeData.class));
    verify(walletOnPBFT, times(1)).selectCursor();
  }

  @Test(timeout = 60_000)
  public void testUntaggedContextUsesClassNameLimiterAndNoCursor() throws Exception {
    assertEquals(200, get("/wallet/ping"));

    verify(fullLimiter, times(1)).acquirePermit(any(RuntimeData.class));
    verify(pbftLimiter, never()).acquirePermit(any(RuntimeData.class));
    verify(walletOnPBFT, never()).selectCursor();
  }

  private int get(String path) throws IOException {
    HttpURLConnection connection =
        (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
    try {
      return connection.getResponseCode();
    } finally {
      connection.disconnect();
    }
  }

  private static IRateLimiter admitting() {
    IRateLimiter limiter = mock(IRateLimiter.class);
    when(limiter.acquirePermit(any(RuntimeData.class))).thenReturn(true);
    return limiter;
  }

  private void inject(String field, Object value) throws Exception {
    Field f = RateLimiterServlet.class.getDeclaredField(field);
    f.setAccessible(true);
    f.set(servlet, value);
  }
}
