package org.tron.common.application;

import java.net.Socket;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
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
    }
  }
}
