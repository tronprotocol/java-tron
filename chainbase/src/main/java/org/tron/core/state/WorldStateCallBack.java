package org.tron.core.state;

import com.google.protobuf.Internal;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.Hash;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;

@Slf4j(topic = "State")
@Component
public class WorldStateCallBack extends WorldStateCallBackUtils {

  private BlockCapsule blockCapsule;
  private volatile MerkleStorage merkleStorage;
  private volatile MerklePatriciaTrie<Bytes, Bytes> trie;

  long cost = 0;

  @Setter
  private ChainBaseManager chainBaseManager;

  private LinkedBlockingQueue<TrieEntry> queue = new LinkedBlockingQueue<>();
  ExecutorService executorService = Executors.newSingleThreadExecutor();

  private final Runnable updateService =
      () -> {
        while (true) {
          TrieEntry trieEntry;
          trieEntry = queue.peek();
          if (trieEntry == null) {
            try {
              Thread.sleep(5);
              continue;
            } catch (InterruptedException e) {
              logger.error("state update failed, get trie entry failed, err: {}", e.getMessage());
              System.exit(-1);
            }
          }

          try {
            if (trieEntry.getData() != null && trieEntry.getData().length != 0) {
              trie.put(Bytes.wrap(trieEntry.getKey()), Bytes.wrap(trieEntry.getData()));
            }
          } catch (Exception e) {
            logger.error("state update failed, put trie entry failed, err: {}", e.getMessage());
            System.exit(-1);
          }
          queue.poll();
        }
      };

  public WorldStateCallBack() {
    this.execute = true;
    this.allowGenerateRoot = CommonParameter.getInstance().getStorage().isAllowStateRoot();
    if (this.allowGenerateRoot) {
      executorService.submit(updateService);
    }
  }

  public void init(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
    this.merkleStorage = chainBaseManager.getMptStoreV2().getMerkleStorage();
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
      rootHash = parentBlockCapsule.getInstance().getBlockHeader()
          .getArchiveStateRoot().toByteArray();
    } catch (Exception e) {
      logger.error("", e);
    }
    if (Arrays.equals(Internal.EMPTY_BYTE_ARRAY, rootHash)) {
      rootHash = Hash.EMPTY_TRIE_HASH;
    }

//    merkleStorage = new KeyValueMerkleStorage(chainBaseManager.getMptStoreV2().getKeyValueStore());
    trie = chainBaseManager.getMptStoreV2().getTrie(rootHash);
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
        Thread.sleep(10);
      } catch (InterruptedException e) {
        logger.error("Fatal error, {}", e.getMessage());
      }
    }

    trie.commit(merkleStorage::put);
    byte[] newRoot = trie.getRootHash().toArray();
    merkleStorage.commit();
    if (ArrayUtils.isEmpty(newRoot)) {
      newRoot = Hash.EMPTY_TRIE_HASH;
    }
    blockCapsule.setArchiveStateRoot(newRoot);
    execute = false;
    cost += System.currentTimeMillis() - start;
    logger.info("state update, block: {}, cost: {}", blockCapsule.getBlockId().getString(), cost);
    logger.debug("trie delete total count: {}", deleteCount);
  }

  public void initGenesis(BlockCapsule blockCapsule, WorldStateTrieStore worldStateTrieStore) {
    if (!exe()) {
      return;
    }
//    merkleStorage = new KeyValueMerkleStorage(chainBaseManager.getMptStoreV2().getKeyValueStore());
    trie = chainBaseManager.getMptStoreV2().getTrie(Hash.EMPTY_TRIE_HASH);
    for (TrieEntry trieEntry : trieEntryList) {
      trie.put(Bytes.wrap(trieEntry.getKey()), Bytes.wrap(trieEntry.getData()));
    }
    trieEntryList.clear();

    trie.commit(merkleStorage::put);
    byte[] newRoot = trie.getRootHash().toArray();
    merkleStorage.commit();
    if (ArrayUtils.isEmpty(newRoot)) {
      newRoot = Hash.EMPTY_TRIE_HASH;
    }
    blockCapsule.setArchiveStateRoot(newRoot);
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
    executorService.shutdown();
  }

}
