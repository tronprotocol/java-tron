package org.tron.core.services.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;

public class HttpInterceptorTest {

  private static final String ENDPOINT = "/wallet/getnowblock";

  private final HttpInterceptor interceptor = new HttpInterceptor();

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
    when(jettyRequest.getContextPath()).thenReturn("");
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

  private long meterCount(String key) {
    return MetricsUtil.getMeter(key).getCount();
  }
}
