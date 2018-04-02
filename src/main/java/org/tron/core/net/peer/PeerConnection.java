package org.tron.core.net.peer;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.server.Channel;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Time;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;

@Slf4j
@Component
@Scope("prototype")
public class PeerConnection extends Channel {

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  public long getConnectTime() {
    return connectTime;
  }

  public void setConnectTime(long connectTime) {
    this.connectTime = connectTime;
  }

  private long connectTime;

  //broadcast
  private Queue<Sha256Hash> invToUs = new LinkedBlockingQueue<>();

  private Queue<Sha256Hash> invWeAdv = new LinkedBlockingQueue<>();

  private Map<Sha256Hash, Long> advObjSpreadToUs = new ConcurrentHashMap<>();

  private Map<Sha256Hash, Long> advObjWeSpread = new ConcurrentHashMap<>();

  private HashMap<Sha256Hash, Long> advObjWeRequested = new HashMap<>();

  public Map<Sha256Hash, Long> getAdvObjSpreadToUs() {
    return advObjSpreadToUs;
  }

  public void setAdvObjSpreadToUs(
      HashMap<Sha256Hash, Long> advObjSpreadToUs) {
    this.advObjSpreadToUs = advObjSpreadToUs;
  }

  public Map<Sha256Hash, Long> getAdvObjWeSpread() {
    return advObjWeSpread;
  }

  public void setAdvObjWeSpread(HashMap<Sha256Hash, Long> advObjWeSpread) {
    this.advObjWeSpread = advObjWeSpread;
  }

  //sync chain
  private BlockId headBlockWeBothHave = new BlockId();

  private long headBlockTimeWeBothHave;

  private Deque<BlockId> syncBlockToFetch = new ConcurrentLinkedDeque<>();

  private HashMap<BlockId, Long> syncBlockRequested = new HashMap<>();

  private Pair<Deque<BlockId>, Long> syncChainRequested = null;

  public Pair<Deque<BlockId>, Long> getSyncChainRequested() {
    return syncChainRequested;
  }

  public void setSyncChainRequested(
      Pair<Deque<BlockId>, Long> syncChainRequested) {
    this.syncChainRequested = syncChainRequested;
  }

  public HashMap<BlockId, Long> getSyncBlockRequested() {
    return syncBlockRequested;
  }

  public void setSyncBlockRequested(
      HashMap<BlockId, Long> syncBlockRequested) {
    this.syncBlockRequested = syncBlockRequested;
  }

  public long getUnfetchSyncNum() {
    return unfetchSyncNum;
  }

  public void setUnfetchSyncNum(long unfetchSyncNum) {
    this.unfetchSyncNum = unfetchSyncNum;
  }

  private long unfetchSyncNum = 0L;

  private boolean needSyncFromPeer;

  private boolean needSyncFromUs;

  public Set<BlockId> getBlockInProc() {
    return blockInProc;
  }

  public void setBlockInProc(Set<BlockId> blockInProc) {
    this.blockInProc = blockInProc;
  }

  private boolean banned;

  private Set<BlockId> blockInProc = new HashSet<>();

  public HashMap<Sha256Hash, Long> getAdvObjWeRequested() {
    return advObjWeRequested;
  }

  public void setAdvObjWeRequested(HashMap<Sha256Hash, Long> advObjWeRequested) {
    this.advObjWeRequested = advObjWeRequested;
  }


  public void cleanInvGarbage() {
    long oldestTimestamp =
        Time.getCurrentMillis() - NetConstants.MAX_INVENTORY_SIZE_IN_MINUTES * 60 * 1000;

    Iterator<Entry<Sha256Hash, Long>> iterator = this.advObjSpreadToUs.entrySet().iterator();

    removeIterator(iterator, oldestTimestamp);

    iterator = this.advObjWeSpread.entrySet().iterator();

    removeIterator(iterator, oldestTimestamp);
  }

