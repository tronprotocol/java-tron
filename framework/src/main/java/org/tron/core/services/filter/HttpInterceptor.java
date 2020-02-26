package org.tron.core.services.filter;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.metrics.MetricsService;

@Slf4j(topic = "httpIntercetpor")
@WebFilter(asyncSupported = true)
public class HttpInterceptor implements Filter {


  public final static String END_POINT = "END_POINT";
  public final static String END_POINT_ALL_REQUESTS_ONE_MINUTE = "END_POINT_ALL_REQUEST_ONE_MINUTE";
  public final static String END_POINT_ALL_REQUESTS_FIVE_MINUTE =
      "END_POINT_ALL_REQUEST_FIVE_MINUTE";
  public final static String END_POINT_ALL_REQUESTS_FIFTEEN_MINUTE =
      "END_POINT_ALL_REQUEST_FIFTEEN_MINUTE";
  public final static String END_POINT_ALL_REQUESTS_RPS =
      "END_POINT_ALL_REQUEST_FIFTEEN_RPS";
  public final static String END_POINT_FAIL_REQUEST_ONE_MINUTE =
      "END_POINT_FAIL_REQUEST_ONE_MINUTE";
  public final static String END_POINT_FAIL_REQUEST_FIVE_MINUTE =
      "END_POINT_FAIL_REQUEST_FIVE_MINUTE";
  public final static String END_POINT_FAIL_REQUEST_FIFTEEN_MINUTE =
      "END_POINT_FAIL_REQUEST_FIFTEEN_MINUTE";
  public final static String END_POINT_FAIL_REQUEST_RPS =
      "END_POINT_FAIL_REQUEST_FIFTEEN_RPS";
  public final static String END_POINT_OUT_TRAFFIC_ONE_MINUTE = "END_POINT_OUT_TRAFFIC_ONE_MINUTE";
  public final static String END_POINT_OUT_TRAFFIC_FIVE_MINUTE =
      "END_POINT_OUT_TRAFFIC_FIVE_MINUTE";
  public final static String END_POINT_OUT_TRAFFIC_FIFTEEN_MINUTE =
      "END_POINT_OUT_TRAFFIC_FIFTEEN_MINUTE";
  public final static String END_POINT_OUT_TRAFFIC_BPS =
      "END_POINT_OUT_TRAFFIC_FIFTEEN_BPS";

  public static Map<String, JSONObject> EndpointCount = new ConcurrentHashMap<String, JSONObject>();
  public static RequestCount totalrequestCount;
  public static RequestCount totalFailRequestCount;
  public static RequestCount outTraffic;
  Timer timer = new Timer();
  private String endpoint;
  private int reponseContentSize;
  private int minuteCount = 0;
  private long startTime;

//  ExecutorService executor = Executors.newFixedThreadPool(5);
  @Autowired
  private MetricsService metricsService;

  public static Map<String, JSONObject> getEndpointMap() {
    return EndpointCount;
  }

  public HttpInterceptor getInstance() {
    return this;
  }

