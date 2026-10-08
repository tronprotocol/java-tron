package org.tron.core.services.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.prometheus.client.CollectorRegistry;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.TestConstants;
import org.tron.common.application.HttpService;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.utils.PublicMethod;
import org.tron.core.config.args.Args;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;

public class HttpInterceptorMetricsTest {

  private static final String BODY = "{\"blockID\":\"0123456789abcdef\"}";
  private static final int MULTI_BYTE_CODE_POINT = 0x6D4B;
  private static final String UTF8_BODY =
      "{\"name\":\"" + new String(Character.toChars(MULTI_BYTE_CODE_POINT)) + "\"}";
  private static final int BIG_BODY_SIZE = 200_000;

  private static final String HTTP_BYTES_SUM = MetricKeys.Histogram.HTTP_BYTES + "_sum";
  private static final String HTTP_BYTES_COUNT = MetricKeys.Histogram.HTTP_BYTES + "_count";
  private static final String[] HTTP_BYTES_LABELS = new String[] {"url", "status"};

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static MetricsHttpService service;
  private static URI serverUri;
  private static CloseableHttpClient client;

  public static class PrintlnServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      resp.setContentType("application/json; charset=utf-8");
      resp.getWriter().println(BODY);
    }
  }

  public static class PrintServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      resp.setContentType("application/json; charset=utf-8");
      resp.getWriter().print(BODY);
    }
  }

  public static class StreamServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      byte[] bytes = BODY.getBytes(StandardCharsets.UTF_8);
      resp.setContentType("application/json-rpc");
      resp.setContentLength(bytes.length);
      resp.getOutputStream().write(bytes);
      resp.getOutputStream().flush();
    }
  }

  public static class Utf8Servlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      resp.setContentType("application/json; charset=utf-8");
      resp.getWriter().println(UTF8_BODY);
    }
  }

  public static class BigBodyServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      resp.setContentType("application/json; charset=utf-8");
      resp.getWriter().print(bigBody());
    }
  }

  public static class ErrorStatusServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.setContentType("application/json; charset=utf-8");
      resp.getWriter().println(BODY);
    }
  }

  public static class CompletionLatchFilter implements Filter {

    private static volatile CountDownLatch latch = new CountDownLatch(0);

    static void expectOneRequest() {
      latch = new CountDownLatch(1);
    }

    static boolean awaitRequestAccounted() throws InterruptedException {
      return latch.await(10, TimeUnit.SECONDS);
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      try {
        chain.doFilter(request, response);
      } finally {
        latch.countDown();
      }
    }

    @Override
    public void destroy() {
    }
  }

  static class MetricsHttpService extends HttpService {
    MetricsHttpService(int port) {
      this.port = port;
      this.contextPath = "/";
    }

    @Override
    protected void addServlet(ServletContextHandler context) {
      context.addServlet(new ServletHolder(new PrintlnServlet()), "/wallet/println");
      context.addServlet(new ServletHolder(new PrintServlet()), "/wallet/print");
      context.addServlet(new ServletHolder(new StreamServlet()), "/wallet/stream");
      context.addServlet(new ServletHolder(new Utf8Servlet()), "/wallet/utf8");
      context.addServlet(new ServletHolder(new BigBodyServlet()), "/wallet/big");
      context.addServlet(new ServletHolder(new ErrorStatusServlet()), "/wallet/error");
    }

    @Override
    protected void addFilter(ServletContextHandler context) {
      context.addFilter(new FilterHolder(new CompletionLatchFilter()), "/*",
          EnumSet.of(DispatcherType.REQUEST));
      context.addFilter(new FilterHolder(new HttpApiAccessFilter()), "/*",
          EnumSet.allOf(DispatcherType.class));
      ServletHandler handler = new ServletHandler();
      FilterHolder fh = handler.addFilterWithMapping(HttpInterceptor.class, "/*",
          EnumSet.of(DispatcherType.REQUEST));
      context.addFilter(fh, "/*", EnumSet.of(DispatcherType.REQUEST));
    }
  }

  @BeforeClass
  public static void setup() throws Exception {
    Args.setParam(new String[] {"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
    CommonParameter.getInstance().setNodeMetricsEnable(true);
    CommonParameter.getInstance().setMetricsPrometheusEnable(true);

    int port = PublicMethod.chooseRandomPort();
    service = new MetricsHttpService(port);
    service.start().get(10, TimeUnit.SECONDS);
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
      try {
        if (service != null) {
          service.stop();
        }
      } finally {
        Args.clearParam();
      }
    }
  }

  @Test
  public void testPrintlnBodyIsCountedExactly() throws Exception {
    assertTrafficMatchesWire("/wallet/println", BODY + System.lineSeparator());
  }

  @Test
  public void testPrintBodyIsCountedExactly() throws Exception {
    assertTrafficMatchesWire("/wallet/print", BODY);
  }

  @Test
  public void testOutputStreamBodyIsCountedExactly() throws Exception {
    assertTrafficMatchesWire("/wallet/stream", BODY);
  }

  @Test
  public void testUtf8BodyIsCountedInBytesNotCharacters() throws Exception {
    assertTrue("the UTF-8 body must be longer in bytes than in characters",
        UTF8_BODY.getBytes(StandardCharsets.UTF_8).length > UTF8_BODY.length());
    assertTrafficMatchesWire("/wallet/utf8", UTF8_BODY + System.lineSeparator());
  }

  @Test
  public void testBodyLargerThanOutputBufferIsCountedExactly() throws Exception {
    assertTrafficMatchesWire("/wallet/big", bigBody());
  }

  @Test
  public void testErrorStatusReportsGlobalTrafficOnly() throws Exception {
    String path = "/wallet/error";
    String detailKey = MetricsKey.NET_API_DETAIL_OUT_TRAFFIC + path;
    long trafficBefore = meterCount(MetricsKey.NET_API_OUT_TRAFFIC);
    long detailBefore = meterCount(detailKey);
    long failBefore = meterCount(MetricsKey.NET_API_FAIL_QPS);
    double histogramBefore = httpBytesSum(MetricLabels.UNDEFINED, "400");

    CompletionLatchFilter.expectOneRequest();
    HttpResponse resp = client.execute(new HttpGet(serverUri.resolve(path)));
    assertEquals(400, resp.getStatusLine().getStatusCode());
    byte[] wire = EntityUtils.toByteArray(resp.getEntity());
    assertEquals(BODY + System.lineSeparator(), new String(wire, StandardCharsets.UTF_8));
    assertTrue("the filter must finish accounting before the metrics are read",
        CompletionLatchFilter.awaitRequestAccounted());

    assertEquals("global out-traffic must equal the bytes on the wire",
        trafficBefore + wire.length, meterCount(MetricsKey.NET_API_OUT_TRAFFIC));
    assertEquals("a 4xx must not reach the per-endpoint traffic meter",
        detailBefore, meterCount(detailKey));
    assertEquals("a 4xx must be counted as a failed call",
        failBefore + 1, meterCount(MetricsKey.NET_API_FAIL_QPS));
    assertEquals("the 4xx histogram is labelled undefined, not with the endpoint",
        histogramBefore + wire.length, httpBytesSum(MetricLabels.UNDEFINED, "400"), 0.0);
  }

  private void assertTrafficMatchesWire(String path, String expectedBody) throws Exception {
    String detailKey = MetricsKey.NET_API_DETAIL_OUT_TRAFFIC + path;
    long trafficBefore = meterCount(MetricsKey.NET_API_OUT_TRAFFIC);
    long detailBefore = meterCount(detailKey);
    double histogramBefore = httpBytesSum(path, "200");
    double observationsBefore = httpBytesCount(path, "200");

    CompletionLatchFilter.expectOneRequest();
    HttpResponse resp = client.execute(new HttpGet(serverUri.resolve(path)));
    assertEquals(200, resp.getStatusLine().getStatusCode());
    byte[] wire = EntityUtils.toByteArray(resp.getEntity());
    assertTrue("the filter must finish accounting before the metrics are read",
        CompletionLatchFilter.awaitRequestAccounted());

    assertEquals("the servlet body must reach the client intact",
        expectedBody, new String(wire, StandardCharsets.UTF_8));
    assertEquals("global out-traffic must equal the bytes on the wire",
        trafficBefore + wire.length, meterCount(MetricsKey.NET_API_OUT_TRAFFIC));
    assertEquals("per-endpoint out-traffic must equal the bytes on the wire",
        detailBefore + wire.length, meterCount(detailKey));
    assertEquals("the histogram must be observed once, labelled with the endpoint",
        observationsBefore + 1, httpBytesCount(path, "200"), 0.0);
    assertEquals("the histogram must record the bytes on the wire",
        histogramBefore + wire.length, httpBytesSum(path, "200"), 0.0);
  }

  private long meterCount(String key) {
    return MetricsUtil.getMeter(key).getCount();
  }

  private double httpBytesSum(String url, String status) {
    return sampleValue(HTTP_BYTES_SUM, url, status);
  }

  private double httpBytesCount(String url, String status) {
    return sampleValue(HTTP_BYTES_COUNT, url, status);
  }

  private double sampleValue(String name, String url, String status) {
    Double value = CollectorRegistry.defaultRegistry.getSampleValue(name, HTTP_BYTES_LABELS,
        new String[] {url, status});
    return value == null ? 0d : value;
  }

  private static String bigBody() {
    StringBuilder sb = new StringBuilder(BIG_BODY_SIZE);
    while (sb.length() < BIG_BODY_SIZE) {
      sb.append('a');
    }
    return sb.toString();
  }
}
