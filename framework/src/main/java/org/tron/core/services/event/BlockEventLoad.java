package org.tron.core.services.event;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.core.db.Manager;
import org.tron.core.services.event.bo.BlockEvent;
import org.tron.core.services.event.bo.Event;

@Service
@Slf4j(topic = "event")
public class BlockEventLoad {

  @Autowired
  private Manager manager;

  @Autowired
  private RealtimeEventService realtimeEventService;

  @Autowired
  private BlockEventGet blockEventGet;

  private final ScheduledExecutorService executor = ExecutorServiceManager
      .newSingleThreadScheduledExecutor("event-load");

  public void init() {
    executor.scheduleWithFixedDelay(() -> {
      try {
        load();
      } catch (Exception e) {
        close();
        logger.error("Event load service fail.", e);
      }
    }, 100, 100, TimeUnit.MILLISECONDS);
    logger.info("Event load service start.");
  }

  public void close() {
    try {
      load();
      executor.shutdown();
      logger.info("Event load service close.");
    } catch (Exception e) {
      logger.warn("Stop event load service fail. {}", e.getMessage());
    }
  }

  public synchronized void load() throws Exception {
    long cacheHeadNum = BlockEventCache.getHead().getBlockId().getNum();
    long tmpNum =  manager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    if (cacheHeadNum >= tmpNum) {
      return;
    }
    synchronized (manager) {
      tmpNum =  manager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
      if (cacheHeadNum >= tmpNum) {
        return;
      }
      List<BlockEvent> l1 = new ArrayList<>();
      List<BlockEvent> l2 = new ArrayList<>();
      BlockEvent tmp = BlockEventCache.getHead();

      BlockEvent blockEvent = blockEventGet.getBlockEvent(tmpNum);
      l1.add(blockEvent);
      while (!blockEvent.getParentId().equals(tmp.getBlockId())) {
        tmpNum--;
        if (tmpNum == tmp.getBlockId().getNum()) {
          l2.add(tmp);
          tmp = BlockEventCache.getBlockEvent(tmp.getParentId());
        }
        blockEvent = blockEventGet.getBlockEvent(tmpNum);
        l1.add(blockEvent);
      }

      l2.forEach(e -> realtimeEventService.add(new Event(e, true)));

      List<BlockEvent> l = Lists.reverse(l1);
      for (BlockEvent e: l) {
        BlockEventCache.add(e);
        realtimeEventService.add(new Event(e, false));
      }
    }
  }

}
