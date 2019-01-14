package org.tron.core.net.messagehandler;

import com.google.common.collect.Iterables;
import com.google.common.primitives.Longs;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.spongycastle.util.encoders.Hex;
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
  private TronProxy tronProxy;

  @Override
  public void processMessage(PeerConnection peer, TronMessage msg) throws P2pException {

    SyncBlockChainMessage syncBlockChainMessage = (SyncBlockChainMessage) msg;

    check(peer, syncBlockChainMessage);

    long remainNum = 0;

    List<BlockId> summaryChainIds = syncBlockChainMessage.getBlockIds();

    LinkedList<BlockId> blockIds = getLostBlockIds(summaryChainIds);

    if (blockIds.size() == 1){
      peer.setNeedSyncFromUs(false);
    }else {
      peer.setNeedSyncFromUs(true);
      remainNum = tronProxy.getHeadBlockId().getNum() - blockIds.peekLast().getNum();
    }
//
//    if (!peer.isNeedSyncFromPeer()
//        && !tronProxy.contain(Iterables.getLast(summaryChainIds), MessageTypes.BLOCK)
//        && tronProxy.canChainRevoke(summaryChainIds.get(0).getNum())) {
//      //startSyncWithPeer(peer);
//    }

    peer.setLastSyncBlockId(blockIds.peekLast());
    peer.setRemainNum(remainNum);
    peer.sendMessage(new ChainInventoryMessage(blockIds, remainNum));
  }

  private void check(PeerConnection peer, SyncBlockChainMessage msg) throws P2pException {
    List<BlockId> blockIds = msg.getBlockIds();
    if (CollectionUtils.isEmpty(blockIds)){
      throw new P2pException(TypeEnum.BAD_MESSAGE, "SyncBlockChain blockIds is empty");
    }

    BlockId firstId = blockIds.get(0);
    if (!tronProxy.containBlockInMainChain(firstId)){
      throw new P2pException(TypeEnum.BAD_MESSAGE, "No first block:" + firstId.getString());
    }

    long headNum = tronProxy.getHeadBlockId().getNum();
    if (firstId.getNum() > headNum){
      throw new P2pException(TypeEnum.BAD_MESSAGE, "First blockNum:" + firstId.getNum() +" gt my head BlockNum:" + headNum);
    }

    BlockId lastSyncBlockId = peer.getLastSyncBlockId();
    long lastNum = blockIds.get(blockIds.size() - 1).getNum();
    if (lastSyncBlockId != null && lastSyncBlockId.getNum() > lastNum) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "lastSyncNum:" + lastSyncBlockId.getNum() + " gt lastNum:" + lastNum);
    }
  }

  private LinkedList<BlockId> getLostBlockIds(List<BlockId> blockIds) throws P2pException {

    BlockId unForkId = null;
    for (int i = blockIds.size() - 1; i >= 0; i--){
      if (tronProxy.containBlockInMainChain(blockIds.get(i))){
        unForkId = blockIds.get(i);
        break;
      }
    }

    long len = Math.min(tronProxy.getHeadBlockId().getNum(), unForkId.getNum() + NodeConstant.SYNC_FETCH_BATCH_NUM);

    LinkedList<BlockId> ids = new LinkedList<>();
    for (long i = unForkId.getNum(); i <= len; i++) {
      BlockId id = tronProxy.getBlockIdByNum(i);
      ids.add(id);
    }
    return ids;
  }

}
