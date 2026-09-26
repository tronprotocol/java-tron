package org.tron.core.jsonrpc;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.TAG_PENDING_SUPPORT_ERROR;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.TAG_SAFE_SUPPORT_ERROR;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.isBlockTag;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.parseBlockNumber;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.parseBlockTag;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import io.prometheus.client.CollectorRegistry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.util.encoders.Hex;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.application.HttpService;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.Metrics;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.capsule.utils.BlockUtil;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionTrace;
import org.tron.core.db2.ISession;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.http.Util;
import org.tron.core.services.interfaceJsonRpcOnPBFT.JsonRpcServiceOnPBFT;
import org.tron.core.services.interfaceJsonRpcOnSolidity.JsonRpcServiceOnSolidity;
import org.tron.core.services.jsonrpc.FullNodeJsonRpcHttpService;
import org.tron.core.services.jsonrpc.JsonRpcErrorResolver;
import org.tron.core.services.jsonrpc.TronJsonRpc;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.TronJsonRpc.LogFilterElement;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.filters.BlockFilterAndResult;
import org.tron.core.services.jsonrpc.filters.LogFilterWrapper;
import org.tron.core.services.jsonrpc.types.BlockResult;
import org.tron.core.services.jsonrpc.types.BuildArguments;
import org.tron.core.services.jsonrpc.types.CallArguments;
import org.tron.core.services.jsonrpc.types.TransactionReceipt;
import org.tron.core.services.jsonrpc.types.TransactionResult;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.json.JSON;
import org.tron.json.JSONArray;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.contract.BalanceContract.TransferContract;


@Slf4j
public class JsonrpcServiceTest extends BaseTest {

  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "first";
  private static final long LATEST_BLOCK_NUM = 10_000L;
  private static final long LATEST_SOLIDIFIED_BLOCK_NUM = 4L;
  private static final String TAG_NOT_SUPPORT_ERROR =
      "TAG [earliest | pending | finalized | safe] not supported";
  // Well-formed selectors that do not identify a block in the test chain.
  private static final String MISSING_BLOCK_HASH =
      "0x1111111111111111111111111111111111111111111111111111111111111111";
  private static final String MISSING_BLOCK_NUMBER = "0x5f5e100";

  private static TronJsonRpcImpl tronJsonRpc;
  @Resource
  private NodeInfoService nodeInfoService;

  private static BlockCapsule blockCapsule0;
  private static BlockCapsule blockCapsule1;
  private static BlockCapsule blockCapsule2;
  private static TransactionCapsule transactionCapsule1;
  @Resource
  private Wallet wallet;

  @Resource
  private FullNodeJsonRpcHttpService fullNodeJsonRpcHttpService;

  @Resource
  private JsonRpcServiceOnPBFT jsonRpcServiceOnPBFT;

  @Resource
  private JsonRpcServiceOnSolidity jsonRpcServiceOnSolidity;

  static {
    Args.setParam(new String[] {"--output-directory", dbPath()}, TestConstants.TEST_CONF);
    CommonParameter.getInstance().setJsonRpcHttpFullNodeEnable(true);
    CommonParameter.getInstance().setJsonRpcHttpFullNodePort(PublicMethod.chooseRandomPort());
    CommonParameter.getInstance().setJsonRpcHttpPBFTNodeEnable(true);
    CommonParameter.getInstance().setJsonRpcHttpPBFTPort(PublicMethod.chooseRandomPort());
    CommonParameter.getInstance().setJsonRpcHttpSolidityNodeEnable(true);
    CommonParameter.getInstance().setJsonRpcHttpSolidityPort(PublicMethod.chooseRandomPort());
    CommonParameter.getInstance().setMetricsPrometheusEnable(true);
    CommonParameter.getInstance().setMetricsPrometheusPort(PublicMethod.chooseRandomPort());
    Metrics.init();

    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
  }

