package org.tron.core.state;

import static org.tron.core.state.WorldStateCallBackUtils.fix32;

import com.beust.jcommander.internal.Lists;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.io.File;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.FileUtil;
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
import org.tron.core.state.trie.TrieImpl2;
import org.tron.protos.contract.SmartContractOuterClass;

public class WorldStateQueryInstanceTest {

  private WorldStateQueryInstance worldStateQueryInstance;
  private TrieImpl2 trieImpl2;

  private static TronApplicationContext context;
  private static Application appTest;
  private static ChainBaseManager chainBaseManager;
  private static WorldStateTrieStore worldStateTrieStore;

  private static ECKey ecKey = new ECKey(Utils.getRandom());
  private static byte[] address = ecKey.getAddress();

  public static String DB_PATH = "trie-query-" + RandomStringUtils.randomAlphanumeric(5);

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", DB_PATH}, "config-localtest.conf");
    // allow account root
    Args.getInstance().setAllowAccountStateRoot(1);
    // init dbBackupConfig to avoid NPE
    Args.getInstance().dbBackupConfig = DbBackupConfig.getInstance();
    context = new TronApplicationContext(DefaultConfig.class);
    appTest = ApplicationFactory.create(context);
    appTest.startup();
    chainBaseManager = context.getBean(ChainBaseManager.class);
    worldStateTrieStore = chainBaseManager.getWorldStateTrieStore();
  }

  @After
  public void destory() {
    appTest.shutdown();
    Args.clearParam();
    FileUtil.deleteDir(new File(DB_PATH));
  }

  @Test
  public void testGet() {
    trieImpl2 = new TrieImpl2(worldStateTrieStore);
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
    testGetDynamicPropertyLong();
  }

  private void testGetAccountAsset() {
    long tokenId = 1000001;
    long amount = 100;
    trieImpl2.put(
        fix32(StateType.encodeKey(StateType.AccountAsset,
            com.google.common.primitives.Bytes.concat(address, Longs.toByteArray(tokenId)))),
        Bytes.of(Longs.toByteArray(amount)));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    worldStateQueryInstance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertEquals(amount,
        worldStateQueryInstance.getAccountAsset(address, tokenId).longValue());
  }

  private void testGetContractState() {
    byte[] value = new ContractStateCapsule(1).getData();
    trieImpl2.put(StateType.encodeKey(StateType.ContractState, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    worldStateQueryInstance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value,
        worldStateQueryInstance.getContractState(address).getData());
  }

  private void testGetContract() {
    byte[] value = new ContractCapsule(
        SmartContractOuterClass.SmartContract.newBuilder()
            .setContractAddress(ByteString.copyFrom(address)).build()).getData();
    trieImpl2.put(StateType.encodeKey(StateType.Contract, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    worldStateQueryInstance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, worldStateQueryInstance.getContract(address).getData());
  }

  private void testGetCode() {
    byte[] value = new CodeCapsule("code".getBytes()).getData();
    trieImpl2.put(StateType.encodeKey(StateType.Code, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    worldStateQueryInstance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, worldStateQueryInstance.getCode(address).getData());
  }

  private void testGetAssetIssue() {
    String tokenId = "100001";
    byte[] value = new AssetIssueCapsule(
        address, tokenId, "token1", "test", 100, 100).getData();
    trieImpl2.put(StateType.encodeKey(StateType.AssetIssue, tokenId.getBytes()), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    worldStateQueryInstance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value,
        worldStateQueryInstance.getAssetIssue(tokenId.getBytes()).getData());
  }

  private void testGetWitness() {
    byte[] value = new WitnessCapsule(ByteString.copyFrom(ecKey.getPubKey()), "http://").getData();
    trieImpl2.put(StateType.encodeKey(StateType.Witness, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    worldStateQueryInstance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, worldStateQueryInstance.getWitness(address).getData());
  }

  private void testGetDelegatedResource() {
    byte[] value = new DelegatedResourceCapsule(ByteString.copyFrom(address),
        ByteString.copyFrom(address)).getData();
    byte[] key = DelegatedResourceCapsule.createDbKey(address, address);
    trieImpl2.put(StateType.encodeKey(StateType.DelegatedResource, key), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    worldStateQueryInstance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, worldStateQueryInstance.getDelegatedResource(key).getData());
  }

  private void testGetDelegation() {
    byte[] value = "test".getBytes();
    trieImpl2.put(StateType.encodeKey(StateType.Delegation, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    worldStateQueryInstance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, worldStateQueryInstance.getDelegation(address).getData());
  }

  private void testGetDelegatedResourceAccountIndex() {
    byte[] value = new DelegatedResourceAccountIndexCapsule(ByteString.copyFrom(address)).getData();
    trieImpl2.put(
        StateType.encodeKey(StateType.DelegatedResourceAccountIndex, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    worldStateQueryInstance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value,
        worldStateQueryInstance.getDelegatedResourceAccountIndex(address).getData());
  }

  private void testGetVotes() {
    byte[] value = new VotesCapsule(ByteString.copyFrom(address), Lists.newArrayList()).getData();
    trieImpl2.put(StateType.encodeKey(StateType.Votes, address), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    worldStateQueryInstance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, worldStateQueryInstance.getVotes(address).getData());
  }

  private void testGetDynamicProperty() {
    byte[] key = "key".getBytes();
    byte[] value = "test".getBytes();
    trieImpl2.put(StateType.encodeKey(StateType.Properties, key), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    worldStateQueryInstance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertArrayEquals(value, worldStateQueryInstance.getDynamicProperty(key).getData());
  }

  private void testGetDynamicPropertyLong() {
    byte[] key = "key".getBytes();
    byte[] value = Longs.toByteArray(12345);
    trieImpl2.put(StateType.encodeKey(StateType.Properties, key), Bytes.wrap(value));
    trieImpl2.commit();
    trieImpl2.flush();
    byte[] root = trieImpl2.getRootHash();
    worldStateQueryInstance = new WorldStateQueryInstance(Bytes32.wrap(root), chainBaseManager);
    Assert.assertEquals(Longs.fromByteArray(value),
        worldStateQueryInstance.getDynamicPropertyLong(key));
  }

}
