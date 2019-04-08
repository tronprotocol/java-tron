package org.tron.core.net.peer;

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
import java.util.concurrent.TimeUnit;
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
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.service.AdvService;
import org.tron.core.net.service.SyncService;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class PeerConnection extends Channel {

  @Autowired
  private TronNetDelegate tronNetDelegate;

  @Autowired
  private SyncService syncService;

  @Autowired
  private AdvService advService;

  private int invCacheSize = 100_000;

  @Setter
  @Getter
  private BlockId signUpErrorBlockId;

  @Setter
  @Getter
  private HelloMessage helloMessage;

  @Setter
  @Getter
  private Cache<Item, Long> advInvReceive = CacheBuilder.newBuilder().maximumSize(invCacheSize)
      .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

  @Setter
  @Getter
  private Cache<Item, Long> advInvSpread = CacheBuilder.newBuilder().maximumSize(invCacheSize)
      .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

  @Setter
  @Getter
  private Map<Item, Long> advInvRequest = new ConcurrentHashMap<>();

  @Getter
  private BlockId blockBothHave = new BlockId();

  public void setBlockBothHave(BlockId blockId) {
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

  public boolean isIdle() {
    return advInvRequest.isEmpty() && syncBlockRequested.isEmpty() && syncChainRequested == null;
  }

  public void sendMessage(Message message) {
    msgQueue.sendMessage(message);
  }

  public void onConnect() {
    if (getHelloMessage().getHeadBlockId().getNum() > tronNetDelegate.getHeadBlockId().getNum()) {
      setTronState(TronState.SYNCING);
      syncService.startSync(this);
    } else {
      setTronState(TronState.SYNC_COMPLETED);
    }
  }

  public void onDisconnect() {
    syncService.onDisconnect(this);
    advService.onDisconnect(this);
    advInvReceive.cleanUp();
    advInvSpread.cleanUp();
    advInvRequest.clear();
    syncBlockIdCache.cleanUp();
    syncBlockToFetch.clear();
    syncBlockRequested.clear();
    syncBlockInProcess.clear();
    syncBlockInProcess.clear();
  }

  public String log() {
    long now = System.currentTimeMillis();
//    logger.info("Peer {}:{} [ {}, ping {} ms]-----------\n"
//            + "connect time: {}\n"
//            + "last know block num: {}\n"
//            + "needSyncFromPeer:{}\n"
//            + "needSyncFromUs:{}\n"
//            + "syncToFetchSize:{}\n"
//            + "syncToFetchSizePeekNum:{}\n"
//            + "syncBlockRequestedSize:{}\n"
//            + "remainNum:{}\n"
//            + "syncChainRequested:{}\n"
//            + "blockInProcess:{}\n"
//            + "{}",
//        this.getNode().getHost(), this.getNode().getPort(), this.getNode().getHexIdShort(),
//        (int) this.getPeerStats().getAvgLatency(),
//        (now - super.getStartTime()) / 1000,
//        blockBothHave.getNum(),
//        isNeedSyncFromPeer(),
//        isNeedSyncFromUs(),
//        syncBlockToFetch.size(),
//        syncBlockToFetch.size() > 0 ? syncBlockToFetch.peek().getNum() : -1,
//        syncBlockRequested.size(),
//        remainNum,
//        syncChainRequested == null ? 0 : (now - syncChainRequested.getValue()) / 1000,
//        syncBlockInProcess.size(),
//        nodeStatistics.toString());
////
    return String.format(
        "Peer %s: [ %18s, ping %6s ms]-----------\n"
            + "connect time: %ds\n"
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
