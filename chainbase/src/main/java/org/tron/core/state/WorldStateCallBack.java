package org.tron.core.state;

import com.google.protobuf.Internal;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.Hash;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.state.trie.TrieImpl;

@Slf4j(topic = "State")
@Component
public class WorldStateCallBack extends WorldStateCallBackUtils {

  private BlockCapsule blockCapsule;
  private volatile TrieImpl trie;

  long cost = 0;

  @Setter
  private ChainBaseManager chainBaseManager;

  private LinkedBlockingQueue<TrieEntry> queue = new LinkedBlockingQueue<>();
  private boolean updateServiceRunning;

  private final Runnable updateService =
      () -> {
        while (updateServiceRunning) {
          TrieEntry trieEntry = null;
          try {
            trieEntry = queue.poll(10, TimeUnit.MILLISECONDS);
          } catch (InterruptedException e) {
            logger.error("state update failed, get trie entry failed, err: {}", e.getMessage());
            System.exit(-1);
          }
          if (trieEntry != null) {
            trie.put(Hash.encodeElement(trieEntry.getKey()), trieEntry.getData());
          }
        }
      };

  public WorldStateCallBack() {
    this.execute = true;
    this.allowGenerateRoot = CommonParameter.getInstance().getStorage().isAllowStateRoot();
    if (this.allowGenerateRoot) {
      this.updateServiceRunning = true;
      new Thread(updateService).start();
    }
  }

  public void preExeTrans() {
    trieEntryList.clear();
  }

  public void exeTransFinish() {
    long start = System.currentTimeMillis();
    queue.addAll(trieEntryList);
    trieEntryList.clear();
    cost += System.currentTimeMillis() - start;
  }

  public void preExecute(BlockCapsule blockCapsule, WorldStateTrieStore worldStateTrieStore) {
    cost = 0;
    this.blockCapsule = blockCapsule;
    this.execute = true;
    if (!exe()) {
      return;
    }
    byte[] rootHash = null;
    try {
      BlockCapsule parentBlockCapsule =
          chainBaseManager.getBlockById(blockCapsule.getParentBlockId());
      rootHash = parentBlockCapsule.getInstance().getStateRoot().toByteArray();
    } catch (Exception e) {
      logger.error("", e);
    }
    if (Arrays.equals(Internal.EMPTY_BYTE_ARRAY, rootHash)) {
      rootHash = Hash.EMPTY_TRIE_HASH;
    }
    trie = new TrieImpl(worldStateTrieStore, rootHash);
  }

  public void executePushFinish() {
    long start = System.currentTimeMillis();
    if (!exe()) {
      return;
    }
    // update state after processTx
    queue.addAll(trieEntryList);
    trieEntryList.clear();
    while (queue.size() != 0) {
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {
        logger.error("Fatal error, {}", e.getMessage());
      }
    }

    byte[] newRoot = trie.getRootHash();
    if (ArrayUtils.isEmpty(newRoot)) {
      newRoot = Hash.EMPTY_TRIE_HASH;
    }
    blockCapsule.setStateRoot(newRoot);
    execute = false;
    cost += System.currentTimeMillis() - start;
    logger.debug("state update, block: {}, cost: {}", blockCapsule.getBlockId().getString(), cost);
  }

  public void initGenesis(BlockCapsule blockCapsule, WorldStateTrieStore worldStateTrieStore) {
    if (!exe()) {
      return;
    }
    trie = new TrieImpl(worldStateTrieStore, Hash.EMPTY_TRIE_HASH);
    for (TrieEntry trieEntry : trieEntryList) {
      trie.put(Hash.encodeElement(trieEntry.getKey()), trieEntry.getData());
    }
    trieEntryList.clear();

    byte[] newRoot = trie.getRootHash();
    if (ArrayUtils.isEmpty(newRoot)) {
      newRoot = Hash.EMPTY_TRIE_HASH;
    }
    blockCapsule.setStateRoot(newRoot);
    execute = false;
  }

  /**
   * As this root can not be consensused now,
   * ignore this logic when generate block.
   */
//  public void executeGenerateFinish() {
//    if (!exe()) {
//      return;
//    }
//    //
//    byte[] newRoot = trie.getRootHash();
//    if (ArrayUtils.isEmpty(newRoot)) {
//      newRoot = Hash.EMPTY_TRIE_HASH;
//    }
//    blockCapsule.setStateRoot(newRoot);
//    execute = false;
//  }

  public void exceptionFinish() {
    execute = false;
  }

  public void stopUpdateService() {
    updateServiceRunning = false;
  }

}
