package org.tron.core.state;

import com.google.common.primitives.Longs;
import java.util.Objects;
import lombok.Getter;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.*;
import org.tron.core.state.trie.TrieImpl;
import org.tron.core.store.DynamicPropertiesStore;

public class WorldStateQueryInstance {

  private TrieImpl trieImpl;

  @Getter
  private byte[] rootHash;

  public WorldStateQueryInstance(byte[] rootHash, ChainBaseManager chainBaseManager) {
    // todo: check hash is legal
    this.rootHash = rootHash;
    this.trieImpl = new TrieImpl(chainBaseManager.getWorldStateTrieStore(), rootHash);
  }

  public AccountCapsule getAccount(byte[] address) {
    byte[] encodeKey = encodeKey(StateType.Account.value(), address);
    byte[] value = trieImpl.get(encodeKey);
    if (Objects.nonNull(value)) {
      // todo: merge asset map from trie
      return new AccountCapsule(value);
    }
    return null;
  }

  // contract
  public ContractCapsule getContract(byte[] address) {
    byte[] encodeKey = encodeKey(StateType.Contract.value(), address);
    byte[] value = trieImpl.get(encodeKey);
    return Objects.nonNull(value) ? new ContractCapsule(value) : null;
  }

  public byte[] getCode(byte[] address) {
    byte[] encodeKey = encodeKey(StateType.Code.value(), address);
    byte[] value = trieImpl.get(encodeKey);
    return Objects.nonNull(value) ? value : null;
  }

  public StorageRowCapsule getStorageRow(byte[] key) {
    byte[] encodeKey = encodeKey(StateType.StorageRow.value(), key);
    byte[] value = trieImpl.get(encodeKey);
    if (Objects.nonNull(value)) {
      StorageRowCapsule storageRowCapsule = new StorageRowCapsule(value);
      storageRowCapsule.setRowKey(key);
      return storageRowCapsule;
    }
    return null;
  }

  // asset
  public AssetIssueCapsule getAssetIssue(byte[] tokenId) {
    byte[] encodeKey = encodeKey(StateType.AccountIssue.value(), tokenId);
    byte[] value = trieImpl.get(encodeKey);
    return Objects.nonNull(value) ? new AssetIssueCapsule(value) : null;
  }

  // witness
  public WitnessCapsule getWitness(byte[] address) {
    byte[] encodeKey = encodeKey(StateType.Witness.value(), address);
    byte[] value = trieImpl.get(encodeKey);
    return Objects.nonNull(value) ? new WitnessCapsule(value) : null;
  }

  // delegate
  public DelegatedResourceCapsule getDelegatedResource(byte[] key) {
    byte[] encodeKey = encodeKey(StateType.DelegatedResource.value(), key);
    byte[] value = trieImpl.get(encodeKey);
    return Objects.nonNull(value) ? new DelegatedResourceCapsule(value) : null;
  }

  public BytesCapsule getDelegation(byte[] key) {
    byte[] encodeKey = encodeKey(StateType.Delegation.value(), key);
    byte[] value = trieImpl.get(encodeKey);
    return Objects.nonNull(value) ? new BytesCapsule(value) : null;
  }

  // vote
  public VotesCapsule getVotes(byte[] address) {
    byte[] encodeKey = encodeKey(StateType.Votes.value(), address);
    byte[] value = trieImpl.get(encodeKey);
    return Objects.nonNull(value) ? new VotesCapsule(value) : null;
  }

  // properties
  public BytesCapsule getDynamicProperty(byte[] key) {
    byte[] encodeKey = encodeKey(StateType.Properties.value(), key);
    byte[] value = trieImpl.get(encodeKey);
    return Objects.nonNull(value) ? new BytesCapsule(value) : null;
  }

  public long getDynamicPropertyLong(byte[] key) {
    byte[] encodeKey = encodeKey(StateType.Properties.value(), key);
    byte[] value = trieImpl.get(encodeKey);
    if (Objects.nonNull(value)) {
      return Longs.fromByteArray(value);
    } else {
      throw new IllegalArgumentException("not found: " + ByteArray.toStr(key));
    }
  }

  public long getLatestBlockHeaderNumber() {
    return getDynamicPropertyLong(DynamicPropertiesStore.LATEST_BLOCK_HEADER_NUMBER);
  }

  public long getLatestBlockHeaderTimestamp() {
    return getDynamicPropertyLong(DynamicPropertiesStore.LATEST_BLOCK_HEADER_TIMESTAMP);
  }

  public long getAllowTvmTransferTrc10() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_TVM_TRANSFER_TRC10);
  }

  public long getAllowMultiSign() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_MULTI_SIGN);
  }

  public long getAllowTvmConstantinople() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_TVM_CONSTANTINOPLE);
  }

  public long getAllowTvmSolidity059() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_TVM_SOLIDITY_059);
  }

  public long getAllowShieldedTRC20Transaction() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_SHIELDED_TRC20_TRANSACTION);
  }

  public long getAllowTvmIstanbul() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_TVM_ISTANBUL);
  }

  public long getAllowTvmFreeze() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_TVM_FREEZE);
  }

  public long getAllowTvmVote() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_TVM_VOTE);
  }

  public long getAllowTvmLondon() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_TVM_LONDON);
  }

  public long getAllowTvmCompatibleEvm() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_TVM_COMPATIBLE_EVM);
  }

  public long getAllowHigherLimitForMaxCpuTimeOfOneTx() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX);
  }

  public long getTotalEnergyCurrentLimit() {
    return getDynamicPropertyLong(DynamicPropertiesStore.DynamicResourceProperties.TOTAL_ENERGY_CURRENT_LIMIT);
  }

  public long getTotalEnergyWeight() {
    return getDynamicPropertyLong(DynamicPropertiesStore.DynamicResourceProperties.TOTAL_ENERGY_WEIGHT);
  }

  public long getTotalNetWeight() {
    return getDynamicPropertyLong(DynamicPropertiesStore.DynamicResourceProperties.TOTAL_NET_WEIGHT);
  }

  private byte[] encodeKey(byte prefix, byte[] key) {
    byte[] p = new byte[]{prefix};
    return Hash.encodeElement(ByteUtil.merge(p, key));
  }
}
