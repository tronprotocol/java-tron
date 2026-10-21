package org.tron.core.services.filter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;

@Slf4j(topic = "httpInterceptor")
public class HttpInterceptor implements Filter {

  private final int HTTP_SUCCESS = 200;
  private final int HTTP_BAD_REQUEST = 400;
  private final int HTTP_NOT_ACCEPTABLE = 406;

  private Map<String, String> endpoints = Collections.emptyMap();

  @Override
  public void init(FilterConfig filterConfig) {
    ServletContext context = filterConfig.getServletContext();
    Map<String, String> registered = new HashMap<>();
    for (ServletRegistration servlet : context.getServletRegistrations().values()) {
      for (String mapping : servlet.getMappings()) {
        if (isExactMapping(mapping)) {
          registered.put(mapping, context.getContextPath() + mapping);
        }
      }
    }
    endpoints = Collections.unmodifiableMap(registered);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    String endpoint = MetricLabels.UNDEFINED;
    try {
      if (!(request instanceof HttpServletRequest)) {
        chain.doFilter(request, response);
        return;
      }
      endpoint = endpoints.getOrDefault(((HttpServletRequest) request).getServletPath(),
          MetricLabels.UNDEFINED);
      chain.doFilter(request, response);
      HttpServletResponse resp = (HttpServletResponse) response;
      int status = resp.getStatus();
      long size = getContentCount(request);
      MetricsUtil.meterMark(MetricsKey.NET_API_OUT_TRAFFIC, size);
      MetricsUtil.meterMark(MetricsKey.NET_API_QPS);
      if (status != HTTP_SUCCESS) {
        MetricsUtil.meterMark(MetricsKey.NET_API_FAIL_QPS);
      }
      if (MetricLabels.UNDEFINED.equals(endpoint)
          || (status >= HTTP_BAD_REQUEST && status <= HTTP_NOT_ACCEPTABLE)) {
        Metrics.histogramObserve(MetricKeys.Histogram.HTTP_BYTES,
            size, MetricLabels.UNDEFINED, String.valueOf(status));
        return;
      }
      if (status == HTTP_SUCCESS) {
        MetricsUtil.meterMark(MetricsKey.NET_API_DETAIL_QPS + endpoint);
      } else {
        MetricsUtil.meterMark(MetricsKey.NET_API_DETAIL_FAIL_QPS + endpoint);
      }
      MetricsUtil.meterMark(MetricsKey.NET_API_DETAIL_OUT_TRAFFIC + endpoint, size);
      Metrics.histogramObserve(MetricKeys.Histogram.HTTP_BYTES,
          size, endpoint, String.valueOf(status));
    } catch (Exception e) {
      String key = MetricsKey.NET_API_DETAIL_QPS + endpoint;
      if (MetricsUtil.getMeters(MetricsKey.NET_API_DETAIL_QPS).containsKey(key)) {
        MetricsUtil.meterMark(key, 1);
        MetricsUtil.meterMark(MetricsKey.NET_API_DETAIL_FAIL_QPS + endpoint, 1);
      }
      MetricsUtil.meterMark(MetricsKey.NET_API_QPS, 1);
      MetricsUtil.meterMark(MetricsKey.NET_API_FAIL_QPS, 1);
      if (e instanceof BadMessageException
          && ((BadMessageException) e).getCode() == HttpStatus.PAYLOAD_TOO_LARGE_413) {
        throw (BadMessageException) e;
      }
    }
  }

  private boolean isExactMapping(String mapping) {
    return mapping.startsWith("/") && mapping.length() > 1 && mapping.indexOf('*') < 0;
  }

  private long getContentCount(ServletRequest request) {
    Request baseRequest = Request.getBaseRequest(request);
    return baseRequest == null ? 0L : baseRequest.getResponse().getContentCount();
  }

  @Override
  public void destroy() {
  }
}
