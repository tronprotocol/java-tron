package org.tron.core.net.service.effective;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.TronNetService;
import org.tron.core.net.peer.PeerConnection;
import org.tron.p2p.discover.Node;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class EffectiveCheckService {

  @Getter
  private final boolean isEffectiveCheck = Args.getInstance().isNodeEffectiveCheckEnable();
  @Autowired
  private TronNetDelegate tronNetDelegate;

  private final Cache<InetSocketAddress, Boolean> nodesCache = CacheBuilder.newBuilder()
      .initialCapacity(100)
      .maximumSize(10000)
      .expireAfterWrite(20, TimeUnit.MINUTES).build();
  @Getter
  @Setter
  private volatile InetSocketAddress cur;
  private final AtomicInteger count = new AtomicInteger(0);
  private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder().setNameFormat("effective-thread-%d").build());
  private long MAX_HANDSHAKE_TIME = 60_000;

  public void init() {
    if (isEffectiveCheck) {
      executor.scheduleWithFixedDelay(() -> {
        try {
          findEffectiveNode();
        } catch (Exception e) {
          logger.error("Check effective connection processing failed", e);
        }
      }, 60, 5, TimeUnit.SECONDS);
    } else {
      logger.info("EffectiveCheckService is disabled");
    }
  }

  public void triggerNext() {
    try {
      executor.submit(this::findEffectiveNode);
    } catch (Exception e) {
      logger.warn("Submit effective service task failed, message:{}", e.getMessage());
    }
  }

  public void close() {
    if (executor != null) {
      try {
        executor.shutdown();
      } catch (Exception e) {
        logger.error("Exception in shutdown effective service worker, {}", e.getMessage());
      }
    }
  }

  public boolean isIsolateLand() {
    return (int) tronNetDelegate.getActivePeer().stream()
        .filter(PeerConnection::isNeedSyncFromUs)
        .count() == tronNetDelegate.getActivePeer().size();
  }

  //try to find node which we can sync from
  private void findEffectiveNode() {
    if (!isIsolateLand()) {
      if (count.get() > 0) {
        logger.info("Success to verify effective node {}", cur);
        resetCount();
      }
      return;
    }

    if (cur != null) {
      tronNetDelegate.getActivePeer().forEach(p -> {
        if (p.getInetSocketAddress().equals(cur)
            && System.currentTimeMillis() - p.getChannel().getStartTime() >= MAX_HANDSHAKE_TIME) {
          // we encounter no effective connection again, so we disconnect with last used node
          logger.info("Disconnect with {}", cur);
          p.disconnect(ReasonCode.BELOW_THAN_ME);
        }
      });
      logger.info("Thread is running");
      return;
    }

    List<Node> tableNodes = TronNetService.getP2pService().getConnectableNodes();
    tableNodes.sort(Comparator.comparingLong(node -> -node.getUpdateTime()));
    Set<InetSocketAddress> usedAddressSet = new HashSet<>();
    tronNetDelegate.getActivePeer().forEach(p -> usedAddressSet.add(p.getInetSocketAddress()));
    Optional<Node> chosenNode = tableNodes.stream()
        .filter(node -> nodesCache.getIfPresent(node.getPreferInetSocketAddress()) == null)
        .filter(node -> !usedAddressSet.contains(node.getPreferInetSocketAddress()))
        .filter(node -> !TronNetService.getP2pConfig().getActiveNodes()
            .contains(node.getPreferInetSocketAddress()))
        .findFirst();
    if (!chosenNode.isPresent()) {
      logger.warn("No available node to choose");
      return;
    }

    count.incrementAndGet();
    nodesCache.put(chosenNode.get().getPreferInetSocketAddress(), true);
    cur = new InetSocketAddress(chosenNode.get().getPreferInetSocketAddress().getAddress(),
        chosenNode.get().getPreferInetSocketAddress().getPort());

    logger.info("Try to get effective connection by using {} at seq {}", cur, count.get());
    TronNetService.getP2pService().connect(chosenNode.get(), future -> {
      if (future.isCancelled()) {
        // Connection attempt cancelled by user
        cur = null;
      } else if (!future.isSuccess()) {
        // You might get a NullPointerException here because the future might not be completed yet.
        logger.warn("Connect to chosen peer {} fail, cause:{}", cur, future.cause().getMessage());
        future.channel().close();
        cur = null;
        triggerNext();
      } else {
        // Connection established successfully
      }
    });
  }

  private void resetCount() {
    count.set(0);
  }

  public void onDisconnect(InetSocketAddress inetSocketAddress) {
    if (inetSocketAddress.equals(cur)) {
      logger.warn("Close chosen peer: {}", cur);
      cur = null;
      triggerNext();
    }
  }
}
