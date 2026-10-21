package org.tron.core.services.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.Meter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;

public class HttpInterceptorTest {

  private static final String ENDPOINT = "/wallet/getnowblock";

  private final HttpInterceptor interceptor = new HttpInterceptor();

  @Before
  public void setup() {
    initInterceptor("", ENDPOINT, "/jsonrpc");
  }

  @Test
  public void testOversizedBadMessagePropagates() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/jsonrpc");
    request.setServletPath("/jsonrpc");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, resp) -> {
      throw new BadMessageException(HttpStatus.PAYLOAD_TOO_LARGE_413,
          "Request body is too large");
    };

    BadMessageException e = assertThrows(BadMessageException.class,
        () -> interceptor.doFilter(request, response, chain));

    assertEquals(HttpStatus.PAYLOAD_TOO_LARGE_413, e.getCode());
  }

  @Test
  public void testNonOversizedExceptionIsStillSwallowed() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/jsonrpc");
    request.setServletPath("/jsonrpc");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, resp) -> {
      throw new ServletException("expected");
    };

    interceptor.doFilter(request, response, chain);
  }

  @Test
  public void testNonJettyRequestRecordsZeroSizeAndNoFailure() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", ENDPOINT);
    request.setServletPath(ENDPOINT);
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean metricsWereEnabled = CommonParameter.getInstance().isNodeMetricsEnable();
    CommonParameter.getInstance().setNodeMetricsEnable(true);
    try {
      long trafficBefore = meterCount(MetricsKey.NET_API_OUT_TRAFFIC);
      long qpsBefore = meterCount(MetricsKey.NET_API_QPS);
      long failBefore = meterCount(MetricsKey.NET_API_FAIL_QPS);

      interceptor.doFilter(request, response, (req, resp) -> resp.getWriter().print("body"));

      assertEquals("body", response.getContentAsString());
      assertEquals(trafficBefore, meterCount(MetricsKey.NET_API_OUT_TRAFFIC));
      assertEquals(qpsBefore + 1, meterCount(MetricsKey.NET_API_QPS));
      assertEquals(failBefore, meterCount(MetricsKey.NET_API_FAIL_QPS));
    } finally {
      CommonParameter.getInstance().setNodeMetricsEnable(metricsWereEnabled);
    }
  }

  @Test
  public void testContentCountRecordedAsOutTraffic() throws Exception {
    Response jettyResponse = mock(Response.class);
    when(jettyResponse.getContentCount()).thenReturn(123L);
    Request jettyRequest = mock(Request.class);
    when(jettyRequest.getResponse()).thenReturn(jettyResponse);
    when(jettyRequest.getServletPath()).thenReturn(ENDPOINT);
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean metricsWereEnabled = CommonParameter.getInstance().isNodeMetricsEnable();
    CommonParameter.getInstance().setNodeMetricsEnable(true);
    try {
      long trafficBefore = meterCount(MetricsKey.NET_API_OUT_TRAFFIC);
      long detailBefore = meterCount(MetricsKey.NET_API_DETAIL_OUT_TRAFFIC + ENDPOINT);
      long failBefore = meterCount(MetricsKey.NET_API_FAIL_QPS);

      interceptor.doFilter(jettyRequest, response, (req, resp) -> {
      });

      assertEquals(trafficBefore + 123L, meterCount(MetricsKey.NET_API_OUT_TRAFFIC));
      assertEquals(detailBefore + 123L,
          meterCount(MetricsKey.NET_API_DETAIL_OUT_TRAFFIC + ENDPOINT));
      assertEquals(failBefore, meterCount(MetricsKey.NET_API_FAIL_QPS));
    } finally {
      CommonParameter.getInstance().setNodeMetricsEnable(metricsWereEnabled);
    }
  }

  @Test
  public void testContextPathComesFromRegistration() throws Exception {
    initInterceptor("/walletpbft", "/getnowblock");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", ENDPOINT);
    request.setContextPath("/request-context");
    request.setServletPath("/getnowblock");
    String detailKey = MetricsKey.NET_API_DETAIL_QPS + "/walletpbft/getnowblock";
    boolean metricsWereEnabled = CommonParameter.getInstance().isNodeMetricsEnable();
    CommonParameter.getInstance().setNodeMetricsEnable(true);
    try {
      long before = meterCount(detailKey);
      Set<String> keys = new HashSet<>(MetricsUtil.getMeters("net.api.detail.").keySet());
      interceptor.doFilter(request, new MockHttpServletResponse(), (req, resp) -> {
      });
      assertEquals(before + 1, meterCount(detailKey));
      keys.add(detailKey);
      keys.add(MetricsKey.NET_API_DETAIL_OUT_TRAFFIC + "/walletpbft/getnowblock");
      assertEquals(keys, MetricsUtil.getMeters("net.api.detail.").keySet());
    } finally {
      CommonParameter.getInstance().setNodeMetricsEnable(metricsWereEnabled);
    }
  }

  @Test
  public void testNonExactMappingsDoNotCreatePathMetrics() throws Exception {
    initInterceptor("", "/", "", "/*", "/dynamic/*", "*.json");
    boolean metricsWereEnabled = CommonParameter.getInstance().isNodeMetricsEnable();
    CommonParameter.getInstance().setNodeMetricsEnable(true);
    try {
      Set<String> keys = new HashSet<>(MetricsUtil.getMeters("net.api.detail.").keySet());
      for (String path : Arrays.asList("/", "", "/unknown", "/dynamic", "/dynamic/child",
          "/unknown.json")) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", path);
        request.setServletPath(path);
        interceptor.doFilter(request, new MockHttpServletResponse(), (req, resp) -> {
        });
      }
      assertEquals(keys, MetricsUtil.getMeters("net.api.detail.").keySet());
    } finally {
      CommonParameter.getInstance().setNodeMetricsEnable(metricsWereEnabled);
    }
  }

  @Test
  public void testExceptionOnUnknownPathIsNotAttributedToPath() throws Exception {
    String path = "/unregistered";
    String qpsKey = MetricsKey.NET_API_DETAIL_QPS + path;
    String failKey = MetricsKey.NET_API_DETAIL_FAIL_QPS + path;
    MetricsUtil.getMeter(qpsKey).mark();
    MockHttpServletRequest request = new MockHttpServletRequest("PATCH", path);
    request.setServletPath(path);
    boolean metricsWereEnabled = CommonParameter.getInstance().isNodeMetricsEnable();
    CommonParameter.getInstance().setNodeMetricsEnable(true);
    try {
      long before = meterCount(qpsKey);
      Set<String> keys = new HashSet<>(MetricsUtil.getMeters("net.api.detail.").keySet());
      long qpsBefore = meterCount(MetricsKey.NET_API_QPS);
      long failBefore = meterCount(MetricsKey.NET_API_FAIL_QPS);
      interceptor.doFilter(request, new MockHttpServletResponse(), (req, resp) -> {
        throw new ServletException("expected");
      });
      assertEquals(before, meterCount(qpsKey));
      assertEquals(0L, meterCount(failKey));
      assertEquals(keys, MetricsUtil.getMeters("net.api.detail.").keySet());
      assertEquals(qpsBefore + 1, meterCount(MetricsKey.NET_API_QPS));
      assertEquals(failBefore + 1, meterCount(MetricsKey.NET_API_FAIL_QPS));
    } finally {
      CommonParameter.getInstance().setNodeMetricsEnable(metricsWereEnabled);
    }
  }

  private void initInterceptor(String contextPath, String... mappings) {
    ServletContext context = mock(ServletContext.class);
    ServletRegistration registration = mock(ServletRegistration.class);
    when(registration.getMappings()).thenReturn(Arrays.asList(mappings));
    doReturn(Collections.singletonMap("servlet", registration)).when(context)
        .getServletRegistrations();
    when(context.getContextPath()).thenReturn(contextPath);
    interceptor.init(new MockFilterConfig(context));
  }

  private long meterCount(String key) {
    Meter meter = MetricsUtil.getMeters(key).get(key);
    return meter == null ? 0L : meter.getCount();
  }
}
