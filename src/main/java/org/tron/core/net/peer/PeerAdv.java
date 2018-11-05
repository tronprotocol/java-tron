package org.tron.core.net.peer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Slf4j
@Component
public class PeerAdv {

  @Getter
  private Cache<Sha256Hash, TransactionMessage> TrxCache = CacheBuilder.newBuilder()
      .maximumSize(50_000).expireAfterWrite(1, TimeUnit.HOURS).initialCapacity(50_000)
      .recordStats().build();

  private Cache<Sha256Hash, BlockMessage> BlockCache = CacheBuilder.newBuilder()
      .maximumSize(10).expireAfterWrite(60, TimeUnit.SECONDS)
      .recordStats().build();

  private ConcurrentHashMap<Sha256Hash, InventoryType> advObjToSpread = new ConcurrentHashMap<>();

  public void broadcast(Message msg) {
    InventoryType type;
    if (msg instanceof BlockMessage) {
      logger.info("Ready to broadcast block {}", ((BlockMessage) msg).getBlockId());
      //freshBlockId.offer(((BlockMessage) msg).getBlockId());
      BlockCache.put(msg.getMessageId(), (BlockMessage) msg);
      type = InventoryType.BLOCK;
    } else if (msg instanceof TransactionMessage) {
      TrxCache.put(msg.getMessageId(), (TransactionMessage) msg);
      type = InventoryType.TRX;
    } else {
      return;
    }
    synchronized (advObjToSpread) {
      advObjToSpread.put(msg.getMessageId(), type);
    }
  }




}
