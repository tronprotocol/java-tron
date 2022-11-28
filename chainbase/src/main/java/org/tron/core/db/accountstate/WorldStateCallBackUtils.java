package org.tron.core.db.accountstate;

import java.util.ArrayList;
import java.util.List;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ProtoCapsule;

public class WorldStateCallBackUtils {

  protected volatile boolean execute = false;
  protected volatile boolean allowGenerateRoot = false;
  protected List<TrieEntry> trieEntryList = new ArrayList<>();

  public void callBack(StateType type, byte[] key, ProtoCapsule capsule) {
    // todo: decouple the asset10 and remove it from account
    byte[] value = null;
    switch (type) {
      case Account:
        AccountCapsule accountCapsule = (AccountCapsule) capsule;
        accountCapsule.clearAsset();
        accountCapsule.clearAssetV2();
        value = accountCapsule.getData();
        break;
      case AccountIndex:
      case AccountIdIndex:
      case AccountIssue:
      case Code:
      case Contract:
      case Delegation:
      case DelegatedResource:
//      case DelegatedResourceAccountIndex:
      case Exchange:
      case ExchangeV2:
      case IncrementalMerkleTree:
      case MarketAccount:
      case MarketOrder:
      case MarketPairPriceToOrder: // todo: should support prefix query
      case MarketPairToPrice:
      case Nullifier:
      case Properties:   // todo: check all properties which is not a state data
      case Proposal:
      case StorageRow:
      case Votes:
      case Witness:
      case WitnessSchedule:
        value = capsule.getData();
        break;
    }
    callBack(type, key, value);
  }

  private void callBack(StateType type, byte[] key, byte[] value) {
    if (!exe()) {
      return;
    }
    // todo: why return
//    if (value == null) {
//      return;
//    }
    trieEntryList.add(TrieEntry.build(encodeKey(type.value(), key), value));
  }

  private byte[] encodeKey(byte prefix, byte[] key) {
    byte[] p = new byte[]{prefix};
    return ByteUtil.merge(p, key);
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

    private byte[] key;
    private byte[] data;

    public static TrieEntry build(byte[] key, byte[] data) {
      TrieEntry trieEntry = new TrieEntry();
      return trieEntry.setKey(key).setData(data);
    }

    public byte[] getKey() {
      return key;
    }

    public TrieEntry setKey(byte[] key) {
      this.key = key;
      return this;
    }

    public byte[] getData() {
      return data;
    }

    public TrieEntry setData(byte[] data) {
      this.data = data;
      return this;
    }
  }

}