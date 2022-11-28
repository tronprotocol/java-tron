package org.tron.core.state;

import com.google.common.base.Objects;
import com.google.common.primitives.Longs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.tuweni.bytes.Bytes;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ProtoCapsule;

@Slf4j(topic = "DB")
public class WorldStateCallBackUtils {

  protected volatile boolean execute = false;
  protected volatile boolean allowGenerateRoot = false;
  protected Map<Bytes, TrieEntry> trieEntryList = new HashMap<>();

  public void callBack(StateType type, byte[] key, ProtoCapsule<?> capsule) {
    if (!exe()) {
      return;
    }

    if (capsule == null || ArrayUtils.isEmpty(capsule.getData())) {
      add(type, key, WorldStateQueryInstance.DELETE);
      return;
    }

    byte[] value;
    switch (type) {
      case Account:
        AccountCapsule accountCapsule = (AccountCapsule) capsule;
        Set<String> dirtySet = accountCapsule.getDirtyAssetSet();
        if (!dirtySet.isEmpty()) {
          Map<String, Long> assetMap = accountCapsule.getAssetMapV2();
          for (String tokenId : dirtySet) {
            if (assetMap.containsKey(tokenId)) {
              add(StateType.AccountAsset,
                      com.google.common.primitives.Bytes.concat(key,
                              Longs.toByteArray(Long.parseLong(tokenId))),
                      Longs.toByteArray(assetMap.get(tokenId)));
            }
          }
        }
        value = accountCapsule.getInstance().toBuilder()
                .clearAsset()
                .clearAssetV2()
                .build().toByteArray();
        break;
      case AccountIndex:
      case AccountIdIndex:
      case AssetIssue:
      case Code:
      case Contract:
      case Delegation:
      case DelegatedResource:
      case Exchange:
      case ExchangeV2:
      case IncrementalMerkleTree:
      case MarketAccount:
      case MarketOrder:
      case MarketPairPriceToOrder:
      case MarketPairToPrice:
      case Nullifier:
      case Properties:
      case Proposal:
      case StorageRow:
      case Votes:
      case Witness:
      case WitnessSchedule:
        value = capsule.getData();
        break;
      default:
         return;
    }
    add(type, key, value);
  }

  private void add(StateType type, byte[] key, byte[] value) {
    Bytes k = Bytes.of(StateType.encodeKey(type, key));
    trieEntryList.put(k, TrieEntry.build(k, Bytes.of(value)));
  }

  protected boolean exe() {
    if (!execute || !allowGenerateRoot) {
      //Agreement same block high to generate account state root
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
