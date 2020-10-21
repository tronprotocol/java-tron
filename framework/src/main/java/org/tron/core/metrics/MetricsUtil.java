package org.tron.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;

import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import metrics_influxdb.InfluxdbReporter;
import metrics_influxdb.api.protocols.InfluxdbProtocols;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Constant;
import org.tron.core.metrics.net.RateInfo;

@Slf4j(topic = "metrics")
public class MetricsUtil {

  private static MetricRegistry metricRegistry = new MetricRegistry();

  public static void init() {
    if (CommonParameter.getInstance().isNodeMetricsEnable()
            && CommonParameter.getInstance().isMetricsStorageEnable()) {
      String ip = CommonParameter.getInstance().getInfluxDbIp();
      int port = CommonParameter.getInstance().getInfluxDbPort();
      String dataBase = CommonParameter.getInstance().getInfluxDbDatabase();
      ScheduledReporter influxReport = InfluxdbReporter
              .forRegistry(metricRegistry)
              .protocol(InfluxdbProtocols.http(ip, port, dataBase))
              .convertRatesTo(TimeUnit.SECONDS)
              .convertDurationsTo(TimeUnit.MILLISECONDS)
              .filter(MetricFilter.ALL)
              .skipIdleMetrics(false)
              .build();
      int interval = CommonParameter.getInstance().getMetricsReportInterval() 
              * Constant.ONE_THOUSAND;
      influxReport.start(interval, TimeUnit.MILLISECONDS);
    }
  }

  public static Histogram getHistogram(String key) {
    return metricRegistry.histogram(key);
  }

  public static SortedMap<String, Histogram> getHistograms(String key) {
    return metricRegistry.getHistograms((s, metric) -> s.startsWith(key));
  }

  /**
   * Histogram update.
   *
   * @param key String
   * @param value long
   */
  public static void histogramUpdate(String key, long value) {
    try {
      if (CommonParameter.getInstance().isNodeMetricsEnable()) {
        metricRegistry.histogram(key).update(value);
      }
    } catch (Exception e) {
      logger.warn("update histogram failed, key:{}, value:{}", key, value);
    }
  }

  public static Meter getMeter(String name) {
    return metricRegistry.meter(name);
  }

  /**
   * get all Meters with same prefix
   *
   * @param key prefix String
   */
  public static SortedMap<String, Meter> getMeters(String key) {
    return metricRegistry.getMeters((s, metric) -> s.startsWith(key));
  }

  /**
   * Meter mark.
   *
   * @param key String
   */
  public static void meterMark(String key) {
    try {
      if (CommonParameter.getInstance().isNodeMetricsEnable()) {
        metricRegistry.meter(key).mark(1L);
      }
    } catch (Exception e) {
      logger.warn("mark meter failed, key:{}", key);
    }
  }

  /**
   * Meter mark.
   *
   * @param key String
   * @param value long
   */
  public static void meterMark(String key, long value) {
    try {
      if (CommonParameter.getInstance().isNodeMetricsEnable()) {
        metricRegistry.meter(key).mark(value);
      }
    } catch (Exception e) {
      logger.warn("mark meter failed, key:{}, value:{}", key, value);
    }
  }

  public static Counter getCounter(String name) {
    return metricRegistry.counter(name);
  }

  public static SortedMap<String, Counter> getCounters(String name) {
    return metricRegistry.getCounters((s, metric) -> s.startsWith(name));
  }

  /**
   * Counter inc.
   *
   * @param key String
   */
  public static void counterInc(String key) {
    try {
      if (CommonParameter.getInstance().isNodeMetricsEnable()) {
        metricRegistry.counter(key).inc(1L);
      }
    } catch (Exception e) {
      logger.warn("inc counter failed, key:{}", key);
    }
  }

  /**
   * get rate info.
   *
   * @param key String
   * @return RateInfo
   */
  public static RateInfo getRateInfo(String key) {
    RateInfo rateInfo = new RateInfo();
    Meter meter = MetricsUtil.getMeter(key);
    rateInfo.setCount(meter.getCount());
    rateInfo.setMeanRate(meter.getMeanRate());
    rateInfo.setOneMinuteRate(meter.getOneMinuteRate());
    rateInfo.setFiveMinuteRate(meter.getFiveMinuteRate());
    rateInfo.setFifteenMinuteRate(meter.getFifteenMinuteRate());
    return rateInfo;
  }


}
