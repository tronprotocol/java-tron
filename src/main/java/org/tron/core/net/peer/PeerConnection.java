package org.tron.core.net.peer;

import static org.tron.core.config.Parameter.NetConstants.MAX_INVENTORY_SIZE_IN_MINUTES;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.server.Channel;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Time;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.net.TronProxy;

@Slf4j
@Component
@Scope("prototype")
public class PeerConnection extends Channel {

  @Autowired
  private TronProxy tronProxy;

  @Setter
  @Getter
  private BlockId lastSyncBlockId;

  @Setter
  @Getter
  private long remainNum;

  @Setter
  @Getter
  private long lastBlockUpdateTime = System.currentTimeMillis();

  @Setter
  private HelloMessage helloMessage;

  @Setter
  @Getter
  private Queue<Sha256Hash> invToUs = new LinkedBlockingQueue<>();

  @Setter
  @Getter
  private Queue<Sha256Hash> invWeAdv = new LinkedBlockingQueue<>();

  @Setter
  @Getter
  private Map<Sha256Hash, Long> advObjSpreadToUs = new ConcurrentHashMap<>();

  @Setter
  @Getter
  private Map<Sha256Hash, Long> advObjWeSpread = new ConcurrentHashMap<>();

  @Setter
  @Getter
  private Map<Item, Long> advObjWeRequested = new ConcurrentHashMap<>();

  @Setter
  @Getter
  private BlockId headBlockWeBothHave = new BlockId();

  @Setter
  @Getter
  private Deque<BlockId> syncBlockToFetch = new ConcurrentLinkedDeque<>();

  @Setter
  @Getter
  private Map<BlockId, Long> syncBlockRequested = new ConcurrentHashMap<>();

  @Setter
  @Getter
  private Pair<Deque<BlockId>, Long> syncChainRequested = null;

  @Setter
  @Getter
  private long unfetchSyncNum = 0L;

  @Setter
  @Getter
  private boolean needSyncFromPeer;

  @Setter
  @Getter
  private boolean needSyncFromUs;

  @Setter
  @Getter
  private Set<BlockId> blockInProc = new HashSet<>();

  public void cleanInvGarbage() {

    long time = Time.getCurrentMillis() - MAX_INVENTORY_SIZE_IN_MINUTES * 60 * 1000;

    Iterator<Entry<Sha256Hash, Long>> iterator = this.advObjSpreadToUs.entrySet().iterator();

    removeIterator(iterator, time);

    iterator = this.advObjWeSpread.entrySet().iterator();

    removeIterator(iterator, time);
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
