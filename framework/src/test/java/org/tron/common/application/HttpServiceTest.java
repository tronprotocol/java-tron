package org.tron.common.application;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Assert;
import org.junit.Test;

public class HttpServiceTest {

  @Test
  public void testInitServerPreservesLiteralListenAddress() {
    TestHttpService service = new TestHttpService("127.0.0.1", 0);
    try {
      service.initializeServer();

      Assert.assertEquals("127.0.0.1", service.getConnector().getHost());
    } finally {
      service.destroyServer();
    }
  }

  @Test
  public void testInitServerLeavesHostUnsetForWildcardBinding() {
    TestHttpService service = new TestHttpService(null, 0);
    try {
      service.initializeServer();

      Assert.assertNull(service.getConnector().getHost());
    } finally {
      service.destroyServer();
    }
  }

  @Test(timeout = 10_000)
  public void testServerBindsConfiguredIpv4Address() throws Exception {
    TestHttpService service = new TestHttpService("127.0.0.1", 0);
    try {
      service.start().get(10, TimeUnit.SECONDS);

      int localPort = service.getConnector().getLocalPort();
      Assert.assertTrue(localPort > 0);
      try (Socket ignored = new Socket("127.0.0.1", localPort)) {
        // Successful construction proves that the configured address accepts connections.
      }
    } finally {
      service.stop().get(10, TimeUnit.SECONDS);
    }
  }

  @Test(timeout = 10_000)
  public void testTraceIsRejectedWithoutEchoingHeaders() throws Exception {
    TestHttpService service = new TestHttpService("127.0.0.1", 0);
    try {
      service.start().get(10, TimeUnit.SECONDS);
      int localPort = service.getConnector().getLocalPort();
      String base = "http://127.0.0.1:" + localPort + "/echo";

      // Read the raw response: HttpURLConnection hides the body of a Content-Length: 0 error.
      String wire;
      try (Socket socket = new Socket("127.0.0.1", localPort)) {
        socket.getOutputStream().write(("TRACE /echo HTTP/1.1\r\n"
            + "Host: 127.0.0.1\r\n"
            + "Authorization: Bearer example-nonsecret-123\r\n"
            + "Cookie: test-cookie=example-nonsecret\r\n"
            + "Connection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
        wire = new String(ByteStreams.toByteArray(socket.getInputStream()),
            StandardCharsets.ISO_8859_1);
      }
      Assert.assertTrue(wire, wire.startsWith("HTTP/1.1 405 "));
      Assert.assertTrue(wire, wire.contains("\r\nContent-Length: 0\r\n"));
      Assert.assertTrue(wire, wire.endsWith("\r\n\r\n"));
      Assert.assertFalse(wire, wire.contains("example-nonsecret"));

      HttpURLConnection get = (HttpURLConnection) new URL(base).openConnection();
      get.setRequestMethod("GET");
      Assert.assertEquals(HttpURLConnection.HTTP_OK, get.getResponseCode());
      Assert.assertEquals("ok", read(get.getInputStream()));
    } finally {
      service.stop().get(10, TimeUnit.SECONDS);
    }
  }

  @Test(timeout = 10_000)
  public void testOptionsDoesNotAdvertiseTrace() throws Exception {
    TestHttpService service = new TestHttpService("127.0.0.1", 0);
    try {
      service.start().get(10, TimeUnit.SECONDS);
      String base = "http://127.0.0.1:" + service.getConnector().getLocalPort() + "/echo";

      // servlet-api default doOptions lists TRACE unconditionally
      HttpURLConnection options = (HttpURLConnection) new URL(base).openConnection();
      options.setRequestMethod("OPTIONS");
      Assert.assertEquals(HttpURLConnection.HTTP_OK, options.getResponseCode());
      Assert.assertEquals("GET, HEAD, OPTIONS", options.getHeaderField("Allow"));
    } finally {
      service.stop().get(10, TimeUnit.SECONDS);
    }
  }

  private static String read(InputStream in) throws IOException {
    if (in == null) {
      return "";
    }
    try (InputStream input = in) {
      return new String(ByteStreams.toByteArray(input), StandardCharsets.UTF_8);
    }
  }

  private static class TestHttpService extends HttpService {

    TestHttpService(String listenAddress, int port) {
      this.listenAddress = listenAddress;
      this.port = port;
      this.contextPath = "/";
    }

    void initializeServer() {
      initServer();
    }

    ServerConnector getConnector() {
      return (ServerConnector) apiServer.getConnectors()[0];
    }

    void destroyServer() {
      if (apiServer != null) {
        apiServer.destroy();
      }
    }

    @Override
    protected void addServlet(ServletContextHandler context) {
      context.addServlet(new ServletHolder(new HttpServlet() {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
          resp.getWriter().print("ok");
        }
      }), "/echo");
    }
  }
}
