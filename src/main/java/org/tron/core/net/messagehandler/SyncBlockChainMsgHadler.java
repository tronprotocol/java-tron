package org.tron.core.net.messagehandler;

import com.google.common.collect.Iterables;
import com.google.common.primitives.Longs;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.node.statistics.MessageCount;
import org.tron.common.overlay.server.Channel.TronState;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.exception.StoreException;
import org.tron.core.net.TronProxy;
import org.tron.core.net.message.ChainInventoryMessage;
import org.tron.core.net.message.FetchInvDataMessage;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.SyncBlockChainMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j
@Component
public class SyncBlockChainMsgHadler implements TronMsgHandler {

  @Autowired
  TronProxy TronProxy;

  private LinkedList<BlockId> getLostBlockIds(List<BlockId> blockIds) throws Exception{

    if (TronProxy.getHeadBlockId().getNum() == 0) {
      return new LinkedList<>();
    }

    BlockId unForkedBlockId;

    if (blockIds.isEmpty() || (blockIds.size() == 1 && blockIds.get(0).equals(TronProxy.getGenesisBlockId()))) {
      unForkedBlockId = TronProxy.getGenesisBlockId();
    } else if (blockIds.size() == 1 && blockIds.get(0).getNum() == 0) {
      return new LinkedList(Arrays.asList(TronProxy.getGenesisBlockId()));
    } else {
      Collections.reverse(blockIds);
      unForkedBlockId = blockIds.stream()
          .filter(blockId -> TronProxy.containBlockInMainChain(blockId))
          .findFirst().orElse(null);
      if (unForkedBlockId == null) {
        return new LinkedList<>();
      }
    }

    long unForkedBlockIdNum = unForkedBlockId.getNum();
    long len = Longs.min(TronProxy.getHeadBlockId().getNum(), unForkedBlockIdNum + NodeConstant.SYNC_FETCH_BATCH_NUM);

    LinkedList<BlockId> ids = new LinkedList<>();
    for (long i = unForkedBlockIdNum; i <= len; i++) {
      BlockId id = TronProxy.getBlockIdByNum(i);
      ids.add(id);
    }
    return ids;Integer.MAX_VALUE
  }

  private void checkSyncBlockChainMessage(PeerConnection peer, SyncBlockChainMessage msg) throws Exception{
    List<BlockId> blockIds = msg.getBlockIds();
    if (CollectionUtils.isEmpty(blockIds)){
      throw new P2pException(TypeEnum.BAD_MESSAGE, "SyncBlockChain blockIds is empty");
    }
    long lastBlockNum = blockIds.get(blockIds.size() - 1).getNum();
    BlockId lastSyncBlockId = peer.getLastSyncBlockId();
    if (lastSyncBlockId != null && lastBlockNum < lastSyncBlockId.getNum()) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "SyncBlockChain firstNum:" + lastBlockNum + ", lastSyncNum:" + lastBlockNum);
    }
  }

  public boolean processMessage(PeerConnection peer, TronMessage msg){

    SyncBlockChainMessage syncBlockChainMessage = (SyncBlockChainMessage) msg;

    peer.setTronState(TronState.SYNCING);
    BlockId headBlockId = TronProxy.getHeadBlockId();
    long remainNum = 0;
    LinkedList<BlockId> blockIds = new LinkedList<>();
    List<BlockId> summaryChainIds = syncMsg.getBlockIds();
    if (!checkSyncBlockChainMessage(peer, syncMsg)) {
      disconnectPeer(peer, ReasonCode.BAD_PROTOCOL);
      return;
    }
    try {
      blockIds = del.getLostBlockIds(summaryChainIds);
    } catch (StoreException e) {
      logger.error(e.getMessage());
    }

    if (blockIds.isEmpty()) {
      if (CollectionUtils.isNotEmpty(summaryChainIds) && !del
          .canChainRevoke(summaryChainIds.get(0).getNum())) {
        logger.info("Node sync block fail, disconnect peer {}, no block {}", peer,
            summaryChainIds.get(0).getString());
        peer.disconnect(ReasonCode.SYNC_FAIL);
        return;
      } else {
        peer.setNeedSyncFromUs(false);
      }
    } else if (blockIds.size() == 1
        && !summaryChainIds.isEmpty()
        && (summaryChainIds.contains(blockIds.peekFirst())
        || blockIds.peek().getNum() == 0)) {
      peer.setNeedSyncFromUs(false);
    } else {
      peer.setNeedSyncFromUs(true);
      remainNum = del.getHeadBlockId().getNum() - blockIds.peekLast().getNum();
    }

    if (!peer.isNeedSyncFromPeer()
        && CollectionUtils.isNotEmpty(summaryChainIds)
        && !del.contain(Iterables.getLast(summaryChainIds), MessageTypes.BLOCK)
        && del.canChainRevoke(summaryChainIds.get(0).getNum())) {
      startSyncWithPeer(peer);
    }

    if (blockIds.peekLast() == null) {
      peer.setLastSyncBlockId(headBlockId);
    } else {
      peer.setLastSyncBlockId(blockIds.peekLast());
    }
    peer.setRemainNum(remainNum);
    peer.sendMessage(new ChainInventoryMessage(blockIds, remainNum));
  }



}
