package org.tron.common.storage.metric;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.prometheus.Metrics;

@Slf4j(topic = "metrics")
@Component
public class DbStatService {
  private static final ScheduledExecutorService statExecutor  =
      Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("db-stats-thread-%d").build());


  public  void register(Stat stat) {
    if (Metrics.enabled()) {
      statExecutor.scheduleWithFixedDelay(stat::stat, 0, 6, TimeUnit.HOURS);
    }
  }

  public void shutdown() {
    if (Metrics.enabled()) {
      try {
        statExecutor.shutdown();
      } catch (Exception e) {
        logger.error("{}", e.getMessage());
      }
    }
  }
}
