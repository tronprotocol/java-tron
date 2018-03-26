package org.tron.core.net.peer;

import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.server.Channel;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
@Scope("prototype")
public class PeerConnection extends Channel{

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  //broadcast
  @Getter
  @Setter
  private Queue<Sha256Hash> invToUs = new LinkedBlockingQueue<>();

  @Getter
  @Setter
  private Queue<Sha256Hash> invWeAdv = new LinkedBlockingQueue<>();

  @Getter
  @Setter
  private HashMap<Sha256Hash, Long> advObjSpreadToUs = new HashMap<>();

  @Getter
  @Setter
  private HashMap<Sha256Hash, Long> advObjWeSpread = new HashMap<>();

  @Getter
  @Setter
  private HashMap<Sha256Hash, Long> advObjWeRequested = new HashMap<>();

  //sync chain
  @Getter
  @Setter
  private BlockId headBlockWeBothHave;

  @Getter
  @Setter
  private long headBlockTimeWeBothHave;

  @Getter
  private Deque<BlockId> syncBlockToFetch = new LinkedList<>();

  @Getter
  @Setter
  private HashMap<BlockId, Long> syncBlockRequested = new HashMap<>();

  @Getter
  @Setter
  private Pair<LinkedList<BlockId>, Long> syncChainRequested = null;

  @Getter
  @Setter
  private long unfetchSyncNum = 0L;

  @Getter
  @Setter
  private boolean needSyncFromPeer;

  @Getter
  @Setter
  private boolean needSyncFromUs;

  @Getter
  @Setter
  private boolean banned;

  @Getter
  @Setter
  private Set<BlockId> blockInProc = new HashSet<>();

  public void cleanInvGarbage() {
    //TODO: clean advObjSpreadToUs and advObjWeSpread accroding cleaning strategy
  }

  public String logSyncStats() {
    //TODO: return tron sync status here.
//    int waitResp = lastReqSentTime > 0 ? (int) (System.currentTimeMillis() - lastReqSentTime) / 1000 : 0;
//    long lifeTime = System.currentTimeMillis() - connectedTime;
    return String.format(
        "Peer %s: [ %18s, ping %6s ms]-----------\n"
            + "last know block num: %s\n "
            + "needSyncFromPeer:%b\n "
            + "needSyncFromUs:%b\n"
            + "syncToFetchSize:%d\n"
            + "syncBlockRequestedSize:%d\n"
            + "unFetchSynNum:%d\n"
            + "blockInPorc:%d\n",
        this.getNode().getHost() + ":" + this.getNode().getPort(),
        this.getPeerIdShort(),
        (int)this.getPeerStats().getAvgLatency(),
        headBlockWeBothHave.getNum(),
        isNeedSyncFromPeer(),
        isNeedSyncFromUs(),
        syncBlockToFetch.size(),
        syncBlockRequested.size(),
        unfetchSyncNum,
        blockInProc.size());
  }

  public boolean isBusy() {
    return !advObjWeRequested.isEmpty()
        && !syncBlockRequested.isEmpty()
        && syncChainRequested != null;
  }

  public void sendMessage(Message message) {
    logger.info("nodeimpl send message" + message);
    msgQueue.sendMessage(message);
    nodeStatistics.ethOutbound.add();
  }

  @Override
  public String toString() {
    return super.toString();// nodeStatistics.toString();
  }
}
