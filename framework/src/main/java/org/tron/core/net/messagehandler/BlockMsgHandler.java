package org.tron.core.net.messagehandler;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.BLOCK_SIZE;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.args.Args;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.relay.RelayService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.core.services.WitnessProductBlockService;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Slf4j(topic = "net")
@Component
public class BlockMsgHandler implements TronMsgHandler {

  @Autowired
  private RelayService relayService;

  @Autowired
  private TronNetDelegate tronNetDelegate;

  @Autowired
  private AdvService advService;

  @Autowired
  private SyncService syncService;

  @Autowired
  private FetchBlockService fetchBlockService;

  @Autowired
  private WitnessProductBlockService witnessProductBlockService;

  private int maxBlockSize = BLOCK_SIZE + Constant.ONE_THOUSAND;

  private boolean fastForward = Args.getInstance().isFastForward();

  @Override
  public void processMessage(PeerConnection peer, TronMessage msg) throws P2pException {

    BlockMessage blockMessage = (BlockMessage) msg;
    BlockId blockId = blockMessage.getBlockId();

    if (!fastForward && !peer.isRelayPeer()) {
      check(peer, blockMessage);
    }

    if (peer.getSyncBlockRequested().containsKey(blockId)) {
      peer.getSyncBlockRequested().remove(blockId);
      syncService.processBlock(peer, blockMessage);
    } else {
      Item item = new Item(blockId, InventoryType.BLOCK);
      long now = System.currentTimeMillis();
      if (peer.isRelayPeer()) {
        peer.getAdvInvSpread().put(item, now);
      }
      Long time = peer.getAdvInvRequest().remove(item);
      if (null != time) {
        MetricsUtil.histogramUpdateUnCheck(MetricsKey.NET_LATENCY_FETCH_BLOCK
                + peer.getInetAddress(), now - time);
        Metrics.histogramObserve(MetricKeys.Histogram.BLOCK_FETCH_LATENCY,
            (now - time) / Metrics.MILLISECONDS_PER_SECOND);
      }
      Metrics.histogramObserve(MetricKeys.Histogram.BLOCK_RECEIVE_DELAY,
          (now - blockMessage.getBlockCapsule().getTimeStamp()) / Metrics.MILLISECONDS_PER_SECOND);
      fetchBlockService.blockFetchSuccess(blockId);
      long interval = blockId.getNum() - tronNetDelegate.getHeadBlockId().getNum();
      processBlock(peer, blockMessage.getBlockCapsule());
      logger.info(
              "Receive block/interval {}/{} from {} fetch/delay {}/{}ms, "
                      + "txs/process {}/{}ms, witness: {}",
              blockId.getNum(),
              interval,
              peer.getInetSocketAddress(),
              time == null ? 0 : now - time,
              now - blockMessage.getBlockCapsule().getTimeStamp(),
              ((BlockMessage) msg).getBlockCapsule().getTransactions().size(),
              System.currentTimeMillis() - now,
              Hex.toHexString(blockMessage.getBlockCapsule().getWitnessAddress().toByteArray()));
    }
  }

  private void check(PeerConnection peer, BlockMessage msg) throws P2pException {
    Item item = new Item(msg.getBlockId(), InventoryType.BLOCK);
    if (!peer.getSyncBlockRequested().containsKey(msg.getBlockId()) && !peer.getAdvInvRequest()
            .containsKey(item)) {
      logger.error("Receive bad block {} from peer {}, with no request",
              msg.getBlockId(), peer.getInetSocketAddress());
      throw new P2pException(TypeEnum.BAD_MESSAGE, "no request");
    }
    BlockCapsule blockCapsule = msg.getBlockCapsule();
    if (blockCapsule.getInstance().getSerializedSize() > maxBlockSize) {
      logger.error("Receive bad block {} from peer {}, block size over limit",
              msg.getBlockId(), peer.getInetSocketAddress());
      throw new P2pException(TypeEnum.BAD_MESSAGE, "block size over limit");
    }
    long gap = blockCapsule.getTimeStamp() - System.currentTimeMillis();
    if (gap >= BLOCK_PRODUCED_INTERVAL) {
      logger.error("Receive bad block {} from peer {}, block time error",
              msg.getBlockId(), peer.getInetSocketAddress());
      throw new P2pException(TypeEnum.BAD_MESSAGE, "block time error");
    }
  }

  private void processBlock(PeerConnection peer, BlockCapsule block) throws P2pException {
    BlockId blockId = block.getBlockId();
    if (!tronNetDelegate.containBlock(block.getParentBlockId())) {
      logger.warn("Get unlink block {} from {}, head is {}", blockId.getString(),
              peer.getInetAddress(), tronNetDelegate.getHeadBlockId().getString());
      syncService.startSync(peer);
      return;
    }

    long headNum = tronNetDelegate.getHeadBlockId().getNum();
    if (block.getNum() < headNum) {
      logger.warn("Receive a low block {}, head {}", blockId.getString(), headNum);
      return;
    }

    boolean flag = tronNetDelegate.validBlock(block);
    if (flag) {
      broadcast(new BlockMessage(block));
    }

    try {
      tronNetDelegate.processBlock(block, false);
      if (!flag) {
        broadcast(new BlockMessage(block));
      }

      witnessProductBlockService.validWitnessProductTwoBlock(block);

      tronNetDelegate.getActivePeer().forEach(p -> {
        if (p.getAdvInvReceive().getIfPresent(blockId) != null) {
          p.setBlockBothHave(blockId);
        }
      });
    } catch (Exception e) {
      logger.warn("Process adv block {} from peer {} failed. reason: {}",
              blockId, peer.getInetAddress(), e.getMessage());
    }
  }

  private void broadcast(BlockMessage blockMessage) {
    if (fastForward) {
      relayService.broadcast(blockMessage);
    } else {
      advService.broadcast(blockMessage);
    }
  }

}
