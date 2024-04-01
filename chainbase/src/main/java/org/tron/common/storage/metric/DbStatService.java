package org.tron.common.storage.metric;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.prometheus.Metrics;

@Slf4j(topic = "metrics")
@Component
public class DbStatService {
  private final String esName = "db-stats";
  private final ScheduledExecutorService statExecutor  =
      ExecutorServiceManager.newSingleThreadScheduledExecutor(esName);

  public  void register(Stat stat) {
    if (Metrics.enabled()) {
      statExecutor.scheduleWithFixedDelay(stat::stat, 0, 6, TimeUnit.HOURS);
    }
  }

  public void shutdown() {
    if (Metrics.enabled()) {
      ExecutorServiceManager.shutdownAndAwaitTermination(statExecutor, esName);
    }
  }
}
