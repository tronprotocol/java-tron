package org.tron.core.jsonrpc;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getByJsonBlockId;

import com.alibaba.fastjson.JSON;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.prometheus.client.CollectorRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.Metrics;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.utils.BlockUtil;
import org.tron.core.config.args.Args;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.interfaceJsonRpcOnPBFT.JsonRpcServiceOnPBFT;
import org.tron.core.services.interfaceJsonRpcOnSolidity.JsonRpcServiceOnSolidity;
import org.tron.core.services.jsonrpc.FullNodeJsonRpcHttpService;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.filters.LogFilterWrapper;
import org.tron.core.services.jsonrpc.types.BlockResult;
import org.tron.core.services.jsonrpc.types.TransactionResult;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.TransferContract;


@Slf4j
public class JsonrpcServiceTest extends BaseTest {

  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "first";
  private static final long LATEST_BLOCK_NUM = 10_000L;
  private static final long LATEST_SOLIDIFIED_BLOCK_NUM = 4L;

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
    Args.setParam(new String[] {"--output-directory", dbPath()}, Constant.TEST_CONF);
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
            "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))), 1,
        ByteString.copyFromUtf8("testAddress"));
    blockCapsule2 = new BlockCapsule(LATEST_SOLIDIFIED_BLOCK_NUM, Sha256Hash.wrap(
        ByteString.copyFrom(ByteArray.fromHexString(
            "9938a342238077182498b464ac029222ae169360e540d1fd6aee7c2ae9575a06"))), 1,
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
    TransactionCapsule transactionCapsule2 = new TransactionCapsule(transferContract2,
        ContractType.TransferContract);
    transactionCapsule2.setBlockNum(blockCapsule1.getNum());
    TransactionCapsule transactionCapsule3 = new TransactionCapsule(transferContract3,
        ContractType.TransferContract);
    transactionCapsule3.setBlockNum(blockCapsule2.getNum());

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

    tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet, dbManager);
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
    String result = "";
    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByNumber("0x0");
    } catch (Exception e) {
      Assert.assertNull(result);
    }

    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByNumber("pending");
    } catch (Exception e) {
      Assert.assertEquals("TAG pending not supported", e.getMessage());
    }

    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByNumber("qqqqq");
    } catch (Exception e) {
      Assert.assertEquals("invalid block number", e.getMessage());
    }

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
    try {
      tronJsonRpc.ethGetBlockByNumber("pending", false);
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG pending not supported", e.getMessage());
    }

    // invalid
    try {
      tronJsonRpc.ethGetBlockByNumber("0x", false);
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("invalid block number", e.getMessage());
    }
  }

  @Test
  public void testGetTransactionByHash() {
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
  public void testGetByJsonBlockId() {
    long blkNum = 0;

    try {
      getByJsonBlockId("pending", wallet);
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG pending not supported", e.getMessage());
    }

    try {
      blkNum = getByJsonBlockId(null, wallet);
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(-1, blkNum);

    try {
      blkNum = getByJsonBlockId("latest", wallet);
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(-1, blkNum);

    try {
      blkNum = getByJsonBlockId("finalized", wallet);
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(LATEST_SOLIDIFIED_BLOCK_NUM, blkNum);

    try {
      blkNum = getByJsonBlockId("0xa", wallet);
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(10L, blkNum);

    try {
      getByJsonBlockId("abc", wallet);
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("Incorrect hex syntax", e.getMessage());
    }

    try {
      getByJsonBlockId("0xxabc", wallet);
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      // https://bugs.openjdk.org/browse/JDK-8176425, from JDK 12, the exception message is changed
      Assert.assertTrue(e.getMessage().startsWith("For input string: \"xabc\""));
    }
  }

  @Test
  public void testGetTrxBalance() {
    String balance = "";

    try {
      tronJsonRpc.getTrxBalance("", "earliest");
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG [earliest | pending | finalized] not supported",
          e.getMessage());
    }

    try {
      tronJsonRpc.getTrxBalance("", "pending");
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG [earliest | pending | finalized] not supported",
          e.getMessage());
    }

    try {
      tronJsonRpc.getTrxBalance("", "finalized");
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG [earliest | pending | finalized] not supported",
          e.getMessage());
    }

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
    try {
      tronJsonRpc.getStorageAt("", "", "earliest");
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG [earliest | pending | finalized] not supported",
          e.getMessage());
    }

    try {
      tronJsonRpc.getStorageAt("", "", "pending");
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG [earliest | pending | finalized] not supported",
          e.getMessage());
    }

    try {
      tronJsonRpc.getStorageAt("", "", "finalized");
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG [earliest | pending | finalized] not supported",
          e.getMessage());
    }
  }

  @Test
  public void testGetABIOfSmartContract() {
    try {
      tronJsonRpc.getABIOfSmartContract("", "earliest");
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG [earliest | pending | finalized] not supported",
          e.getMessage());
    }

    try {
      tronJsonRpc.getABIOfSmartContract("", "pending");
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG [earliest | pending | finalized] not supported",
          e.getMessage());
    }

    try {
      tronJsonRpc.getABIOfSmartContract("", "finalized");
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG [earliest | pending | finalized] not supported",
          e.getMessage());
    }
  }

  @Test
  public void testGetCall() {
    try {
      tronJsonRpc.getCall(null, "earliest");
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG [earliest | pending | finalized] not supported",
          e.getMessage());
    }

    try {
      tronJsonRpc.getCall(null, "pending");
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG [earliest | pending | finalized] not supported",
          e.getMessage());
    }

    try {
      tronJsonRpc.getCall(null, "finalized");
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("TAG [earliest | pending | finalized] not supported",
          e.getMessage());
    }
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
    try {
      new LogFilterWrapper(new FilterRequest("0x78", "0x14",
          null, null, null), 100, null, false);
      Assert.fail("Expected to be thrown");
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals("please verify: fromBlock <= toBlock", e.getMessage());
    }

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
    try {
      new LogFilterWrapper(new FilterRequest("pending", null, null, null, null),
          100, null, false);
      Assert.fail("Expected to be thrown");
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals("TAG pending not supported", e.getMessage());
    }
    try {
      LogFilterWrapper logFilterWrapper = new LogFilterWrapper(new FilterRequest("finalized", null,
          null, null, null), 100, wallet, false);
      Assert.assertEquals(LATEST_SOLIDIFIED_BLOCK_NUM, logFilterWrapper.getFromBlock());
      Assert.assertEquals(Long.MAX_VALUE, logFilterWrapper.getToBlock());
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      new LogFilterWrapper(new FilterRequest("test", null, null, null, null),
          100, null, false);
      Assert.fail("Expected to be thrown");
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals("Incorrect hex syntax", e.getMessage());
    }

    // to = 8000
    try {
      new LogFilterWrapper(new FilterRequest("0x0", "0x1f40", null,
          null, null), LATEST_BLOCK_NUM, null, false);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    try {
      new LogFilterWrapper(new FilterRequest("0x0", "0x1f40", null,
          null, null), LATEST_BLOCK_NUM, null, true);
      Assert.fail("Expected to be thrown");
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals(
          "exceed max block range: " + Args.getInstance().jsonRpcMaxBlockRange,
          e.getMessage());
    }

    try {
      new LogFilterWrapper(new FilterRequest("0x0", "latest", null,
          null, null), LATEST_BLOCK_NUM, null, false);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    try {
      new LogFilterWrapper(new FilterRequest("0x0", "latest", null,
          null, null), LATEST_BLOCK_NUM, null, true);
      Assert.fail("Expected to be thrown");
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals(
          "exceed max block range: " + Args.getInstance().jsonRpcMaxBlockRange,
          e.getMessage());
    }

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
    try {
      new LogFilterWrapper(new FilterRequest("0x64", "latest", null,
          null, null), LATEST_BLOCK_NUM, null, true);
      Assert.fail("Expected to be thrown");
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals(
          "exceed max block range: " + Args.getInstance().jsonRpcMaxBlockRange,
          e.getMessage());
    }
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

    try {
      new LogFilterWrapper(new FilterRequest("0xbb8", "0x1f40",
          null, topics.toArray(), null), LATEST_BLOCK_NUM, null, false);
      Assert.fail("Expected to be thrown");
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals(
          "exceed max topics: " + Args.getInstance().getJsonRpcMaxSubTopics(),
          e.getMessage());
    }

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
  public void testNewFilterFinalizedBlock() {

    try {
      tronJsonRpc.newFilter(new FilterRequest(null, null, null, null, null));
    } catch (Exception e) {
      Assert.fail();
    }

    try {
      tronJsonRpc.newFilter(new FilterRequest("finalized", null, null, null, null));
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("invalid block range params", e.getMessage());
    }

    try {
      tronJsonRpc.newFilter(new FilterRequest(null, "finalized", null, null, null));
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("invalid block range params", e.getMessage());
    }

    try {
      tronJsonRpc.newFilter(new FilterRequest("finalized", "latest", null, null, null));
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("invalid block range params", e.getMessage());
    }

    try {
      tronJsonRpc.newFilter(new FilterRequest("0x1", "finalized", null, null, null));
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("invalid block range params", e.getMessage());
    }

    try {
      tronJsonRpc.newFilter(new FilterRequest("finalized", "finalized", null, null, null));
      Assert.fail("Expected to be thrown");
    } catch (Exception e) {
      Assert.assertEquals("invalid block range params", e.getMessage());
    }
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
}
