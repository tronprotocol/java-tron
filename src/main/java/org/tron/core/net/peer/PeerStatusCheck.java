package org.tron.core.net.peer;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.net.TronProxy;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j
@Component
public class PeerStatusCheck {

  @Autowired
  private TronProxy tronProxy;

  private ScheduledExecutorService peerStatusCheckExecutor = Executors.newSingleThreadScheduledExecutor();

  private int blockUpdateTimeout = 20_000;

  public void init () {
    peerStatusCheckExecutor.scheduleWithFixedDelay(() -> {
      try {
        statusCheck();
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 5, 2, TimeUnit.SECONDS);
  }

  public void close () {
    peerStatusCheckExecutor.shutdown();
  }

  public void statusCheck() {

    long now = System.currentTimeMillis();

    tronProxy.getActivePeer().forEach(peer -> {

      final boolean[] isDisconnected = {false};

      if (peer.isNeedSyncFromPeer() && peer.getBlockBothHaveUpdateTime() < now - blockUpdateTimeout){
        logger.warn("Peer {} not sync for a long time.", peer.getInetAddress());
        isDisconnected[0] = true;
      }

      if (!isDisconnected[0]) {
        peer.getAdvInvSpread().values().stream()
            .filter(time -> time < now - NetConstants.ADV_TIME_OUT)
            .findFirst()
            .ifPresent(time -> isDisconnected[0] = true);
      }

      if (!isDisconnected[0]) {
        peer.getSyncBlockRequested().values().stream()
            .filter(time -> time < now - NetConstants.SYNC_TIME_OUT)
            .findFirst()
            .ifPresent(time -> isDisconnected[0] = true);
      }

      if (isDisconnected[0]) {
        peer.disconnect(ReasonCode.TIME_OUT);
      }
    });
  }

}
