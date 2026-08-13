package org.tron.p2p.discover.protocol.kad;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.protocol.kad.table.KademliaOptions;
import org.tron.p2p.utils.NetUtil;

@Slf4j(topic = "net")
public class DiscoverTask {

  private ScheduledExecutorService discoverer = Executors.newSingleThreadScheduledExecutor(
      BasicThreadFactory.builder().namingPattern("discoverTask").build());

  private KadService kadService;

  private int loopNum = 0;
  private byte[] nodeId;

  public DiscoverTask(KadService kadService) {
    this.kadService = kadService;
  }

  public void init() {
    discoverer.scheduleWithFixedDelay(() -> {
      try {
        loopNum++;
        if (loopNum % KademliaOptions.MAX_LOOP_NUM == 0) {
          loopNum = 0;
          nodeId = kadService.getPublicHomeNode().getId();
        } else {
          nodeId = NetUtil.getNodeId();
        }
        discover(nodeId, 0, new ArrayList<>());
      } catch (Exception e) {
        log.error("DiscoverTask fails to be executed", e);
      }
    }, 1, KademliaOptions.DISCOVER_CYCLE, TimeUnit.MILLISECONDS);
    log.debug("DiscoverTask started");
  }

  private void discover(byte[] nodeId, int round, List<Node> prevTriedNodes) {

    List<Node> closest = kadService.getTable().getClosestNodes(nodeId);
    List<Node> tried = new ArrayList<>();
    for (Node n : closest) {
      if (!tried.contains(n) && !prevTriedNodes.contains(n)) {
        try {
          kadService.getNodeHandler(n).sendFindNode(nodeId);
          tried.add(n);
        } catch (Exception e) {
          log.error("Unexpected Exception occurred while sending FindNodeMessage", e);
        }
      }

      if (tried.size() == KademliaOptions.ALPHA) {
        break;
      }
    }

    try {
      Thread.sleep(KademliaOptions.WAIT_TIME);
    } catch (InterruptedException e) {
      log.warn("Discover task interrupted");
      Thread.currentThread().interrupt();
    }

    if (tried.isEmpty()) {
      return;
    }

    if (++round == KademliaOptions.MAX_STEPS) {
      return;
    }
    tried.addAll(prevTriedNodes);
    discover(nodeId, round, tried);
  }

  public void close() {
    discoverer.shutdownNow();
  }
}
