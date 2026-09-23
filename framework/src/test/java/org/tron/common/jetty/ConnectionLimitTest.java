package org.tron.common.jetty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.AbstractConnector;
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
 * Tests the connection limit configured in {@link HttpService}: once connections that send
 * nothing hold every slot, the server closes them after the limit's idle timeout and serves
 * new clients, instead of waiting for the connector's 30-second idle timeout.
 */
public class ConnectionLimitTest {

  private static final int MAX_CONNECTIONS = 2;

  // Below the connector's default 30-second idle timeout, above the limit's 10-second one
  // applied twice (Jetty half-closes an idle connection before closing it).
  private static final int TIMEOUT_MS = 25_000;

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static TestHttpService httpService;
  private static int port;

  public static class OkServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.getWriter().print("ok");
    }
  }

  static class TestHttpService extends HttpService {
    TestHttpService(int port) {
      this.port = port;
      this.contextPath = "/";
    }

    @Override
    protected void addServlet(ServletContextHandler context) {
      context.addServlet(new ServletHolder(new OkServlet()), "/*");
    }

    int connectedEndPoints() {
      return ((AbstractConnector) apiServer.getConnectors()[0]).getConnectedEndPoints().size();
    }
  }

  @BeforeClass
  public static void setup() throws Exception {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
    Args.getInstance().setMaxHttpConnectNumber(MAX_CONNECTIONS);
    port = PublicMethod.chooseRandomPort();
    httpService = new TestHttpService(port);
    httpService.start().get(10, TimeUnit.SECONDS);
  }

  @AfterClass
  public static void teardown() throws Exception {
    try {
      if (httpService != null) {
        httpService.stop();
      }
    } finally {
      Args.clearParam();
    }
  }

  @Test(timeout = 60_000)
  public void testIdleConnectionsDoNotLockOutClients() throws Exception {
    List<Socket> idleSockets = new ArrayList<>();
    try {
      for (int i = 0; i < MAX_CONNECTIONS; i++) {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("localhost", port), TIMEOUT_MS);
        idleSockets.add(socket);
        awaitConnectedEndPoints(i + 1);
      }

      Assert.assertEquals("HTTP/1.1 200 OK", get());
      for (Socket socket : idleSockets) {
        assertClosedByServer(socket);
      }
    } finally {
      for (Socket socket : idleSockets) {
        socket.close();
      }
    }
  }

  private static void awaitConnectedEndPoints(int expected) throws InterruptedException {
    long deadline = System.currentTimeMillis() + TIMEOUT_MS;
    while (httpService.connectedEndPoints() < expected) {
      Assert.assertTrue("server did not accept connection " + expected,
          System.currentTimeMillis() < deadline);
      Thread.sleep(10);
    }
  }

  private static String get() throws IOException {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress("localhost", port), TIMEOUT_MS);
      socket.setSoTimeout(TIMEOUT_MS);
      OutputStream out = socket.getOutputStream();
      out.write("GET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
          .getBytes(StandardCharsets.US_ASCII));
      out.flush();
      BufferedReader in = new BufferedReader(
          new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
      return in.readLine();
    }
  }

  private static void assertClosedByServer(Socket socket) throws IOException {
    socket.setSoTimeout(TIMEOUT_MS);
    try {
      Assert.assertEquals(-1, socket.getInputStream().read());
    } catch (SocketException e) {
      // a reset also means the server dropped the connection
    }
  }
}
