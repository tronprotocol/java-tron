package org.tron.core.state;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;
import lombok.Getter;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.CodeCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.ContractStateCapsule;
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.state.trie.TrieImpl2;
import org.tron.core.store.DynamicPropertiesStore;

public class WorldStateQueryInstance {

  private final TrieImpl2 trieImpl;

  @Getter
  private final Bytes32 rootHash;

  public static final byte[] DELETE = UInt256.ZERO.toArray();

  private final WorldStateGenesis worldStateGenesis;

  public WorldStateQueryInstance(Bytes32 rootHash, ChainBaseManager chainBaseManager) {
    this.rootHash = rootHash;
    this.trieImpl = new TrieImpl2(chainBaseManager.getWorldStateTrieStore(), rootHash);
    this.worldStateGenesis = chainBaseManager.getWorldStateGenesis();
  }

  private byte[] get(StateType type, byte[] key) {
    byte[] encodeKey = StateType.encodeKey(type, key);
    Bytes value = trieImpl.get(encodeKey);

    if (Objects.nonNull(value)) {
      if (Objects.equals(value, UInt256.ZERO)) {
        return null;
      }
      return value.toArrayUnsafe();
    }
    return worldStateGenesis.get(type, key);
  }

  public AccountCapsule getAccount(byte[] address) {
    byte[] value = get(StateType.Account, address);
    AccountCapsule accountCapsule = null;
    if (Objects.nonNull(value)) {
      accountCapsule = new AccountCapsule(value);
      accountCapsule.setFlag(true);
    }
    return accountCapsule;
  }

  public AccountCapsule getAccount(byte[] address, byte[] tokenId) {
    long balance = Optional
        .ofNullable(getAccountAsset(address, tokenId))
        .orElse(0L);
    AccountCapsule accountCapsule = getAccount(address);
    if (Objects.nonNull(accountCapsule)) {
      accountCapsule.setInstance(
          accountCapsule.getInstance().toBuilder()
              .putAssetV2(ByteArray.toStr(tokenId), balance).build());
    }
    return accountCapsule;
  }

  public Long getAccountAsset(byte[] address, byte[] tokenId) {
    byte[] value = get(StateType.AccountAsset,
        com.google.common.primitives.Bytes.concat(address, tokenId));
    return Objects.nonNull(value) ? Longs.fromByteArray(value) : null;
  }

  // contract
  public ContractCapsule getContract(byte[] address) {
    byte[] value = get(StateType.Contract, address);
    return Objects.nonNull(value) ? new ContractCapsule(value) : null;
  }

  public ContractStateCapsule getContractState(byte[] address) {
    byte[] value = get(StateType.ContractState, address);
    return Objects.nonNull(value) ? new ContractStateCapsule(value) : null;
  }

  public CodeCapsule getCode(byte[] address) {
    byte[] value = get(StateType.Code, address);
    return Objects.nonNull(value) ? new CodeCapsule(value) : null;
  }

  public StorageRowCapsule getStorageRow(byte[] key) {
    byte[] value = get(StateType.StorageRow, key);
    if (Objects.nonNull(value)) {
      StorageRowCapsule storageRowCapsule = new StorageRowCapsule(value);
      storageRowCapsule.setRowKey(key);
      return storageRowCapsule;
    }
    return null;
  }

  // asset
  public AssetIssueCapsule getAssetIssue(byte[] tokenId) {
    byte[] value = get(StateType.AssetIssue, tokenId);
    return Objects.nonNull(value) ? new AssetIssueCapsule(value) : null;
  }

  // witness
  public WitnessCapsule getWitness(byte[] address) {
    byte[] value = get(StateType.Witness, address);
    return Objects.nonNull(value) ? new WitnessCapsule(value) : null;
  }

  // delegate
  public DelegatedResourceCapsule getDelegatedResource(byte[] key) {
    byte[] value = get(StateType.DelegatedResource, key);
    return Objects.nonNull(value) ? new DelegatedResourceCapsule(value) : null;
  }

  public BytesCapsule getDelegation(byte[] key) {
    byte[] value = get(StateType.Delegation, key);
    return Objects.nonNull(value) ? new BytesCapsule(value) : null;
  }

  public DelegatedResourceAccountIndexCapsule getDelegatedResourceAccountIndex(byte[] key) {
    byte[] value = get(StateType.DelegatedResourceAccountIndex, key);
    return Objects.nonNull(value) ? new DelegatedResourceAccountIndexCapsule(value) : null;
  }

  // vote
  public VotesCapsule getVotes(byte[] address) {
    byte[] value = get(StateType.Votes, address);
    return Objects.nonNull(value) ? new VotesCapsule(value) : null;
  }

  // properties
  public BytesCapsule getDynamicProperty(byte[] key) {
    byte[] value = get(StateType.Properties, key);
    if (Objects.nonNull(value)) {
      return new BytesCapsule(value);
    } else {
      throw new IllegalArgumentException("not found: " + ByteArray.toStr(key));
    }
  }

  public long getDynamicPropertyLong(byte[] key) {
    return ByteArray.toLong(getDynamicProperty(key).getData());
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
    return getDynamicPropertyLong(
            DynamicPropertiesStore.ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX);
  }

  public long getTotalEnergyCurrentLimit() {
    return getDynamicPropertyLong(
            DynamicPropertiesStore.DynamicResourceProperties.TOTAL_ENERGY_CURRENT_LIMIT);
  }

  public long getTotalEnergyWeight() {
    return getDynamicPropertyLong(
            DynamicPropertiesStore.DynamicResourceProperties.TOTAL_ENERGY_WEIGHT);
  }

  public long getTotalNetWeight() {
    return getDynamicPropertyLong(
            DynamicPropertiesStore.DynamicResourceProperties.TOTAL_NET_WEIGHT);
  }

  public long getTotalTronPowerWeight() {
    return getDynamicPropertyLong(
            DynamicPropertiesStore.DynamicResourceProperties.TOTAL_TRON_POWER_WEIGHT);
  }

  public boolean supportUnfreezeDelay() {
    return getDynamicPropertyLong(DynamicPropertiesStore.UNFREEZE_DELAY_DAYS) > 0;
  }

  public long getAllowOptimizedReturnValueOfChainId() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID);
  }

  public long getAllowDynamicEnergy() {
    return getDynamicPropertyLong(DynamicPropertiesStore.ALLOW_DYNAMIC_ENERGY);
  }

  public long getDynamicEnergyThreshold() {
    return getDynamicPropertyLong(DynamicPropertiesStore.DYNAMIC_ENERGY_THRESHOLD);
  }

  public long getDynamicEnergyIncreaseFactor() {
    return getDynamicPropertyLong(DynamicPropertiesStore.DYNAMIC_ENERGY_INCREASE_FACTOR);
  }

  public long getDynamicEnergyMaxFactor() {
    return getDynamicPropertyLong(DynamicPropertiesStore.DYNAMIC_ENERGY_MAX_FACTOR);
  }
}