  @Override public void init(FilterConfig filterConfig) throws ServletException {
    // code here
    startTime = System.currentTimeMillis();
    totalrequestCount = new RequestCount();
    totalFailRequestCount = new RequestCount();
    outTraffic = new RequestCount();
    timer.schedule(new resetCountEveryMinute(), 0, 1000 * 60);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    int second = (int) ((System.currentTimeMillis() - startTime) / 1000);
    int seconds = second == 0 ? 1 : second;
    if (request instanceof HttpServletRequest) {
      endpoint = ((HttpServletRequest) request).getRequestURI();

      JSONObject obj = new JSONObject();
      if (EndpointCount.containsKey(endpoint)) {
        obj = EndpointCount.get(endpoint);
      } else {
        obj.put(END_POINT_ALL_REQUESTS_ONE_MINUTE, 0);
        obj.put(END_POINT_ALL_REQUESTS_FIVE_MINUTE, 0);
        obj.put(END_POINT_ALL_REQUESTS_FIFTEEN_MINUTE, 0);
        obj.put(END_POINT_ALL_REQUESTS_RPS, 0D);
        obj.put("total_all_request", 0);
        obj.put(END_POINT_FAIL_REQUEST_ONE_MINUTE, 0);
        obj.put(END_POINT_FAIL_REQUEST_FIVE_MINUTE, 0);
        obj.put(END_POINT_FAIL_REQUEST_FIFTEEN_MINUTE, 0);
        obj.put(END_POINT_FAIL_REQUEST_RPS, 0D);
        obj.put("total_all_fail_request", 0);
        obj.put(END_POINT_OUT_TRAFFIC_ONE_MINUTE, 0);
        obj.put(END_POINT_OUT_TRAFFIC_FIVE_MINUTE, 0);
        obj.put(END_POINT_OUT_TRAFFIC_FIFTEEN_MINUTE, 0);
        obj.put(END_POINT_OUT_TRAFFIC_BPS, 0D);
        obj.put("total_out_traffic", 0);
        obj.put(END_POINT, endpoint);
      }
      totalrequestCount.allIncrement();
      totalrequestCount.caculteMeanRate(seconds);

      CharResponseWrapper responseWrapper = new CharResponseWrapper((HttpServletResponse) response);
      chain.doFilter(request, responseWrapper);

      reponseContentSize = responseWrapper.getByteSize();


      obj.put(END_POINT_ALL_REQUESTS_ONE_MINUTE,
          (int) obj.get(END_POINT_ALL_REQUESTS_ONE_MINUTE) + 1);
      obj.put(END_POINT_ALL_REQUESTS_FIVE_MINUTE,
          (int) obj.get(END_POINT_ALL_REQUESTS_FIVE_MINUTE) + 1);
      obj.put(END_POINT_ALL_REQUESTS_FIFTEEN_MINUTE,
          (int) obj.get(END_POINT_ALL_REQUESTS_FIFTEEN_MINUTE) + 1);

      obj.put("total_all_request",
          (int) obj.get("total_all_request") + 1);
      obj.put(END_POINT_ALL_REQUESTS_RPS,
          (double) ((int) obj.get("total_all_request") + 1) / seconds);


      obj.put(END_POINT_OUT_TRAFFIC_ONE_MINUTE,
          (int) obj.get(END_POINT_OUT_TRAFFIC_ONE_MINUTE) + reponseContentSize);
      obj.put(END_POINT_OUT_TRAFFIC_FIVE_MINUTE,
          (int) obj.get(END_POINT_OUT_TRAFFIC_FIVE_MINUTE) + reponseContentSize);
      obj.put(END_POINT_OUT_TRAFFIC_FIFTEEN_MINUTE,
          (int) obj.get(END_POINT_OUT_TRAFFIC_FIFTEEN_MINUTE) + reponseContentSize);

      obj.put("total_out_traffic",
          (int) obj.get("total_out_traffic") + reponseContentSize);
      obj.put(END_POINT_OUT_TRAFFIC_BPS,
          (double) ((int) obj.get("total_out_traffic") + reponseContentSize) / seconds);

      outTraffic.allIncrement(reponseContentSize);
      outTraffic.caculteMeanRate(seconds);

//      executor.execute(() -> {    // async execute
//
//        logger.info("execute update concurrency map");
//       metricsService.getMeter(MetricsService.BLOCKCHAIN_FAIL_FORK_COUNT)
//          .mark();
//       metricsService.getMeter(MetricsService.OUT_TRAFFIC)
//          .mark(reponseContentSize);
//        logger.info("execute update concurrency map  3");
//       metricsService.getMeter(MetricsService.END_POINT_ALL_REQUESTS)
//            .mark();
//        logger.info("execute update concurrency map  4");
//        JSONObject objCopy = EndpointCount.get(endpoint);
//        objCopy.put(END_POINT_ALL_REQUESTS_ONE_MINUTE,
//            (int)  Math.round(metricsService.getMeter(MetricsService.END_POINT_ALL_REQUESTS).
//                getOneMinuteRate()*60));;
//        objCopy.put(END_POINT_ALL_REQUESTS_FIVE_MINUTE,
//            (int)  Math.round(metricsService.getMeter(MetricsService.END_POINT_ALL_REQUESTS).
//                getFiveMinuteRate()*5*60));;
//        objCopy.put(END_POINT_ALL_REQUESTS_FIFTEEN_MINUTE,
//            (int) Math.round(metricsService.getMeter(MetricsService.END_POINT_ALL_REQUESTS).
//                getFifteenMinuteRate()*15*60));;
//        objCopy.put(END_POINT_ALL_REQUESTS_FIFTEEN_MINUTE,
//            (int) Math.round(metricsService.getMeter(MetricsService.END_POINT_ALL_REQUESTS).
//                getFifteenMinuteRate()*15*60));;
//        objCopy.put(END_POINT_ALL_REQUESTS_RPS,
//            (double)metricsService.getMeter(MetricsService.END_POINT_ALL_REQUESTS).
//                getMeanRate());;
//        logger.info("execute update concurrency map  5");
//
//       metricsService.getMeter(MetricsService.END_POINT_OUT_TRAFFIC)
//            .mark(reponseContentSize);
//
//        objCopy.put(END_POINT_OUT_TRAFFIC_ONE_MINUTE,
//            (int)  Math.round(metricsService.getMeter(MetricsService.END_POINT_OUT_TRAFFIC).
//                getOneMinuteRate()*60));;
//        objCopy.put(END_POINT_OUT_TRAFFIC_FIVE_MINUTE,
//            (int)  Math.round(metricsService.getMeter(MetricsService.END_POINT_OUT_TRAFFIC).
//                getFiveMinuteRate()*5*60));;
//        objCopy.put(END_POINT_OUT_TRAFFIC_FIFTEEN_MINUTE,
//            (int) Math.round(metricsService.getMeter(MetricsService.END_POINT_OUT_TRAFFIC).
//                getFifteenMinuteRate()*15*60));;
//        objCopy.put(END_POINT_OUT_TRAFFIC_BPS,
//            (double)metricsService.getMeter(MetricsService.END_POINT_OUT_TRAFFIC).
//                getMeanRate());;
//        logger.info("update concurrency map");
//        EndpointCount.put(endpoint, objCopy);
//        logger.info("execute update concurrency map  6");
//
//
//      });


      HttpServletResponse resp = (HttpServletResponse) response;
      if (resp.getStatus() != 200) {
//        executor.submit(() -> {    // async execute
//          metricsService.getMeter(MetricsService.FAIL_REQUEST)
//              .mark();
//
//          metricsService.getMeter(MetricsService.END_POINT_FAIL_REQUESTS)
//              .mark();
//          JSONObject objCopy = EndpointCount.get(endpoint);
//          objCopy.put(END_POINT_FAIL_REQUEST_ONE_MINUTE,
//              (int)  Math.round(metricsService.getMeter(MetricsService.END_POINT_FAIL_REQUESTS).
//                  getOneMinuteRate()*60));;
//          objCopy.put(END_POINT_FAIL_REQUEST_FIVE_MINUTE,
//              (int)  Math.round(metricsService.getMeter(MetricsService.END_POINT_FAIL_REQUESTS).
//                  getFiveMinuteRate()*5*60));;
//          objCopy.put(END_POINT_FAIL_REQUEST_FIFTEEN_MINUTE,
//              (int) Math.round(metricsService.getMeter(MetricsService.END_POINT_FAIL_REQUESTS).
//                  getFifteenMinuteRate()*15*60));;
//          objCopy.put(END_POINT_FAIL_REQUEST_RPS,
//              (double)metricsService.getMeter(MetricsService.END_POINT_FAIL_REQUESTS).
//                  getMeanRate());;
//
//          EndpointCount.put(endpoint, objCopy);
//
//        });
        totalFailRequestCount.allIncrement();
        totalFailRequestCount.caculteMeanRate(seconds);

        obj.put(END_POINT_FAIL_REQUEST_ONE_MINUTE,
            (int) obj.get(END_POINT_FAIL_REQUEST_ONE_MINUTE) + 1);
        obj.put(END_POINT_FAIL_REQUEST_FIVE_MINUTE,
            (int) obj.get(END_POINT_FAIL_REQUEST_FIVE_MINUTE) + 1);
        obj.put(END_POINT_FAIL_REQUEST_FIFTEEN_MINUTE,
            (int) obj.get(END_POINT_FAIL_REQUEST_FIFTEEN_MINUTE) + 1);

        obj.put("total_all_fail_request",
            (int) obj.get("total_all_fail_request") + 1);
        obj.put(END_POINT_FAIL_REQUEST_RPS,
            (double) ((int) obj.get("total_all_fail_request") + 1) / seconds);

      }

      EndpointCount.put(endpoint, obj);

    } else {
      chain.doFilter(request, response);
    }


  }


