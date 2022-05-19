package org.tron.common.prometheus;

import static io.prometheus.client.SampleNameFilter.ALLOW_ALL;

import com.sun.management.OperatingSystemMXBean;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Predicate;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports metrics about Operating System.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 *  new OperatingSystemExports().register(CollectorRegistry.defaultRegistry);
 * }
 * </pre>
 * Example metrics being exported:
 * <pre>
 *   system_available_cpus 12.0
 *   process_cpu_load{} 200
 *   system_cpu_load 0.3876289375889973
 *   system_free_physical_memory_bytes 1.96481024E8
 * </pre>
 */
public class OperatingSystemExports extends Collector {

  private static final String SYSTEM_AVAILABLE_CPUS = "system_available_cpus";
  private static final String PROCESS_CPU_LOAD = "process_cpu_load";
  private static final String SYSTEM_CPU_LOAD = "system_cpu_load";
  private static final String SYSTEM_LOAD_AVERAGE = "system_load_average";
  private static final String SYSTEM_TOTAL_PHYSICAL_MEMORY_BYTES =
      "system_total_physical_memory_bytes";
  private static final String SYSTEM_FREE_PHYSICAL_MEMORY_BYTES =
      "system_free_physical_memory_bytes";
  private static final String SYSTEM_TOTAL_SWAP_SPACES_BYTES = "system_total_swap_spaces_bytes";
  private static final String SYSTEM_FREE_SWAP_SPACES_BYTES = "system_free_swap_spaces_bytes";


  private final OperatingSystemMXBean operatingSystemMXBean;

  public OperatingSystemExports() {
    this((OperatingSystemMXBean) ManagementFactory
        .getOperatingSystemMXBean());
  }

  public OperatingSystemExports(OperatingSystemMXBean operatingSystemMXBean) {
    this.operatingSystemMXBean = operatingSystemMXBean;
  }

  void addOperatingSystemMetrics(List<MetricFamilySamples> sampleFamilies,
                                 Predicate<String> nameFilter) {
    if (nameFilter.test(SYSTEM_AVAILABLE_CPUS)) {
      sampleFamilies.add(
          new GaugeMetricFamily(
              SYSTEM_AVAILABLE_CPUS,
              "System available cpus",
              operatingSystemMXBean.getAvailableProcessors()));
    }

    if (nameFilter.test(PROCESS_CPU_LOAD)) {
      sampleFamilies.add(
          new GaugeMetricFamily(
              PROCESS_CPU_LOAD,
              "Process cpu load",
              operatingSystemMXBean.getProcessCpuLoad()));
    }

    if (nameFilter.test(SYSTEM_CPU_LOAD)) {
      sampleFamilies.add(
          new GaugeMetricFamily(
              SYSTEM_CPU_LOAD,
              "System cpu load",
              operatingSystemMXBean.getSystemCpuLoad()));
    }

    if (nameFilter.test(SYSTEM_LOAD_AVERAGE)) {
      sampleFamilies.add(
          new GaugeMetricFamily(
              SYSTEM_LOAD_AVERAGE,
              "System cpu load average",
              operatingSystemMXBean.getSystemLoadAverage()));
    }

    if (nameFilter.test(SYSTEM_TOTAL_PHYSICAL_MEMORY_BYTES)) {
      sampleFamilies.add(
          new GaugeMetricFamily(
              SYSTEM_TOTAL_PHYSICAL_MEMORY_BYTES,
              "System total physical memory bytes",
              operatingSystemMXBean.getTotalPhysicalMemorySize()));
    }

    if (nameFilter.test(SYSTEM_FREE_PHYSICAL_MEMORY_BYTES)) {
      sampleFamilies.add(
          new GaugeMetricFamily(
              SYSTEM_FREE_PHYSICAL_MEMORY_BYTES,
              "System free physical memory bytes",
              operatingSystemMXBean.getFreePhysicalMemorySize()));
    }
    if (nameFilter.test(SYSTEM_TOTAL_SWAP_SPACES_BYTES)) {
      sampleFamilies.add(
          new GaugeMetricFamily(
              SYSTEM_TOTAL_SWAP_SPACES_BYTES,
              "System free swap spaces bytes",
              operatingSystemMXBean.getTotalSwapSpaceSize()));
    }
    if (nameFilter.test(SYSTEM_FREE_SWAP_SPACES_BYTES)) {
      sampleFamilies.add(
          new GaugeMetricFamily(
              SYSTEM_FREE_SWAP_SPACES_BYTES,
              "System free swap spaces",
              operatingSystemMXBean.getFreeSwapSpaceSize()));
    }
  }

  @Override
  public List<MetricFamilySamples> collect() {
    return collect(null);
  }

  @Override
  public List<MetricFamilySamples> collect(Predicate<String> nameFilter) {
    List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
    addOperatingSystemMetrics(mfs, nameFilter == null ? ALLOW_ALL : nameFilter);
    return mfs;
  }
}