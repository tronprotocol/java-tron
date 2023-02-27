package org.tron.core.state;

import com.google.common.base.Objects;
import com.google.common.primitives.Longs;
import java.util.HashMap;
import java.util.Map;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db2.common.Value;

@Slf4j(topic = "DB")
public class WorldStateCallBackUtils {

  @Setter
  protected volatile boolean execute = false;
  protected volatile boolean allowGenerateRoot = false;
  protected Map<Bytes, Bytes> trieEntryList = new HashMap<>();
  @Setter
  protected ChainBaseManager chainBaseManager;

  public void callBack(StateType type, byte[] key, byte[] value, Value.Operator op) {
    if (!exe() || type == StateType.UNDEFINED) {
      return;
    }
    if (op == Value.Operator.DELETE || ArrayUtils.isEmpty(value)) {
      if (type == StateType.Account) {
        // @see org.tron.core.db2.core.SnapshotRoot#remove(byte[] key)
        AccountCapsule accountCapsule = new AccountCapsule(value);
        if (accountCapsule.getAssetOptimized()) {
          accountCapsule.getAssetMapV2().keySet().forEach(tokenId -> addFix32(
                  StateType.AccountAsset, com.google.common.primitives.Bytes.concat(key,
                          Longs.toByteArray(Long.parseLong(tokenId))),
                  WorldStateQueryInstance.DELETE));
        }
      }
      add(type, key, WorldStateQueryInstance.DELETE);
      return;
    }
    if (type == StateType.Account) {
      if (chainBaseManager.getDynamicPropertiesStore()
              .getAllowAccountAssetOptimizationFromRoot() == 1) {
        // @see org.tron.core.db2.core.SnapshotRoot#put(byte[] key, byte[] value)
        AccountCapsule accountCapsule = new AccountCapsule(value);
        if (accountCapsule.getAssetOptimized()) {
          accountCapsule.getDirtyAssetSet().forEach(tokenId -> addFix32(
                  StateType.AccountAsset, com.google.common.primitives.Bytes.concat(key,
                          Longs.toByteArray(Long.parseLong(tokenId))),
                  Longs.toByteArray(accountCapsule.getAssetV2(tokenId))));
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
    if (!execute || !allowGenerateRoot) {
      //Agreement same block high to generate archive root
      execute = false;
      return false;
    }
    return true;
  }

  public static class TrieEntry {

    private Bytes key;
    private Bytes data;

    public static TrieEntry build(Bytes key, Bytes data) {
      TrieEntry trieEntry = new TrieEntry();
      return trieEntry.setKey(key).setData(data);
    }

    public org.apache.tuweni.bytes.Bytes getKey() {
      return key;
    }

    public TrieEntry setKey(Bytes key) {
      this.key = key;
      return this;
    }

    public Bytes getData() {
      return data;
    }

    public TrieEntry setData(Bytes data) {
      this.data = data;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TrieEntry trieEntry = (TrieEntry) o;
      return Objects.equal(key, trieEntry.key);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(key);
    }
  }

}