  private void removeIterator(Iterator<Entry<Sha256Hash, Long>> iterator, long oldestTimestamp) {
    while (iterator.hasNext()) {
      Map.Entry entry = iterator.next();
      Long ts = (Long) entry.getValue();

      if (ts < oldestTimestamp) {
        iterator.remove();
      }
    }
  }

  public boolean isBanned() {
    return banned;
  }

  public void setBanned(boolean banned) {
    this.banned = banned;
  }

  public BlockId getHeadBlockWeBothHave() {
    return headBlockWeBothHave;
  }

  public void setHeadBlockWeBothHave(BlockId headBlockWeBothHave) {
    this.headBlockWeBothHave = headBlockWeBothHave;
  }

  public long getHeadBlockTimeWeBothHave() {
    return headBlockTimeWeBothHave;
  }

  public void setHeadBlockTimeWeBothHave(long headBlockTimeWeBothHave) {
    this.headBlockTimeWeBothHave = headBlockTimeWeBothHave;
  }

  public Deque<BlockId> getSyncBlockToFetch() {
    return syncBlockToFetch;
  }

  public boolean isNeedSyncFromPeer() {
    return needSyncFromPeer;
  }

  public void setNeedSyncFromPeer(boolean needSyncFromPeer) {
    this.needSyncFromPeer = needSyncFromPeer;
  }

  public boolean isNeedSyncFromUs() {
    return needSyncFromUs;
  }

  public void setNeedSyncFromUs(boolean needSyncFromUs) {
    this.needSyncFromUs = needSyncFromUs;
  }

  public Queue<Sha256Hash> getInvToUs() {
    return invToUs;
  }

  public void setInvToUs(Queue<Sha256Hash> invToUs) {
    this.invToUs = invToUs;
  }

  public Queue<Sha256Hash> getInvWeAdv() {
    return invWeAdv;
  }

  public void setInvWeAdv(Queue<Sha256Hash> invWeAdv) {
    this.invWeAdv = invWeAdv;
  }

  public String logSyncStats() {
    //TODO: return tron sync status here.
//    int waitResp = lastReqSentTime > 0 ? (int) (System.currentTimeMillis() - lastReqSentTime) / 1000 : 0;
//    long lifeTime = System.currentTimeMillis() - connectedTime;
    return String.format(
        "Peer %s: [ %18s, ping %6s ms]-----------\n"
            + "connect time: %s\n"
            + "last know block num: %s\n "
            + "needSyncFromPeer:%b\n "
            + "needSyncFromUs:%b\n"
            + "syncToFetchSize:%d\n"
            + "syncToFetchSizePeekNum:%d\n"
            + "syncBlockRequestedSize:%d\n"
            + "unFetchSynNum:%d\n"
            + "syncChainRequested:%s\n"
            + "blockInPorc:%d\n",
        this.getNode().getHost() + ":" + this.getNode().getPort(),
        this.getPeerIdShort(),
        (int) this.getPeerStats().getAvgLatency(),
        Time.getTimeString(getConnectTime()),
        headBlockWeBothHave.getNum(),
        isNeedSyncFromPeer(),
        isNeedSyncFromUs(),
        syncBlockToFetch.size(),
        syncBlockToFetch.size() > 0 ? syncBlockToFetch.peek().getNum() : -1,
        syncBlockRequested.size(),
        unfetchSyncNum,
        syncChainRequested == null ? "NULL" : Time.getTimeString(syncChainRequested.getValue()),
        blockInProc.size());
  }

  public boolean isBusy() {
    return !idle();
  }

  public boolean idle() {
    return advObjWeRequested.isEmpty()
        && syncBlockRequested.isEmpty()
        && syncChainRequested == null;
  }

  public void sendMessage(Message message) {
    msgQueue.sendMessage(message);
    nodeStatistics.ethOutbound.add();
  }

  @Override
  public String toString() {
    return super.toString();// nodeStatistics.toString();
  }
}
