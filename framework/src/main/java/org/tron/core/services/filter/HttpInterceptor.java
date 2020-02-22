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

@Slf4j(topic = "httpIntercetpor")
public class HttpInterceptor implements Filter {

  public static String TOTAL_REQUST = "TOTAL_REQUEST";
  public static String FAIL_REQUST = "FAIL_REQUEST";
  private static int totalCount = 0;
  private static int failCount = 0;
  private static int interval = 1;      // 1 minute interval
  private static HashMap<String, JSONObject> EndpointCount = new HashMap<String, JSONObject>();
  public String END_POINT = "END_POINT";
  public long gapMilliseconds = interval * 60 * 1000;
  private long preciousTime = 0;
  private boolean enableInterval = false;

  public int getTotalCount() {
    return this.totalCount;
  }

  public int getInterval() {
    return this.interval;
  }

  public int getFailCount() {
    return this.failCount;
  }

  public HashMap<String, JSONObject> getEndpointMap() {
    return this.EndpointCount;
  }

  public HttpInterceptor getInstance() {
    return this;
  }

  @Override public void init(FilterConfig filterConfig) throws ServletException {
    // code here
    preciousTime = System.currentTimeMillis();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    long currentTime = System.currentTimeMillis();
    if (currentTime - preciousTime > gapMilliseconds && this.enableInterval) {   //reset every
      totalCount = 0;
      failCount = 0;
      preciousTime = currentTime;
      EndpointCount.clear();
    }

    if (request instanceof HttpServletRequest) {
      String endpoint = ((HttpServletRequest) request).getRequestURI();
      JSONObject obj = new JSONObject();
      if (EndpointCount.containsKey(endpoint)) {
        obj = EndpointCount.get(endpoint);
      } else {
        obj.put(TOTAL_REQUST, 0);
        obj.put(FAIL_REQUST, 0);
        obj.put(END_POINT, endpoint);
      }
      obj.put(TOTAL_REQUST, (int) obj.get(TOTAL_REQUST) + 1);
      totalCount++;
      chain.doFilter(request, response);
      HttpServletResponse resp = (HttpServletResponse) response;
      if (resp.getStatus() != 200) {
        failCount++;
        obj.put(FAIL_REQUST, (int) obj.get(FAIL_REQUST) + 1);
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
