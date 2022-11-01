package org.tron.common.prometheus;

import static io.prometheus.client.SampleNameFilter.ALLOW_ALL;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Predicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.tron.common.cache.CacheManager;

/**
 * Exports metrics about for guava cache.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 *   new GuavaCacheExports().register();
 * }
 * </pre>
 * Example metrics being exported:
 * <pre>
 *   tron:guava_cache_hit_rate{type="account"} 0.135679
 *   tron:guava_cache_request{type="account"} 3000
 * </pre>
 */
public class GuavaCacheExports extends Collector {

  private static final String TRON_GUAVA_CACHE_HIT_RATE = "tron:guava_cache_hit_rate";
  private static final String TRON_GUAVA_CACHE_REQUEST = "tron:guava_cache_request";
  private static final String TRON_GUAVA_CACHE_EVICTION_COUNT = "tron:guava_cache_eviction_count";


  public GuavaCacheExports() {
  }


  void addHitRateMetrics(List<MetricFamilySamples> sampleFamilies, Predicate<String> nameFilter) {
    if (nameFilter.test(TRON_GUAVA_CACHE_HIT_RATE)) {
      GaugeMetricFamily hitRate = new GaugeMetricFamily(
          TRON_GUAVA_CACHE_HIT_RATE,
          "Hit rate of a guava cache.",
          Collections.singletonList("type"));
      CacheManager.stats().forEach((k, v) -> hitRate
          .addMetric(Collections.singletonList(k), v.hitRate()));
      sampleFamilies.add(hitRate);
    }
  }

  void addRequestMetrics(List<MetricFamilySamples> sampleFamilies, Predicate<String> nameFilter) {
    if (nameFilter.test(TRON_GUAVA_CACHE_REQUEST)) {
      GaugeMetricFamily request = new GaugeMetricFamily(
          TRON_GUAVA_CACHE_REQUEST,
          "Request of a guava cache.",
          Collections.singletonList("type"));
      CacheManager.stats().forEach((k, v) -> request
          .addMetric(Collections.singletonList(k), v.requestCount()));
      sampleFamilies.add(request);
    }
  }

  void addEvictionCountMetrics(List<MetricFamilySamples> sampleFamilies,
                               Predicate<String> nameFilter) {
    if (nameFilter.test(TRON_GUAVA_CACHE_EVICTION_COUNT)) {
      GaugeMetricFamily request = new GaugeMetricFamily(
          TRON_GUAVA_CACHE_EVICTION_COUNT,
          "Eviction count of a guava cache.",
          Collections.singletonList("type"));
      CacheManager.stats().forEach((k, v) -> request
          .addMetric(Collections.singletonList(k), v.evictionCount()));
      sampleFamilies.add(request);
    }
  }

  @Override
  public List<MetricFamilySamples> collect() {
    return collect(null);
  }

  @Override
  public List<MetricFamilySamples> collect(Predicate<String> nameFilter) {
    List<MetricFamilySamples> mfs = new ArrayList<>();
    addHitRateMetrics(mfs, nameFilter == null ? ALLOW_ALL : nameFilter);
    addRequestMetrics(mfs, nameFilter == null ? ALLOW_ALL : nameFilter);
    addEvictionCountMetrics(mfs, nameFilter == null ? ALLOW_ALL : nameFilter);
    return mfs;
  }
}
