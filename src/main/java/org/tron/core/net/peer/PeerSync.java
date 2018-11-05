package org.tron.core.net.peer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.server.Channel.TronState;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.TronManager;
import org.tron.core.net.TronProxy;
import org.tron.core.net.message.SyncBlockChainMessage;

@Slf4j
@Component
public class PeerSync {

  @Autowired
  private TronProxy tronProxy;

  @Autowired
  private TronManager tronManager;

  public void startSync(PeerConnection peer) {
    peer.setTronState(TronState.SYNCING);
    peer.setNeedSyncFromPeer(true);
    peer.getSyncBlockToFetch().clear();
    peer.setRemainNum(0);
    peer.setHeadBlockWeBothHave(tronProxy.getGenesisBlockId());
    syncNext(peer);
  }

  public void syncNext(PeerConnection peer) {
    if (peer.getSyncChainRequested() != null) {
      logger.warn("Peer {} is in sync.", peer.getNode().getHost());
      return;
    }
    LinkedList<BlockId> chainSummary = getBlockChainSummary(peer);
    peer.setSyncChainRequested(new Pair<>(chainSummary, System.currentTimeMillis()));
    peer.sendMessage(new SyncBlockChainMessage(chainSummary));
  }

  private LinkedList<BlockId> getBlockChainSummary(PeerConnection peer) throws Exception {

    BlockId beginBlockId = peer.getHeadBlockWeBothHave();
    List<BlockId> blockIds = new ArrayList<>(peer.getSyncBlockToFetch());
    LinkedList<BlockId> forkList = new LinkedList<>();
    LinkedList<BlockId> retSummary = new LinkedList<>();
    long syncBeginNumber = tronProxy.getSyncBeginNumber();
    long low = syncBeginNumber < 0 ? 0 : syncBeginNumber;
    long highNoFork;
    long high;

    if (beginBlockId.getNum() == 0){
      highNoFork = high = tronProxy.getHeadBlockId().getNum();
    }else {
      if (tronProxy.containBlockInMainChain(beginBlockId)) {
        highNoFork = high = beginBlockId.getNum();
      } else {
        forkList = tronProxy.getBlockChainHashesOnFork(beginBlockId);
        if (forkList.isEmpty()) {
          throw new P2pException(TypeEnum.SYNC_FAILED, "can't find blockId: " + beginBlockId.getString());
        }
        highNoFork = forkList.peekLast().getNum();
        forkList.pollLast();
        Collections.reverse(forkList);
        high = highNoFork + forkList.size();
      }
    }

    logger.info("Get block chain summary, low: {}, highNoFork: {}, high: {}", low, highNoFork, high);

    tronManager.check(low <= highNoFork, "low gt highNoFork");

    long realHighBlkNum = high + blockIds.size();
    while (low <= realHighBlkNum) {
      if (low <= highNoFork) {
        retSummary.offer(tronProxy.getBlockIdByNum(low));
      } else if (low <= high) {
        retSummary.offer(forkList.get((int) (low - highNoFork - 1)));
      } else {
        retSummary.offer(blockIds.get((int) (low - high - 1)));
      }
      low += (realHighBlkNum - low + 2) / 2;
    }

    return retSummary;
  }

}
