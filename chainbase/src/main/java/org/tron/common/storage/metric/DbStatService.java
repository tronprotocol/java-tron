package org.tron.common.storage.metric;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.tron.common.prometheus.Metrics;
import org.tron.core.db.common.DbSourceInter;
import org.tron.core.db2.common.DB;

@Slf4j(topic = "metrics")
@Component
public class DbStatService implements ApplicationListener<ContextRefreshedEvent> {
  private static final ScheduledExecutorService statExecutor  =
      Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("db-stats-thread-%d").build());

  private Map<String, DB> statDbMap = new HashMap<>();
  private Map<String, DbSourceInter> statDbSourceMap = new HashMap<>();

  public synchronized void register(String name, DB<byte[], byte[]> db) {
    if (Metrics.enabled()) {
      statDbMap.put(name, db);
    }
  }

  public synchronized void register(String name, DbSourceInter<byte[]> db) {
    if (Metrics.enabled()) {
      statDbSourceMap.put(name, db);
    }
  }

  public synchronized void unregisterDb(String name) {
    if (Metrics.enabled()) {
      statDbMap.remove(name);
    }
  }

  public synchronized void unregisterDbSource(String name) {
    if (Metrics.enabled()) {
      statDbSourceMap.remove(name);
    }
  }

  public synchronized void shutdown() {
    if (Metrics.enabled()) {
      try {
        statDbMap.clear();
        statDbSourceMap.clear();
        statExecutor.shutdown();
      } catch (Exception e) {
        logger.error("{}", e.getMessage());
      }
    }
  }

  private synchronized void stat(){
    statDbMap.values().stream().forEach(db -> db.stat());
    statDbSourceMap.values().stream().forEach(dbSourceInter -> dbSourceInter.stat());
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (Metrics.enabled()) {
      statExecutor.scheduleWithFixedDelay(this::stat, 0, 6, TimeUnit.HOURS);
    }
  }

}
