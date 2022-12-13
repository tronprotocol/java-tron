package org.tron.core.state;

import com.google.protobuf.Internal;
import java.util.Arrays;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
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
  private TrieImpl trie;

  @Setter
  private ChainBaseManager chainBaseManager;

//  @Autowired
//  private WorldStateTrieStore worldStateTrieStore;

  public WorldStateCallBack() {
    this.execute = true;
    this.allowGenerateRoot = CommonParameter.getInstance().getAllowStateRoot() == 1;
  }

  public void preExeTrans() {
    trieEntryList.clear();
  }

  public void exeTransFinish() {
    for (TrieEntry trieEntry : trieEntryList) {
      trie.put(Hash.encodeElement(trieEntry.getKey()), trieEntry.getData());
    }
    trieEntryList.clear();
  }

  public void preExecute(BlockCapsule blockCapsule, WorldStateTrieStore worldStateTrieStore) {
    this.blockCapsule = blockCapsule;
    this.execute = true;
    this.allowGenerateRoot = chainBaseManager.getDynamicPropertiesStore().allowStateRoot();
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
    if (!exe()) {
      return;
    }
    // update state after processTx
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

}
