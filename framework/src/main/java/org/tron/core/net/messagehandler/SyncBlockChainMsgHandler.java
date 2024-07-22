package org.tron.core.net.messagehandler;

import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.message.sync.ChainInventoryMessage;
import org.tron.core.net.message.sync.SyncBlockChainMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol;

@Slf4j(topic = "net")
@Component
public class SyncBlockChainMsgHandler implements TronMsgHandler {

  @Autowired
  private TronNetDelegate tronNetDelegate;

  @Override
  public void processMessage(PeerConnection peer, TronMessage msg) throws P2pException {

    SyncBlockChainMessage syncBlockChainMessage = (SyncBlockChainMessage) msg;

    if (!check(peer, syncBlockChainMessage)) {
      peer.disconnect(Protocol.ReasonCode.BAD_PROTOCOL);
      return;
    }
    long remainNum = 0;

    List<BlockId> summaryChainIds = syncBlockChainMessage.getBlockIds();
    BlockId headID = tronNetDelegate.getHeadBlockId();
    LinkedList<BlockId> blockIds = getLostBlockIds(summaryChainIds, headID);

    if (blockIds.size() == 0) {
      logger.warn("Can't get lost block Ids");
      peer.disconnect(Protocol.ReasonCode.INCOMPATIBLE_CHAIN);
      return;
    } else if (blockIds.size() == 1) {
      peer.setNeedSyncFromUs(false);
    } else {
      peer.setNeedSyncFromUs(true);
      remainNum = headID.getNum() - blockIds.peekLast().getNum();
    }

    peer.setLastSyncBlockId(blockIds.peekLast());
    peer.setRemainNum(remainNum);
    peer.sendMessage(new ChainInventoryMessage(blockIds, remainNum));
  }

  private boolean check(PeerConnection peer, SyncBlockChainMessage msg) throws P2pException {
    List<BlockId> blockIds = msg.getBlockIds();
    if (CollectionUtils.isEmpty(blockIds)) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "SyncBlockChain blockIds is empty");
    }

    BlockId firstId = blockIds.get(0);
    if (!tronNetDelegate.containBlockInMainChain(firstId)) {
      logger.warn("Sync message from peer {} without the first block: {}",
              peer.getInetSocketAddress(), firstId.getString());
      return false;
    }

    long headNum = tronNetDelegate.getHeadBlockId().getNum();
    if (firstId.getNum() > headNum) {
      throw new P2pException(TypeEnum.BAD_MESSAGE,
          "First blockNum:" + firstId.getNum() + " gt my head BlockNum:" + headNum);
    }

    BlockId lastSyncBlockId = peer.getLastSyncBlockId();
    long lastNum = blockIds.get(blockIds.size() - 1).getNum();
    if (lastSyncBlockId != null && lastSyncBlockId.getNum() > lastNum) {
      throw new P2pException(TypeEnum.BAD_MESSAGE,
          "lastSyncNum:" + lastSyncBlockId.getNum() + " gt lastNum:" + lastNum);
    }

    return true;
  }

  private LinkedList<BlockId> getLostBlockIds(List<BlockId> blockIds, BlockId headID)
      throws P2pException {

    BlockId unForkId = getUnForkId(blockIds);
    LinkedList<BlockId> ids = getBlockIds(unForkId.getNum(), headID);

    if (ids.isEmpty() || !unForkId.equals(ids.peekFirst())) {
      unForkId = getUnForkId(blockIds);
      ids = getBlockIds(unForkId.getNum(), headID);
    }

    return ids;
  }

  private BlockId getUnForkId(List<BlockId> blockIds) throws P2pException {
    BlockId unForkId = null;
    for (int i = blockIds.size() - 1; i >= 0; i--) {
      if (tronNetDelegate.containBlockInMainChain(blockIds.get(i))) {
        unForkId = blockIds.get(i);
        break;
      }
    }

    if (unForkId == null) {
      throw new P2pException(TypeEnum.SYNC_FAILED, "unForkId is null");
    }

    return unForkId;
  }

  private LinkedList<BlockId> getBlockIds(Long unForkNum, BlockId headID) throws P2pException {
    long headNum = headID.getNum();

    long len = Math.min(headNum, unForkNum + NetConstants.SYNC_FETCH_BATCH_NUM);

    LinkedList<BlockId> ids = new LinkedList<>();
    for (long i = unForkNum; i <= len; i++) {
      if (i == headNum) {
        ids.add(headID);
      } else {
        BlockId id = tronNetDelegate.getBlockIdByNum(i);
        ids.add(id);
      }
    }
    return ids;
  }

}
