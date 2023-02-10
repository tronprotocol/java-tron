package org.tron.core.state;

import com.google.common.primitives.Longs;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import lombok.Getter;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.CodeCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.store.DynamicPropertiesStore;

public class WorldStateQueryInstance {

  public static byte[] DELETE = Hash.EMPTY_TRIE_HASH;

  private MerklePatriciaTrie<Bytes, Bytes> trieImpl;

  @Getter
  private byte[] rootHash;

  private WorldStateGenesis worldStateGenesis;

  public WorldStateQueryInstance(byte[] rootHash, ChainBaseManager chainBaseManager) {
    this.rootHash = rootHash;
    this.trieImpl = chainBaseManager.getMptStoreV2().getTrie(rootHash);
    this.worldStateGenesis = chainBaseManager.getWorldStateGenesis();
  }

  public AccountCapsule getAccount(byte[] address) {
    byte[] value = get(StateType.Account, address);
    return Objects.nonNull(value) ? new AccountCapsule(value) : null;
  }

  // contract
  public ContractCapsule getContract(byte[] address) {
    byte[] value = get(StateType.Contract, address);
    return Objects.nonNull(value) ? new ContractCapsule(value) : null;
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
    byte[] value = get(StateType.AccountIssue, tokenId);
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
    byte[] value = get(StateType.Properties, key);
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

  private byte[] get(StateType type, byte[] key) {
    byte[] encodeKey = encodeKey(type.value(), key);
    Optional<Bytes> op = trieImpl.get(Bytes.wrap(encodeKey));
    byte[] value = null;
    if (op.isPresent()) {
      value = op.get().toArray();  // todo
    }
    if (Arrays.equals(value, DELETE)) {
      return null;
    }
    if (Objects.nonNull(value)) {
      return value;
    }
    return worldStateGenesis.get(type, key);
  }

  private byte[] encodeKey(byte prefix, byte[] key) {
    byte[] p = new byte[]{prefix};
    return ByteUtil.merge(p, key);
  }
}
