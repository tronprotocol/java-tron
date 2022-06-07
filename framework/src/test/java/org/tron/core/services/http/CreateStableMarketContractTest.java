package org.tron.core.services.http;

import static org.tron.common.utils.Commons.adjustBalance;
import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.collections.Maps;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.entity.Dec;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.JsonUtil;
import org.tron.common.utils.Utils;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.interfaceOnPBFT.http.PBFT.HttpApiOnPBFTService;
import org.tron.core.services.interfaceOnSolidity.http.solidity.HttpApiOnSolidityService;
import org.tron.protos.Protocol;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.StableMarketContractOuterClass.StableMarketContract;

public class CreateStableMarketContractTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  private static String OWNER_ADDRESS;
  private static String TO_ADDRESS;
  private static final long TOBIN_FEE = 5;  // 0.5%
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "stable-test";
  private static final String URL = "https://tron.network";
  private static final String SOURCE_TOKEN = "source_token";
  private static final String DEST_TOKEN = "dest_token";
  private static String SOURCE_TOKEN_ID = "";
  private static String DEST_TOKEN_ID = "";
  private static long AMOUNT = 1_000L;

  private static TronApplicationContext context;
  private static String ip = "127.0.0.1";
  private static int fullHttpPort;
  private static Application appTest;
  private static CloseableHttpClient httpClient = HttpClients.createDefault();
  private static WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private static ECKey ecKey1;
  private static ECKey ecKey2;

  private static String dbPath = "output_stable_contract_test";

  private static ChainBaseManager chainBaseManager;

  /**
   * init dependencies.
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, "config-localtest.conf");
    Args.getInstance().setFullNodeAllowShieldedTransactionArgs(false);
    context = new TronApplicationContext(DefaultConfig.class);
    appTest = ApplicationFactory.create(context);
    FullNodeHttpApiService httpApiService = context
            .getBean(FullNodeHttpApiService.class);
    HttpApiOnSolidityService httpApiOnSolidityService = context
            .getBean(HttpApiOnSolidityService.class);
    HttpApiOnPBFTService httpApiOnPBFTService = context
            .getBean(HttpApiOnPBFTService.class);
    RpcApiService rpcApiService = context
        .getBean(RpcApiService.class);
    appTest.addService(httpApiService);
    appTest.addService(httpApiOnSolidityService);
    appTest.addService(httpApiOnPBFTService);
    appTest.addService(rpcApiService);
    appTest.initServices(Args.getInstance());
    appTest.startServices();
    appTest.startup();

    chainBaseManager = context.getBean(ChainBaseManager.class);

    ecKey1 = new ECKey(Utils.getRandom());
    ecKey2 = new ECKey(Utils.getRandom());
    OWNER_ADDRESS = ByteArray.toHexString(ecKey1.getAddress());
    TO_ADDRESS = ByteArray.toHexString(ecKey2.getAddress());

    AccountCapsule fromAccountCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8("fromAccount"),
            Protocol.AccountType.Normal);
    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            ByteString.copyFromUtf8("toAccount"),
            Protocol.AccountType.Normal);
    chainBaseManager.getAccountStore()
        .put(fromAccountCapsule.getAddress().toByteArray(), fromAccountCapsule);
    chainBaseManager.getAccountStore()
        .put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
    chainBaseManager.getStableMarketStore().setOracleExchangeRate(
        ByteArray.fromString(TRX_SYMBOL), Dec.oneDec());

    String rpcNode = String.format("%s:%d", ip,
        Args.getInstance().getRpcPort());
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(rpcNode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    initAsset();
  }

  /**
   * destroy the context.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    appTest.shutdownServices();
    appTest.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void testHttpCreateContract() throws IOException {
    fullHttpPort = Args.getInstance().getFullNodeHttpPort();
    String urlPath = "/wallet/createstablemarketcontract";
    String url = String.format("http://%s:%d%s", ip, fullHttpPort, urlPath);
    Map<String, Object> param = Maps.newHashMap();
    param.put("owner_address", OWNER_ADDRESS);
    param.put("to_address", TO_ADDRESS);
    param.put("source_token_id", TRX_SYMBOL);
    param.put("dest_token_id", DEST_TOKEN_ID);
    param.put("amount", AMOUNT);
    String response = sendPostRequest(url, JsonUtil.obj2Json(param));
    Map<String, Object> result = JsonUtil.json2Obj(response, Map.class);
    Assert.assertNotNull(result);
  }

  @Test
  public void testRpcCreateContract() {
    StableMarketContract stableMarketContract = StableMarketContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setSourceTokenId(TRX_SYMBOL)
        .setDestTokenId(DEST_TOKEN_ID)
        .setAmount(AMOUNT)
        .build();
    GrpcAPI.TransactionExtention tx =
        blockingStubFull.createStableMarketContract(stableMarketContract);
    Assert.assertNotNull(tx);
  }

  private String sendPostRequest(String url, String body) throws IOException {
    HttpPost request = new HttpPost(url);
    request.setHeader("User-Agent", "Java client");
    StringEntity entity = new StringEntity(body);
    request.setEntity(entity);
    HttpResponse response = httpClient.execute(request);
    BufferedReader rd = new BufferedReader(
        new InputStreamReader(response.getEntity().getContent()));
    StringBuffer result = new StringBuffer();
    String line;
    while ((line = rd.readLine()) != null) {
      result.append(line);
    }
    return result.toString();
  }

  public static void setMarketParam(Dec basePool, Dec minSpread, Dec delta) {
    chainBaseManager.getStableMarketStore().setBasePool(basePool);
    chainBaseManager.getStableMarketStore().setMinSpread(minSpread);
    chainBaseManager.getStableMarketStore().setPoolDelta(delta);
  }

  public static long initToken(String owner, String tokenName, long totalSupply, Dec exchangeRate) {
    long token = createStableAsset(owner, tokenName, totalSupply);
    chainBaseManager.getStableMarketStore().setOracleExchangeRate(
        ByteArray.fromString(String.valueOf(token)), exchangeRate);
    return token;
  }

  public static void setTrxBalance(String owner, long balance) throws BalanceInsufficientException {
    AccountCapsule accountCapsule =
        chainBaseManager.getAccountStore().get(ByteArray.fromHexString(owner));
    adjustBalance(chainBaseManager.getAccountStore(), accountCapsule, balance);
  }

  public static long createStableAsset(String owner, String assetName, long totalSupply) {
    long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(id);

    AccountCapsule ownerCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(owner));

    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContractOuterClass.AssetIssueContract assetIssueContract =
        AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owner)))
            .setName(ByteString.copyFrom(ByteArray.fromString(assetName)))
            .setId(Long.toString(id))
            .setTotalSupply(totalSupply)
            .setPrecision(6)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsuleV2 = new AssetIssueCapsule(assetIssueContract);
    ownerCapsule.setAssetIssuedName(assetIssueCapsuleV2.createDbKey());
    ownerCapsule.setAssetIssuedID(assetIssueCapsuleV2.createDbV2Key());
    ownerCapsule.addAssetV2(assetIssueCapsuleV2.createDbV2Key(), totalSupply);

    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    chainBaseManager.getAssetIssueV2Store()
        .put(assetIssueCapsuleV2.createDbKeyFinal(
            chainBaseManager.getDynamicPropertiesStore()), assetIssueCapsuleV2);

    chainBaseManager.getStableMarketStore()
        .setStableCoin(ByteArray.fromString(String.valueOf(id)), TOBIN_FEE);
    return id;
  }

  public static void initAsset() {
    // prepare param
    long sourceTotalSupply = 10_000_000;
    long destTotalSupply = 10_000_000;
    long fromTrxBalance = 10_000_000;
    long toTrxBalance = 10_000_000;
    Dec sourceExchangeRate = Dec.newDec(1);
    Dec destExchangeRate = Dec.newDec(1);
    // set trx balance
    try {
      setTrxBalance(OWNER_ADDRESS, fromTrxBalance);
      setTrxBalance(TO_ADDRESS, toTrxBalance);
    } catch (BalanceInsufficientException e) {
      Assert.fail();
    }
    SOURCE_TOKEN_ID = String.valueOf(
        initToken(OWNER_ADDRESS, SOURCE_TOKEN, sourceTotalSupply, sourceExchangeRate));
    DEST_TOKEN_ID = String.valueOf(
        initToken(TO_ADDRESS, DEST_TOKEN, destTotalSupply, destExchangeRate));

    Dec basePool = Dec.newDec(1_000_000_000);
    Dec minSpread = Dec.newDecWithPrec(5, 3);
    Dec delta = Dec.newDec(10_000_000);
    setMarketParam(basePool, minSpread, delta);
  }
}
