package org.tron.common.storage.metric;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.prometheus.Metrics;
import org.tron.core.db.common.DbSourceInter;
import org.tron.core.db2.common.DB;

@Slf4j(topic = "metrics")
@Component
public class DbStatService {
  private final String name = "db-stats";
  private final ScheduledExecutorService statExecutor  =
      ExecutorServiceManager.newSingleThreadScheduledExecutor(name);

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
      logger.info("Db stat service shutdown...");
      ExecutorServiceManager.shutdownAndAwaitTermination(statExecutor, name);
      logger.info("Db stat service shutdown complete");
    }
  }
}
