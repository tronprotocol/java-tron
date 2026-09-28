package org.tron.core.net.service.fetchblock;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Slf4j(topic = "net")
@Component
public class FetchBlockService {

  @Autowired
  private TronNetDelegate tronNetDelegate;

  @Autowired
  private ChainBaseManager chainBaseManager;

  private volatile FetchBlockInfo fetchBlockInfo;

  private final long fetchTimeOut = CommonParameter.getInstance().fetchBlockTimeout;

  private static final double BLOCK_FETCH_LEFT_TIME_PERCENT = 0.5;

  private final String esName = "fetch-block";

  private final ScheduledExecutorService fetchBlockWorkerExecutor =
      ExecutorServiceManager.newSingleThreadScheduledExecutor(esName);

  public void init() {
    fetchBlockWorkerExecutor.scheduleWithFixedDelay(() -> {
      try {
        fetchBlockProcess(fetchBlockInfo);
      } catch (Exception e) {
        logger.error("FetchBlockWorkerSchedule thread error", e);
      }
    }, 0L, 50L, TimeUnit.MILLISECONDS);
  }

  public void close() {
    ExecutorServiceManager.shutdownAndAwaitTermination(fetchBlockWorkerExecutor, esName);
  }

  public synchronized void fetchBlock(List<Sha256Hash> sha256HashList, PeerConnection peer) {
    if (!sha256HashList.isEmpty()) {
      logger.info("Begin fetch block {} from {}",
          new BlockCapsule.BlockId(sha256HashList.get(0)).getString(),
          peer.getInetAddress());
    }
    long headNum = chainBaseManager.getHeadBlockNum();
    if (fetchBlockInfo != null
        && new BlockCapsule.BlockId(fetchBlockInfo.getHash()).getNum() > headNum) {
      return;
    }
    fetchBlockInfo = null;
    sha256HashList.stream()
        .filter(sha256Hash -> new BlockCapsule.BlockId(sha256Hash).getNum() == headNum + 1)
        .findFirst()
        .ifPresent(sha256Hash -> {
          Long requestTime = peer.getAdvInvRequest().get(new Item(sha256Hash, InventoryType.BLOCK));
          if (requestTime != null) {
            fetchBlockInfo = new FetchBlockInfo(sha256Hash, peer, requestTime);
            logger.info("Set fetchBlockInfo, block: {}, peer: {}, time: {}", sha256Hash,
                peer.getInetAddress(), requestTime);
          }
        });
  }

  public synchronized void blockFetchSuccess(Sha256Hash sha256Hash) {
    FetchBlockInfo fetchBlockInfoTemp = this.fetchBlockInfo;
    if (null == fetchBlockInfoTemp || !fetchBlockInfoTemp.getHash().equals(sha256Hash)) {
      return;
    }
    logger.info("Fetch block success, {}", new BlockCapsule.BlockId(sha256Hash).getString());
    this.fetchBlockInfo = null;
  }

  public synchronized void onDisconnect(PeerConnection peer) {
    if (fetchBlockInfo != null && fetchBlockInfo.getPeer().equals(peer)) {
      fetchBlockInfo = null;
    }
  }

  public boolean canFetchBlock(PeerConnection peer, Item item, long now) {
    if (peer.isDisconnect() || peer.isNeedSyncFromPeer() || peer.isNeedSyncFromUs()
        || !peer.isBlockFetchIdle()) {
      return false;
    }
    Long received = peer.getAdvInvReceive().getIfPresent(item);
    return received != null && received >= now - NetConstants.ADV_TIME_OUT;
  }

  private synchronized void fetchBlockProcess(FetchBlockInfo fetchBlock) {
    if (fetchBlock == null || fetchBlockInfo != fetchBlock) {
      return;
    }
    if (new BlockCapsule.BlockId(fetchBlock.getHash()).getNum()
        <= chainBaseManager.getHeadBlockNum()) {
      fetchBlockInfo = null;
      return;
    }
    long now = System.currentTimeMillis();
    if (now - fetchBlock.getTime() >= NetConstants.ADV_TIME_OUT) {
      // PeerStatusCheck owns the final deadline and disconnects the responsible provider.
      return;
    }
    Item item = new Item(fetchBlock.getHash(), InventoryType.BLOCK);
    Optional<PeerConnection> optionalPeerConnection = tronNetDelegate.getActivePeer().stream()
        .filter(peer -> canFetchBlock(peer, item, now))
        .min(Comparator.comparingDouble(this::getPeerTop75));

    if (optionalPeerConnection.isPresent()) {
      optionalPeerConnection.ifPresent(firstPeer -> {
        if (shouldFetchBlock(firstPeer, fetchBlock)
            && firstPeer.checkAndPutAdvInvRequest(item, now)) {
          this.fetchBlockInfo = new FetchBlockInfo(item.getHash(), firstPeer, now);
          firstPeer.sendMessage(new FetchInvDataMessage(Collections.singletonList(item.getHash()),
              item.getType()));
        }
      });
    }
  }

  private boolean shouldFetchBlock(PeerConnection newPeer, FetchBlockInfo fetchBlock) {
    double newPeerTop75 = getPeerTop75(newPeer);
    double oldPeerTop75 = getPeerTop75(fetchBlock.getPeer());
    long oldPeerSpendTime = System.currentTimeMillis() - fetchBlock.getTime();
    if (oldPeerTop75 > fetchTimeOut || oldPeerSpendTime >= fetchTimeOut) {
      return true;
    }

    double oldPeerLeftTime = oldPeerTop75 - oldPeerSpendTime;
    return newPeerTop75 < oldPeerLeftTime * BLOCK_FETCH_LEFT_TIME_PERCENT
        && oldPeerSpendTime + newPeerTop75 < fetchTimeOut;
  }

  private double getPeerTop75(PeerConnection peerConnection) {
    return MetricsUtil.getHistogram(MetricsKey.NET_LATENCY_FETCH_BLOCK
        + peerConnection.getInetAddress()).getSnapshot().get75thPercentile();
  }

  private static class FetchBlockInfo {

    @Getter
    private final PeerConnection peer;

    @Getter
    private final Sha256Hash hash;

    @Getter
    private final long time;

    public FetchBlockInfo(Sha256Hash hash, PeerConnection peer, long time) {
      this.peer = peer;
      this.hash = hash;
      this.time = time;
    }
  }
}
