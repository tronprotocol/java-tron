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
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.TronNetService;
import org.tron.core.net.peer.PeerConnection;
import org.tron.p2p.discover.Node;
import org.tron.p2p.utils.NetUtil;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class EffectiveService {

  @Autowired
  private TronNetDelegate tronNetDelegate;

  private final Cache<InetSocketAddress, Boolean> nodesCache = CacheBuilder.newBuilder()
      .initialCapacity(100)
      .maximumSize(1000)
      .expireAfterWrite(10, TimeUnit.MINUTES).build();
  @Getter
  private InetSocketAddress cur = null;
  private final AtomicInteger tryCount = new AtomicInteger(0);
  @Setter
  private boolean found = false;

  private final ScheduledExecutorService executor = Executors
      .newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("effective-thread-%d").build());

  public void init() {
    executor.scheduleWithFixedDelay(() -> {
      try {
        findEffectiveNode();
      } catch (Exception e) {
        logger.error("Check effective connection processing failed", e);
      }
    }, 1 * 60, 5, TimeUnit.SECONDS);
  }

  public void close() {
    try {
      executor.shutdownNow();
    } catch (Exception e) {
      logger.error("Exception in shutdown effective service worker, {}", e.getMessage());
    }
  }

  public boolean isIsolateLand() {
    int count = (int) tronNetDelegate.getActivePeer().stream()
        .filter(PeerConnection::isNeedSyncFromUs)
        .count();
    return count == tronNetDelegate.getActivePeer().size();
  }

  private synchronized void findEffectiveNode() throws InterruptedException {
    if (!isIsolateLand()) {
      resetCount();
      return;
    }
    if (found) {
      Thread.sleep(10_000);//wait found node to sync
      if (isIsolateLand()) {
        found = false;
        disconnect();
      }
      return;
    }

    //hashcode of PeerConnection = hashcode of InetSocketAddress
    if (cur != null && tronNetDelegate.getActivePeer().contains(cur)) {
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
      logger.warn("Failed to find effective node, have tried {} times", tryCount.get());
      resetCount();
      return;
    }

    tryCount.incrementAndGet();
    nodesCache.put(chosenNode.get().getPreferInetSocketAddress(), true);
    cur = new InetSocketAddress(chosenNode.get().getPreferInetSocketAddress().getAddress(),
        chosenNode.get().getPreferInetSocketAddress().getPort());

    logger.info("Try to get effective connection by using {} at times {}", cur,
        tryCount.get());
    TronNetService.getP2pService().connect(chosenNode.get(), future -> {
      if (future.isCancelled()) {
        // Connection attempt cancelled by user
      } else if (!future.isSuccess()) {
        // You might get a NullPointerException here because the future might not be completed yet.
        logger.warn("Connect to chosen peer {} fail, cause:{}",
            chosenNode.get().getPreferInetSocketAddress(), future.cause().getMessage());
        future.channel().close();

        findEffectiveNode();
      } else {
        // Connection established successfully
        future.channel().closeFuture().addListener((ChannelFutureListener) closeFuture -> {
          logger.info("Close chosen channel:{}", chosenNode.get().getPreferInetSocketAddress());
          if (isIsolateLand()) {
            findEffectiveNode();
          }
        });
      }
    });
  }

  public void resetCount() {
    tryCount.set(0);
  }

  public int getCount() {
    return tryCount.get();
  }

  private void disconnect() {
    tronNetDelegate.getActivePeer().forEach(p -> {
      if (p.getInetSocketAddress().equals(cur)) {
        p.disconnect(ReasonCode.UNKNOWN);
      }
    });
  }
}
