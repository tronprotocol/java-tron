package org.tron.core.services.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;

@Slf4j(topic = "httpInterceptor")
public class HttpInterceptor implements Filter {

  private String endpoint;
  private final int HTTP_NOT_FOUND = 404;
  private final int HTTP_SUCCESS = 200;

  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    try {
      if (request instanceof HttpServletRequest) {
        endpoint = ((HttpServletRequest) request).getRequestURI();

        CharResponseWrapper responseWrapper = new CharResponseWrapper(
            (HttpServletResponse) response);
        chain.doFilter(request, responseWrapper);

        HttpServletResponse resp = (HttpServletResponse) response;

        if (resp.getStatus() != HTTP_NOT_FOUND) {  // correct endpoint
          String endpointQPS = MetricsKey.NET_API_DETAIL_QPS + endpoint;
          MetricsUtil.meterMark(MetricsKey.NET_API_QPS);
          MetricsUtil.meterMark(endpointQPS);

          int reposeContentSize = responseWrapper.getByteSize();
          String endpointOutTraffic = MetricsKey.NET_API_DETAIL_OUT_TRAFFIC + endpoint;
          MetricsUtil.meterMark(MetricsKey.NET_API_OUT_TRAFFIC,
              reposeContentSize);
          MetricsUtil.meterMark(endpointOutTraffic, reposeContentSize);

          if (resp.getStatus() != HTTP_SUCCESS) {  //http fail
            String endpointFailQPS = MetricsKey.NET_API_DETAIL_FAIL_QPS + endpoint;
            MetricsUtil.meterMark(MetricsKey.NET_API_FAIL_QPS);
            MetricsUtil.meterMark(endpointFailQPS);
          }
        } else { // wrong endpoint
          MetricsUtil.meterMark(MetricsKey.NET_API_QPS);
          MetricsUtil.meterMark(MetricsKey.NET_API_FAIL_QPS);
        }

      } else {
        chain.doFilter(request, response);
      }

    } catch (Exception e) {

      if (MetricsUtil.getMeters(MetricsKey.NET_API_DETAIL_QPS).containsKey(
          MetricsKey.NET_API_DETAIL_QPS + endpoint)) {   // correct endpoint
        MetricsUtil.meterMark(MetricsKey.NET_API_DETAIL_FAIL_QPS
            + endpoint, 1);
        MetricsUtil.meterMark(MetricsKey.NET_API_DETAIL_QPS
            + endpoint, 1);
      }
      MetricsUtil.meterMark(MetricsKey.NET_API_QPS, 1);
      MetricsUtil.meterMark(MetricsKey.NET_API_FAIL_QPS, 1);

    }

  }

  @Override
  public void destroy() {

  }

}



