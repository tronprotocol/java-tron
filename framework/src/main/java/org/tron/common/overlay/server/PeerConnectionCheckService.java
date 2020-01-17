package org.tron.common.overlay.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.overlay.discover.node.statistics.NodeStatistics;
import org.tron.common.utils.CollectionUtils;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Service
public class PeerConnectionCheckService {

  public static final long CHECK_TIME = 5 * 60 * 1000L;
  private double disconnectNumberFactor = Args.getInstance().getDisconnectNumberFactor();
  private double maxConnectNumberFactor = Args.getInstance().getMaxConnectNumberFactor();

  @Autowired
  private SyncPool pool;

  @Autowired
  private ChannelManager channelManager;

  @Autowired
  private Manager manager;

  private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2,
      r -> new Thread(r, "check-peer-connect"));

  @PostConstruct
  public void check() {
    logger.info("start the PeerConnectionCheckService");
    scheduledExecutorService
        .scheduleWithFixedDelay(new CheckDataTransferTask(), 5, 5, TimeUnit.MINUTES);
    if (Args.getInstance().isOpenFullTcpDisconnect()) {
      scheduledExecutorService
          .scheduleWithFixedDelay(new CheckConnectNumberTask(), 4, 1, TimeUnit.MINUTES);
    }
  }

  @PreDestroy
  public void destroy() {
    scheduledExecutorService.shutdown();
  }

  private class CheckDataTransferTask implements Runnable {

    @Override
    public void run() {
      List<PeerConnection> peerConnectionList = pool.getActivePeers();
      List<PeerConnection> willDisconnectPeerList = new ArrayList<>();
      for (PeerConnection peerConnection : peerConnectionList) {
        NodeStatistics nodeStatistics = peerConnection.getNodeStatistics();
        if (!nodeStatistics.nodeIsHaveDataTransfer()
            && System.currentTimeMillis() - peerConnection.getStartTime() >= CHECK_TIME
            && !peerConnection.isTrustPeer()
            && !nodeStatistics.isPredefined()) {
          //if xxx minutes not have data transfer,disconnect the peer,
          //exclude trust peer and active peer
          willDisconnectPeerList.add(peerConnection);
        }
        nodeStatistics.resetTcpFlow();
      }
      if (!willDisconnectPeerList.isEmpty() && peerConnectionList.size()
          > Args.getInstance().getNodeMaxActiveNodes() * maxConnectNumberFactor) {
        Collections.shuffle(willDisconnectPeerList);
        for (int i = 0; i < willDisconnectPeerList.size() * disconnectNumberFactor; i++) {
          logger.error("{} does not have data transfer, disconnect the peer",
              willDisconnectPeerList.get(i).getInetAddress());
          willDisconnectPeerList.get(i).disconnect(ReasonCode.TOO_MANY_PEERS);
        }
      }
    }
  }

  private class CheckConnectNumberTask implements Runnable {

    @Override
    public void run() {
      if (pool.getActivePeers().size() >= Args.getInstance().getNodeMaxActiveNodes()) {
        logger.warn("connection pool is full");
        List<PeerConnection> peerList = new ArrayList<>();
        for (PeerConnection peer : pool.getActivePeers()) {
          if (!peer.isTrustPeer() && !peer.getNodeStatistics().isPredefined()) {
            peerList.add(peer);
          }
        }
        if (peerList.size() >= 2) {
          peerList.sort(
              Comparator.comparingInt((PeerConnection o) -> o.getNodeStatistics().getReputation()));
          peerList = CollectionUtils.truncateRandom(peerList, 2, 1);
        }
        for (PeerConnection peerConnection : peerList) {
          logger.warn("connection pool is full, disconnect the peer: {}",
              peerConnection.getInetAddress());
          peerConnection.disconnect(ReasonCode.RESET);
        }
      }
    }
  }

}