  @Before
  public void init() {
    AccountCapsule accountCapsule =
        new AccountCapsule(ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            Protocol.AccountType.Normal, 10000_000_000L);
    dbManager.getAccountStore().put(accountCapsule.getAddress().toByteArray(), accountCapsule);

    blockCapsule0 = BlockUtil.newGenesisBlockCapsule();
    blockCapsule1 = new BlockCapsule(LATEST_BLOCK_NUM, Sha256Hash.wrap(ByteString.copyFrom(
        ByteArray.fromHexString(
            "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))), 1000000,
        ByteString.copyFromUtf8("testAddress"));
    blockCapsule2 = new BlockCapsule(LATEST_SOLIDIFIED_BLOCK_NUM, Sha256Hash.wrap(
        ByteString.copyFrom(ByteArray.fromHexString(
            "9938a342238077182498b464ac029222ae169360e540d1fd6aee7c2ae9575a06"))), 2000000,
        ByteString.copyFromUtf8("testAddress"));

    TransferContract transferContract1 = TransferContract.newBuilder().setAmount(1L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes())).setToAddress(
            ByteString.copyFrom(ByteArray.fromHexString(
                (Wallet.getAddressPreFixString() + "A389132D6639FBDA4FBC8B659264E6B7C90DB086"))))
        .build();
    TransferContract transferContract2 = TransferContract.newBuilder().setAmount(2L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes())).setToAddress(
            ByteString.copyFrom(ByteArray.fromHexString(
                (Wallet.getAddressPreFixString() + "ED738B3A0FE390EAA71B768B6D02CDBD18FB207B"))))
        .build();
    TransferContract transferContract3 = TransferContract.newBuilder().setAmount(3L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes())).setToAddress(
            ByteString.copyFrom(ByteArray.fromHexString(
                (Wallet.getAddressPreFixString() + "ED738B3A0FE390EAA71B768B6D02CDBD18FB207B"))))
        .build();


    transactionCapsule1 = new TransactionCapsule(transferContract1, ContractType.TransferContract);
    transactionCapsule1.setBlockNum(blockCapsule1.getNum());
    transactionCapsule1.setTimestamp(blockCapsule1.getTimeStamp());
    TransactionCapsule transactionCapsule2 = new TransactionCapsule(transferContract2,
        ContractType.TransferContract);
    transactionCapsule2.setBlockNum(blockCapsule1.getNum());
    transactionCapsule2.setTimestamp(blockCapsule1.getTimeStamp());
    TransactionCapsule transactionCapsule3 = new TransactionCapsule(transferContract3,
        ContractType.TransferContract);
    transactionCapsule3.setBlockNum(blockCapsule2.getNum());
    transactionCapsule3.setTimestamp(blockCapsule2.getTimeStamp());
    blockCapsule1.addTransaction(transactionCapsule1);
    blockCapsule1.addTransaction(transactionCapsule2);
    blockCapsule2.addTransaction(transactionCapsule3);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(LATEST_BLOCK_NUM);
    dbManager.getBlockIndexStore().put(blockCapsule1.getBlockId());
    dbManager.getBlockStore().put(blockCapsule1.getBlockId().getBytes(), blockCapsule1);

    dbManager.getDynamicPropertiesStore().saveLatestSolidifiedBlockNum(LATEST_SOLIDIFIED_BLOCK_NUM);
    dbManager.getBlockIndexStore().put(blockCapsule2.getBlockId());
    dbManager.getBlockStore().put(blockCapsule2.getBlockId().getBytes(), blockCapsule2);

    dbManager.getTransactionStore()
        .put(transactionCapsule1.getTransactionId().getBytes(), transactionCapsule1);
    dbManager.getTransactionStore()
        .put(transactionCapsule2.getTransactionId().getBytes(), transactionCapsule2);
    dbManager.getTransactionStore()
        .put(transactionCapsule3.getTransactionId().getBytes(), transactionCapsule3);

    dbManager.getTransactionStore()
        .put(transactionCapsule3.getTransactionId().getBytes(), transactionCapsule3);

    List<Protocol.TransactionInfo.Log> logs = new ArrayList<>();
    logs.add(Protocol.TransactionInfo.Log.newBuilder()
        .setAddress(ByteString.copyFrom("address1".getBytes()))
        .setData(ByteString.copyFrom("data1".getBytes()))
        .addTopics(ByteString.copyFrom("topic1".getBytes()))
        .build());
    logs.add(Protocol.TransactionInfo.Log.newBuilder()
        .setAddress(ByteString.copyFrom("address2".getBytes()))
        .setData(ByteString.copyFrom("data2".getBytes()))
        .addTopics(ByteString.copyFrom("topic2".getBytes()))
        .build());

    TransactionRetCapsule transactionRetCapsule1 = new TransactionRetCapsule();
    blockCapsule1.getTransactions().forEach(tx -> {
      TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();
      transactionInfoCapsule.setId(tx.getTransactionId().getBytes());
      transactionInfoCapsule.setBlockNumber(blockCapsule1.getNum());
      transactionInfoCapsule.setBlockTimeStamp(blockCapsule1.getTimeStamp());
      transactionInfoCapsule.addAllLog(logs);
      transactionRetCapsule1.addTransactionInfo(transactionInfoCapsule.getInstance());
    });
    dbManager.getTransactionRetStore()
        .put(ByteArray.fromLong(blockCapsule1.getNum()), transactionRetCapsule1);

    TransactionRetCapsule transactionRetCapsule2 = new TransactionRetCapsule();
    blockCapsule2.getTransactions().forEach(tx -> {
      TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();
      transactionInfoCapsule.setId(tx.getTransactionId().getBytes());
      transactionInfoCapsule.setBlockNumber(blockCapsule2.getNum());
      transactionInfoCapsule.setBlockTimeStamp(blockCapsule2.getTimeStamp());
      transactionRetCapsule2.addTransactionInfo(transactionInfoCapsule.getInstance());
    });
    dbManager.getTransactionRetStore()
        .put(ByteArray.fromLong(blockCapsule2.getNum()), transactionRetCapsule2);

    tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet);
    tronJsonRpc.setManager(dbManager);
  }

  @Test
  public void testWeb3Sha3() {
    String result = "";
    try {
      result = tronJsonRpc.web3Sha3("0x1122334455667788");
    } catch (Exception e) {
      Assert.fail();
    }

    Assert.assertEquals("0x1360118a9c9fd897720cf4e26de80683f402dd7c28e000aa98ea51b85c60161c",
        result);

    try {
      tronJsonRpc.web3Sha3("1122334455667788");
    } catch (Exception e) {
      Assert.assertEquals("invalid input value", e.getMessage());
    }
  }

  @Test
  public void testGetBlockTransactionCountByHash() {
    JsonRpcInvalidParamsException nullError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> tronJsonRpc.ethGetBlockTransactionCountByHash(null));
    Assert.assertEquals("invalid hash value", nullError.getMessage());

    try {
      tronJsonRpc.ethGetBlockTransactionCountByHash("0x111111");
    } catch (Exception e) {
      Assert.assertEquals("invalid hash value", e.getMessage());
    }

    String result = "";
    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByHash(
          "0x1111111111111111111111111111111111111111111111111111111111111111");
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertNull(result);

    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByHash(
          Hex.toHexString((blockCapsule1.getBlockId().getBytes())));
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getTransactions().size()), result);

  }

  @Test
  public void testGetBlockTransactionCountByNumber() {
    JsonRpcInvalidParamsException nullError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> tronJsonRpc.ethGetBlockTransactionCountByNumber(null));
    Assert.assertEquals("invalid block number", nullError.getMessage());

    String result = "";
    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByNumber("0x0");
    } catch (Exception e) {
      Assert.assertNull(result);
    }

    Exception pendingEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.ethGetBlockTransactionCountByNumber("pending"));
    Assert.assertEquals(TAG_PENDING_SUPPORT_ERROR, pendingEx.getMessage());

    Exception malformedEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.ethGetBlockTransactionCountByNumber("qqqqq"));
    Assert.assertEquals("invalid block number", malformedEx.getMessage());

    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByNumber("latest");
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getTransactions().size()), result);

    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByNumber(
          ByteArray.toJsonHex(blockCapsule1.getNum()));
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getTransactions().size()), result);

    // safe tag is not supported (new tag added in this refactor)
    Exception safeEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.ethGetBlockTransactionCountByNumber("safe"));
    Assert.assertEquals(TAG_SAFE_SUPPORT_ERROR, safeEx.getMessage());

    // hex that overflows long -> longValueExact rejects (previously silently truncated)
    Exception overflowEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.ethGetBlockTransactionCountByNumber("0x10000000000000000"));
    Assert.assertEquals("invalid block number", overflowEx.getMessage());
  }

  @Test
  public void testGetBlockByHash() {
    BlockResult blockResult = null;
    try {
      blockResult =
          tronJsonRpc.ethGetBlockByHash(Hex.toHexString((blockCapsule1.getBlockId().getBytes())),
              false);
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getNum()), blockResult.getNumber());
    Assert.assertEquals(blockCapsule1.getTransactions().size(),
        blockResult.getTransactions().length);

    JsonRpcInvalidParamsException nullFullTxByHashError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> tronJsonRpc.ethGetBlockByHash(
            Hex.toHexString(blockCapsule1.getBlockId().getBytes()), null));
    Assert.assertEquals("invalid params", nullFullTxByHashError.getMessage());

    JsonRpcInvalidParamsException nullHashError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> tronJsonRpc.ethGetBlockByHash(null, true));
    Assert.assertEquals("invalid hash value", nullHashError.getMessage());
  }

  @Test
  public void testGetBlockByNumber() {
    BlockResult blockResult = null;

    // by number
    try {
      blockResult =
          tronJsonRpc.ethGetBlockByNumber(ByteArray.toJsonHex(blockCapsule1.getNum()), false);
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getNum()), blockResult.getNumber());

    JsonRpcInvalidParamsException nullFullTxByNumberError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> tronJsonRpc.ethGetBlockByNumber("latest", null));
    Assert.assertEquals("invalid params", nullFullTxByNumberError.getMessage());

    JsonRpcInvalidParamsException nullNumberError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> tronJsonRpc.ethGetBlockByNumber(null, true));
    Assert.assertEquals("invalid block number", nullNumberError.getMessage());
    Assert.assertEquals(blockCapsule1.getTransactions().size(),
        blockResult.getTransactions().length);
    Assert.assertEquals("0x0000000000000000", blockResult.getNonce());

    // earliest
    try {
      blockResult = tronJsonRpc.ethGetBlockByNumber("earliest", false);
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(0L), blockResult.getNumber());
    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule0.getNum()), blockResult.getNumber());
    Assert.assertEquals(blockResult.getTransactions().length,
        blockCapsule0.getTransactions().size());

    // latest
    try {
      blockResult = tronJsonRpc.ethGetBlockByNumber("latest", false);
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(LATEST_BLOCK_NUM), blockResult.getNumber());
    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getNum()), blockResult.getNumber());

    // finalized
    try {
      blockResult = tronJsonRpc.ethGetBlockByNumber("finalized", false);
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(LATEST_SOLIDIFIED_BLOCK_NUM), blockResult.getNumber());
    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule2.getNum()), blockResult.getNumber());

    // pending
    Exception e1 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.ethGetBlockByNumber("pending", false));
    Assert.assertEquals("TAG pending not supported", e1.getMessage());

    // invalid
    Exception e2 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.ethGetBlockByNumber("0x", false));
    Assert.assertEquals("invalid block number", e2.getMessage());

    // safe
    Exception e3 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.ethGetBlockByNumber("safe", false));
    Assert.assertEquals(TAG_SAFE_SUPPORT_ERROR, e3.getMessage());

    // hex overflows long -> longValueExact rejects
    Exception e4 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.ethGetBlockByNumber("0x10000000000000000", false));
    Assert.assertEquals("invalid block number", e4.getMessage());
  }

  @Test
  public void testGetTransactionByHash() {
    JsonRpcInvalidParamsException nullError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.getTransactionByHash(null));
    Assert.assertEquals("invalid hash value", nullError.getMessage());

    TransactionResult transactionResult = null;
    try {
      transactionResult = tronJsonRpc.getTransactionByHash(
          "0x1111111111111111111111111111111111111111111111111111111111111111");
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertNull(transactionResult);

    try {
      transactionResult = tronJsonRpc.getTransactionByHash(
          ByteArray.toJsonHex(transactionCapsule1.getTransactionId().getBytes()));
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(transactionCapsule1.getBlockNum()),
        transactionResult.getBlockNumber());
  }

  @Test
  public void testGetBlockByNumber2() {
    fullNodeJsonRpcHttpService.start();
    JsonArray params = new JsonArray();
    params.add(ByteArray.toJsonHex(blockCapsule1.getNum()));
    params.add(false);
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("jsonrpc", "2.0");
    requestBody.addProperty("method", "eth_getBlockByNumber");
    requestBody.add("params", params);
    requestBody.addProperty("id", 1);
    CloseableHttpResponse response;
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpPost httpPost = new HttpPost("http://127.0.0.1:"
          + CommonParameter.getInstance().getJsonRpcHttpFullNodePort() + "/jsonrpc");
      httpPost.addHeader("Content-Type", "application/json");
      httpPost.setEntity(new StringEntity(requestBody.toString()));
      response = httpClient.execute(httpPost);
      String resp = EntityUtils.toString(response.getEntity());
      BlockResult blockResult = JSON.parseObject(resp).getObject("result", BlockResult.class);
      Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getNum()), blockResult.getNumber());
      Assert.assertEquals(blockCapsule1.getTransactions().size(),
          blockResult.getTransactions().length);
      Assert.assertEquals("0x0000000000000000", blockResult.getNonce());
      response.close();
      Assert.assertEquals(1, CollectorRegistry.defaultRegistry.getSampleValue(
          "tron:jsonrpc_service_latency_seconds_count", new String[] {"method"},
          new String[] {"eth_getBlockByNumber"}).intValue());
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    } finally {
      fullNodeJsonRpcHttpService.stop();
    }
  }

  @Test
  public void testServicesInit() {
    try {
      jsonRpcServiceOnPBFT.start();
      jsonRpcServiceOnSolidity.start();
    } finally {
      jsonRpcServiceOnPBFT.stop();
      jsonRpcServiceOnSolidity.stop();
    }
  }

  @Test
  public void testBlockTagParsing() {
    // isBlockTag
    Assert.assertTrue(isBlockTag("pending"));
    Assert.assertTrue(isBlockTag("latest"));
    Assert.assertTrue(isBlockTag("earliest"));
    Assert.assertTrue(isBlockTag("finalized"));
    Assert.assertTrue(isBlockTag("safe"));
    Assert.assertFalse(isBlockTag(null));
    Assert.assertFalse(isBlockTag("0xa"));
    Assert.assertFalse(isBlockTag(""));

    // parseBlockTag: pending throws
    Exception pendingEx = Assert.assertThrows(Exception.class,
        () -> parseBlockTag("pending", wallet));
    Assert.assertEquals(TAG_PENDING_SUPPORT_ERROR, pendingEx.getMessage());

    // parseBlockTag: safe throws
    Exception safeEx = Assert.assertThrows(Exception.class,
        () -> parseBlockTag("safe", wallet));
    Assert.assertEquals(TAG_SAFE_SUPPORT_ERROR, safeEx.getMessage());

    // parseBlockTag: latest -> headBlockNum
    try {
      long blkNum = parseBlockTag("latest", wallet);
      Assert.assertEquals(LATEST_BLOCK_NUM, blkNum);
    } catch (Exception e) {
      Assert.fail();
    }

    // parseBlockTag: earliest -> 0
    try {
      long blkNum = parseBlockTag("earliest", wallet);
      Assert.assertEquals(0L, blkNum);
    } catch (Exception e) {
      Assert.fail();
    }

    // parseBlockTag: finalized -> solidBlockNum
    try {
      long blkNum = parseBlockTag("finalized", wallet);
      Assert.assertEquals(LATEST_SOLIDIFIED_BLOCK_NUM, blkNum);
    } catch (Exception e) {
      Assert.fail();
    }

    // parseBlockNumber: hex -> number
    try {
      long blkNum = parseBlockNumber("0xa", wallet);
      Assert.assertEquals(10L, blkNum);
    } catch (Exception e) {
      Assert.fail();
    }

    // parseBlockNumber: bad hex -> throws
    Exception abcEx = Assert.assertThrows(Exception.class,
        () -> parseBlockNumber("abc", wallet));
    Assert.assertEquals("Incorrect hex syntax", abcEx.getMessage());

    Exception hexEx = Assert.assertThrows(Exception.class,
        () -> parseBlockNumber("0xxabc", wallet));
    Assert.assertEquals("invalid block number", hexEx.getMessage());
  }

  @Test
  public void testGetTrxBalance() {
    String balance = "";

    Exception e1 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getTrxBalance("", "earliest"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e1.getMessage());

    Exception e2 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getTrxBalance("", "pending"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e2.getMessage());

    Exception e3 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getTrxBalance("", "finalized"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e3.getMessage());

    Exception e4 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getTrxBalance("", "safe"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e4.getMessage());

    try {
      balance = tronJsonRpc.getTrxBalance("0xabd4b9367799eaa3197fecb144eb71de1e049abc",
          "latest");
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals("0x2540be400", balance);
  }

  @Test
  public void testGetStorageAt() {
    Exception e1 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getStorageAt("", "", "earliest"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e1.getMessage());

    Exception e2 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getStorageAt("", "", "pending"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e2.getMessage());

    Exception e3 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getStorageAt("", "", "finalized"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e3.getMessage());

    Exception e4 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getStorageAt("", "", "safe"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e4.getMessage());

    // hex block number -> QUANTITY_NOT_SUPPORT_ERROR
    Exception e5 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getStorageAt("", "", "0x1"));
    Assert.assertEquals(
        "QUANTITY not supported, just support TAG as latest", e5.getMessage());

    // malformed hex -> BLOCK_NUM_ERROR
    Exception e6 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getStorageAt("", "", "abc"));
    Assert.assertEquals("invalid block number", e6.getMessage());

    // storageIdx length oversized -> invalid storage key value
    String addr = "0xabd4b9367799eaa3197fecb144eb71de1e049abc";
    Exception e7 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getStorageAt(addr,
            "0x" + new String(new char[65]).replace('\0', 'a'), "latest"));
    Assert.assertEquals("invalid storage key value", e7.getMessage());

    // storageIdx is null -> invalid storage key value
    Exception e8 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getStorageAt(addr, null, "latest"));
    Assert.assertEquals("invalid storage key value", e8.getMessage());

    // storageIdx is valid length but decodes to >32 bytes (66 hex chars without 0x = 33 bytes):
    // DataWord rejects it -> invalid storage key value (-32602), not a leaked Internal error.
    Exception e9 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getStorageAt(addr,
            new String(new char[66]).replace('\0', 'a'), "latest"));
    Assert.assertEquals("invalid storage key value", e9.getMessage());

    // latest happy path: address is an account, not a contract, so returns 32 zero bytes
    try {
      String value = tronJsonRpc.getStorageAt(
          addr, "0x0", "latest");
      Assert.assertEquals(ByteArray.toJsonHex(new byte[32]), value);
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testGetABIOfSmartContract() {
    Exception e1 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getABIOfSmartContract("", "earliest"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e1.getMessage());

    Exception e2 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getABIOfSmartContract("", "pending"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e2.getMessage());

    Exception e3 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getABIOfSmartContract("", "finalized"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e3.getMessage());

    Exception e4 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getABIOfSmartContract("", "safe"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e4.getMessage());

    // hex block number -> QUANTITY_NOT_SUPPORT_ERROR
    Exception e5 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getABIOfSmartContract("", "0x1"));
    Assert.assertEquals(
        "QUANTITY not supported, just support TAG as latest", e5.getMessage());

    // malformed hex -> BLOCK_NUM_ERROR
    Exception e6 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getABIOfSmartContract("", "abc"));
    Assert.assertEquals("invalid block number", e6.getMessage());

    // latest happy path: address is an account, not a contract, so returns "0x"
    try {
      String code = tronJsonRpc.getABIOfSmartContract(
          "0xabd4b9367799eaa3197fecb144eb71de1e049abc", "latest");
      Assert.assertEquals("0x", code);
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testGetCall() {
    CallArguments validArgs = newValidCallArguments();
    Exception e1 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getCall(validArgs, "earliest"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e1.getMessage());

    Exception e2 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getCall(validArgs, "pending"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e2.getMessage());

    Exception e3 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getCall(validArgs, "finalized"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e3.getMessage());

    Exception e4 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getCall(validArgs, "safe"));
    Assert.assertEquals(TAG_NOT_SUPPORT_ERROR, e4.getMessage());

    JsonRpcInvalidParamsException nullArgsError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.getCall(null, "latest"));
    Assert.assertEquals("invalid params", nullArgsError.getMessage());

    JsonRpcInvalidParamsException nullArgsWithUnsupportedTagError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.getCall(null, "earliest"));
    Assert.assertEquals("invalid params", nullArgsWithUnsupportedTagError.getMessage());

    JsonRpcInvalidParamsException doubleNullError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.getCall(null, null));
    Assert.assertEquals("invalid params", doubleNullError.getMessage());
  }

  @Test
  public void testTopLevelNullParametersAtServiceLayer() throws Exception {
    JsonRpcInvalidParamsException estimateError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.estimateGas(null));
    Assert.assertEquals("invalid params", estimateError.getMessage());

    JsonRpcInvalidParamsException buildError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.buildTransaction(null));
    Assert.assertEquals("invalid params", buildError.getMessage());

    JsonRpcInvalidParamsException logsError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.getLogs(null));
    Assert.assertEquals("invalid filter request", logsError.getMessage());

    JsonRpcInvalidParamsException newFilterError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.newFilter(null));
    Assert.assertEquals("invalid filter request", newFilterError.getMessage());

    JsonRpcInvalidParamsException uninstallError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.uninstallFilter(null));
    Assert.assertEquals("invalid params", uninstallError.getMessage());

    JsonRpcInvalidParamsException changesError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.getFilterChanges(null));
    Assert.assertEquals("invalid params", changesError.getMessage());

    JsonRpcInvalidParamsException filterLogsError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.getFilterLogs(null));
    Assert.assertEquals("invalid params", filterLogsError.getMessage());

    Assert.assertFalse(tronJsonRpc.uninstallFilter("0xdeadbeef"));

    String existingHash = ByteArray.toJsonHex(blockCapsule1.getBlockId().getBytes());
    for (String hash : new String[] {existingHash, MISSING_BLOCK_HASH}) {
      JsonRpcInvalidParamsException e = Assert.assertThrows(hash,
          JsonRpcInvalidParamsException.class, () -> tronJsonRpc.ethGetBlockByHash(hash, null));
      Assert.assertEquals(hash, "invalid params", e.getMessage());
    }
    for (String number : new String[] {"latest", MISSING_BLOCK_NUMBER}) {
      JsonRpcInvalidParamsException e = Assert.assertThrows(number,
          JsonRpcInvalidParamsException.class,
          () -> tronJsonRpc.ethGetBlockByNumber(number, null));
      Assert.assertEquals(number, "invalid params", e.getMessage());
    }

    // Parameter 0 is validated before the flag, so its error wins when both are invalid.
    JsonRpcInvalidParamsException bothInvalidByHash = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.ethGetBlockByHash(null, null));
    Assert.assertEquals("invalid hash value", bothInvalidByHash.getMessage());
    JsonRpcInvalidParamsException bothInvalidByNumber = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.ethGetBlockByNumber(null, null));
    Assert.assertEquals("invalid block number", bothInvalidByNumber.getMessage());

    JsonRpcInvalidParamsException badHash = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.ethGetBlockByHash("0xzz", null));
    Assert.assertEquals("invalid hash value", badHash.getMessage());
    JsonRpcInvalidParamsException emptyHexNumber = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.ethGetBlockByNumber("0x", null));
    Assert.assertEquals("invalid block number", emptyHexNumber.getMessage());
    JsonRpcInvalidParamsException safeTag = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.ethGetBlockByNumber("safe", null));
    Assert.assertEquals(TAG_SAFE_SUPPORT_ERROR, safeTag.getMessage());

    JsonRpcInvalidParamsException receiptError = Assert.assertThrows(
        JsonRpcInvalidParamsException.class, () -> tronJsonRpc.getTransactionReceipt(null));
    Assert.assertEquals("invalid hash value", receiptError.getMessage());
  }

  @Test
  public void testNullParameterWireContract() throws Exception {
    JsonRpcServer server = newJsonRpcServer();

    assertMappedError(handleJsonRpc(server, "eth_getLogs", jsonParams((Object) null)),
        -32602, "invalid filter request");
    assertMappedError(handleJsonRpc(server, "eth_newFilter", jsonParams((Object) null)),
        -32602, "invalid filter request");
    assertMappedError(handleJsonRpc(server, "eth_estimateGas", jsonParams((Object) null)),
        -32602, "invalid params");
    assertMappedError(handleJsonRpc(server, "buildTransaction", jsonParams((Object) null)),
        -32602, "invalid params");

    assertFrameworkError(handleJsonRpc(server, "eth_call", jsonParams((Object) null)),
        -32602, "method parameters invalid");
    assertMappedError(handleJsonRpc(server, "eth_call", jsonParams(null, null)),
        -32602, "invalid params");
    assertMappedError(handleJsonRpc(server, "eth_call", jsonParams(null, "latest")),
        -32602, "invalid params");

    assertMappedError(handleJsonRpc(server, "eth_uninstallFilter", jsonParams((Object) null)),
        -32602, "invalid params");
    assertBooleanResult(handleJsonRpc(server, "eth_uninstallFilter",
        jsonParams("0xdeadbeef")), false);
    assertMappedError(handleJsonRpc(server, "eth_getFilterChanges", jsonParams((Object) null)),
        -32602, "invalid params");
    assertMappedError(handleJsonRpc(server, "eth_getFilterLogs", jsonParams((Object) null)),
        -32602, "invalid params");

    String blockHash = ByteArray.toJsonHex(blockCapsule1.getBlockId().getBytes());
    for (String hash : new String[] {blockHash, MISSING_BLOCK_HASH}) {
      assertMappedError(handleJsonRpc(server, "eth_getBlockByHash", jsonParams(hash, null)),
          -32602, "invalid params");
    }
    for (String number : new String[] {"latest", MISSING_BLOCK_NUMBER}) {
      assertMappedError(handleJsonRpc(server, "eth_getBlockByNumber", jsonParams(number, null)),
          -32602, "invalid params");
    }
    assertMappedError(handleJsonRpc(server, "eth_getBlockByHash", jsonParams(null, null)),
        -32602, "invalid hash value");
    assertMappedError(handleJsonRpc(server, "eth_getBlockByNumber", jsonParams(null, null)),
        -32602, "invalid block number");
    assertMappedError(handleJsonRpc(server, "eth_getBlockByHash", jsonParams("0xzz", null)),
        -32602, "invalid hash value");
    assertMappedError(handleJsonRpc(server, "eth_getBlockByNumber", jsonParams("0x", null)),
        -32602, "invalid block number");
    assertMappedError(handleJsonRpc(server, "eth_getBlockByNumber", jsonParams("safe", null)),
        -32602, TAG_SAFE_SUPPORT_ERROR);

    assertMappedError(handleJsonRpc(server, "eth_getBlockByHash", jsonParams(null, true)),
        -32602, "invalid hash value");
    assertMappedError(handleJsonRpc(server, "eth_getBlockByNumber", jsonParams(null, true)),
        -32602, "invalid block number");

    String address = "0xabd4b9367799eaa3197fecb144eb71de1e049abc";
    assertMappedError(handleJsonRpc(server, "eth_getStorageAt",
            jsonParams(address, null, "latest")), -32602, "invalid storage key value");
    assertMappedError(handleJsonRpc(server, "eth_getBlockTransactionCountByNumber",
            jsonParams((Object) null)), -32602, "invalid block number");

    assertMappedError(handleJsonRpc(server, "eth_getBlockReceipts", jsonParams((Object) null)),
        -32602, "invalid block number");
    assertMappedError(handleJsonRpc(server, "eth_getTransactionByHash", jsonParams((Object) null)),
        -32602, "invalid hash value");
    assertMappedError(handleJsonRpc(server, "eth_getTransactionReceipt",
            jsonParams((Object) null)), -32602, "invalid hash value");
    assertMappedError(handleJsonRpc(server, "eth_getBlockTransactionCountByHash",
            jsonParams((Object) null)), -32602, "invalid hash value");
  }

  @Test
  public void testUninstallFilterLookupMisses() throws Exception {
    JsonRpcServer server = newJsonRpcServer();
    for (String filterId : new String[] {"0xdeadbeef", "", "0x", "not-hex", "0xz"}) {
      Assert.assertFalse("filter ID: " + filterId, tronJsonRpc.uninstallFilter(filterId));
      assertBooleanResult(handleJsonRpc(server, "eth_uninstallFilter", jsonParams(filterId)),
          false);
    }
  }

  @Test
  public void testNullFullTransactionObjectsRejectedBeforeBlockLookup() throws Exception {
    Wallet mockWallet = Mockito.mock(Wallet.class);
    try (TronJsonRpcImpl rpc = new TronJsonRpcImpl(nodeInfoService, mockWallet)) {
      String hash = ByteArray.toJsonHex(blockCapsule1.getBlockId().getBytes());
      Assert.assertThrows(JsonRpcInvalidParamsException.class,
          () -> rpc.ethGetBlockByHash(hash, null));
      for (String selector : new String[] {"latest", "earliest", "finalized", "0x1"}) {
        Assert.assertThrows(selector, JsonRpcInvalidParamsException.class,
            () -> rpc.ethGetBlockByNumber(selector, null));
      }
      // Resolving "finalized" reads the solidified block number, not a block.
      Mockito.verify(mockWallet, Mockito.never()).getBlockById(Mockito.any());
      Mockito.verify(mockWallet, Mockito.never()).getBlockByNum(Mockito.anyLong());
      Mockito.verify(mockWallet, Mockito.never()).getNowBlock();
    }
  }

  @Test
  public void testUninstallInstalledFilters() throws Exception {
    String eventId = tronJsonRpc.newFilter(new FilterRequest());
    String blockId = tronJsonRpc.newBlockFilter();
    String eventKey = ByteArray.fromHex(eventId);
    String blockKey = ByteArray.fromHex(blockId);

    Assert.assertTrue(tronJsonRpc.getEventFilter2ResultFull().containsKey(eventKey));
    Assert.assertTrue(tronJsonRpc.getBlockFilter2ResultFull().containsKey(blockKey));
    Assert.assertTrue(tronJsonRpc.uninstallFilter(eventId));
    Assert.assertFalse(tronJsonRpc.getEventFilter2ResultFull().containsKey(eventKey));
    Assert.assertTrue(tronJsonRpc.getBlockFilter2ResultFull().containsKey(blockKey));
    Assert.assertFalse(tronJsonRpc.uninstallFilter(eventId));

    Assert.assertTrue(tronJsonRpc.uninstallFilter(blockId));
    Assert.assertFalse(tronJsonRpc.getBlockFilter2ResultFull().containsKey(blockKey));
    Assert.assertFalse(tronJsonRpc.uninstallFilter(blockId));
  }

  @Test
  public void testUninstallInstalledFiltersOverWire() throws Exception {
    JsonRpcServer server = newJsonRpcServer();
    for (String method : new String[] {"eth_newFilter", "eth_newBlockFilter"}) {
      boolean eventFilter = "eth_newFilter".equals(method);
      JSONObject created = handleJsonRpc(server, method,
          eventFilter ? jsonParams(new JsonObject()) : jsonParams());
      Assert.assertFalse(created.toJSONString(), created.containsKey("error"));
      Assert.assertTrue(created.toJSONString(), created.get("result") instanceof String);
      String filterId = created.getString("result");
      String key = ByteArray.fromHex(filterId);
      Map<String, ?> filters = eventFilter
          ? tronJsonRpc.getEventFilter2ResultFull() : tronJsonRpc.getBlockFilter2ResultFull();
      Assert.assertTrue(filters.containsKey(key));

      assertBooleanResult(handleJsonRpc(server, "eth_uninstallFilter", jsonParams(filterId)), true);
      Assert.assertFalse(filters.containsKey(key));
      assertBooleanResult(handleJsonRpc(server, "eth_uninstallFilter", jsonParams(filterId)),
          false);
    }
  }

  @Test
  public void testFilterLookupsStillRejectUnknownIds() throws Exception {
    String filterId = "0xdeadbeef";
    ItemNotFoundException changesError = Assert.assertThrows(ItemNotFoundException.class,
        () -> tronJsonRpc.getFilterChanges(filterId));
    Assert.assertEquals("filter not found", changesError.getMessage());
    ItemNotFoundException logsError = Assert.assertThrows(ItemNotFoundException.class,
        () -> tronJsonRpc.getFilterLogs(filterId));
    Assert.assertEquals("filter not found", logsError.getMessage());

    JsonRpcServer server = newJsonRpcServer();
    assertMappedError(handleJsonRpc(server, "eth_getFilterChanges", jsonParams(filterId)),
        -32000, "filter not found");
    assertMappedError(handleJsonRpc(server, "eth_getFilterLogs", jsonParams(filterId)),
        -32000, "filter not found");
  }

  @Test
  public void testUninstallFilterPreservesIdNormalization() throws Exception {
    for (String filterId : new String[] {"0xa", "a", "0x0a", "0a"}) {
      tronJsonRpc.getBlockFilter2ResultFull().put("0a", new BlockFilterAndResult());
      Assert.assertTrue("filter ID: " + filterId, tronJsonRpc.uninstallFilter(filterId));
      Assert.assertFalse(tronJsonRpc.getBlockFilter2ResultFull().containsKey("0a"));
    }
  }

  @Test(timeout = 15000)
  public void testConcurrentUninstallRemovesFilterOnce() throws Exception {
    String filterId = tronJsonRpc.newBlockFilter();
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    // Keep cleanup failures suppressed when the test itself has already failed.
    try (AutoCloseable cleanup = () -> {
      start.countDown();
      executor.shutdownNow();
      try {
        Assert.assertTrue("workers must terminate", executor.awaitTermination(5, TimeUnit.SECONDS));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw e;
      }
    }) {
      List<Future<Boolean>> results = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        results.add(executor.submit(() -> {
          ready.countDown();
          if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("timed out waiting to uninstall filter");
          }
          return tronJsonRpc.uninstallFilter(filterId);
        }));
      }
      Assert.assertTrue("workers must be ready", ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      Assert.assertNotEquals(results.get(0).get(5, TimeUnit.SECONDS),
          results.get(1).get(5, TimeUnit.SECONDS));
      Assert.assertFalse(tronJsonRpc.getBlockFilter2ResultFull()
          .containsKey(ByteArray.fromHex(filterId)));
    }
  }

  @Test
  public void testFilterFieldNullSemanticsOverWire() throws Exception {
    JsonRpcServer server = newJsonRpcServer();
    String topic =
        "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    JsonObject nullAddress = new JsonObject();
    nullAddress.add("address", JsonNull.INSTANCE);
    assertSuccessfulLogsResult(handleJsonRpc(server, "eth_getLogs", jsonParams(nullAddress)));

    JsonObject nullTopics = new JsonObject();
    nullTopics.add("topics", JsonNull.INSTANCE);
    assertSuccessfulLogsResult(handleJsonRpc(server, "eth_getLogs", jsonParams(nullTopics)));

    JsonArray positionalTopics = new JsonArray();
    positionalTopics.add(topic);
    positionalTopics.add(JsonNull.INSTANCE);
    JsonObject positionalWildcard = new JsonObject();
    positionalWildcard.add("topics", positionalTopics);
    assertSuccessfulLogsResult(handleJsonRpc(
        server, "eth_getLogs", jsonParams(positionalWildcard)));

    JsonArray invalidOrList = new JsonArray();
    invalidOrList.add(topic);
    invalidOrList.add(JsonNull.INSTANCE);
    JsonArray nestedTopics = new JsonArray();
    nestedTopics.add(invalidOrList);
    JsonObject invalidOrWildcard = new JsonObject();
    invalidOrWildcard.add("topics", nestedTopics);
    assertMappedError(handleJsonRpc(server, "eth_getLogs", jsonParams(invalidOrWildcard)),
        -32602, "invalid topic(s): null");
  }

  @Test
  public void testOptionalDtoNullFieldsOverWire() throws Exception {
    JsonRpcServer server = newJsonRpcServer();

    JSONObject omittedTransfer = handleJsonRpc(
        server, "buildTransaction", jsonParams(newTransferBuildArguments()));
    assertSuccessfulResponse(omittedTransfer);
    String[] optionalBuildFields = {
        "tokenId",
        "tokenValue",
        "consumeUserResourcePercent",
        "originEnergyLimit",
        "permissionId",
        "extraData"
    };
    for (String field : optionalBuildFields) {
      JsonObject explicitNullArgs = newTransferBuildArguments();
      explicitNullArgs.add(field, JsonNull.INSTANCE);
      JSONObject explicitNull = handleJsonRpc(
          server, "buildTransaction", jsonParams(explicitNullArgs));
      assertEquivalentBuiltTransaction(omittedTransfer, explicitNull);
    }

    JsonObject omittedConsume = newCreateBuildArguments();
    omittedConsume.addProperty("originEnergyLimit", 10_000_000L);
    JsonObject nullConsume = newCreateBuildArguments();
    nullConsume.add("consumeUserResourcePercent", JsonNull.INSTANCE);
    nullConsume.addProperty("originEnergyLimit", 10_000_000L);
    JSONObject omittedConsumeResponse = handleJsonRpc(
        server, "buildTransaction", jsonParams(omittedConsume));
    JSONObject nullConsumeResponse = handleJsonRpc(
        server, "buildTransaction", jsonParams(nullConsume));
    assertEquivalentCreatedContract(omittedConsumeResponse, nullConsumeResponse);
    Assert.assertEquals(0L, getNewContract(nullConsumeResponse)
        .getLongValue("consume_user_resource_percent"));
    Assert.assertEquals(10_000_000L, getNewContract(nullConsumeResponse)
        .getLongValue("origin_energy_limit"));
    assertSuccessfulVmExecution(omittedConsumeResponse);
    assertSuccessfulVmExecution(nullConsumeResponse);

    JsonObject omittedOrigin = newCreateBuildArguments();
    omittedOrigin.addProperty("consumeUserResourcePercent", 10L);
    JsonObject nullOrigin = newCreateBuildArguments();
    nullOrigin.addProperty("consumeUserResourcePercent", 10L);
    nullOrigin.add("originEnergyLimit", JsonNull.INSTANCE);
    JSONObject omittedOriginResponse = handleJsonRpc(
        server, "buildTransaction", jsonParams(omittedOrigin));
    JSONObject nullOriginResponse = handleJsonRpc(
        server, "buildTransaction", jsonParams(nullOrigin));
    assertEquivalentCreatedContract(omittedOriginResponse, nullOriginResponse);
    Assert.assertEquals(0L,
        getNewContract(nullOriginResponse).getLongValue("origin_energy_limit"));
    ContractValidateException omittedOriginError = Assert.assertThrows(
        ContractValidateException.class,
        () -> executeBuiltTransaction(omittedOriginResponse));
    ContractValidateException nullOriginError = Assert.assertThrows(
        ContractValidateException.class,
        () -> executeBuiltTransaction(nullOriginResponse));
    Assert.assertEquals("The originEnergyLimit must be > 0", omittedOriginError.getMessage());
    Assert.assertEquals(omittedOriginError.getMessage(), nullOriginError.getMessage());

    JsonObject omittedFrom = newEstimateGasArguments();
    JsonObject nullFrom = newEstimateGasArguments();
    nullFrom.add("from", JsonNull.INSTANCE);
    JSONObject omittedFromResponse = handleJsonRpc(
        server, "eth_estimateGas", jsonParams(omittedFrom));
    JSONObject nullFromResponse = handleJsonRpc(
        server, "eth_estimateGas", jsonParams(nullFrom));
    Assert.assertEquals(omittedFromResponse.toJSONString(), nullFromResponse.toJSONString());
  }

  @Test
  public void testGetTransactionByBlockNumberAndIndex() {
    // valid hex block number: blockCapsule1 has 2 txs; index 0 is transactionCapsule1.
    // Assert the returned tx actually resolves to transactionCapsule1's hash,
    // block number, and index rather than just non-null.
    try {
      TransactionResult result = tronJsonRpc.getTransactionByBlockNumberAndIndex(
          ByteArray.toJsonHex(blockCapsule1.getNum()), "0x0");
      Assert.assertNotNull(result);
      Assert.assertEquals(
          ByteArray.toJsonHex(transactionCapsule1.getTransactionId().getBytes()),
          result.getHash());
      Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getNum()), result.getBlockNumber());
      Assert.assertEquals(ByteArray.toJsonHex(0L), result.getTransactionIndex());
    } catch (Exception e) {
      Assert.fail();
    }

    // index out of range in an existing block returns null
    try {
      TransactionResult result = tronJsonRpc.getTransactionByBlockNumberAndIndex(
          ByteArray.toJsonHex(blockCapsule1.getNum()), "0x5");
      Assert.assertNull(result);
    } catch (Exception e) {
      Assert.fail();
    }

    // negative index is out of range too -> null (not an Internal error)
    try {
      TransactionResult result = tronJsonRpc.getTransactionByBlockNumberAndIndex(
          ByteArray.toJsonHex(blockCapsule1.getNum()), "0x-1");
      Assert.assertNull(result);
    } catch (Exception e) {
      Assert.fail();
    }

    // leading zeros are tolerated: "0x00" parses to index 0
    try {
      TransactionResult result = tronJsonRpc.getTransactionByBlockNumberAndIndex(
          ByteArray.toJsonHex(blockCapsule1.getNum()), "0x00");
      Assert.assertNotNull(result);
      Assert.assertEquals(ByteArray.toJsonHex(0L), result.getTransactionIndex());
    } catch (Exception e) {
      Assert.fail();
    }

    // oversized index (> 8 hex digits) rejected before parsing
    Exception oversizedEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getTransactionByBlockNumberAndIndex(
            ByteArray.toJsonHex(blockCapsule1.getNum()), "0x123456789"));
    Assert.assertEquals("invalid index value", oversizedEx.getMessage());

    // latest -> blockCapsule1 (head)
    try {
      TransactionResult result = tronJsonRpc.getTransactionByBlockNumberAndIndex("latest", "0x0");
      Assert.assertNotNull(result);
      Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getNum()), result.getBlockNumber());
    } catch (Exception e) {
      Assert.fail();
    }

    // finalized -> blockCapsule2 (solid), has 1 tx
    try {
      TransactionResult result =
          tronJsonRpc.getTransactionByBlockNumberAndIndex("finalized", "0x0");
      Assert.assertNotNull(result);
    } catch (Exception e) {
      Assert.fail();
    }

    // non-existent block number returns null (not an error)
    try {
      TransactionResult result = tronJsonRpc.getTransactionByBlockNumberAndIndex("0x1", "0x0");
      Assert.assertNull(result);
    } catch (Exception e) {
      Assert.fail();
    }

    // pending tag rejected
    Exception pendingEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getTransactionByBlockNumberAndIndex("pending", "0x0"));
    Assert.assertEquals(TAG_PENDING_SUPPORT_ERROR, pendingEx.getMessage());

    // safe tag rejected (new tag)
    Exception safeEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getTransactionByBlockNumberAndIndex("safe", "0x0"));
    Assert.assertEquals(TAG_SAFE_SUPPORT_ERROR, safeEx.getMessage());

    // malformed hex rejected
    Exception qqqEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getTransactionByBlockNumberAndIndex("qqq", "0x0"));
    Assert.assertEquals("invalid block number", qqqEx.getMessage());

    // hex overflows long -> longValueExact rejects
    Exception overflowEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getTransactionByBlockNumberAndIndex("0x10000000000000000", "0x0"));
    Assert.assertEquals("invalid block number", overflowEx.getMessage());
  }

  /**
   * Tests the object-form second argument of eth_call:
   * {"blockNumber": "0x..."} or {"blockHash": "0x..."}.
   * Only the block-selector parsing is exercised here; the call()
   * execution path is covered by other tests.
   */
  @Test
  public void testGetCallWithBlockObject() {
    CallArguments validArgs = newValidCallArguments();
    // neither HashMap nor String -> invalid json request
    Exception nonMapEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getCall(validArgs, new Object()));
    Assert.assertEquals("invalid json request", nonMapEx.getMessage());

    // HashMap without blockNumber/blockHash keys -> invalid json request
    Exception emptyMapEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getCall(validArgs, new HashMap<String, String>()));
    Assert.assertEquals("invalid json request", emptyMapEx.getMessage());

    // blockNumber with malformed hex -> invalid block number
    HashMap<String, String> badHexParams = new HashMap<>();
    badHexParams.put("blockNumber", "xxx");
    Exception badHexEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getCall(validArgs, badHexParams));
    Assert.assertEquals("invalid block number", badHexEx.getMessage());

    // blockNumber overflows long -> invalid block number (longValueExact)
    HashMap<String, String> overflowParams = new HashMap<>();
    overflowParams.put("blockNumber", "0x10000000000000000");
    Exception overflowEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getCall(validArgs, overflowParams));
    Assert.assertEquals("invalid block number", overflowEx.getMessage());

    // blockNumber points to a non-existent block -> header not found
    HashMap<String, String> missingNumParams = new HashMap<>();
    missingNumParams.put("blockNumber", "0x1");
    Exception missingNumEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getCall(validArgs, missingNumParams));
    Assert.assertEquals("header not found", missingNumEx.getMessage());

    // blockHash of an unknown block -> header for hash not found
    HashMap<String, String> missingHashParams = new HashMap<>();
    missingHashParams.put("blockHash",
        "0x1111111111111111111111111111111111111111111111111111111111111111");
    Exception missingHashEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getCall(validArgs, missingHashParams));
    Assert.assertEquals("header for hash not found", missingHashEx.getMessage());
  }

  /**
   * test fromBlock and toBlock parameters
   */
  @Test
  public void testLogFilterWrapper() {

    // fromBlock and toBlock are both empty
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest(null, null, null, null, null), 100, null, false);
      Assert.assertEquals(100, logFilterWrapper.getFromBlock());
      Assert.assertEquals(Long.MAX_VALUE, logFilterWrapper.getToBlock());
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    // fromBlock is not empty and smaller than currentMaxBlockNum, toBlock is empty
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest("0x14", null, null, null, null), 100, null, false);
      Assert.assertEquals(20, logFilterWrapper.getFromBlock());
      Assert.assertEquals(Long.MAX_VALUE, logFilterWrapper.getToBlock());
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    // fromBlock is not empty and bigger than currentMaxBlockNum, toBlock is empty
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest("0x78", null, null, null, null), 100, null, false);
      Assert.assertEquals(120, logFilterWrapper.getFromBlock());
      Assert.assertEquals(Long.MAX_VALUE, logFilterWrapper.getToBlock());
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    // fromBlock is empty, toBlock is not empty and smaller than currentMaxBlockNum
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest(null, "0x14", null, null, null), 100, null, false);
      Assert.assertEquals(20, logFilterWrapper.getFromBlock());
      Assert.assertEquals(20, logFilterWrapper.getToBlock());
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    // fromBlock is empty, toBlock is not empty and bigger than currentMaxBlockNum
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest(null, "0x78", null, null, null), 100, null, false);
      Assert.assertEquals(100, logFilterWrapper.getFromBlock());
      Assert.assertEquals(120, logFilterWrapper.getToBlock());
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    // fromBlock is not empty, toBlock is not empty
    try {
      LogFilterWrapper logFilterWrapper = new LogFilterWrapper(new FilterRequest("0x14", "0x78",
          null, null, null), 100, null, false);
      Assert.assertEquals(20, logFilterWrapper.getFromBlock());
      Assert.assertEquals(120, logFilterWrapper.getToBlock());
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    JsonRpcInvalidParamsException fromToEx =
        Assert.assertThrows(JsonRpcInvalidParamsException.class,
            () -> new LogFilterWrapper(new FilterRequest("0x78", "0x14",
                null, null, null), 100, null, false));
    Assert.assertEquals("please verify: fromBlock <= toBlock", fromToEx.getMessage());

    //fromBlock or toBlock is not hex num
    try {
      LogFilterWrapper logFilterWrapper = new LogFilterWrapper(new FilterRequest("earliest", null,
          null, null, null), 100, null, false);
      Assert.assertEquals(0, logFilterWrapper.getFromBlock());
      Assert.assertEquals(Long.MAX_VALUE, logFilterWrapper.getToBlock());
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      LogFilterWrapper logFilterWrapper = new LogFilterWrapper(new FilterRequest("latest", null,
          null, null, null), 100, null, false);
      Assert.assertEquals(100, logFilterWrapper.getFromBlock());
      Assert.assertEquals(Long.MAX_VALUE, logFilterWrapper.getToBlock());
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    JsonRpcInvalidParamsException pendingFilterEx = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> new LogFilterWrapper(new FilterRequest("pending", null, null, null, null),
            100, null, false));
    Assert.assertEquals("TAG pending not supported", pendingFilterEx.getMessage());
    try {
      LogFilterWrapper logFilterWrapper = new LogFilterWrapper(new FilterRequest("finalized", null,
          null, null, null), 100, wallet, false);
      Assert.assertEquals(LATEST_SOLIDIFIED_BLOCK_NUM, logFilterWrapper.getFromBlock());
      Assert.assertEquals(Long.MAX_VALUE, logFilterWrapper.getToBlock());
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    JsonRpcInvalidParamsException testSyntaxEx = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> new LogFilterWrapper(new FilterRequest("test", null, null, null, null),
            100, null, false));
    Assert.assertEquals("Incorrect hex syntax", testSyntaxEx.getMessage());

    // to = 8000
    try {
      new LogFilterWrapper(new FilterRequest("0x0", "0x1f40", null,
          null, null), LATEST_BLOCK_NUM, null, false);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    JsonRpcInvalidParamsException rangeEx1 = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> new LogFilterWrapper(new FilterRequest("0x0", "0x1f40", null,
            null, null), LATEST_BLOCK_NUM, null, true));
    Assert.assertEquals(
        "exceed max block range: " + Args.getInstance().jsonRpcMaxBlockRange,
        rangeEx1.getMessage());

    try {
      new LogFilterWrapper(new FilterRequest("0x0", "latest", null,
          null, null), LATEST_BLOCK_NUM, null, false);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    JsonRpcInvalidParamsException rangeEx2 = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> new LogFilterWrapper(new FilterRequest("0x0", "latest", null,
            null, null), LATEST_BLOCK_NUM, null, true));
    Assert.assertEquals(
        "exceed max block range: " + Args.getInstance().jsonRpcMaxBlockRange,
        rangeEx2.getMessage());

    // from = 100, current = 5_000, to = Long.MAX_VALUE
    try {
      new LogFilterWrapper(new FilterRequest("0x64", "latest", null,
          null, null), 5_000, null, true);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      new LogFilterWrapper(new FilterRequest("0x64", "latest", null,
          null, null), LATEST_BLOCK_NUM, null, false);
    } catch (Exception e) {
      Assert.fail();
    }

    // from = 100
    JsonRpcInvalidParamsException rangeEx3 = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> new LogFilterWrapper(new FilterRequest("0x64", "latest", null,
            null, null), LATEST_BLOCK_NUM, null, true));
    Assert.assertEquals(
        "exceed max block range: " + Args.getInstance().jsonRpcMaxBlockRange,
        rangeEx3.getMessage());
    try {
      new LogFilterWrapper(new FilterRequest("0x64", "latest", null,
          null, null), LATEST_BLOCK_NUM, null, false);
    } catch (Exception e) {
      Assert.fail();
    }

    // from = 9_000
    try {
      new LogFilterWrapper(new FilterRequest("0x2328", "latest", null,
          null, null), LATEST_BLOCK_NUM, null, true);
    } catch (Exception e) {
      Assert.fail();
    }
    try {
      new LogFilterWrapper(new FilterRequest("0x2328", "latest", null,
          null, null), LATEST_BLOCK_NUM, null, false);
    } catch (Exception e) {
      Assert.fail();
    }

    try {
      new LogFilterWrapper(new FilterRequest("latest", "latest", null,
          null, null), LATEST_BLOCK_NUM, null, true);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      new LogFilterWrapper(new FilterRequest("latest", "latest", null,
          null, null), LATEST_BLOCK_NUM, null, false);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    int oldMaxBlockRange = Args.getInstance().getJsonRpcMaxBlockRange();
    Args.getInstance().setJsonRpcMaxBlockRange(10_000);
    try {
      new LogFilterWrapper(new FilterRequest("0x0", "0x1f40", null,
          null, null), LATEST_BLOCK_NUM, null, true);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      new LogFilterWrapper(new FilterRequest("0x0", "0x1f40", null,
          null, null), LATEST_BLOCK_NUM, null, false);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    Args.getInstance().setJsonRpcMaxBlockRange(0);
    try {
      new LogFilterWrapper(new FilterRequest("0x0", "0x1f40", null,
          null, null), LATEST_BLOCK_NUM, null, true);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      new LogFilterWrapper(new FilterRequest("0x0", "0x1f40", null,
          null, null), LATEST_BLOCK_NUM, null, false);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    Args.getInstance().setJsonRpcMaxBlockRange(-2);
    try {
      new LogFilterWrapper(new FilterRequest("0x0", "0x1f40", null,
          null, null), LATEST_BLOCK_NUM, null, true);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      new LogFilterWrapper(new FilterRequest("0x0", "0x1f40", null,
          null, null), LATEST_BLOCK_NUM, null, false);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    JsonRpcInvalidParamsException shortHashEx = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> new LogFilterWrapper(new FilterRequest(null, null, null,
            null, "0x111111"),
            100, null, false));
    Assert.assertEquals("invalid hash value", shortHashEx.getMessage());

    JsonRpcInvalidParamsException oversizedHashEx = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> new LogFilterWrapper(new FilterRequest(null, null, null,
            null,
            "0x" + new String(new char[1000]).replace('\0', 'a')),
            100, null, false));
    Assert.assertEquals("invalid hash value", oversizedHashEx.getMessage());

    JsonRpcInvalidParamsException validHashEx = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> new LogFilterWrapper(new FilterRequest(null, null, null,
            null,
            "0x" + new String(new char[64]).replace('\0', 'a')),
            100, null, false));
    Assert.assertEquals("invalid blockHash", validHashEx.getMessage());

    // reset
    Args.getInstance().setJsonRpcMaxBlockRange(oldMaxBlockRange);
  }

  @Test
  public void testMaxSubTopics() {
    List<Object> topics = new ArrayList<>();
    topics.add(new ArrayList<>(Collections.singletonList(
        "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef")));
    topics.add(new ArrayList<>(Collections.EMPTY_LIST));
    List<String> subTopics = new ArrayList<>();
    for (int i = 0; i < Args.getInstance().getJsonRpcMaxSubTopics() + 1; i++) {
      subTopics.add("0x0000000000000000000000414de17123a3c706ab197957e131350b2537dd4883");
    }
    topics.add(subTopics);

    JsonRpcInvalidParamsException topicsEx = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> new LogFilterWrapper(new FilterRequest("0xbb8", "0x1f40",
            null, topics.toArray(), null), LATEST_BLOCK_NUM, null, false));
    Assert.assertEquals(
        "exceed max topics: " + Args.getInstance().getJsonRpcMaxSubTopics(),
        topicsEx.getMessage());

    try {
      tronJsonRpc.getLogs(new FilterRequest("0xbb8", "0x1f40",
          null, topics.toArray(), null));
      Assert.fail("Expected to be thrown");
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals(
          "exceed max topics: " + Args.getInstance().getJsonRpcMaxSubTopics(),
          e.getMessage());
    } catch (Exception e) {
      Assert.fail();
    }

    try {
      tronJsonRpc.newFilter(new FilterRequest("0xbb8", "0x1f40",
          null, topics.toArray(), null));
      Assert.fail("Expected to be thrown");
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals(
          "exceed max topics: " + Args.getInstance().getJsonRpcMaxSubTopics(),
          e.getMessage());
    } catch (Exception e) {
      Assert.fail();
    }

    int oldMaxSubTopics = Args.getInstance().getJsonRpcMaxSubTopics();
    Args.getInstance().setJsonRpcMaxSubTopics(2_000);
    try {
      new LogFilterWrapper(new FilterRequest("0xbb8", "0x1f40",
          null, topics.toArray(), null), LATEST_BLOCK_NUM, null, false);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    Args.getInstance().setJsonRpcMaxSubTopics(0);
    try {
      new LogFilterWrapper(new FilterRequest("0xbb8", "0x1f40",
          null, topics.toArray(), null), LATEST_BLOCK_NUM, null, false);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      tronJsonRpc.newFilter(new FilterRequest("0xbb8", "0x1f40",
          null, topics.toArray(), null));
    } catch (Exception e) {
      Assert.fail();
    }

    Args.getInstance().setJsonRpcMaxSubTopics(-2);
    try {
      new LogFilterWrapper(new FilterRequest("0xbb8", "0x1f40",
          null, topics.toArray(), null), LATEST_BLOCK_NUM, null, false);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      tronJsonRpc.newFilter(new FilterRequest("0xbb8", "0x1f40",
          null, topics.toArray(), null));
    } catch (Exception e) {
      Assert.fail();
    }

    Args.getInstance().setJsonRpcMaxSubTopics(oldMaxSubTopics);
  }

  @Test
  public void testMethodBlockRange() {
    try {
      tronJsonRpc.getLogs(new FilterRequest("0x0", "0x1f40", null,
          null, null));
      Assert.fail("Expected to be thrown");
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals(
          "exceed max block range: " + Args.getInstance().jsonRpcMaxBlockRange,
          e.getMessage());
    } catch (Exception e) {
      Assert.fail();
    }

    try {
      tronJsonRpc.newFilter(new FilterRequest("0x0", "0x1f40", null,
          null, null));
    } catch (Exception e) {
      Assert.fail();
    }

    try {
      tronJsonRpc.getLogs(new FilterRequest("0x0", "0x1", null,
          null, null));
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testGetLogs() {
    try {
      LogFilterElement[] logs = tronJsonRpc.getLogs(
          new FilterRequest("0x2710", "0x2710", null, null, null));
      Assert.assertTrue(logs.length > 0);
      LogFilterElement log = logs[0];
      Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getNum()), log.getBlockNumber());
      Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getBlockId().toString()),
          log.getBlockHash());
      Assert.assertEquals("0x0", log.getLogIndex());
      Assert.assertFalse(log.isRemoved());
      Assert.assertEquals(1, log.getTopics().length);
      Assert.assertEquals(
          "0x0000000000000000000000000000000000000000000000000000746f70696331",
          log.getTopics()[0]);
      Assert.assertEquals(ByteArray.toJsonHex("data1".getBytes()), log.getData());
      Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getTimeStamp() / 1000),
          log.getBlockTimestamp());
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testNewFilterFinalizedBlock() {

    try {
      tronJsonRpc.newFilter(new FilterRequest(null, null, null, null, null));
    } catch (Exception e) {
      Assert.fail();
    }

    Exception e1 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.newFilter(new FilterRequest("finalized", null, null, null, null)));
    Assert.assertEquals("invalid block range params", e1.getMessage());

    Exception e2 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.newFilter(new FilterRequest(null, "finalized", null, null, null)));
    Assert.assertEquals("invalid block range params", e2.getMessage());

    Exception e3 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.newFilter(new FilterRequest("finalized", "latest", null, null, null)));
    Assert.assertEquals("invalid block range params", e3.getMessage());

    Exception e4 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.newFilter(new FilterRequest("0x1", "finalized", null, null, null)));
    Assert.assertEquals("invalid block range params", e4.getMessage());

    Exception e5 = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.newFilter(new FilterRequest("finalized", "finalized", null, null, null)));
    Assert.assertEquals("invalid block range params", e5.getMessage());
  }

  /**
   * Tag handling at the RPC boundary for eth_newFilter / eth_getLogs / eth_getFilterLogs.
   * - safe/pending are rejected inside LogFilterWrapper (parseBlockNumber -> parseBlockTag)
   * - finalized is intercepted by newFilter's upfront guard, but allowed by getLogs
   * - getFilterLogs round-trips a filter created with concrete block numbers
   */
  @Test
  public void testLogFilterTagHandling() {
    // eth_newFilter: safe in fromBlock -> TAG_SAFE_SUPPORT_ERROR
    Exception newFilterSafeFromEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.newFilter(new FilterRequest("safe", null, null, null, null)));
    Assert.assertEquals(TAG_SAFE_SUPPORT_ERROR, newFilterSafeFromEx.getMessage());

    // eth_newFilter: safe in toBlock
    Exception newFilterSafeToEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.newFilter(new FilterRequest("0x1", "safe", null, null, null)));
    Assert.assertEquals(TAG_SAFE_SUPPORT_ERROR, newFilterSafeToEx.getMessage());

    // eth_newFilter: pending in fromBlock
    Exception newFilterPendingFromEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.newFilter(new FilterRequest("pending", null, null, null, null)));
    Assert.assertEquals(TAG_PENDING_SUPPORT_ERROR, newFilterPendingFromEx.getMessage());

    // eth_newFilter: pending in toBlock
    Exception newFilterPendingToEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.newFilter(new FilterRequest("0x1", "pending", null, null, null)));
    Assert.assertEquals(TAG_PENDING_SUPPORT_ERROR, newFilterPendingToEx.getMessage());

    // eth_getLogs: safe in fromBlock
    Exception getLogsSafeFromEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getLogs(new FilterRequest("safe", null, null, null, null)));
    Assert.assertEquals(TAG_SAFE_SUPPORT_ERROR, getLogsSafeFromEx.getMessage());

    // eth_getLogs: safe in toBlock
    Exception getLogsSafeToEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getLogs(new FilterRequest(null, "safe", null, null, null)));
    Assert.assertEquals(TAG_SAFE_SUPPORT_ERROR, getLogsSafeToEx.getMessage());

    // eth_getLogs: pending in fromBlock
    Exception getLogsPendingFromEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getLogs(new FilterRequest("pending", null, null, null, null)));
    Assert.assertEquals(TAG_PENDING_SUPPORT_ERROR, getLogsPendingFromEx.getMessage());

    // eth_getLogs: pending in toBlock
    Exception getLogsPendingToEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getLogs(new FilterRequest(null, "pending", null, null, null)));
    Assert.assertEquals(TAG_PENDING_SUPPORT_ERROR, getLogsPendingToEx.getMessage());

    // eth_getLogs: finalized is accepted (resolves to solidBlockNum via parseBlockTag).
    // With fromBlock empty, Strategy 2 resolves the range to [solid, solid]. blockCapsule2
    // (solid=4) has no logs in test fixtures, so result must be empty.
    try {
      LogFilterElement[] result =
          tronJsonRpc.getLogs(new FilterRequest(null, "finalized", null, null, null));
      Assert.assertNotNull(result);
      Assert.assertEquals(0, result.length);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    // End-to-end happy path for eth_getLogs and eth_getFilterLogs.
    // Query range [head, head] = [blockCapsule1, blockCapsule1]. No address/topic filter,
    // so LogBlockQuery marks all blocks in the range as candidates. LogMatch then iterates
    // blockCapsule1's 2 txs * 2 logs each = 4 LogFilterElements.
    String headHex = ByteArray.toJsonHex(blockCapsule1.getNum());
    int expectedLogs = blockCapsule1.getTransactions().size() * 2;

    try {
      LogFilterElement[] directResult =
          tronJsonRpc.getLogs(new FilterRequest(headHex, headHex, null, null, null));
      Assert.assertEquals(expectedLogs, directResult.length);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    try {
      String filterIdHex = tronJsonRpc.newFilter(
          new FilterRequest(headHex, headHex, null, null, null));
      LogFilterElement[] filterResult = tronJsonRpc.getFilterLogs(filterIdHex);
      Assert.assertEquals(expectedLogs, filterResult.length);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testGetFilterLogsEnforcesBlockRange() throws Exception {
    int oldMaxBlockRange = Args.getInstance().getJsonRpcMaxBlockRange();
    Args.getInstance().setJsonRpcMaxBlockRange(5_000);
    try {
      // toBlock=0xf4240 (1_000_000) is clamped to head (LATEST_BLOCK_NUM = 10_000), so the span
      // is min(1_000_000, 10_000) - 0 = 10_000 > 5_000 cap.
      String wideFilterId = tronJsonRpc.newFilter(
          new FilterRequest("0x0", "0xf4240", null, null, null));
      Assert.assertNotNull(wideFilterId);

      // eth_getFilterLogs must reject it at query time (previously bypassed -> unbounded scan).
      JsonRpcInvalidParamsException ex = Assert.assertThrows(
          JsonRpcInvalidParamsException.class,
          () -> tronJsonRpc.getFilterLogs(wideFilterId));
      Assert.assertEquals("exceed max block range: 5000", ex.getMessage());

      // A within-cap filter (span 0) is still served normally - no false rejection.
      String headHex = ByteArray.toJsonHex(blockCapsule1.getNum());
      int expectedLogs = blockCapsule1.getTransactions().size() * 2;
      String okFilterId = tronJsonRpc.newFilter(
          new FilterRequest(headHex, headHex, null, null, null));
      LogFilterElement[] okResult = tronJsonRpc.getFilterLogs(okFilterId);
      Assert.assertEquals(expectedLogs, okResult.length);
    } finally {
      Args.getInstance().setJsonRpcMaxBlockRange(oldMaxBlockRange);
    }
  }

  @Test
  public void testGetBlockReceipts() {

    try {
      List<TransactionReceipt> transactionReceiptList = tronJsonRpc.getBlockReceipts("0x2710");
      Assert.assertFalse(transactionReceiptList.isEmpty());
      for (TransactionReceipt transactionReceipt: transactionReceiptList) {
        TransactionReceipt transactionReceipt1
            = tronJsonRpc.getTransactionReceipt(transactionReceipt.getTransactionHash());

        Assert.assertEquals(
            JSON.toJSONString(transactionReceipt), JSON.toJSONString(transactionReceipt1));

        Assert.assertTrue(transactionReceipt1.getLogs().length > 0);
        Assert.assertEquals(ByteArray.toJsonHex(blockCapsule1.getTimeStamp() / 1000),
            transactionReceipt1.getLogs()[0].getBlockTimestamp());
      }
    } catch (JsonRpcInvalidParamsException | JsonRpcInternalException e) {
      throw new RuntimeException(e);
    }

    try {
      List<TransactionReceipt> transactionReceiptList = tronJsonRpc.getBlockReceipts("earliest");
      Assert.assertNull(transactionReceiptList);
    } catch (JsonRpcInvalidParamsException | JsonRpcInternalException e) {
      throw new RuntimeException(e);
    }

    try {
      List<TransactionReceipt> transactionReceiptList = tronJsonRpc.getBlockReceipts("latest");
      Assert.assertFalse(transactionReceiptList.isEmpty());
    } catch (JsonRpcInvalidParamsException | JsonRpcInternalException e) {
      throw new RuntimeException(e);
    }

    try {
      List<TransactionReceipt> transactionReceiptList = tronJsonRpc.getBlockReceipts("finalized");
      Assert.assertFalse(transactionReceiptList.isEmpty());
    } catch (JsonRpcInvalidParamsException | JsonRpcInternalException e) {
      throw new RuntimeException(e);
    }

    Exception pendingReceiptsEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getBlockReceipts("pending"));
    Assert.assertEquals(TAG_PENDING_SUPPORT_ERROR, pendingReceiptsEx.getMessage());

    Exception testReceiptsEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getBlockReceipts("test"));
    Assert.assertEquals("invalid block number", testReceiptsEx.getMessage());

    Exception nullReceiptsEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getBlockReceipts(null));
    Assert.assertEquals("invalid block number", nullReceiptsEx.getMessage());

    try {
      List<TransactionReceipt> transactionReceiptList = tronJsonRpc.getBlockReceipts("0x2");
      Assert.assertNull(transactionReceiptList);
    } catch (JsonRpcInvalidParamsException | JsonRpcInternalException e) {
      throw new RuntimeException(e);
    }

    try {
      String blockHash = blockCapsule1.getBlockId().toString();
      List<TransactionReceipt> transactionReceiptList
          = tronJsonRpc.getBlockReceipts(blockHash);
      List<TransactionReceipt> transactionReceiptList2
          = tronJsonRpc.getBlockReceipts("0x" + blockHash);

      Assert.assertFalse(transactionReceiptList.isEmpty());
      Assert.assertEquals(JSON.toJSONString(transactionReceiptList),
          JSON.toJSONString(transactionReceiptList2));
    } catch (JsonRpcInvalidParamsException | JsonRpcInternalException e) {
      throw new RuntimeException(e);
    }

    Exception safeReceiptsEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getBlockReceipts("safe"));
    Assert.assertEquals(TAG_SAFE_SUPPORT_ERROR, safeReceiptsEx.getMessage());

    Exception overflowReceiptsEx = Assert.assertThrows(Exception.class,
        () -> tronJsonRpc.getBlockReceipts("0x10000000000000000"));
    Assert.assertEquals("invalid block number", overflowReceiptsEx.getMessage());
  }

  @Test
  public void testBuildTransactionTransfer() {
    // End-to-end smoke test for the buildTransaction JSON-RPC path:
    // posts through fullNodeJsonRpcHttpService and asserts the response's
    // `transaction` field is a structurally valid JSON object with the
    // expected protobuf-derived fields (type, amount). Coverage was
    // missing before; not specific to the fastjson→jackson migration.
    fullNodeJsonRpcHttpService.start();
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      JsonObject buildArgs = new JsonObject();
      buildArgs.addProperty("from", "0xabd4b9367799eaa3197fecb144eb71de1e049abc");
      buildArgs.addProperty("to", "0x548794500882809695a8a687866e76d4271a1abc");
      buildArgs.addProperty("value", "0x1f4");
      JsonArray params = new JsonArray();
      params.add(buildArgs);
      JsonObject requestBody = new JsonObject();
      requestBody.addProperty("jsonrpc", "2.0");
      requestBody.addProperty("method", "buildTransaction");
      requestBody.add("params", params);
      requestBody.addProperty("id", 1);

      HttpPost httpPost = new HttpPost("http://127.0.0.1:"
          + CommonParameter.getInstance().getJsonRpcHttpFullNodePort() + "/jsonrpc");
      httpPost.addHeader("Content-Type", "application/json");
      httpPost.setEntity(new StringEntity(requestBody.toString()));
      try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
        String resp = EntityUtils.toString(response.getEntity());
        JSONObject tx = JSON.parseObject(resp).getJSONObject("result")
            .getJSONObject("transaction");
        Assert.assertNotNull("transaction must be a JSON object", tx);
        Assert.assertNotNull(tx.getString("txID"));
        Assert.assertNotNull(tx.getString("raw_data_hex"));

        JSONArray contracts = tx.getJSONObject("raw_data").getJSONArray("contract");
        Assert.assertEquals(1, contracts.size());
        JSONObject contract = contracts.getJSONObject(0);
        Assert.assertEquals("TransferContract", contract.getString("type"));
        Assert.assertEquals(500L, contract.getJSONObject("parameter")
            .getJSONObject("value").getLongValue("amount"));
      }
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    } finally {
      fullNodeJsonRpcHttpService.stop();
    }
  }

  @Test
  public void testBuildCreateSmartContractAcceptsNullAbiOutputsOverHttp() {
    fullNodeJsonRpcHttpService.start();
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      JsonObject buildArgs = new JsonObject();
      buildArgs.addProperty("from", "0xabd4b9367799eaa3197fecb144eb71de1e049abc");
      buildArgs.addProperty("data", "608060405234801561001057600080fd5b50");
      buildArgs.addProperty("gas", "0x3b9aca00");
      buildArgs.addProperty("abi", "[{\"inputs\":[],\"name\":\"test\",\"outputs\":null,"
          + "\"type\":\"function\"}]");
      JsonArray params = new JsonArray();
      params.add(buildArgs);
      JsonObject requestBody = new JsonObject();
      requestBody.addProperty("jsonrpc", "2.0");
      requestBody.addProperty("method", "buildTransaction");
      requestBody.add("params", params);
      requestBody.addProperty("id", 1);

      HttpPost httpPost = new HttpPost("http://127.0.0.1:"
          + CommonParameter.getInstance().getJsonRpcHttpFullNodePort() + "/jsonrpc");
      httpPost.addHeader("Content-Type", "application/json");
      httpPost.setEntity(new StringEntity(requestBody.toString()));
      try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
        String resp = EntityUtils.toString(response.getEntity());
        JSONObject json = JSON.parseObject(resp);
        Assert.assertNull(resp, json.getJSONObject("error"));
        JSONObject tx = json.getJSONObject("result").getJSONObject("transaction");
        Assert.assertNotNull("transaction must be a JSON object", tx);

        JSONArray contracts = tx.getJSONObject("raw_data").getJSONArray("contract");
        Assert.assertEquals(1, contracts.size());
        JSONObject contract = contracts.getJSONObject(0);
        Assert.assertEquals("CreateSmartContract", contract.getString("type"));
        JSONObject value = contract.getJSONObject("parameter").getJSONObject("value");
        JSONObject abi = value.getJSONObject("new_contract").getJSONObject("abi");
        JSONArray entrys = abi.getJSONArray("entrys");
        Assert.assertEquals(1, entrys.size());
        Assert.assertFalse(entrys.getJSONObject(0).containsKey("outputs"));
      }
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    } finally {
      fullNodeJsonRpcHttpService.stop();
    }
  }

  @Test
  public void testBuildTransactionRejectsDeeplyNestedAbi() {
    // A deeply nested ABI must surface as invalid-params (-32602), not as a generic
    // internal error.
    int depth = 200_000;
    StringBuilder abi = new StringBuilder("[],\"x\":");
    for (int i = 0; i < depth; i++) {
      abi.append('[');
    }
    for (int i = 0; i < depth; i++) {
      abi.append(']');
    }

    BuildArguments args = new BuildArguments();
    args.setFrom("0xabd4b9367799eaa3197fecb144eb71de1e049abc");
    args.setData("60806040");
    args.setAbi(abi.toString());

    JsonRpcInvalidParamsException e = Assert.assertThrows(JsonRpcInvalidParamsException.class,
        () -> tronJsonRpc.buildTransaction(args));
    Assert.assertEquals("invalid abi", e.getMessage());
  }

  @Test
  public void testWeb3ClientVersion() {
    try {
      String[] versions = tronJsonRpc.web3ClientVersion().split("/");
      String javaVersion = versions[versions.length - 1];
      Assert.assertTrue("Java1.8".equals(javaVersion) || "Java17".equals(javaVersion));
    } catch (Exception e) {
      Assert.fail();
    }
  }

  /**
   * Verifies SizeLimitHandler integration with the real JsonRpcServlet + jsonrpc4j stack.
   *
   * Covers: normal request no regression, Content-Length oversized 413,
   * and chunked oversized 413 during streaming body reads.
   */
  @Test
  public void testJsonRpcSizeLimitIntegration() {
    long testLimit = 1024;
    long originalLimit = fullNodeJsonRpcHttpService.getMaxRequestSize();
    try {
      fullNodeJsonRpcHttpService.setMaxRequestSize(testLimit);

      fullNodeJsonRpcHttpService.start();
      String url = "http://127.0.0.1:"
          + CommonParameter.getInstance().getJsonRpcHttpFullNodePort() + "/jsonrpc";

      try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        // Normal JSON-RPC request passes through SizeLimitHandler
        JsonObject req = new JsonObject();
        req.addProperty("jsonrpc", "2.0");
        req.addProperty("method", "web3_clientVersion");
        req.addProperty("id", 1);

        HttpPost post = new HttpPost(url);
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(req.toString()));
        CloseableHttpResponse resp = httpClient.execute(post);
        Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
        String body = EntityUtils.toString(resp.getEntity());
        Assert.assertTrue("Normal JSON-RPC response should contain result",
            body.contains("result"));
        resp.close();

        // Oversized request with Content-Length -> 413 before JsonRpcServlet
        HttpPost overPost = new HttpPost(url);
        overPost.addHeader("Content-Type", "application/json");
        overPost.setEntity(new StringEntity(
            new String(new char[(int) testLimit + 1]).replace('\0', 'x')));
        resp = httpClient.execute(overPost);
        Assert.assertEquals(HttpStatus.PAYLOAD_TOO_LARGE_413,
            resp.getStatusLine().getStatusCode());
        resp.close();

        // Chunked oversized -> BadMessageException thrown during body read.
        // Body read IS truncated at the limit - OOM protection effective.
        byte[] chunkedData = new String(new char[(int) testLimit * 2])
            .replace('\0', 'x').getBytes("UTF-8");
        HttpPost chunkedPost = new HttpPost(url);
        chunkedPost.setEntity(new InputStreamEntity(
            new ByteArrayInputStream(chunkedData), -1));
        resp = httpClient.execute(chunkedPost);
        Assert.assertEquals(HttpStatus.PAYLOAD_TOO_LARGE_413,
            resp.getStatusLine().getStatusCode());
        resp.close();
      }
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    } finally {
      fullNodeJsonRpcHttpService.setMaxRequestSize(originalLimit);
      fullNodeJsonRpcHttpService.stop();
    }
  }

  private static JsonRpcServer newJsonRpcServer() {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Object compositeService = ProxyUtil.createCompositeServiceProxy(
        classLoader,
        new Object[] {tronJsonRpc},
        new Class[] {TronJsonRpc.class},
        true);
    JsonRpcServer server = new JsonRpcServer(compositeService);
    server.setErrorResolver(JsonRpcErrorResolver.INSTANCE);
    server.setShouldLogInvocationErrors(false);
    return server;
  }

  private static JSONObject handleJsonRpc(JsonRpcServer server, String method, JsonArray params)
      throws Exception {
    JsonObject request = new JsonObject();
    request.addProperty("jsonrpc", "2.0");
    request.addProperty("method", method);
    request.add("params", params);
    request.addProperty("id", 1);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    server.handleRequest(
        new ByteArrayInputStream(request.toString().getBytes("UTF-8")), output);
    String responseBody = output.toString("UTF-8");

    Assert.assertFalse(responseBody, responseBody.contains("java."));
    Assert.assertFalse(responseBody, responseBody.contains("NullPointerException"));
    JSONObject response = JSON.parseObject(responseBody);
    Assert.assertEquals("2.0", response.getString("jsonrpc"));
    Assert.assertEquals(1, response.getIntValue("id"));
    return response;
  }

  private static JsonArray jsonParams(Object... values) {
    JsonArray params = new JsonArray();
    for (Object value : values) {
      if (value == null) {
        params.add(JsonNull.INSTANCE);
      } else if (value instanceof JsonElement) {
        params.add((JsonElement) value);
      } else if (value instanceof String) {
        params.add((String) value);
      } else if (value instanceof Boolean) {
        params.add((Boolean) value);
      } else if (value instanceof Number) {
        params.add((Number) value);
      } else {
        throw new IllegalArgumentException("unsupported JSON parameter type: " + value.getClass());
      }
    }
    return params;
  }

  private static void assertMappedError(JSONObject response, int code, String message) {
    JSONObject error = response.getJSONObject("error");
    Assert.assertNotNull(response.toJSONString(), error);
    Assert.assertEquals(code, error.getIntValue("code"));
    Assert.assertEquals(message, error.getString("message"));
    Assert.assertTrue(error.toJSONString(), error.containsKey("data"));
    Assert.assertEquals("{}", error.get("data"));
  }

  private static void assertFrameworkError(JSONObject response, int code, String message) {
    JSONObject error = response.getJSONObject("error");
    Assert.assertNotNull(response.toJSONString(), error);
    Assert.assertEquals(code, error.getIntValue("code"));
    Assert.assertEquals(message, error.getString("message"));
    Assert.assertFalse(error.toJSONString(), error.containsKey("data"));
  }

  private static void assertSuccessfulLogsResult(JSONObject response) {
    Assert.assertFalse(response.toJSONString(), response.containsKey("error"));
    Assert.assertNotNull(response.toJSONString(), response.getJSONArray("result"));
  }

  private static void assertBooleanResult(JSONObject response, boolean expected) {
    Assert.assertEquals(response.toJSONString(), Boolean.valueOf(expected), response.get("result"));
    Assert.assertFalse(response.toJSONString(), response.containsKey("error"));
  }

  private static JsonObject newTransferBuildArguments() {
    JsonObject args = new JsonObject();
    args.addProperty("from", "0xabd4b9367799eaa3197fecb144eb71de1e049abc");
    args.addProperty("to", "0x548794500882809695a8a687866e76d4271a1abc");
    args.addProperty("value", "0x1f4");
    return args;
  }

  private static JsonObject newCreateBuildArguments() {
    JsonObject args = new JsonObject();
    args.addProperty("from", "0xabd4b9367799eaa3197fecb144eb71de1e049abc");
    args.addProperty("data", "60006000f3");
    args.addProperty("gas", "0xf4240");
    args.addProperty("abi", "[]");
    return args;
  }

  private static JsonObject newEstimateGasArguments() {
    JsonObject args = new JsonObject();
    args.addProperty("to", "0x548794500882809695a8a687866e76d4271a1abc");
    args.addProperty("value", "0x1");
    return args;
  }

  private static void assertSuccessfulResponse(JSONObject response) {
    Assert.assertFalse(response.toJSONString(), response.containsKey("error"));
    Assert.assertNotNull(response.toJSONString(), response.getJSONObject("result"));
  }

  private void assertSuccessfulVmExecution(JSONObject response) throws Exception {
    TransactionTrace trace = executeBuiltTransaction(response);
    Assert.assertEquals(contractResult.SUCCESS, trace.getRuntimeResult().getResultCode());
    Assert.assertFalse(trace.getRuntimeResult().isRevert());
    Assert.assertNull(trace.getRuntimeError());
  }

  private TransactionTrace executeBuiltTransaction(JSONObject response) throws Exception {
    assertSuccessfulResponse(response);
    JSONObject transactionJson = response.getJSONObject("result").getJSONObject("transaction");
    Transaction transaction = Util.packTransaction(transactionJson.toJSONString(), false);
    Assert.assertNotNull(transactionJson.toJSONString(), transaction);

    boolean loaderWasDisabled = ConfigLoader.disable;
    boolean energyLimitForkWasEnabled = VMConfig.getEnergyLimitHardFork();
    try {
      ConfigLoader.disable = true;
      VMConfig.initVmHardFork(true);
      try (ISession ignored = dbManager.getRevokingStore().buildSession()) {
        dbManager.getDynamicPropertiesStore().saveMaxCpuTimeOfOneTx(5_000L);
        TransactionTrace trace = new TransactionTrace(
            new TransactionCapsule(transaction), StoreFactory.getInstance(), new RuntimeImpl());
        trace.init(null);
        trace.exec();
        return trace;
      }
    } finally {
      ConfigLoader.disable = loaderWasDisabled;
      VMConfig.initVmHardFork(energyLimitForkWasEnabled);
    }
  }

  private static void assertEquivalentBuiltTransaction(JSONObject expected, JSONObject actual) {
    assertSuccessfulResponse(expected);
    assertSuccessfulResponse(actual);
    JSONObject expectedRawData = expected.getJSONObject("result")
        .getJSONObject("transaction").getJSONObject("raw_data");
    JSONObject actualRawData = actual.getJSONObject("result")
        .getJSONObject("transaction").getJSONObject("raw_data");
    Assert.assertEquals(expectedRawData.getJSONArray("contract").toJSONString(),
        actualRawData.getJSONArray("contract").toJSONString());
    Assert.assertEquals(expectedRawData.get("data"), actualRawData.get("data"));
  }

  private static void assertEquivalentCreatedContract(JSONObject expected, JSONObject actual) {
    assertSuccessfulResponse(expected);
    assertSuccessfulResponse(actual);
    Assert.assertEquals(getNewContract(expected).toJSONString(),
        getNewContract(actual).toJSONString());
  }

  private static JSONObject getNewContract(JSONObject response) {
    return response.getJSONObject("result")
        .getJSONObject("transaction")
        .getJSONObject("raw_data")
        .getJSONArray("contract")
        .getJSONObject(0)
        .getJSONObject("parameter")
        .getJSONObject("value")
        .getJSONObject("new_contract");
  }

  private static CallArguments newValidCallArguments() {
    CallArguments args = new CallArguments();
    args.setFrom("0x0000000000000000000000000000000000000000");
    args.setTo("0x0000000000000000000000000000000000000001");
    args.setValue("0x0");
    args.setData("0x");
    return args;
  }
}