  // need a thread to run it every minute, but it consume so much time ,
// which downgrade performance
  public void resetCount() {
    // reset every one, five, fifteen minute
    minuteCount++;
    if (minuteCount % 15 == 0) {
      totalrequestCount.resetFifteenMinute();
      totalFailRequestCount.resetFifteenMinute();
      outTraffic.resetFifteenMinute();
      EndpointCount.forEach((key, obj) -> {
        obj.put(END_POINT_ALL_REQUESTS_FIFTEEN_MINUTE, 0);
        obj.put(END_POINT_FAIL_REQUEST_FIFTEEN_MINUTE, 0);
        obj.put(END_POINT_OUT_TRAFFIC_FIFTEEN_MINUTE, 0);
        // update map
        EndpointCount.put(key, obj);
      });
    } else if (minuteCount % 5 == 0) {
      totalrequestCount.resetFiveMinte();
      totalFailRequestCount.resetFiveMinte();
      outTraffic.resetFiveMinte();
      for (Map.Entry<String, JSONObject> entry : EndpointCount.entrySet()) {
        JSONObject obj = entry.getValue();
        obj.put(END_POINT_ALL_REQUESTS_FIVE_MINUTE, 0);
        obj.put(END_POINT_FAIL_REQUEST_FIVE_MINUTE, 0);
        obj.put(END_POINT_OUT_TRAFFIC_FIVE_MINUTE, 0);
        // update map
        EndpointCount.put(entry.getKey(), obj);
      }
    } else {
      totalrequestCount.resetOneMinute();
      totalFailRequestCount.resetOneMinute();
      outTraffic.resetOneMinute();
      for (Map.Entry<String, JSONObject> entry : EndpointCount.entrySet()) {
        JSONObject obj = entry.getValue();
        obj.put(END_POINT_ALL_REQUESTS_ONE_MINUTE, 0);
        obj.put(END_POINT_FAIL_REQUEST_ONE_MINUTE, 0);
        obj.put(END_POINT_OUT_TRAFFIC_ONE_MINUTE, 0);
        // update map
        EndpointCount.put(entry.getKey(), obj);
      }
    }

  }

