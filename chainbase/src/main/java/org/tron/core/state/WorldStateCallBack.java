package org.tron.core.state;

import io.prometheus.client.Histogram;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.ethereum.trie.MerkleTrieException;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.state.trie.TrieImpl2;

@Slf4j(topic = "State")
@Component
public class WorldStateCallBack extends WorldStateCallBackUtils {

  private BlockCapsule blockCapsule;

  @Getter
  private volatile TrieImpl2 trie;

  public WorldStateCallBack() {
    this.execute = true;
    this.allowGenerateRoot = CommonParameter.getInstance().getStorage().isAllowStateRoot();
  }

  public void clear() {
    if (!exe()) {
      return;
    }
    trieEntryList.forEach(trie::put);
    trieEntryList.clear();
  }

  public void preExeTrans() {
    clear();
  }

  public void exeTransFinish() {
    clear();
  }

  public void preExecute(BlockCapsule blockCapsule) {
    this.blockCapsule = blockCapsule;
    this.execute = true;
    if (!exe()) {
      return;
    }
    try {
      BlockCapsule parentBlockCapsule =
          chainBaseManager.getBlockById(blockCapsule.getParentBlockId());
      Bytes32 rootHash = parentBlockCapsule.getArchiveRoot();
      trie = new TrieImpl2(chainBaseManager.getWorldStateTrieStore(), rootHash);
    } catch (Exception e) {
      throw new MerkleTrieException(e.getMessage());
    }
  }

  public void executePushFinish() {
    if (!exe()) {
      return;
    }
    final Histogram.Timer timer =
            Metrics.histogramStartTimer(MetricKeys.Histogram.BLOCK_WORLD_STATE_LATENCY);
    try {
      clear();
      trie.commit();
      trie.flush();
      Bytes32 newRoot = trie.getRootHashByte32();
      blockCapsule.setArchiveRoot(newRoot.toArray());
      execute = false;
    } finally {
      Metrics.histogramObserve(timer);
    }
  }

  public void initGenesis(BlockCapsule blockCapsule) {
    if (!exe()) {
      return;
    }
    trie = new TrieImpl2(chainBaseManager.getWorldStateTrieStore());
    clear();
    trie.commit();
    trie.flush();
    Bytes32 newRoot = trie.getRootHashByte32();
    blockCapsule.setArchiveRoot(newRoot.toArray());
    execute = false;
  }

  public void exceptionFinish() {
    execute = false;
  }

  public void stopUpdateService() {
  }

}
