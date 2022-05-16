package org.tron.common.storage.metric;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.prometheus.Metrics;
import org.tron.core.db.common.DbSourceInter;
import org.tron.core.db2.common.DB;

@Slf4j(topic = "metrics")
@Component
public class DbStatService {
  private static final ScheduledExecutorService statExecutor  =
      Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("db-stats-thread-%d").build());


  public  void register(DB<byte[], byte[]> db) {
    if (Metrics.enabled()) {
      statExecutor.scheduleWithFixedDelay(db::stat, 0, 6, TimeUnit.HOURS);
    }
  }

  public  void register(DbSourceInter<byte[]> db) {
    if (Metrics.enabled()) {
      statExecutor.scheduleWithFixedDelay(db::stat, 0, 6, TimeUnit.HOURS);
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
