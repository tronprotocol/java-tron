package org.tron.core.services.filter;

import com.google.common.base.Strings;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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

  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    String endpoint = MetricLabels.UNDEFINED;
    try {
      if (!(request instanceof HttpServletRequest)) {
        chain.doFilter(request, response);
        return;
      }
      endpoint = ((HttpServletRequest) request).getRequestURI();
      CharResponseWrapper responseWrapper = new CharResponseWrapper(
              (HttpServletResponse) response);
      chain.doFilter(request, responseWrapper);
      HttpServletResponse resp = (HttpServletResponse) response;
      int size = responseWrapper.getByteSize();
      MetricsUtil.meterMark(MetricsKey.NET_API_OUT_TRAFFIC, size);
      MetricsUtil.meterMark(MetricsKey.NET_API_QPS);
      if (resp.getStatus() >= HTTP_BAD_REQUEST && resp.getStatus() <= HTTP_NOT_ACCEPTABLE) {
        MetricsUtil.meterMark(MetricsKey.NET_API_FAIL_QPS);
        Metrics.histogramObserve(MetricKeys.Histogram.HTTP_BYTES,
                size, MetricLabels.UNDEFINED, String.valueOf(responseWrapper.getStatus()));
        return;
      }
      if (resp.getStatus() == HTTP_SUCCESS) {
        MetricsUtil.meterMark(MetricsKey.NET_API_DETAIL_QPS + endpoint);
      } else {
        MetricsUtil.meterMark(MetricsKey.NET_API_FAIL_QPS);
        MetricsUtil.meterMark(MetricsKey.NET_API_DETAIL_FAIL_QPS + endpoint);
      }
      MetricsUtil.meterMark(MetricsKey.NET_API_DETAIL_OUT_TRAFFIC + endpoint, size);
      Metrics.histogramObserve(MetricKeys.Histogram.HTTP_BYTES,
              size, endpoint, String.valueOf(responseWrapper.getStatus()));
    } catch (Exception e) {
      String key = MetricsKey.NET_API_DETAIL_QPS + endpoint;
      if (MetricsUtil.getMeters(MetricsKey.NET_API_DETAIL_QPS).containsKey(key)) {
        MetricsUtil.meterMark(key, 1);
        MetricsUtil.meterMark(MetricsKey.NET_API_DETAIL_FAIL_QPS + endpoint, 1);
      }
      MetricsUtil.meterMark(MetricsKey.NET_API_QPS, 1);
      MetricsUtil.meterMark(MetricsKey.NET_API_FAIL_QPS, 1);
    }
  }

  @Override
  public void destroy() {
  }
}



