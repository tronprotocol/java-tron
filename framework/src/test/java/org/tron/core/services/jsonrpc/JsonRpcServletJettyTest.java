package org.tron.core.services.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.TestConstants;
import org.tron.common.application.HttpService;
import org.tron.common.utils.PublicMethod;
import org.tron.core.config.args.Args;
import org.tron.core.services.filter.HttpInterceptor;
import org.tron.core.services.http.RateLimiterServlet;
import org.tron.core.services.ratelimiter.RateLimiterContainer;

public class JsonRpcServletJettyTest {

  private static final String FATAL_MARKER = "fatal-response-marker";
  private static final String OBSERVATION_HEADER = "X-Test-Observation";

  @ClassRule
  public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

  private TestJsonRpcHttpService httpService;
  private CloseableHttpClient client;
  private FatalServiceImpl fatalService;
  private final Map<String, CompletableFuture<Boolean>> commitObservations =
      new ConcurrentHashMap<>();

  @Before
  public void setUp() throws Exception {
    Args.setParam(new String[]{"-d", TEMPORARY_FOLDER.newFolder().toString()},
        TestConstants.TEST_CONF);

    fatalService = new FatalServiceImpl();
    client = HttpClients.custom().disableAutomaticRetries()
        .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectTimeout(5000).setSocketTimeout(5000).build())
        .build();
  }

  private URI startServer(boolean withFilter) throws Exception {
    JsonRpcServer rpcServer = new JsonRpcServer(fatalService, FatalService.class);
    rpcServer.setErrorResolver(JsonRpcErrorResolver.INSTANCE);
    rpcServer.setShouldLogInvocationErrors(false);

    TestJsonRpcServlet servlet = new TestJsonRpcServlet(rpcServer);
    Field containerField = RateLimiterServlet.class.getDeclaredField("container");
    containerField.setAccessible(true);
    containerField.set(servlet, new RateLimiterContainer());

    int port = PublicMethod.chooseRandomPort();
    httpService = new TestJsonRpcHttpService(port, servlet, withFilter, commitObservations);
    httpService.start().get(10, TimeUnit.SECONDS);
    return new URI(String.format("http://localhost:%d/jsonrpc", port));
  }

  @After
  public void tearDown() throws Exception {
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
        Args.clearParam();
      }
    }
  }

  @Test
  public void fatalErrorDoesNotReachJettyDefaultErrorPage() throws Exception {
    URI endpoint = startServer(false);
    HttpPost request = new HttpPost(endpoint);
    request.setHeader("Accept", "application/json");
    request.setEntity(new StringEntity(
        "{\"jsonrpc\":\"2.0\",\"method\":\"test_fatal\",\"params\":[],\"id\":1}",
        ContentType.APPLICATION_JSON));

    try (CloseableHttpResponse response = client.execute(request)) {
      HttpEntity entity = response.getEntity();
      byte[] body = entity == null ? new byte[0] : EntityUtils.toByteArray(entity);
      String text = new String(body, StandardCharsets.UTF_8);

      Assert.assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          response.getStatusLine().getStatusCode());
      Assert.assertEquals(0, body.length);
      Assert.assertFalse(text.contains(FATAL_MARKER));
      Assert.assertFalse(text.contains(StackOverflowError.class.getName()));
    } catch (NoHttpResponseException | ConnectionClosedException | SocketException expected) {
      // A fatal Error may close the connection after the best-effort empty 500 is committed.
    }

    Assert.assertTrue("the request must reach the JSON-RPC method", fatalService.invoked.get());
  }

  @Test
  public void fatalErrorWithProductionFilterDoesNotReachJettyDefaultErrorPage() throws Exception {
    URI endpoint = startServer(true);
    String[] acceptTypes = {"application/json", "text/html", "text/plain"};
    for (String acceptType : acceptTypes) {
      fatalService.invoked.set(false);
      CompletableFuture<Boolean> committed = new CompletableFuture<>();
      commitObservations.put(acceptType, committed);
      HttpPost request = new HttpPost(endpoint);
      request.setHeader("Accept", acceptType);
      request.setHeader("Connection", "close");
      request.setHeader(OBSERVATION_HEADER, acceptType);
      request.setEntity(new StringEntity(
          "{\"jsonrpc\":\"2.0\",\"method\":\"test_fatal\",\"params\":[],\"id\":1}",
          ContentType.APPLICATION_JSON));

      try (CloseableHttpResponse response = client.execute(request)) {
        HttpEntity entity = response.getEntity();
        byte[] body = entity == null ? new byte[0] : EntityUtils.toByteArray(entity);
        String text = new String(body, StandardCharsets.UTF_8);

        Assert.assertEquals(acceptType, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            response.getStatusLine().getStatusCode());
        Assert.assertEquals(acceptType, 0, body.length);
        Assert.assertFalse(text.contains(FATAL_MARKER));
        Assert.assertFalse(text.contains(StackOverflowError.class.getName()));
      } catch (NoHttpResponseException | ConnectionClosedException | SocketException expected) {
        // Jetty can abort the connection after the guard commits. Require server-side evidence
        // below instead of treating an arbitrary network failure as a successful cleanup.
      }
      Assert.assertTrue("the underlying response must be committed as an empty 500",
          committed.get(5, TimeUnit.SECONDS));
      Assert.assertTrue("the request must reach the method for " + acceptType,
          fatalService.invoked.get());
    }
  }

  @Test
  public void successfulRequestWithProductionFilterPreservesResponse() throws Exception {
    HttpPost request = new HttpPost(startServer(true));
    request.setEntity(new StringEntity(
        "{\"jsonrpc\":\"2.0\",\"method\":\"test_ok\",\"params\":[],\"id\":7}",
        ContentType.APPLICATION_JSON));

    try (CloseableHttpResponse response = client.execute(request)) {
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
      Assert.assertEquals("application/json-rpc",
          ContentType.get(response.getEntity()).getMimeType());
      ObjectMapper mapper = new ObjectMapper();
      JsonNode body = mapper.readTree(EntityUtils.toByteArray(response.getEntity()));
      Assert.assertEquals(
          mapper.readTree("{\"jsonrpc\":\"2.0\",\"result\":\"ok\",\"id\":7}"), body);
    }
  }

  public interface FatalService {

    @JsonRpcMethod("test_fatal")
    String fatal();

    @JsonRpcMethod("test_ok")
    String ok();
  }

  private static class FatalServiceImpl implements FatalService {

    private final AtomicBoolean invoked = new AtomicBoolean();

    @Override
    public String fatal() {
      invoked.set(true);
      throw new StackOverflowError(FATAL_MARKER);
    }

    @Override
    public String ok() {
      return "ok";
    }
  }

  private static class TestJsonRpcServlet extends JsonRpcServlet {

    private final JsonRpcServer testRpcServer;

    TestJsonRpcServlet(JsonRpcServer testRpcServer) {
      this.testRpcServer = testRpcServer;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
      setRpcServer(testRpcServer);
    }
  }

  private static class TestJsonRpcHttpService extends HttpService {

    private final JsonRpcServlet servlet;
    private final boolean withFilter;
    private final Map<String, CompletableFuture<Boolean>> commitObservations;

    TestJsonRpcHttpService(int port, JsonRpcServlet servlet, boolean withFilter,
        Map<String, CompletableFuture<Boolean>> commitObservations) {
      this.port = port;
      this.contextPath = "/";
      this.servlet = servlet;
      this.withFilter = withFilter;
      this.commitObservations = commitObservations;
    }

    @Override
    protected void addServlet(ServletContextHandler context) {
      context.addServlet(new ServletHolder(servlet), "/jsonrpc");
    }

    @Override
    protected void addFilter(ServletContextHandler context) {
      if (withFilter) {
        context.addFilter(new FilterHolder(new CommitObserver(commitObservations)), "/*",
            EnumSet.of(DispatcherType.REQUEST));
        context.addFilter(new FilterHolder(new HttpInterceptor()), "/*",
            EnumSet.of(DispatcherType.REQUEST));
      }
    }
  }

  private static class CommitObserver implements Filter {

    private final Map<String, CompletableFuture<Boolean>> observations;

    CommitObserver(Map<String, CompletableFuture<Boolean>> observations) {
      this.observations = observations;
    }

    @Override
    public void init(FilterConfig config) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      try {
        chain.doFilter(request, response);
      } catch (Throwable failure) {
        String key = ((HttpServletRequest) request).getHeader(OBSERVATION_HEADER);
        CompletableFuture<Boolean> observation = key == null ? null : observations.get(key);
        if (observation != null) {
          HttpServletResponse actual = (HttpServletResponse) response;
          observation.complete(actual.isCommitted()
              && actual.getStatus() == HttpServletResponse.SC_INTERNAL_SERVER_ERROR
              && "0".equals(actual.getHeader("Content-Length")));
        }
        throw failure;
      }
    }

    @Override
    public void destroy() {
    }
  }
}
