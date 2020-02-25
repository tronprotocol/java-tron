package org.tron.core.services.filter;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.metrics.MonitorMetric;

@Slf4j(topic = "httpIntercetpor")
public class HttpInterceptor implements Filter {


  public final static String END_POINT = "END_POINT";
  public final static String OUT_TRAFFIC = "OUT_TRAFFIC";
  private static HashMap<String, JSONObject> EndpointCount = new HashMap<String, JSONObject>();
  private static long outAPITraffic = 0;

  @Autowired
  private MonitorMetric monitorMetric;


  public int getTotalCount() {
    return (int) monitorMetric.getMeter(MonitorMetric.TOTAL_REQUST).getCount();
  }


  public int getFailCount() {
    return (int) monitorMetric.getMeter(MonitorMetric.FAIL_REQUST).getCount();
  }

  public HashMap<String, JSONObject> getEndpointMap() {
    return this.EndpointCount;
  }

  public String getOutAPITraffic() {
    return Long.toString(monitorMetric.getMeter(MonitorMetric.OUT_TRAFFIC).getCount());
  }


  public HttpInterceptor getInstance() {
    return this;
  }

  @Override public void init(FilterConfig filterConfig) throws ServletException {
    // code here
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {


    if (request instanceof HttpServletRequest) {
      String endpoint = ((HttpServletRequest) request).getRequestURI();

      JSONObject obj = new JSONObject();
      if (EndpointCount.containsKey(endpoint)) {
        obj = EndpointCount.get(endpoint);
      } else {
        obj.put(monitorMetric.TOTAL_REQUST, 0);
        obj.put(monitorMetric.FAIL_REQUST, 0);
        obj.put(OUT_TRAFFIC, 0L);
        obj.put(END_POINT, endpoint);
      }
      obj.put(monitorMetric.TOTAL_REQUST, (int) obj.get(monitorMetric.TOTAL_REQUST) + 1);
      monitorMetric.getMeter(MonitorMetric.TOTAL_REQUST)
          .mark();

      CharResponseWrapper responseWrapper = new CharResponseWrapper((HttpServletResponse) response);
      chain.doFilter(request, responseWrapper);

      obj.put(OUT_TRAFFIC, (long) obj.get(OUT_TRAFFIC) + responseWrapper.getByteSize());

      monitorMetric.getMeter(MonitorMetric.OUT_TRAFFIC)
          .mark(responseWrapper.getByteSize());

      HttpServletResponse resp = (HttpServletResponse) response;
      if (resp.getStatus() != 200) {
        monitorMetric.getMeter(MonitorMetric.FAIL_REQUST)
            .mark();
        obj.put(monitorMetric.FAIL_REQUST, (int) obj.get(monitorMetric.FAIL_REQUST) + 1);
      }

      // update map
      EndpointCount.put(endpoint, obj);

    } else {
      chain.doFilter(request, response);
    }

  }

  @Override public void destroy() {

  }
}



