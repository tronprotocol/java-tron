package org.tron.common.overlay.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.core.config.args.Args;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol;

@Slf4j(topic = "net")
@Service
public class PeerConnectionCheckService {
  private int maxConnections = Args.getInstance().getMaxConnections();
  private boolean isFastForward = Args.getInstance().isFastForward();
  private boolean isOpenFullTcpDisconnect = Args.getInstance().isOpenFullTcpDisconnect();
  private ScheduledExecutorService poolLoopExecutor = Executors.newSingleThreadScheduledExecutor();

  @Autowired
  private SyncPool syncPool;

  public void init() {
    if (isFastForward || !isOpenFullTcpDisconnect) {
      return;
    }
    logger.info("Start peer connection check service.");
    poolLoopExecutor.scheduleWithFixedDelay(() -> {
      try {
        check();
      } catch (Throwable t) {
        logger.error("Exception in peer connection check.", t);
      }
    }, 10, 30, TimeUnit.SECONDS);
  }

  public void close() {
    logger.info("Close peer connection check service.");
    poolLoopExecutor.shutdown();
  }

  public void check() {
    if (syncPool.getActivePeers().size() < maxConnections) {
      return;
    }
    Collection<PeerConnection> peers = syncPool.getActivePeers().stream()
            .filter(peer -> peer.isIdle())
            .filter(peer -> !peer.isTrustPeer())
            .filter(peer -> !peer.isActive())
            .collect(Collectors.toList());
    if (peers.size() == 0) {
      return;
    }
    List<PeerConnection> list = new ArrayList();
    peers.forEach(p -> list.add(p));
    PeerConnection peer = list.get(new Random().nextInt(peers.size()));
    peer.disconnect(Protocol.ReasonCode.RANDOM_ELIMINATION);
  }
}
