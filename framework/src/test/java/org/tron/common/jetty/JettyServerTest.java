package org.tron.common.jetty;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@Slf4j
public class JettyServerTest {
  private static Server server;
  private static URI serverUri;

  @BeforeClass
  public static void startJetty() throws Exception {
    server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(0);
    server.addConnector(connector);

    ServletContextHandler context = new ServletContextHandler();
    ServletHolder defaultServ = new ServletHolder("default", DefaultServlet.class);
    context.addServlet(defaultServ, "/");
    server.setHandler(context);
    server.start();
    String host = connector.getHost();
    if (host == null) {
      host = "localhost";
    }
    int port = connector.getLocalPort();
    serverUri = new URI(String.format("http://%s:%d/", host, port));
  }

  @AfterClass
  public static void stopJetty() {
    try {
      server.stop();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testGet() throws Exception {
    HttpClient client = new DefaultHttpClient();
    HttpGet request = new HttpGet(serverUri.resolve("/"));
    request.setHeader("Content-Length", "+450");
    HttpResponse mockResponse = client.execute(request);
    Assert.assertTrue(mockResponse.getStatusLine().toString().contains(
        "400 Invalid Content-Length Value"));
  }

}
