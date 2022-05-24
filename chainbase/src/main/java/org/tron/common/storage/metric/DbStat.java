package org.tron.common.storage.metric;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;

@Slf4j(topic = "metrics")
public abstract class DbStat {

  protected void statProperty() {
    try {
      getStats().forEach(stat -> {
        String[] tmp = stat.trim().replaceAll(" +", ",").split(",");
        String level = tmp[0];
        double files = Double.parseDouble(tmp[1]);
        double size = Double.parseDouble(tmp[2]) * 1048576.0;
        Metrics.gaugeSet(MetricKeys.Gauge.DB_SST_LEVEL, files, getEngine(), getName(), level);
        Metrics.gaugeSet(MetricKeys.Gauge.DB_SIZE_BYTES, size, getEngine(), getName(), level);
        logger.info("DB {}, level:{},files:{},size:{} M",
            getName(), level, files, size / 1048576.0);
      });
    } catch (Exception e) {
      logger.warn("DB {} stats error", getName(), e);
    }
  }

  public abstract List<String> getStats() throws Exception;

  public abstract String getEngine();

  public abstract String getName();

}
