package org.tron.common.overlay.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.overlay.server.Channel.TronState;
import org.tron.core.config.args.Args;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j
@Service
public class PeerConnectionCheckService {

  public static final int HANDSHAKE_WAITING_TIME = 60000;

  private static final double disconnectNumberFactor = 0.3;
  private static final double maxConnectNumberFactor = 0.8;

  @Autowired
  private SyncPool pool;

  @Autowired
  private TcpFlowStats tcpFlowStats;

  private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2,
      r -> new Thread(r, "check-peer-connect"));

  @PostConstruct
  public void check() {
    logger.info("start the PeerConnectionCheckService");
    scheduledExecutorService
        .scheduleWithFixedDelay(new CheckHandshakeTask(), 60, 60, TimeUnit.SECONDS);
    scheduledExecutorService
        .scheduleWithFixedDelay(new CheckDataTransferTask(), 5, 5, TimeUnit.MINUTES);
  }

  private class CheckHandshakeTask implements Runnable {

    @Override
    public void run() {
      List<PeerConnection> peerConnectionList = pool.getActivePeers();
      for (PeerConnection peerConnection : peerConnectionList) {
        if (peerConnection.getStartTime() + HANDSHAKE_WAITING_TIME < System.currentTimeMillis()
            && peerConnection.getTronState() == TronState.INIT && !peerConnection.isActive()) {
          //if 60s not handshake,disconnect the peer
          logger.error("{} handshake timeout ,timeout time  is {}", peerConnection.getInetAddress(),
              HANDSHAKE_WAITING_TIME);
          peerConnection.disconnect(ReasonCode.TIME_OUT);
        }
      }
    }
  }

  private class CheckDataTransferTask implements Runnable {

    @Override
    public void run() {
      List<PeerConnection> peerConnectionList = pool.getActivePeers();
      List<Channel> willDisconnectPeerList = new ArrayList<>();
      for (PeerConnection peerConnection : peerConnectionList) {
        if (!tcpFlowStats.peerIsHaveDataTransfer(peerConnection)) {
          //&& !peerConnection.isActive()
          //if xxx minutes not have data transfer,disconnect the peer
          willDisconnectPeerList.add(peerConnection);
        }
        tcpFlowStats.resetPeerFlow(peerConnection);
      }
      if (!willDisconnectPeerList.isEmpty() && peerConnectionList.size()
          > Args.getInstance().getNodeMaxActiveNodes() * maxConnectNumberFactor) {
        Collections.shuffle(willDisconnectPeerList);
        for (int i = 0; i < willDisconnectPeerList.size() * disconnectNumberFactor; i++) {
          logger.error("{} not have data transfer, disconnect the peer",
              willDisconnectPeerList.get(i).getInetAddress());
          willDisconnectPeerList.get(i).disconnect(ReasonCode.TOO_MANY_PEERS);
          tcpFlowStats.resetPeerFlow(willDisconnectPeerList.get(i));
        }
      }
    }
  }

}
