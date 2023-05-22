package org.tron.core.state;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Longs;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.ethereum.trie.MerkleTrieException;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db2.common.Value;
import org.tron.core.state.trie.TrieImpl2;

@Slf4j(topic = "State")
@Component
public class WorldStateCallBack {

  @Setter
  protected volatile boolean execute;
  protected volatile boolean allowGenerateRoot;
  protected Map<Bytes, Bytes> trieEntryList = new HashMap<>();
  @Setter
  protected ChainBaseManager chainBaseManager;

  private BlockCapsule blockCapsule;

  @Getter
  @VisibleForTesting
  private volatile TrieImpl2 trie;

  public WorldStateCallBack() {
    // set false when p2p is disabled
    this.execute = !CommonParameter.getInstance().isP2pDisable();
    this.allowGenerateRoot = CommonParameter.getInstance().getStorage().isAllowStateRoot();
  }

  public void callBack(StateType type, byte[] key, byte[] value, Value.Operator op) {
    if (!exe() || type == StateType.UNDEFINED) {
      return;
    }
    if (op == Value.Operator.DELETE || ArrayUtils.isEmpty(value)) {
      if (type == StateType.Account && chainBaseManager.getDynamicPropertiesStore()
              .getAllowAccountAssetOptimizationFromRoot() == 1) {
        // @see org.tron.core.db2.core.SnapshotRoot#remove(byte[] key)
        // @see org.tron.core.db2.core.SnapshotRoot#put(byte[] key, byte[] value)
        AccountCapsule accountCapsule = new AccountCapsule(value);
        accountCapsule.getAssetMapV2().keySet().forEach(tokenId -> addFix32(
                StateType.AccountAsset, com.google.common.primitives.Bytes.concat(key,
                        Longs.toByteArray(Long.parseLong(tokenId))),
                WorldStateQueryInstance.DELETE));
      }
      add(type, key, WorldStateQueryInstance.DELETE);
      return;
    }
    if (type == StateType.Account && chainBaseManager.getDynamicPropertiesStore()
            .getAllowAccountAssetOptimizationFromRoot() == 1) {
      // @see org.tron.core.db2.core.SnapshotRoot#put(byte[] key, byte[] value)
      AccountCapsule accountCapsule = new AccountCapsule(value);
      if (accountCapsule.getAssetOptimized()) {
        accountCapsule.getInstance().getAssetV2Map().forEach((tokenId, amount) -> addFix32(
                StateType.AccountAsset, com.google.common.primitives.Bytes.concat(key,
                        Longs.toByteArray(Long.parseLong(tokenId))),
                Longs.toByteArray(amount)));
      } else {
        accountCapsule.getAssetMapV2().forEach((tokenId, amount) -> addFix32(
                StateType.AccountAsset, com.google.common.primitives.Bytes.concat(key,
                        Longs.toByteArray(Long.parseLong(tokenId))),
                Longs.toByteArray(amount)));
        accountCapsule.setAssetOptimized(true);
      }
      value = accountCapsule.getInstance().toBuilder()
              .clearAsset()
              .clearAssetV2()
              .build().toByteArray();

    }
    add(type, key, value);
  }

  private void add(StateType type, byte[] key, byte[] value) {
    trieEntryList.put(Bytes.of(StateType.encodeKey(type, key)), Bytes.of(value));
  }

  private void addFix32(StateType type, byte[] key, byte[] value) {
    trieEntryList.put(fix32(StateType.encodeKey(type, key)), Bytes.of(value));
  }

  public static Bytes32 fix32(byte[] key) {
    return Bytes32.rightPad(Bytes.wrap(key));
  }

  public static Bytes32 fix32(Bytes key) {
    return Bytes32.rightPad(key);
  }


  protected boolean exe() {
    if (!allowGenerateRoot || !execute) {
      //Agreement same block high to generate archive root
      execute = false;
      return false;
    }
    return true;
  }

  @VisibleForTesting
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
    clear();
    trie.commit();
    trie.flush();
    Bytes32 newRoot = trie.getRootHashByte32();
    blockCapsule.setArchiveRoot(newRoot.toArray());
    execute = false;
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

}
