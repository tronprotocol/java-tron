package org.tron.core.services.filter;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.*;
import java.io.IOException;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BytesCapsule;
import java.util.concurrent.*;

@Slf4j(topic = "httpIntercetpor")
public class HttpInterceptor implements Filter {

  public static String TOTAL_REQUST = "TOTAL_REQUEST";
  public static String FAIL_REQUST = "FAIL_REQUEST";
  public String END_POINT = "END_POINT";
  public int totalCount = 0;
  public int failCount = 0;
  public long gapMilliseconds = 1 * 1000;
  public HashMap<String, JSONObject> EndpointCount = new HashMap<String, JSONObject>();

  @Autowired
  @Getter
  private ChainBaseManager chainBaseManager;
  private long preciousTime = 0;
  ExecutorService executor = Executors.newFixedThreadPool(2);
  @Override public void init(FilterConfig filterConfig) throws ServletException {
    // code here
    executor.submit(() -> {  // asyn  get time
      preciousTime = chainBaseManager.getDynamicPropertiesStore().getRecordRequestTime();
    });

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    long currentTime = System.currentTimeMillis();
    if (currentTime - preciousTime > gapMilliseconds) {   //reset every 24 hours
      ExecutorService executor = Executors.newFixedThreadPool(1);
      totalCount = 0;
      failCount = 0;
      preciousTime = currentTime;
      EndpointCount.clear();
//        chainBaseManager.getCommonStore().put("preciousTime".getBytes(),
//            new BytesCapsule(ByteArray.fromLong(preciousTime)));
      executor.submit(() -> {    // asyn write data
        chainBaseManager.getDynamicPropertiesStore().saveRecordRequestTime(preciousTime);
      });

    }

    if (request instanceof HttpServletRequest) {
      String endpoint = ((HttpServletRequest) request).getRequestURI();
      JSONObject obj = new JSONObject();
      if (EndpointCount.containsKey(endpoint)) {
        obj = EndpointCount.get(endpoint);
      } else {
//        obj = new JSONObject();
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
//      logger.info("make rpc call  *********************" + request.getRemoteAddr());
      chain.doFilter(request, response);
    }


  }

  @Override public void destroy() {
        executor.shutdown();
  }
}
