package org.tron.core.state;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.prometheus.client.CollectorRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.Metrics;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.WalletUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db2.ISession;
import org.tron.core.exception.JsonRpcInternalException;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.exception.JsonRpcInvalidRequestException;
import org.tron.core.exception.StoreException;
import org.tron.core.services.jsonrpc.FullNodeJsonRpcHttpService;
import org.tron.core.services.jsonrpc.TronJsonRpc;
import org.tron.core.services.jsonrpc.types.CallArguments;
import org.tron.core.state.store.StorageRowStateStore;
import org.tron.core.vm.program.Storage;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorldStateQueryTest {
  private static TronApplicationContext context;
  private static ChainBaseManager chainBaseManager;
  private static Manager manager;

  @Autowired
  private static TronJsonRpc tronJsonRpc;
  private static final long TOKEN_ID1 = 1000001L;
  private static final long TOKEN_ID2 = 1000002L;

  private static final ECKey account1Prikey = new ECKey();
  private static final ECKey account2Prikey = new ECKey();

  byte[] contractAddress;
  private static WorldStateCallBack worldStateCallBack;

  private static final  int PORT_JSON_RPC = 18549;
  private static final  int PORT_METRICS = 18890;

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final AtomicInteger cnt = new AtomicInteger(0);

  /**
   * init logic.
   */
  @BeforeClass
  public static void init() throws IOException {

    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()},
        "config-localtest.conf");
    // disable p2p
    Args.getInstance().setP2pDisable(true);
    // allow account root
    Args.getInstance().setAllowAccountStateRoot(1);
    // init dbBackupConfig to avoid NPE
    Args.getInstance().dbBackupConfig = DbBackupConfig.getInstance();
    CommonParameter.getInstance().setJsonRpcHttpFullNodePort(PORT_JSON_RPC);
    CommonParameter.getInstance().setMetricsPrometheusPort(PORT_METRICS);
    CommonParameter.getInstance().setMetricsPrometheusEnable(true);
    Metrics.init();
    context = new TronApplicationContext(DefaultConfig.class);
    Application appTest = ApplicationFactory.create(context);
    appTest.addService(context.getBean(FullNodeJsonRpcHttpService.class));
    appTest.initServices(Args.getInstance());
    appTest.startServices();
    appTest.startup();
    chainBaseManager = context.getBean(ChainBaseManager.class);
    // open account asset optimize
    worldStateCallBack = context.getBean(WorldStateCallBack.class);
    worldStateCallBack.setExecute(true);
    chainBaseManager.getDynamicPropertiesStore().setAllowAssetOptimization(1);
    worldStateCallBack.setExecute(false);
    manager = context.getBean(Manager.class);
    tronJsonRpc = context.getBean(TronJsonRpc.class);

    Protocol.Account acc = Protocol.Account.newBuilder()
        .setAccountName(ByteString.copyFrom(Objects.requireNonNull(ByteArray.fromString("acc"))))
        .setAddress(ByteString.copyFrom(account1Prikey.getAddress()))
        .setType(Protocol.AccountType.AssetIssue)
        .setBalance(10000000000000000L).build();
    Protocol.Account sun = Protocol.Account.newBuilder()
        .setAccountName(ByteString.copyFrom(Objects.requireNonNull(ByteArray.fromString("sun"))))
        .setAddress(ByteString.copyFrom(account2Prikey.getAddress()))
        .setType(Protocol.AccountType.AssetIssue)
        .setBalance(10000000000000000L).build();
    chainBaseManager.getAccountStore().put(account1Prikey.getAddress(), new AccountCapsule(acc));
    chainBaseManager.getAccountStore().put(account2Prikey.getAddress(), new AccountCapsule(sun));
    createAsset();
  }

  @AfterClass
  public static void destroy() {
    context.destroy();
    Args.clearParam();
  }

  public static void createAsset() {
    Assert.assertEquals(TOKEN_ID1,manager.getDynamicPropertiesStore().getTokenIdNum() + 1);
    manager.getDynamicPropertiesStore().saveTokenIdNum(TOKEN_ID1);
    AssetIssueContractOuterClass.AssetIssueContract assetIssueContract =
            AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
                    .setOwnerAddress(ByteString.copyFrom(account1Prikey.getAddress()))
                    .setName(ByteString.copyFrom(ByteArray.fromString("token1")))
                    .setId(Long.toString(TOKEN_ID1))
                    .setTotalSupply(10)
                    .setTrxNum(10)
                    .setNum(1)
                    .setStartTime(1)
                    .setEndTime(2)
                    .setVoteScore(2)
                    .setDescription(ByteString.copyFrom(ByteArray.fromString("token1")))
                    .setUrl(ByteString.copyFrom(ByteArray.fromString("https://tron.network")))
                    .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    manager.getAssetIssueV2Store()
            .put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);

    Assert.assertEquals(TOKEN_ID2,manager.getDynamicPropertiesStore().getTokenIdNum() + 1);
    manager.getDynamicPropertiesStore().saveTokenIdNum(TOKEN_ID2);
    AssetIssueContractOuterClass.AssetIssueContract assetIssueContract2 =
            AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
                    .setOwnerAddress(ByteString.copyFrom(account1Prikey.getAddress()))
                    .setName(ByteString.copyFrom(ByteArray.fromString("token2")))
                    .setId(Long.toString(TOKEN_ID2))
                    .setTotalSupply(10)
                    .setTrxNum(10)
                    .setNum(1)
                    .setStartTime(1)
                    .setEndTime(2)
                    .setVoteScore(2)
                    .setDescription(ByteString.copyFrom(ByteArray.fromString("token2")))
                    .setUrl(ByteString.copyFrom(ByteArray.fromString("https://tron.network")))
                    .build();
    AssetIssueCapsule assetIssueCapsule2 = new AssetIssueCapsule(assetIssueContract2);
    manager.getAssetIssueV2Store()
            .put(assetIssueCapsule2.createDbV2Key(), assetIssueCapsule2);
    AccountCapsule ownerCapsule = manager.getAccountStore()
            .get(account1Prikey.getAddress());
    ownerCapsule.addAssetV2(Long.toString(TOKEN_ID1).getBytes(), 1000);
    ownerCapsule.addAssetV2(Long.toString(TOKEN_ID2).getBytes(), 5000);
    manager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

  }

  @Test
  public void testTransfer() throws InterruptedException, JsonRpcInvalidParamsException {
    manager.initGenesis();
    List<BlockCapsule> blockCapsules = chainBaseManager
        .getBlockStore().getBlockByLatestNum(1);
    BlockCapsule blockCapsule = blockCapsules.get(0);
    try {
      manager.pushBlock(buildTransferBlock(blockCapsule));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    Thread.sleep(3000);
    blockCapsules = chainBaseManager.getBlockStore().getBlockByLatestNum(1);
    blockCapsule = blockCapsules.get(0);
    Bytes32 rootHash = blockCapsule.getArchiveRoot();
    WorldStateQueryInstance worldStateQueryInstance = ChainBaseManager.fetch(rootHash);
    checkAccount(worldStateQueryInstance, blockCapsule.getNum());

    try {
      manager.pushBlock(buildTransferBlock(blockCapsule));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    Thread.sleep(3000);

    blockCapsules = chainBaseManager.getBlockStore().getBlockByLatestNum(1);
    blockCapsule = blockCapsules.get(0);
    rootHash = blockCapsule.getArchiveRoot();
    worldStateQueryInstance = ChainBaseManager.fetch(rootHash);
    checkAccount(worldStateQueryInstance, blockCapsule.getNum());
  }

  @Test
  public void testTransferAsset() throws InterruptedException, JsonRpcInvalidParamsException {
    manager.initGenesis();
    List<BlockCapsule> blockCapsules = chainBaseManager
            .getBlockStore().getBlockByLatestNum(1);
    BlockCapsule blockCapsule = blockCapsules.get(0);
    try {
      manager.pushBlock(buildTransferAssetBlock(blockCapsule));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    Thread.sleep(3000);
    blockCapsules = chainBaseManager.getBlockStore().getBlockByLatestNum(1);
    blockCapsule = blockCapsules.get(0);
    Bytes32 rootHash = blockCapsule.getArchiveRoot();
    WorldStateQueryInstance worldStateQueryInstance = ChainBaseManager.fetch(rootHash);
    checkAccount(worldStateQueryInstance, blockCapsule.getNum());

    try {
      manager.pushBlock(buildTransferAssetBlock(blockCapsule));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    Thread.sleep(3000);

    blockCapsules = chainBaseManager.getBlockStore().getBlockByLatestNum(1);
    blockCapsule = blockCapsules.get(0);
    rootHash = blockCapsule.getArchiveRoot();
    worldStateQueryInstance = ChainBaseManager.fetch(rootHash);
    checkAccount(worldStateQueryInstance, blockCapsule.getNum());
  }

  @Test
  public void testContract() throws InterruptedException, JsonRpcInvalidParamsException,
          JsonRpcInvalidRequestException, JsonRpcInternalException {
    manager.initGenesis();
    List<BlockCapsule> blockCapsules = chainBaseManager
        .getBlockStore().getBlockByLatestNum(1);
    BlockCapsule blockCapsule = blockCapsules.get(0);
    try {
      manager.pushBlock(buildContractBlock(blockCapsule));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    Thread.sleep(3000);
    blockCapsules = chainBaseManager.getBlockStore().getBlockByLatestNum(1);
    blockCapsule = blockCapsules.get(0);
    Bytes32 rootHash = blockCapsule.getArchiveRoot();
    WorldStateQueryInstance worldStateQueryInstance = ChainBaseManager.fetch(rootHash);
    Assert.assertArrayEquals(chainBaseManager.getContractStore().get(contractAddress).getData(),
        worldStateQueryInstance.getContract(contractAddress).getData());
    checkAccount(worldStateQueryInstance, blockCapsule.getNum());

    try {
      manager.pushBlock(buildTriggerBlock(blockCapsule));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    Thread.sleep(3000);

    blockCapsules = chainBaseManager.getBlockStore().getBlockByLatestNum(1);
    blockCapsule = blockCapsules.get(0);
    rootHash = blockCapsule.getArchiveRoot();
    worldStateQueryInstance = ChainBaseManager.fetch(rootHash);
    ContractCapsule contractCapsule = worldStateQueryInstance.getContract(contractAddress);
    try (StorageRowStateStore store = new StorageRowStateStore(worldStateQueryInstance)) {
      Storage storage = new Storage(contractAddress, store);
      storage.setContractVersion(contractCapsule.getInstance().getVersion());

      DataWord value1 = storage.getValue(new DataWord(ByteArray.fromHexString("0")));

      DataWord value2 = storage.getValue(new DataWord(ByteArray.fromHexString("0")));
      Assert.assertArrayEquals(value1.getData(), value2.getData());
    }
    checkAccount(worldStateQueryInstance, blockCapsule.getNum());
    Assert.assertEquals(tronJsonRpc.getABIOfSmartContract(ByteArray.toHexString(contractAddress),
            ByteArray.toJsonHex(blockCapsule.getNum())),
            tronJsonRpc.getABIOfSmartContract(ByteArray.toHexString(contractAddress),
                    "latest"));

    Assert.assertEquals(tronJsonRpc.getStorageAt(ByteArray.toHexString(contractAddress),
                    "0x0", ByteArray.toJsonHex(blockCapsule.getNum())),
            tronJsonRpc.getStorageAt(ByteArray.toHexString(contractAddress),
                    "0x0", "latest"));

    byte[] triggerData = TvmTestUtils.parseAbi("increment()", null);
    CallArguments callArguments = new CallArguments();
    callArguments.setFrom(ByteArray.toHexString(account1Prikey.getAddress()));
    callArguments.setTo(ByteArray.toHexString(contractAddress));
    callArguments.setGas("0x0");
    callArguments.setGasPrice("0x0");
    callArguments.setValue("0x0");
    callArguments.setData(ByteArray.toHexString(triggerData));

    Assert.assertEquals(tronJsonRpc.getCall(callArguments, ByteArray.toJsonHex(
            blockCapsule.getNum())), tronJsonRpc.getCall(callArguments, "latest"));


  }

  private void checkAccount(WorldStateQueryInstance worldStateQueryInstance, long blockNum)
          throws JsonRpcInvalidParamsException {
    AccountCapsule account1Capsule = chainBaseManager.getAccountStore()
        .get(account1Prikey.getAddress());
    AccountCapsule account2Capsule = chainBaseManager.getAccountStore()
        .get(account2Prikey.getAddress());
    Assert.assertEquals(account1Capsule.getBalance(),
        worldStateQueryInstance.getAccount(account1Prikey.getAddress()).getBalance());
    Assert.assertEquals(account2Capsule.getBalance(),
        worldStateQueryInstance.getAccount(account2Prikey.getAddress()).getBalance());
    Assert.assertEquals(account1Capsule.getAssetMapV2(),
        worldStateQueryInstance.getAccount(account1Prikey.getAddress()).getAssetMapV2());
    Assert.assertEquals(account2Capsule.getAssetMapV2(),
        worldStateQueryInstance.getAccount(account2Prikey.getAddress()).getAssetMapV2());
    Assert.assertEquals(tronJsonRpc.getTrxBalance(
            ByteArray.toHexString(account1Prikey.getAddress()),
            ByteArray.toJsonHex(blockNum)),
            tronJsonRpc.getTrxBalance(
                    ByteArray.toHexString(account1Prikey.getAddress()), "latest"));

    Assert.assertEquals(tronJsonRpc.getToken10(
                    ByteArray.toHexString(account1Prikey.getAddress()),
                    ByteArray.toJsonHex(blockNum)),
            tronJsonRpc.getToken10(
                    ByteArray.toHexString(account1Prikey.getAddress()), "latest"));

    Assert.assertEquals(tronJsonRpc.getToken10(
                    ByteArray.toHexString(account2Prikey.getAddress()),
                    ByteArray.toJsonHex(blockNum)),
            tronJsonRpc.getToken10(
                    ByteArray.toHexString(account2Prikey.getAddress()), "latest"));

    Map<String, Long> asset = new HashMap<>();
    for (TronJsonRpc.Token10Result t : tronJsonRpc.getToken10(
            ByteArray.toHexString(account2Prikey.getAddress()), "latest")) {
      asset.put(Long.toString(ByteArray.jsonHexToLong(t.getKey())),
              ByteArray.jsonHexToLong(t.getValue()));
    }
    Assert.assertEquals(account2Capsule.getAssetMapV2(), asset);

    Assert.assertEquals(tronJsonRpc.getToken10(
            ByteArray.toHexString(account1Prikey.getAddress()), "latest"),
            tronJsonRpc.getToken10(
            ByteArray.toHexString(account1Prikey.getAddress()), ByteArray.toJsonHex(blockNum)));

    Assert.assertEquals(tronJsonRpc.getToken10ById(ByteArray.toHexString(
            account2Prikey.getAddress()), ByteArray.toJsonHex(TOKEN_ID1),
                    ByteArray.toJsonHex(blockNum)),
            tronJsonRpc.getToken10ById(ByteArray.toHexString(
                    account2Prikey.getAddress()), ByteArray.toJsonHex(TOKEN_ID1),
                    "latest"));

    Assert.assertEquals(tronJsonRpc.getToken10ById(ByteArray.toHexString(
                            account2Prikey.getAddress()), ByteArray.toJsonHex(TOKEN_ID2),
                    ByteArray.toJsonHex(blockNum)),
            tronJsonRpc.getToken10ById(ByteArray.toHexString(
                            account2Prikey.getAddress()), ByteArray.toJsonHex(TOKEN_ID2),
                    "latest"));

    Assert.assertEquals(account2Capsule.getAssetV2(Long.toString(TOKEN_ID2)),
            ByteArray.jsonHexToLong(tronJsonRpc.getToken10ById(ByteArray.toHexString(
                    account2Prikey.getAddress()), ByteArray.toJsonHex(TOKEN_ID2),
            ByteArray.toJsonHex(blockNum)).getValue()));

    List<TronJsonRpc.Token10Result> list = new ArrayList<>();
    list.add(tronJsonRpc.getToken10ById(ByteArray.toHexString(
                            account1Prikey.getAddress()), ByteArray.toJsonHex(TOKEN_ID1),
                    ByteArray.toJsonHex(blockNum)));
    list.add(tronJsonRpc.getToken10ById(ByteArray.toHexString(
                    account1Prikey.getAddress()), ByteArray.toJsonHex(TOKEN_ID2),
            ByteArray.toJsonHex(blockNum)));

    Assert.assertEquals(list, tronJsonRpc.getToken10(
            ByteArray.toHexString(account1Prikey.getAddress()), ByteArray.toJsonHex(blockNum)));
    try {
      Assert.assertNotNull(worldStateQueryInstance.getBlockByNum(blockNum));
    } catch (StoreException e) {
      Assert.fail();
    }
    JsonArray params = new JsonArray();
    params.add(ByteArray.toHexString(account1Prikey.getAddress()));
    params.add(ByteArray.toJsonHex(blockNum));
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("jsonrpc", "2.0");
    requestBody.addProperty("method", "eth_getBalance");
    requestBody.add("params", params);
    requestBody.addProperty("id", 1);

    CloseableHttpResponse response;
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      cnt.incrementAndGet();
      HttpPost httpPost = new HttpPost("http://127.0.0.1:" + PORT_JSON_RPC + "/jsonrpc");
      httpPost.addHeader("Content-Type", "application/json");
      httpPost.setEntity(new StringEntity(requestBody.toString()));
      response = httpClient.execute(httpPost);
      String responseString = EntityUtils.toString(response.getEntity());
      JSONObject jsonObject = JSON.parseObject(responseString);
      long balance = ByteArray.jsonHexToLong(jsonObject.getString("result"));
      Assert.assertEquals(account1Capsule.getBalance(), balance);
      response.close();
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    Assert.assertEquals(cnt.get(), CollectorRegistry.defaultRegistry.getSampleValue(
        "tron:jsonrpc_service_latency_seconds_count",
        new String[] {"method"}, new String[] {"eth_getBalance"}).intValue());
  }

  private BlockCapsule buildTransferBlock(BlockCapsule parentBlock) {
    BalanceContract.TransferContract transferContract = BalanceContract
        .TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(account1Prikey.getAddress()))
        .setToAddress(ByteString.copyFrom(account2Prikey.getAddress()))
        .setAmount(1).build();

    Protocol.Transaction.raw.Builder transactionBuilder = Protocol
        .Transaction.raw.newBuilder().addContract(
        Protocol.Transaction.Contract.newBuilder()
            .setType(ContractType.TransferContract).setParameter(
            Any.pack(transferContract)).build());
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder()
        .setRawData(transactionBuilder.build()).build();

    TransactionCapsule transactionCapsule = setAndSignTx(transaction, parentBlock, account1Prikey);

    BlockCapsule blockCapsule =
        new BlockCapsule(
            parentBlock.getNum() + 1,
            Sha256Hash.wrap(parentBlock.getBlockId().getByteString()),
            System.currentTimeMillis(),
            ByteString.copyFrom(
                ECKey.fromPrivate(
                        org.tron.common.utils.ByteArray.fromHexString(
                            Args.getLocalWitnesses().getPrivateKey()))
                    .getAddress()));
    blockCapsule.addTransaction(transactionCapsule);
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(
        ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));
    return blockCapsule;
  }

  private BlockCapsule buildTransferAssetBlock(BlockCapsule parentBlock) {
    TransactionCapsule transactionCapsule = buildTransferAsset(TOKEN_ID1, 5, parentBlock);
    TransactionCapsule transactionCapsule2 = buildTransferAsset(TOKEN_ID2, 10, parentBlock);
    BlockCapsule blockCapsule = new BlockCapsule(parentBlock.getNum() + 1,
            Sha256Hash.wrap(parentBlock.getBlockId().getByteString()), System.currentTimeMillis(),
            ByteString.copyFrom(ECKey.fromPrivate(ByteArray.fromHexString(
                    Args.getLocalWitnesses().getPrivateKey())).getAddress()));
    blockCapsule.addTransaction(transactionCapsule);
    blockCapsule.addTransaction(transactionCapsule2);
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));
    return blockCapsule;
  }

  private TransactionCapsule buildTransferAsset(long token, long amt, BlockCapsule parentBlock) {
    AssetIssueContractOuterClass.TransferAssetContract transferAssetContract =
            AssetIssueContractOuterClass.TransferAssetContract.newBuilder()
                    .setAssetName(ByteString.copyFrom(Long.toString(token).getBytes()))
                    .setOwnerAddress(ByteString.copyFrom(account1Prikey.getAddress()))
                    .setToAddress(ByteString.copyFrom(account2Prikey.getAddress()))
                    .setAmount(amt)
                    .build();

    Protocol.Transaction.raw.Builder transactionBuilder = Protocol.Transaction.raw.newBuilder()
            .addContract(Protocol.Transaction.Contract.newBuilder()
                    .setType(ContractType.TransferAssetContract)
                    .setParameter(Any.pack(transferAssetContract))
                    .build());
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder()
            .setRawData(transactionBuilder.build()).build();
    return setAndSignTx(transaction, parentBlock, account1Prikey);
  }

  private BlockCapsule buildContractBlock(BlockCapsule parentBlock) {
    long value = 0L;
    long feeLimit = 1_000_000_000L;
    long consumeUserResourcePercent = 0L;
    String contractName = "increment";
    String ABI = "[]";
    String code = "60806040526000805534801561001457600080fd5b50610181806100246000396000f3fe608060"
        + "405234801561001057600080fd5b50600436106100415760003560e01c806342cbb15c146100465780636d4c"
        + "e63c14610064578063d09de08a14610082575b600080fd5b61004e61008c565b60405161005b91906100cd56"
        + "5b60405180910390f35b61006c610094565b60405161007991906100cd565b60405180910390f35b61008a61"
        + "009d565b005b600043905090565b60008054905090565b60016000546100ac9190610117565b600081905550"
        + "565b6000819050919050565b6100c7816100b4565b82525050565b60006020820190506100e2600083018461"
        + "00be565b92915050565b7f4e487b710000000000000000000000000000000000000000000000000000000060"
        + "0052601160045260246000fd5b6000610122826100b4565b915061012d836100b4565b925082820190508082"
        + "1115610145576101446100e8565b5b9291505056fea26469706673582212207c5e242c88722ac1f7f5f1ea67"
        + "0cf1a784cad42b058651ceaf6fe0fc10ebff8264736f6c63430008110033";
    String libraryAddressPair = null;
    Protocol.Transaction transaction = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, account1Prikey.getAddress(), ABI, code, value, feeLimit,
        consumeUserResourcePercent, libraryAddressPair);
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    transactionCapsule.setResultCode(Protocol.Transaction.Result.contractResult.SUCCESS);

    transactionCapsule = setAndSignTx(transactionCapsule.getInstance(),
        parentBlock, account1Prikey);


    BlockCapsule blockCapsule =
        new BlockCapsule(
            parentBlock.getNum() + 1,
            Sha256Hash.wrap(parentBlock.getBlockId().getByteString()),
            System.currentTimeMillis(),
            ByteString.copyFrom(
                ECKey.fromPrivate(
                        org.tron.common.utils.ByteArray.fromHexString(
                            Args.getLocalWitnesses().getPrivateKey()))
                    .getAddress()));
    blockCapsule.addTransaction(transactionCapsule);
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(
        ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));
    contractAddress = WalletUtil.generateContractAddress(transactionCapsule.getInstance());
    return blockCapsule;
  }

  private BlockCapsule buildTriggerBlock(BlockCapsule parentBlock) {
    long value = 0L;
    long feeLimit = 1_000_000_000L;
    byte[] triggerData = TvmTestUtils.parseAbi("increment()", null);
    Protocol.Transaction transaction = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
        account1Prikey.getAddress(), contractAddress, triggerData, value, feeLimit);

    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    transactionCapsule.setResultCode(Protocol.Transaction.Result.contractResult.SUCCESS);

    transactionCapsule = setAndSignTx(transactionCapsule.getInstance(),
        parentBlock, account1Prikey);

    BlockCapsule blockCapsule =
        new BlockCapsule(
            parentBlock.getNum() + 1,
            Sha256Hash.wrap(parentBlock.getBlockId().getByteString()),
            System.currentTimeMillis(),
            ByteString.copyFrom(
                ECKey.fromPrivate(
                        org.tron.common.utils.ByteArray.fromHexString(
                            Args.getLocalWitnesses().getPrivateKey()))
                    .getAddress()));
    blockCapsule.addTransaction(transactionCapsule);
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(
        ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));
    return blockCapsule;
  }

  private TransactionCapsule setAndSignTx(Protocol.Transaction transaction,
                                          BlockCapsule parentBlock, ECKey account) {
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    transactionCapsule.setReference(parentBlock.getNum(), parentBlock.getBlockId().getBytes());
    transactionCapsule.setExpiration(parentBlock.getTimeStamp() + 60 * 60 * 1000);
    return new TransactionCapsule(PublicMethod.signTransaction(account,
        transactionCapsule.getInstance()));
  }

  @Test
  public void mockSuicide() {
    worldStateCallBack.setExecute(true);
    byte[] address = ByteArray.fromHexString("41B68F8AEC4279E8527D678A52BFDFD23B1AA69729");
    Protocol.Account.Builder builder =  Protocol.Account.newBuilder();
    builder.setAddress(ByteString.copyFrom(address));
    builder.setBalance(100L);
    builder.putAssetV2("1000001", 10);
    builder.putAssetV2("1000002", 20);

    AccountCapsule capsule = new AccountCapsule(builder.build());
    try (ISession tmpSession = manager.getRevokingStore().buildSession()) {
      chainBaseManager.getAccountStore().put(capsule.createDbKey(), capsule);
      tmpSession.commit();
    }

    try (ISession tmpSession = manager.getRevokingStore().buildSession()) {
      chainBaseManager.getAccountStore().delete(capsule.createDbKey());
      tmpSession.commit();
    }

    builder =  Protocol.Account.newBuilder();
    builder.setAddress(ByteString.copyFrom(address));
    builder.setBalance(200L);
    builder.putAssetV2("1000003", 30);

    capsule = new AccountCapsule(builder.build());
    try (ISession tmpSession = manager.getRevokingStore().buildSession()) {
      chainBaseManager.getAccountStore().put(capsule.createDbKey(), capsule);
      tmpSession.commit();
    }

    WorldStateQueryInstance instance =  getQueryInstance();
    AccountCapsule fromState = instance.getAccount(address);
    AccountCapsule fromDb = chainBaseManager.getAccountStore().get(address);
    Assert.assertEquals(fromState.getBalance(), 200L);
    Assert.assertEquals(fromState.getBalance(), fromDb.getBalance());

    Map<String, Long> assetV2 = new HashMap<>();
    assetV2.put("1000003", 30L);

    Assert.assertEquals(assetV2, fromDb.getAssetV2MapForTest());

    Assert.assertEquals(assetV2, fromState.getAssetV2MapForTest());
    worldStateCallBack.setExecute(false);
  }

  private WorldStateQueryInstance getQueryInstance() {
    Assert.assertNotNull(worldStateCallBack.getTrie());
    worldStateCallBack.clear();
    worldStateCallBack.getTrie().commit();
    worldStateCallBack.getTrie().flush();
    return new WorldStateQueryInstance(worldStateCallBack.getTrie().getRootHashByte32(),
            chainBaseManager);
  }

}
