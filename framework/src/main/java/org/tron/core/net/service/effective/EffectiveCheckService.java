package org.tron.core.net.service.effective;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelFutureListener;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.TronNetService;
import org.tron.core.net.peer.PeerConnection;
import org.tron.p2p.discover.Node;
import org.tron.p2p.utils.NetUtil;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class EffectiveCheckService {

  @Getter
  private final boolean isEffectiveCheck = CommonParameter.getInstance()
      .isNodeEffectiveCheckEnable();
  @Autowired
  private TronNetDelegate tronNetDelegate;

  private final Cache<InetSocketAddress, Boolean> nodesCache = CacheBuilder.newBuilder()
      .initialCapacity(100)
      .maximumSize(1000)
      .expireAfterWrite(10, TimeUnit.MINUTES).build();
  @Getter
  private InetSocketAddress cur;
  private final AtomicInteger count = new AtomicInteger(0);
  @Setter
  private boolean found = false;
  private ScheduledExecutorService executor = null;

  public void init() {
    if (isEffectiveCheck) {
      executor = Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("effective-thread-%d").build());
      executor.scheduleWithFixedDelay(() -> {
        try {
          findEffectiveNode();
        } catch (Exception e) {
          logger.error("Check effective connection processing failed", e);
        }
      }, 1 * 60, 5, TimeUnit.SECONDS);
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

  public boolean haveEffectiveConnection() {
    return (int) tronNetDelegate.getActivePeer().stream()
        .filter(PeerConnection::isNeedSyncFromUs)
        .count() != tronNetDelegate.getActivePeer().size();
  }

  private synchronized void findEffectiveNode() throws InterruptedException {
    if (haveEffectiveConnection()) {
      resetCount();
      return;
    }
    if (found) {
      Thread.sleep(10_000);//wait found node to sync
      if (!haveEffectiveConnection()) {
        found = false;
        disconnect();
      }
      return;
    }

    if (cur != null && tronNetDelegate.getActivePeer().stream()
        .anyMatch(p -> p.getInetSocketAddress().equals(cur))) {
      // we encounter no effective connection again, so we disconnect with last used node
      disconnect();
      return;
    }

    List<Node> tableNodes = TronNetService.getP2pService().getAllNodes();
    for (Node node : tableNodes) {
      if (node.getId() == null) {
        node.setId(NetUtil.getNodeId());
      }
    }

    Optional<Node> chosenNode = tableNodes.stream()
        .filter(node -> nodesCache.getIfPresent(node.getPreferInetSocketAddress()) == null)
        .filter(node -> !TronNetService.getP2pConfig().getActiveNodes()
            .contains(node.getPreferInetSocketAddress()))
        .findFirst();
    if (!chosenNode.isPresent()) {
      logger.warn("Failed to find effective node, have tried {} times", count.get());
      resetCount();
      return;
    }

    count.incrementAndGet();
    nodesCache.put(chosenNode.get().getPreferInetSocketAddress(), true);
    cur = new InetSocketAddress(chosenNode.get().getPreferInetSocketAddress().getAddress(),
        chosenNode.get().getPreferInetSocketAddress().getPort());

    logger.info("Try to get effective connection by using {} at times {}", cur, count.get());
    TronNetService.getP2pService().connect(chosenNode.get(), future -> {
      if (future.isCancelled()) {
        // Connection attempt cancelled by user
        logger.warn("Channel {} has been cancelled by user", cur);
      } else if (!future.isSuccess()) {
        // You might get a NullPointerException here because the future might not be completed yet.
        logger.warn("Connect to chosen peer {} fail, cause:{}", cur, future.cause().getMessage());
        future.channel().close();

        findEffectiveNode();
      } else {
        // Connection established successfully
        future.channel().closeFuture().addListener((ChannelFutureListener) closeFuture -> {
          logger.info("Close chosen channel:{}", cur);
          if (!haveEffectiveConnection()) {
            findEffectiveNode();
          }
        });
      }
    });
  }

  public void resetCount() {
    count.set(0);
  }

  public int getCount() {
    return count.get();
  }

  private void disconnect() {
    tronNetDelegate.getActivePeer().forEach(p -> {
      if (p.getInetSocketAddress().equals(cur)) {
        p.disconnect(ReasonCode.BELOW_THAN_ME);
      }
    });
  }
}
