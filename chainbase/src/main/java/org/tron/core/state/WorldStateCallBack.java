package org.tron.core.state;

import io.prometheus.client.Histogram;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.ethereum.trie.MerkleTrieException;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.common.utils.StringUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.state.trie.TrieImpl2;

@Slf4j(topic = "State")
@Component
public class WorldStateCallBack extends WorldStateCallBackUtils {

  private BlockCapsule blockCapsule;
  private volatile TrieImpl2 trie;

  @Setter
  private ChainBaseManager chainBaseManager;

  private final LinkedBlockingQueue<TrieEntry> queue = new LinkedBlockingQueue<>();
  private boolean updateServiceRunning;

  private final Runnable updateService =
      () -> {
        while (updateServiceRunning) {
          try {
            TrieEntry trieEntry  = queue.poll(10, TimeUnit.MILLISECONDS);
            if (trieEntry != null) {
              try {
                trie.put(trieEntry.getKey(), trieEntry.getData());
              } catch (MerkleTrieException e) {
                logger.error(
                        "put trie entry failed, key: {}, value: {}, err: {}",
                        StringUtil.createReadableString(trieEntry.getKey().toArray()),
                        StringUtil.createReadableString(trieEntry.getData().toArrayUnsafe()),
                        e.getMessage());
                System.exit(-1);
              }

            }
          } catch (InterruptedException e) {
            logger.error("state update failed, get trie entry failed, err: {}", e.getMessage());
            System.exit(-1);
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
    queue.addAll(trieEntryList.values());
    trieEntryList.clear();
  }

  public void preExecute(BlockCapsule blockCapsule, WorldStateTrieStore worldStateTrieStore) {
    this.blockCapsule = blockCapsule;
    this.execute = true;
    if (!exe()) {
      return;
    }
    try {
      BlockCapsule parentBlockCapsule =
          chainBaseManager.getBlockById(blockCapsule.getParentBlockId());
      Bytes32 rootHash = parentBlockCapsule.getStateRoot();
      trie = new TrieImpl2(worldStateTrieStore, rootHash);
    } catch (Exception e) {
      throw new MerkleTrieException(e.getMessage());
    }

  }

  public void executePushFinish() {
    final Histogram.Timer timer =
            Metrics.histogramStartTimer(MetricKeys.Histogram.BLOCK_WORLD_STATE_LATENCY);
    try {
      if (!exe()) {
        return;
      }
      // update state after processTx
      queue.addAll(trieEntryList.values());
      trieEntryList.clear();
      while (queue.size() != 0) {
        try {
          Thread.sleep(5);
        } catch (InterruptedException e) {
          logger.error("Fatal error, {}", e.getMessage());
        }
      }
      trie.commit();
      trie.flush();
      Bytes32 newRoot = trie.getRootHashByte32();
      if (newRoot.isZero()) {
        logger.error("executePushFinish failed, trie root hash is null");
        System.exit(-1);
      }
      blockCapsule.setStateRoot(newRoot.toArray());
      execute = false;
    } finally {
      Metrics.histogramObserve(timer);
    }
  }

  public void initGenesis(BlockCapsule blockCapsule, WorldStateTrieStore worldStateTrieStore) {
    if (!exe()) {
      return;
    }
    trie = new TrieImpl2(worldStateTrieStore);
    for (TrieEntry trieEntry : trieEntryList.values()) {
      trie.put(trieEntry.getKey(), trieEntry.getData());
    }
    trieEntryList.clear();
    trie.commit();
    trie.flush();

    Bytes32 newRoot = trie.getRootHashByte32();
    if (newRoot.isZero()) {
      logger.error("initGenesis failed, trie root hash is null");
      System.exit(-1);
    }
    blockCapsule.setStateRoot(newRoot.toArray());
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
