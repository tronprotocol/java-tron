package org.tron.core.net.service.effective;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class ResilienceService {

  private static final long inactiveThreshold =
      CommonParameter.getInstance().getInactiveThreshold() * 1000L;
  public static final long blockNotChangeThreshold = 90 * 1000L;

  //when node is isolated, retention percent peers will not be disconnected
  public static final double retentionPercent = 0.8;
  private static final int initialDelay = 300;
  private static final String esName = "resilience-service";
  private final ScheduledExecutorService executor = ExecutorServiceManager
      .newSingleThreadScheduledExecutor(esName);

  @Autowired
  private TronNetDelegate tronNetDelegate;

  @Autowired
  private ChainBaseManager chainBaseManager;

  public void init() {
    if (Args.getInstance().isOpenFullTcpDisconnect) {
      executor.scheduleWithFixedDelay(() -> {
        try {
          disconnectRandom();
        } catch (Exception e) {
          logger.error("DisconnectRandom node failed", e);
        }
      }, initialDelay, 60, TimeUnit.SECONDS);
    } else {
      logger.info("OpenFullTcpDisconnect is disabled");
    }

    executor.scheduleWithFixedDelay(() -> {
      try {
        disconnectLan();
      } catch (Exception e) {
        logger.error("DisconnectLan node failed", e);
      }
    }, initialDelay, 10, TimeUnit.SECONDS);

    executor.scheduleWithFixedDelay(() -> {
      try {
        disconnectIsolated2();
      } catch (Exception e) {
        logger.error("DisconnectIsolated node failed", e);
      }
    }, initialDelay, 30, TimeUnit.SECONDS);
  }

  private void disconnectRandom() {
    int peerSize = tronNetDelegate.getActivePeer().size();
    if (peerSize >= CommonParameter.getInstance().getMaxConnections()) {
      long now = System.currentTimeMillis();
      List<PeerConnection> peers = tronNetDelegate.getActivePeer().stream()
          .filter(peer -> !peer.isDisconnect())
          .filter(peer -> now - peer.getLastActiveTime() >= inactiveThreshold)
          .filter(peer -> !peer.getChannel().isTrustPeer())
          .collect(Collectors.toList());
      if (!peers.isEmpty()) {
        int index = new Random().nextInt(peers.size());
        disconnectFromPeer(peers.get(index), ReasonCode.RANDOM_ELIMINATION);
      }
    }
  }

  private void disconnectLan() {
    if (isLanNode()) {
      // disconnect from the node that has keep inactive for more than inactiveThreshold
      // and its lastActiveTime is smallest
      int peerSize = tronNetDelegate.getActivePeer().size();
      if (peerSize >= CommonParameter.getInstance().getMinConnections()) {
        long now = System.currentTimeMillis();
        Optional<PeerConnection> one = tronNetDelegate.getActivePeer().stream()
            .filter(peer -> !peer.isDisconnect())
            .filter(peer -> now - peer.getLastActiveTime() >= inactiveThreshold)
            .filter(peer -> !peer.getChannel().isTrustPeer())
            .min(Comparator.comparing(PeerConnection::getLastActiveTime, Long::compareTo));

        one.ifPresent(peer -> disconnectFromPeer(peer, ReasonCode.BAD_PROTOCOL));
      }
    }
  }

  private void disconnectIsolated2() {
    if (isIsolateLand2()) {
      logger.info("Node is isolated, try to disconnect from peers");
      int peerSize = tronNetDelegate.getActivePeer().size();

      //disconnect from the node whose lastActiveTime is smallest
      if (peerSize >= CommonParameter.getInstance().getMinActiveConnections()) {
        Optional<PeerConnection> one = tronNetDelegate.getActivePeer().stream()
            .filter(peer -> !peer.isDisconnect())
            .filter(peer -> !peer.getChannel().isTrustPeer())
            .filter(peer -> peer.getChannel().isActive())
            .min(Comparator.comparing(PeerConnection::getLastActiveTime, Long::compareTo));

        one.ifPresent(peer -> disconnectFromPeer(peer, ReasonCode.BAD_PROTOCOL));
      }

      //disconnect from some passive nodes, make sure retention nodes' num <= 0.8 * maxConnection
      peerSize = tronNetDelegate.getActivePeer().size();
      int threshold = (int) (CommonParameter.getInstance().getMaxConnections() * retentionPercent);
      if (peerSize > threshold) {
        int disconnectSize = peerSize - threshold;
        List<PeerConnection> peers = tronNetDelegate.getActivePeer().stream()
            .filter(peer -> !peer.isDisconnect())
            .filter(peer -> !peer.getChannel().isTrustPeer())
            .filter(peer -> !peer.getChannel().isActive())
            .sorted(Comparator.comparing(PeerConnection::getLastActiveTime, Long::compareTo))
            .collect(Collectors.toList());

        if (peers.size() > disconnectSize) {
          peers = peers.subList(0, disconnectSize);
        }
        peers.forEach(peer -> disconnectFromPeer(peer, ReasonCode.BAD_PROTOCOL));
      }
    }
  }

  private boolean isLanNode() {
    int peerSize = tronNetDelegate.getActivePeer().size();
    int activePeerSize = (int) tronNetDelegate.getActivePeer().stream()
        .filter(peer -> peer.getChannel().isActive())
        .count();
    return peerSize > 0 && peerSize == activePeerSize;
  }

  private boolean isIsolateLand2() {
    int advPeerCount = (int) tronNetDelegate.getActivePeer().stream()
        .filter(peer -> !peer.isNeedSyncFromPeer() && !peer.isNeedSyncFromUs())
        .count();
    long diff = System.currentTimeMillis() - chainBaseManager.getLatestSaveBlockTime();
    return advPeerCount >= 1 && diff >= blockNotChangeThreshold;
  }

  private void disconnectFromPeer(PeerConnection peer, ReasonCode reasonCode) {
    int inactiveSeconds = (int) ((System.currentTimeMillis() - peer.getLastActiveTime()) / 1000);
    logger.info("Disconnect from peer {}, inactive seconds {}", peer.getInetSocketAddress(),
        inactiveSeconds);
    peer.disconnect(reasonCode);
  }

  public void close() {
    ExecutorServiceManager.shutdownAndAwaitTermination(executor, esName);
  }
}
