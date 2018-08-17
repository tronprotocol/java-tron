package org.tron.core.net.peer;

import static org.tron.core.config.Parameter.NetConstants.MAX_INVENTORY_SIZE_IN_MINUTES;
import static org.tron.core.config.Parameter.NetConstants.NET_MAX_TRX_PER_SECOND;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.server.Channel;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Time;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.net.node.Item;

@Slf4j
@Component
@Scope("prototype")
public class PeerConnection extends Channel {

  @Getter
  private Cache<Sha256Hash, Integer> syncBlockIdCache = CacheBuilder.newBuilder().maximumSize(2 * NodeConstant.SYNC_FETCH_BATCH_NUM).build();

  @Setter
  @Getter
  private BlockId lastSyncBlockId;

  @Setter
  @Getter
  private long remainNum;

  private volatile boolean syncFlag = true;

  private HelloMessage helloMessage;

  //broadcast
  private Queue<Sha256Hash> invToUs = new LinkedBlockingQueue<>();

  private Queue<Sha256Hash> invWeAdv = new LinkedBlockingQueue<>();

  private Map<Sha256Hash, Long> advObjSpreadToUs = new ConcurrentHashMap<>();

  private Map<Sha256Hash, Long> advObjWeSpread = new ConcurrentHashMap<>();

  private Map<Item, Long> advObjWeRequested = new ConcurrentHashMap<>();

  private boolean advInhibit = false;

  public Map<Sha256Hash, Long> getAdvObjSpreadToUs() {
    return advObjSpreadToUs;
  }

  public Map<Sha256Hash, Long> getAdvObjWeSpread() {
    return advObjWeSpread;
  }

  public boolean isAdvInhibit() {
    return advInhibit;
  }

  public void setAdvInhibit(boolean advInhibit) {
    this.advInhibit = advInhibit;
  }

  //sync chain
  private BlockId headBlockWeBothHave = new BlockId();

  private long headBlockTimeWeBothHave;

  private Deque<BlockId> syncBlockToFetch = new ConcurrentLinkedDeque<>();

  private Map<BlockId, Long> syncBlockRequested = new ConcurrentHashMap<>();

  private Pair<Deque<BlockId>, Long> syncChainRequested = null;

  public Pair<Deque<BlockId>, Long> getSyncChainRequested() {
    return syncChainRequested;
  }

  public Cache<Sha256Hash, Integer> getSyncBlockIdCache(){
    return syncBlockIdCache;
  }

  public void setSyncChainRequested(
      Pair<Deque<BlockId>, Long> syncChainRequested) {
    this.syncChainRequested = syncChainRequested;
  }

  public Map<BlockId, Long> getSyncBlockRequested() {
    return syncBlockRequested;
  }

  public void setSyncBlockRequested(ConcurrentHashMap<BlockId, Long> syncBlockRequested) {
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

  public Map<Item, Long> getAdvObjWeRequested() {
    return advObjWeRequested;
  }

  public void setAdvObjWeRequested(ConcurrentHashMap<Item, Long> advObjWeRequested) {
    this.advObjWeRequested = advObjWeRequested;
  }

  public void setHelloMessage(HelloMessage helloMessage) {
    this.helloMessage = helloMessage;
  }

  public HelloMessage getHelloMessage() {
    return this.helloMessage;
  }

  public void cleanAll() {
    setStartTime(0);
    getSyncBlockToFetch().clear();
    getSyncBlockRequested().clear();
    getBlockInProc().clear();
    getAdvObjWeRequested().clear();
    getAdvObjWeSpread().clear();
    getAdvObjSpreadToUs().clear();
    getInvToUs().clear();
    getInvWeAdv().clear();
    getSyncBlockIdCache().cleanUp();
  }

  public void cleanInvGarbage() {
    long oldestTimestamp =
        Time.getCurrentMillis() - MAX_INVENTORY_SIZE_IN_MINUTES * 60 * 1000;

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

  public boolean isAdvInvFull() {
   return advObjSpreadToUs.size() > MAX_INVENTORY_SIZE_IN_MINUTES * 60 * NET_MAX_TRX_PER_SECOND;
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

  public boolean getSyncFlag() {
    return syncFlag;
  }

  public void setSyncFlag(boolean syncFlag) {
    this.syncFlag = syncFlag;
  }

  public String logSyncStats() {
    return String.format(
        "Peer %s: [ %18s, ping %6s ms]-----------\n"
            + "connect time: %s\n"
            + "last know block num: %s\n"
            + "needSyncFromPeer:%b\n"
            + "needSyncFromUs:%b\n"
            + "syncToFetchSize:%d\n"
            + "syncToFetchSizePeekNum:%d\n"
            + "syncBlockRequestedSize:%d\n"
            + "unFetchSynNum:%d\n"
            + "syncChainRequested:%s\n"
            + "blockInPorc:%d\n",
        this.getNode().getHost() + ":" + this.getNode().getPort(),
        this.getNode().getHexIdShort(),
        (int) this.getPeerStats().getAvgLatency(),
        Time.getTimeString(super.getStartTime()),
        headBlockWeBothHave.getNum(),
        isNeedSyncFromPeer(),
        isNeedSyncFromUs(),
        syncBlockToFetch.size(),
        syncBlockToFetch.size() > 0 ? syncBlockToFetch.peek().getNum() : -1,
        syncBlockRequested.size(),
        unfetchSyncNum,
        syncChainRequested == null ? "NULL" : Time.getTimeString(syncChainRequested.getValue()),
        blockInProc.size())
        + nodeStatistics.toString() + "\n";
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
  }
}
