package org.tron.core.net.service.fetchblock;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.Parameter;
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

  private FetchBlockInfo fetchBlockInfo = null;

  private final long fetchTimeOut = CommonParameter.getInstance().fetchBlockTimeout;

  private static final int BLOCK_FETCH_TIME_OUT_LIMIT =
      2 * Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

  private static final double BLOCK_FETCH_LEFT_TIME_PERCENT = 0.5;

  private final ScheduledExecutorService fetchBlockWorkerExecutor =
      new ScheduledThreadPoolExecutor(1,
          new BasicThreadFactory.Builder().namingPattern("FetchBlockWorkerSchedule-").build());

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
    fetchBlockWorkerExecutor.shutdown();
  }

  public void fetchBlock(List<Sha256Hash> sha256HashList, PeerConnection peer) {
    if (sha256HashList.size() > 0) {
      logger.info("Begin fetch block {} from {}",
          new BlockCapsule.BlockId(sha256HashList.get(0)).getString(),
          peer.getInetAddress());
    }
    if (null != fetchBlockInfo) {
      return;
    }
    sha256HashList.stream().filter(sha256Hash -> new BlockCapsule.BlockId(sha256Hash).getNum()
        == chainBaseManager.getHeadBlockNum() + 1)
        .findFirst().ifPresent(sha256Hash -> {
          if (System.currentTimeMillis() - chainBaseManager.getHeadBlockTimeStamp()
              < BLOCK_FETCH_TIME_OUT_LIMIT) {
            fetchBlockInfo = new FetchBlockInfo(sha256Hash, peer, System.currentTimeMillis());
            logger.info("Set fetchBlockInfo, block: {}, peer: {}, time: {}", sha256Hash,
                fetchBlockInfo.getPeer().getInetAddress(), fetchBlockInfo.getTime());
          }
        });
  }


  public void blockFetchSuccess(Sha256Hash sha256Hash) {
    logger.info("Fetch block success, {}", new BlockCapsule.BlockId(sha256Hash).getString());
    FetchBlockInfo fetchBlockInfoTemp = this.fetchBlockInfo;
    if (null == fetchBlockInfoTemp || !fetchBlockInfoTemp.getHash().equals(sha256Hash)) {
      return;
    }
    this.fetchBlockInfo = null;
  }

  private void fetchBlockProcess(FetchBlockInfo fetchBlock) {
    if (null == fetchBlock) {
      return;
    }
    if (System.currentTimeMillis() - chainBaseManager.getHeadBlockTimeStamp()
        >= BLOCK_FETCH_TIME_OUT_LIMIT) {
      this.fetchBlockInfo = null;
      logger.info("Clear fetchBlockInfo due to {} ms past head block time",
              BLOCK_FETCH_TIME_OUT_LIMIT);
      return;
    }
    Item item = new Item(fetchBlock.getHash(), InventoryType.BLOCK);
    Optional<PeerConnection> optionalPeerConnection = tronNetDelegate.getActivePeer().stream()
        .filter(PeerConnection::isIdle)
        .filter(filterPeer -> !filterPeer.equals(fetchBlock.getPeer()))
        .filter(filterPeer -> filterPeer.getAdvInvReceive().getIfPresent(item) != null)
        .filter(filterPeer -> getPeerTop75(filterPeer)
            <= CommonParameter.getInstance().fetchBlockTimeout)
        .min(Comparator.comparingDouble(this::getPeerTop75));

    if (optionalPeerConnection.isPresent()) {
      optionalPeerConnection.ifPresent(firstPeer -> {
        if (shouldFetchBlock(firstPeer, fetchBlock)) {
          firstPeer.getAdvInvRequest().put(item, System.currentTimeMillis());
          firstPeer.sendMessage(new FetchInvDataMessage(Collections.singletonList(item.getHash()),
              item.getType()));
          this.fetchBlockInfo = null;
        }
      });
    } else {
      if (System.currentTimeMillis() - fetchBlock.getTime() >= fetchTimeOut) {
        logger.info("Clear fetchBlockInfo due to fetch block {} timeout {}ms",
                fetchBlock.getHash(), fetchTimeOut);
        this.fetchBlockInfo = null;
      }
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
    @Setter
    private PeerConnection peer;

    @Getter
    @Setter
    private Sha256Hash hash;

    @Getter
    @Setter
    private long time;

    public FetchBlockInfo(Sha256Hash hash, PeerConnection peer, long time) {
      this.peer = peer;
      this.hash = hash;
      this.time = time;
    }

  }

}