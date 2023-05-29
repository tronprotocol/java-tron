package org.tron.core.state;

import static org.tron.core.state.WorldStateCallBack.fix32;

import com.beust.jcommander.internal.Lists;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.io.IOException;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.TemporaryFolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.crypto.ECKey;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.Utils;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.CodeCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.ContractStateCapsule;
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.state.store.AccountStateStore;
import org.tron.core.state.store.AssetIssueV2StateStore;
import org.tron.core.state.store.DelegationStateStore;
import org.tron.core.state.store.DynamicPropertiesStateStore;
import org.tron.core.state.store.StorageRowStateStore;
import org.tron.core.state.trie.TrieImpl2;
import org.tron.core.vm.program.Storage;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorldStateQueryInstanceTest {

  private WorldStateQueryInstance instance;
  private TrieImpl2 trieImpl2;

  private static TronApplicationContext context;
  private static ChainBaseManager chainBaseManager;
  private static MerkleStorage merkleStorage;

  private static final ECKey ecKey = new ECKey(Utils.getRandom());
  private static final byte[] address = ecKey.getAddress();

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void init() throws IOException {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString(),
        "--p2p-disable", "true"}, "config-localtest.conf");
    // allow account root
    Args.getInstance().setAllowAccountStateRoot(1);
    // init dbBackupConfig to avoid NPE
    Args.getInstance().dbBackupConfig = DbBackupConfig.getInstance();
    context = new TronApplicationContext(DefaultConfig.class);
    chainBaseManager = context.getBean(ChainBaseManager.class);
    merkleStorage =  context.getBean(MerkleStorage.class);
  }

  @After
  public void destroy() {
    context.destroy();
    Args.clearParam();
  }

  @Test
  public void testGet() {
    trieImpl2 = new TrieImpl2(merkleStorage);
    testGetAccount();
    testGetAccountAsset();
    testGetContractState();
    testGetContract();
    testGetCode();
    testGetAssetIssue();
    testGetWitness();
    testGetDelegatedResource();
    testGetDelegation();
    testGetDelegatedResourceAccountIndex();
    testGetVotes();
    testGetDynamicProperty();
    testGetStorageRow();
  }

  private void testGetAccount() {
    byte[] key = address;
    byte[] value = Protocol.Account.newBuilder().setAddress(ByteString.copyFrom(address)).build()
            .toByteArray();
    trieImpl2.put(StateType.encodeKey(StateType.Account, key), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    try (AccountStateStore store = new AccountStateStore(instance)) {
      Assert.assertArrayEquals(value, store.get(key).getData());
      testUnsupportedOperation(store, key);
      Assert.assertEquals(store.getDbName(),(Bytes32.wrap(root).toHexString()));
    }
  }

  private void testGetAccountAsset() {
    long tokenId = 1000001;
    long amount = 100;
    trieImpl2.put(
            fix32(StateType.encodeKey(StateType.AccountAsset,
                    com.google.common.primitives.Bytes.concat(address,
                            Longs.toByteArray(tokenId)))), Bytes.of(Longs.toByteArray(amount)));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertEquals(amount, instance.getAccountAsset(
            Protocol.Account.newBuilder().setAddress(ByteString.copyFrom(address)).build(),
            tokenId));
    Assert.assertEquals(instance.getRootHash(),trieImpl2.getRootHashByte32());
    trieImpl2.put(
            fix32(StateType.encodeKey(StateType.AccountAsset,
                    com.google.common.primitives.Bytes.concat(
                            address, Longs.toByteArray(tokenId)))), UInt256.ZERO);
    trieImpl2.commit();
    trieImpl2.flush();
    instance = new WorldStateQueryInstance(trieImpl2.getRootHashByte32(),
            chainBaseManager);
    Assert.assertEquals(0, instance.getAccountAsset(
            Protocol.Account.newBuilder().setAddress(ByteString.copyFrom(address)).build(),
            tokenId));
    Assert.assertFalse(instance.hasAssetV2(
            Protocol.Account.newBuilder().setAddress(ByteString.copyFrom(address)).build(),
            tokenId));

  }

  private void testGetContractState() {
    byte[] value = new ContractStateCapsule(1).getData();
    trieImpl2.put(StateType.encodeKey(StateType.ContractState, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, instance.getContractState(address).getData());
  }

  private void testGetContract() {
    byte[] value = new ContractCapsule(SmartContractOuterClass.SmartContract.newBuilder()
            .setContractAddress(ByteString.copyFrom(address)).build()).getData();
    trieImpl2.put(StateType.encodeKey(StateType.Contract, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, instance.getContract(address).getData());
  }

  private void testGetCode() {
    byte[] value = new CodeCapsule("code".getBytes()).getData();
    trieImpl2.put(StateType.encodeKey(StateType.Code, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, instance.getCode(address).getData());
  }

  private void testGetAssetIssue() {
    String tokenId = "100001";
    byte[] value = new AssetIssueCapsule(address, tokenId, "token1", "test", 100, 100).getData();
    trieImpl2.put(StateType.encodeKey(StateType.AssetIssue, tokenId.getBytes()), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    try (AssetIssueV2StateStore store = new AssetIssueV2StateStore(instance)) {
      Assert.assertArrayEquals(value, store.get(tokenId.getBytes()).getData());
      testUnsupportedOperation(store, tokenId.getBytes());
      Assert.assertEquals(store.getDbName(),(Bytes32.wrap(root).toHexString()));
    }
  }

  private void testGetWitness() {
    byte[] value = new WitnessCapsule(ByteString.copyFrom(ecKey.getPubKey()), "http://").getData();
    trieImpl2.put(StateType.encodeKey(StateType.Witness, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, instance.getWitness(address).getData());
  }

  private void testGetDelegatedResource() {
    byte[] value = new DelegatedResourceCapsule(ByteString.copyFrom(address),
            ByteString.copyFrom(address)).getData();
    byte[] key = DelegatedResourceCapsule.createDbKey(address, address);
    trieImpl2.put(StateType.encodeKey(StateType.DelegatedResource, key), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, instance.getDelegatedResource(key).getData());
  }

  private void testGetDelegation() {
    byte[] value = "test".getBytes();
    byte[] key = address;
    trieImpl2.put(StateType.encodeKey(StateType.Delegation, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    try (DelegationStateStore store = new DelegationStateStore(instance)) {
      Assert.assertArrayEquals(value, store.get(key).getData());
      testUnsupportedOperation(store, key);
      Assert.assertEquals(store.getDbName(),(Bytes32.wrap(root).toHexString()));
    }
  }

  private void testGetDelegatedResourceAccountIndex() {
    byte[] value = new DelegatedResourceAccountIndexCapsule(ByteString.copyFrom(address)).getData();
    trieImpl2.put(StateType.encodeKey(StateType.DelegatedResourceAccountIndex, address),
            Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, instance.getDelegatedResourceAccountIndex(address).getData());
  }

  private void testGetVotes() {
    byte[] value = new VotesCapsule(ByteString.copyFrom(address), Lists.newArrayList()).getData();
    trieImpl2.put(StateType.encodeKey(StateType.Votes, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, instance.getVotes(address).getData());
  }

  private void testGetDynamicProperty() {
    byte[] key = "key".getBytes();
    byte[] value = "test".getBytes();
    trieImpl2.put(StateType.encodeKey(StateType.Properties, key), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    try (DynamicPropertiesStateStore store = new DynamicPropertiesStateStore(instance)) {
      try {
        Assert.assertArrayEquals(value, store.get(key).getData());
      } catch (ItemNotFoundException e) {
        Assert.fail();
      }
      try {
        Assert.assertArrayEquals(value, store.get("not-key".getBytes()).getData());
        Assert.fail();
      } catch (ItemNotFoundException e) {
        Assert.assertTrue(true);
      }

      testUnsupportedOperation(store, key);
      Assert.assertEquals(store.getDbName(), Bytes32.wrap(root).toHexString());
    }
  }

  private void testGetStorageRow() {
    trieImpl2 = new TrieImpl2(merkleStorage);
    byte[] key = address;
    byte[] value = "test".getBytes();
    trieImpl2.put(StateType.encodeKey(StateType.StorageRow, key), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    instance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    try (StorageRowStateStore store = new StorageRowStateStore(instance)) {
      Assert.assertArrayEquals(value, store.get(key).getData());
      testUnsupportedOperation(store, key);
      Assert.assertEquals(store.getDbName(), Bytes32.wrap(root).toHexString());
      try {
        Storage storage = new Storage(address, store);
        storage.put(new DataWord(0), new DataWord(0));
        storage.commit();
        Assert.fail();
      } catch (UnsupportedOperationException e) {
        Assert.assertTrue(true);
      }
    }
  }

  private void testUnsupportedOperation(TronStoreWithRevoking<?> store, byte[] key) {
    try {
      store.put(key, null);
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      Assert.assertTrue(true);
    }
    Assert.assertFalse(store.has("not-key".getBytes()));

    try {
      store.delete(key);
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      Assert.assertTrue(true);
    }

    try {
      store.setCursor(Chainbase.Cursor.HEAD);
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      Assert.assertTrue(true);
    }

    try {
      store.iterator();
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      Assert.assertTrue(true);
    }

    try {
      store.prefixQuery(key);
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      Assert.assertTrue(true);
    }

    try {
      store.size();
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      Assert.assertTrue(true);
    }

    try {
      store.isNotEmpty();
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      Assert.assertTrue(true);
    }

    try {
      store.of(null);
    } catch (UnsupportedOperationException e) {
      Assert.assertTrue(true);
    } catch (BadItemException e) {
      Assert.fail();
    }
  }

}
