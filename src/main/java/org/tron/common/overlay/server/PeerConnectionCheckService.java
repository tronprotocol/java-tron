package org.tron.common.overlay.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.core.config.args.Args;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j
@Service
public class PeerConnectionCheckService {
  
  private double disconnectNumberFactor = Args.getInstance().getDisconnectNumberFactor();
  private double maxConnectNumberFactor = Args.getInstance().getMaxConnectNumberFactor();

  @Autowired
  private SyncPool pool;

  @Autowired
  private TcpFlowStats tcpFlowStats;

  private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1,
      r -> new Thread(r, "check-peer-connect"));

  @PostConstruct
  public void check() {
    logger.info("start the PeerConnectionCheckService");
    scheduledExecutorService
        .scheduleWithFixedDelay(new CheckDataTransferTask(), 5, 5, TimeUnit.MINUTES);
  }

  @PreDestroy
  public void destroy() {
    scheduledExecutorService.shutdown();
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
