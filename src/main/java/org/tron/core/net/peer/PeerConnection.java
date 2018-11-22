package org.tron.core.net.peer;

import static org.tron.core.config.Parameter.NetConstants.MAX_INVENTORY_SIZE_IN_MINUTES;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.net.TronProxy;

@Slf4j
@Component
@Scope("prototype")
public class PeerConnection extends Channel {

  @Autowired
  private TronProxy tronProxy;

  @Autowired
  private PeerSync peerSync;

  @Autowired
  private PeerAdv peerAdv;

  @Setter
  @Getter
  private BlockId signUpErrorBlockId;

  @Setter
  @Getter
  private HelloMessage helloMessage;

  @Setter
  @Getter
  private Map<Item, Long> advInvReceive = new ConcurrentHashMap<>();

  @Setter
  @Getter
  private Map<Item, Long> advInvSpread = new ConcurrentHashMap<>();

  @Setter
  @Getter
  private Map<Item, Long> advInvRequest = new ConcurrentHashMap<>();

  @Getter
  private BlockId blockBothHave  = new BlockId();
  public void setBlockBothHave (BlockId blockId) {
    this.blockBothHave = blockId;
    this.blockBothHaveUpdateTime = System.currentTimeMillis();
  }

  @Getter
  private long blockBothHaveUpdateTime = System.currentTimeMillis();

  @Setter
  @Getter
  private BlockId lastSyncBlockId;

  @Setter
  @Getter
  private long remainNum;

  @Getter
  private Cache<Sha256Hash, Long> syncBlockIdCache = CacheBuilder.newBuilder()
      .maximumSize(2 * NodeConstant.SYNC_FETCH_BATCH_NUM).recordStats().build();

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
  private Set<BlockId> syncBlockInProcess = new HashSet<>();

  @Setter
  @Getter
  private boolean needSyncFromPeer;

  @Setter
  @Getter
  private boolean needSyncFromUs;

  public void cleanInvGarbage() {

    long time = Time.getCurrentMillis() - MAX_INVENTORY_SIZE_IN_MINUTES * 60 * 1000;

    Iterator<Entry<Item, Long>> iterator = this.advInvReceive.entrySet().iterator();

    removeIterator(iterator, time);

    iterator = this.advInvSpread.entrySet().iterator();

    removeIterator(iterator, time);
  }

  private void removeIterator(Iterator<Entry<Item, Long>> iterator, long oldestTimestamp) {
    while (iterator.hasNext()) {
      Map.Entry entry = iterator.next();
      Long ts = (Long) entry.getValue();
      if (ts < oldestTimestamp) {
        iterator.remove();
      }
    }
  }

  public boolean isIdle() {
    return advInvRequest.isEmpty() && syncBlockRequested.isEmpty() && syncChainRequested == null;
  }

  public void sendMessage(Message message) {
    msgQueue.sendMessage(message);
  }

  public void onConnectPeer() {
    if (getHelloMessage().getHeadBlockId().getNum() > tronProxy.getHeadBlockId().getNum()) {
      setTronState(TronState.SYNCING);
      peerSync.startSync(this);
    } else {
      setTronState(TronState.SYNC_COMPLETED);
    }
  }

  public void onDisconnectPeer() {
    peerSync.onDisconnect(this);
    peerAdv.onDisconnect(this);
  }

  @Override
  public String toString () {
    long now = System.currentTimeMillis();
    return String.format(
        "Peer %s: [ %18s, ping %6s ms]-----------\n"
            + "connect time: %d\n"
            + "last know block num: %s\n"
            + "needSyncFromPeer:%b\n"
            + "needSyncFromUs:%b\n"
            + "syncToFetchSize:%d\n"
            + "syncToFetchSizePeekNum:%d\n"
            + "syncBlockRequestedSize:%d\n"
            + "remainNum:%d\n"
            + "syncChainRequested:%d\n"
            + "blockInProcess:%d\n",
        this.getNode().getHost() + ":" + this.getNode().getPort(),
        this.getNode().getHexIdShort(),
        (int) this.getPeerStats().getAvgLatency(),
        (now - super.getStartTime()) / 1000,
        blockBothHave.getNum(),
        isNeedSyncFromPeer(),
        isNeedSyncFromUs(),
        syncBlockToFetch.size(),
        syncBlockToFetch.size() > 0 ? syncBlockToFetch.peek().getNum() : -1,
        syncBlockRequested.size(),
        remainNum,
        syncChainRequested == null ? 0 : (now - syncChainRequested.getValue()) / 1000,
        syncBlockInProcess.size())
        + nodeStatistics.toString() + "\n";
  }

}