  @Override public void destroy() {

  }

  public static class RequestCount {
    private int oneMinuteCount;
    private int fiveMinuteCount;
    private int fifteenMinuteCount;
    private long total;
    private double meanRate;

    public RequestCount() {
      oneMinuteCount = 0;
      fiveMinuteCount = 0;
      fifteenMinuteCount = 0;
      meanRate = 0.0;
      total = 0;
    }

    public void allIncrement() {
      oneMinuteCount++;
      fiveMinuteCount++;
      fifteenMinuteCount++;
      total++;
    }

    public void allIncrement(int size) {
      oneMinuteCount = oneMinuteCount + size;
      fiveMinuteCount = fiveMinuteCount + size;
      fifteenMinuteCount = fifteenMinuteCount + size;
      total = total + size;
    }

    public void allReset() {
      oneMinuteCount = 0;
      fiveMinuteCount = 0;
      fifteenMinuteCount = 0;
    }

    public void resetOneMinute() {
      oneMinuteCount = 0;
    }

    public void resetFiveMinte() {
      fiveMinuteCount = 0;
    }

    public void resetFifteenMinute() {
      fifteenMinuteCount = 0;
    }

    public void caculteMeanRate(long seconds) {
      meanRate = (double) total / seconds;
    }

    public int getOneMinuteCount() {
      return oneMinuteCount;
    }

    public int getFiveMinuteCount() {
      return fiveMinuteCount;
    }

    public int getFifteenMinuteCount() {
      return fifteenMinuteCount;
    }

    public double getMeanRate() {
      return meanRate;
    }

  }

  class resetCountEveryMinute extends TimerTask {
    public void run() {
      resetCount();
    }
  }
}



