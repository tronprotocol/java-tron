package org.tron.core.net.messagehandler;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.BLOCK_SIZE;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.common.utils.Sha256Hash;
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

    BlockCapsule blockCapsule = blockMessage.getBlockCapsule();
    if (blockCapsule.getInstance().getSerializedSize() > maxBlockSize) {
      logger.error("Receive bad block {} from peer {}, block size over limit",
          blockMessage.getBlockId(), peer.getInetSocketAddress());
      throw new P2pException(TypeEnum.BAD_MESSAGE, "block size over limit");
    }
    long gap = blockCapsule.getTimeStamp() - System.currentTimeMillis();
    if (gap >= BLOCK_PRODUCED_INTERVAL) {
      logger.error("Receive bad block {} from peer {}, block time error",
          blockMessage.getBlockId(), peer.getInetSocketAddress());
      throw new P2pException(TypeEnum.BAD_MESSAGE, "block time error");
    }
    if (!fastForward && !peer.isRelayPeer()) {
      check(peer, blockMessage);
    }

    if (blockCapsule.getNum() <= 0
        || blockCapsule.getParentHashStr().size() != Sha256Hash.LENGTH
        || new BlockId(blockCapsule.getParentHash()).getNum() != blockCapsule.getNum() - 1) {
      throw new P2pException(TypeEnum.BAD_BLOCK, "block number does not follow parent");
    }

    blockMessage.sanitize();

    if (peer.getSyncBlockRequested().containsKey(blockId)) {
      peer.getSyncBlockRequested().remove(blockId);
      peer.getSyncBlockInProcess().add(blockId);
      syncService.processBlock(peer, blockMessage);
    } else {
      Item item = new Item(blockId, InventoryType.BLOCK);
      long now = System.currentTimeMillis();
      if (peer.isRelayPeer()) {
        peer.getAdvInvSpread().put(item, now);
      }
      Long time = peer.getAdvInvRequest().get(item);
      long interval = blockId.getNum() - tronNetDelegate.getHeadBlockId().getNum();
      BlockResult result = processBlock(peer, blockMessage.getBlockCapsule());
      if (result == BlockResult.ACCEPTED || result == BlockResult.SYNC_REQUIRED) {
        peer.setBlockRcvTime(System.currentTimeMillis());
      }
      if (null != time) {
        MetricsUtil.histogramUpdateUnCheck(MetricsKey.NET_LATENCY_FETCH_BLOCK
                + peer.getInetAddress(), now - time);
        Metrics.histogramObserve(MetricKeys.Histogram.BLOCK_FETCH_LATENCY,
            (now - time) / Metrics.MILLISECONDS_PER_SECOND);
      }
      Metrics.histogramObserve(MetricKeys.Histogram.BLOCK_RECEIVE_DELAY,
          (now - blockMessage.getBlockCapsule().getTimeStamp()) / Metrics.MILLISECONDS_PER_SECOND);
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
  }

  private BlockResult processBlock(PeerConnection peer, BlockCapsule block) throws P2pException {
    BlockId blockId = block.getBlockId();
    boolean activeWitness = tronNetDelegate.validBlock(block);
    // Keep the request until validation succeeds so disconnect can retry invalid data.
    fetchBlockService.blockFetchSuccess(blockId);
    peer.getAdvInvRequest().remove(new Item(blockId, InventoryType.BLOCK));
    if (!activeWitness) {
      logger.warn("Receive a block from an inactive witness, peer {}, block {}, witness {}",
          peer.getInetSocketAddress(), blockId.getString(),
          Hex.toHexString(block.getWitnessAddress().toByteArray()));
      syncService.startSync(peer);
      return BlockResult.STATE_FAILED;
    }

    peer.updateLastInteractiveTime(System.currentTimeMillis());
    long headNum = tronNetDelegate.getHeadBlockId().getNum();
    if (block.getNum() < headNum || tronNetDelegate.containBlock(blockId)) {
      logger.warn("Receive a low block {}, head {}", blockId.getString(), headNum);
      return BlockResult.IGNORED;
    }

    if (!tronNetDelegate.containBlock(block.getParentBlockId())) {
      logger.warn("Get unlink block {} from {}, head is {}", blockId.getString(),
              peer.getInetAddress(), tronNetDelegate.getHeadBlockId().getString());
      syncService.startSync(peer);
      return BlockResult.SYNC_REQUIRED;
    }

    broadcast(new BlockMessage(block));

    try {
      tronNetDelegate.processBlock(block, false);
    } catch (Exception e) {
      logger.warn("Process adv block {} from peer {} failed. reason: {}",
              blockId, peer.getInetAddress(), e.getMessage());
      syncService.startSync(peer);
      return BlockResult.STATE_FAILED;
    }
    if (tronNetDelegate.isHitDown()) {
      return BlockResult.IGNORED;
    }
    advService.confirmBlockInventory(blockId);
    witnessProductBlockService.validWitnessProductTwoBlock(block);

    Item item = new Item(blockId, InventoryType.BLOCK);
    tronNetDelegate.getActivePeer().forEach(p -> {
      if (p.getAdvInvReceive().getIfPresent(item) != null) {
        p.setBlockBothHave(blockId);
      }
    });
    return BlockResult.ACCEPTED;
  }

  private enum BlockResult {
    ACCEPTED, SYNC_REQUIRED, IGNORED, STATE_FAILED
  }

  private void broadcast(BlockMessage blockMessage) {
    if (fastForward) {
      relayService.broadcast(blockMessage);
    } else {
      advService.broadcast(blockMessage);
    }
  }

}
