package stest.tron.wallet.onlinestress;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.zeromq.ZMQ;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.JsonRpcBase;
import stest.tron.wallet.common.client.utils.PublicMethed;
import zmq.ZMQ.Event;

@Slf4j
public class ContractEvent001 extends JsonRpcBase {

  private final String testKey002 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  String txid;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] event001Address = ecKey1.getAddress();
  String event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] event002Address = ecKey2.getAddress();
  String event002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  // byte[] contractAddress = null;
  String param = "10";
  static HttpResponse response;
  static JSONObject responseContent;
  private String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
  private String fullnode1 =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit =
      Configuration.getByPath("testng.conf").getLong("defaultParameter.maxFeeLimit");

  

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1).usePlaintext(true).build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true)
  public void test1ContractEventAndLog() {
    ecKey1 = new ECKey(Utils.getRandom());
    event001Address = ecKey1.getAddress();
    event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(event001Key);

    ecKey2 = new ECKey(Utils.getRandom());
    event002Address = ecKey2.getAddress();
    event002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(event001Key);
    PublicMethed.printAddress(testKey002);

    Assert.assertTrue(
        PublicMethed.sendcoin(
            event001Address, maxFeeLimit * 30, fromAddress, testKey002, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.sendcoin(
            event002Address, maxFeeLimit * 30, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull1);

    AccountResourceMessage accountResource =
        PublicMethed.getAccountResource(event001Address, blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();
    Long balanceBefore = PublicMethed.queryAccount(event001Key, blockingStubFull).getBalance();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    logger.info("before balance is " + Long.toString(balanceBefore));

    String contractName = "addressDemo";
    String code =
        Configuration.getByPath("testng.conf").getString("code.code_ContractEventAndLog1");
    String abi = Configuration.getByPath("testng.conf").getString("abi.abi_ContractEventAndLog1");
    byte[] contractAddress =
        PublicMethed.deployContract(
            contractName,
            abi,
            code,
            "",
            maxFeeLimit,
            0L,
            50,
            null,
            event001Key,
            event001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);

    Integer i = 0;
    for (i = 0; i < 1; i++) {
      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForEventCycle(uint256)",
              "100",
              false,
              1L,
              100000000L,
              event002Address,
              event002Key,
              blockingStubFull);
      logger.info(txid);

      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForLogCycle(uint256)",
              "100",
              false,
              2L,
              100000000L,
              event002Address,
              event002Key,
              blockingStubFull);
      logger.info(txid);

      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "triggerUintEvent()",
              "#",
              false,
              0,
              maxFeeLimit,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);

      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "triggerintEvent()",
              "#",
              false,
              0,
              maxFeeLimit,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);

      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForEventAndLog()",
              "#",
              false,
              1,
              maxFeeLimit,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);
      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForEventNoIndex()",
              "#",
              false,
              0L,
              100000000L,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);
      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForLog()",
              "#",
              false,
              1L,
              100000000L,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);

      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForEventNoIndex()",
              "#",
              false,
              1L,
              100000000L,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);

      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForEventOneIndex()",
              "#",
              false,
              1L,
              100000000L,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);

      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForEventTwoIndex()",
              "#",
              false,
              2L,
              100000000L,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);

      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForEvent()",
              "#",
              false,
              3L,
              100000000L,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);

      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForEventCycle(uint256)",
              "100",
              false,
              1L,
              100000000L,
              event002Address,
              event002Key,
              blockingStubFull);
      logger.info(txid);

      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForLogCycle(uint256)",
              "100",
              false,
              2L,
              100000000L,
              event002Address,
              event002Key,
              blockingStubFull);
      logger.info(txid);

      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForAnonymousHasLog()",
              "#",
              false,
              4L,
              100000000L,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);

      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "depositForAnonymousNoLog()",
              "#",
              false,
              5L,
              100000000L,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);

      String param = "\"" + code + "\"" + "," + "\"" + code + "\"";
      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "triggerStringEvent(string,string)",
              param,
              false,
              0L,
              100000000L,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);

      param = "\"" + "true1" + "\"" + "," + "\"" + "false1" + "\"";
      txid =
          PublicMethed.triggerContract(
              contractAddress,
              "triggerBoolEvent(bool,bool)",
              param,
              false,
              0L,
              100000000L,
              event001Address,
              event001Key,
              blockingStubFull);
      logger.info(txid);
      String filename = "/Users/wangzihe/Documents/modify_fullnode/java-tron/tooLongString.txt";
      try {
        FileReader fr = new FileReader(filename);
        InputStreamReader read = new InputStreamReader(new FileInputStream(new File(filename)));
        BufferedReader reader = new BufferedReader(read);
        String tooLongString = reader.readLine();
        param = "\"" + tooLongString + "\"" + "," + "\"" + tooLongString + "\"";
        txid =
            PublicMethed.triggerContract(
                contractAddress,
                "triggerStringEventAnonymous(string,string)",
                param,
                false,
                0L,
                100000000L,
                event001Address,
                event001Key,
                blockingStubFull);
        logger.info(txid);

        txid =
            PublicMethed.triggerContract(
                contractAddress,
                "triggerStringEvent(string,string)",
                param,
                false,
                0L,
                100000000L,
                event001Address,
                event001Key,
                blockingStubFull);
        logger.info(txid);

      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }

    contractName = "addressDemo";
    code = Configuration.getByPath("testng.conf").getString("code.code_ContractEventAndLog2");
    abi = Configuration.getByPath("testng.conf").getString("abi.abi_ContractEventAndLog2");
    contractAddress =
        PublicMethed.deployContract(
            contractName,
            abi,
            code,
            "",
            maxFeeLimit,
            0L,
            50,
            null,
            event001Key,
            event001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);

    txid =
        PublicMethed.triggerContract(
            contractAddress,
            "triggerEventBytes()",
            "#",
            false,
            0,
            maxFeeLimit,
            event001Address,
            event001Key,
            blockingStubFull);
    logger.info(txid);
  }

  @Test(
      enabled = true,
      threadPoolSize = 5,
      invocationCount = 5,
      description = "test eth_getFilterChanges")
  public void testEthGetFilterChanges() throws InterruptedException {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] event001Address = ecKey1.getAddress();
    logger.info("event001Address:" + event001Address);
    String event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] event002Address = ecKey2.getAddress();
    logger.info("event002Address:" + event002Address);
    String event002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(event001Key);
    PublicMethed.printAddress(testKey002);

    Assert.assertTrue(
        PublicMethed.sendcoin(
            event001Address, maxFeeLimit * 30, fromAddress, testKey002, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.sendcoin(
            event002Address, maxFeeLimit * 30, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull1);

    String contractName = "SolidityTest";
    String filePath = "./src/test/resources/soliditycode/testGetFilterChange.sol";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractAddress =
        PublicMethed.deployContract(
            contractName,
            abi,
            code,
            "",
            maxFeeLimit,
            0L,
            50,
            null,
            event001Key,
            event001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
    logger.info("11111111111111111111111111111111111");
    Thread.sleep(180000);

    long txidNum = 0;
    HttpResponse response = getNowBlock(httpFullNode);
    JSONObject responseContent = parseResponseContent(response);
    long blockBefore =
        responseContent.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    logger.info("blockBefore:" + blockBefore);
    Thread.sleep(180000);
    for (int i = 0; i < 5000; i++) {
      String txid =
          PublicMethed.triggerContract(
              contractAddress,
              "getResult(uint256)",
              param,
              false,
              2L,
              100000000L,
              event002Address,
              event002Key,
              blockingStubFull);
      logger.info("txid:" + txid);
      txidNum++;
    }
    Thread.sleep(180000);
    response = getNowBlock(httpFullNode);
    responseContent = parseResponseContent(response);
    long blockAfter =
        responseContent.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");

    logger.info("txidNum:" + txidNum);

    // 扫块
    long sumLogs = 0;
    long totalTransactionsSize = 0;
    logger.info("blockBefore:" + blockBefore);
    logger.info("blockAfter:" + blockAfter);
    for (long i = blockBefore; i <= blockAfter; i++) {

      response = getTransactionCountByBlocknum(httpFullNode, (int) i);
      responseContent = parseResponseContent(response);
      long transactionsSize = responseContent.getLong("count");

      totalTransactionsSize += transactionsSize;
    }
    logger.info(
        (int) (Thread.currentThread().getId())
            + "sumLogs:"
            + totalTransactionsSize * Long.parseLong(param));
  }

  public static String[] arr =
      new String[] {
        "00",
        "0x6b5c9c34aae469576dfcde3655c9036d",
        "0x450de4565abf4434d66948fb2a568608",
        "0x02a65b2cc37d2d34808a63b50b86e0cd",
        "0x7474d244cecf3a943bf8ac6dbd7d60fa",
        "0x4ab110c02b04d7781f774eeffa6432a3"
      };

  @Test(
      enabled = true,
      threadPoolSize = 5,
      invocationCount = 5,
      description = "Eth api of eth_getFilterChanges .")
  public void test09GetFilterChanges() {
    long sumSize = 0;
    while (true) {
      JsonArray params = new JsonArray();
      String id = arr[(int) (Thread.currentThread().getId()) - 16];
      params.add(id);
      JsonObject requestBody = getJsonRpcBody("eth_getFilterChanges", params);
      HttpResponse response = getJsonRpc(jsonRpcNode, requestBody);
      JSONObject responseContent = parseResponseContent(response);
      long size = responseContent.getJSONArray("result").size();
      sumSize += size;
      logger.info(Thread.currentThread().getId() + ":sumSize:" + sumSize);
    }
  }

  /** constructor. */
  public static JSONObject parseResponseContent(HttpResponse response) {
    try {
      String result = EntityUtils.toString(response.getEntity());
      StringEntity entity = new StringEntity(result, Charset.forName("UTF-8"));
      response.setEntity(entity);
      JSONObject obj = JSONObject.parseObject(result);
      return obj;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /*  public static HttpResponse getNowBlock(String httpNode, Boolean visible) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getnowblock";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }*/

  /** constructor. */
  public static HttpResponse getTransactionCountByBlocknum(String httpNode, long blocknum) {
    HttpResponse response;
    try {

      String requestUrl = "http://" + httpNode + "/wallet/gettransactioncountbyblocknum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", blocknum);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();

      return null;
    }
    return response;
  }

  public static HttpResponse getNowBlock(String httpNode) {
    return getNowBlock(httpNode, false);
  }

  /** constructor. */
  public static HttpResponse getNowBlock(String httpNode, Boolean visible) {

    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getnowblock";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();

      return null;
    }
    return response;
  }

  /** constructor. */
  public static HttpResponse createConnect(String url, JsonObject requestBody) {
    HttpResponse response;
    HttpPost httppost;
    HttpClient httpClient;
    Integer connectionTimeout =
        Configuration.getByPath("testng.conf").getInt("defaultParameter.httpConnectionTimeout");
    Integer soTimeout =
        Configuration.getByPath("testng.conf").getInt("defaultParameter.httpSoTimeout");
    PoolingClientConnectionManager pccm = new PoolingClientConnectionManager();
    pccm.setDefaultMaxPerRoute(80);
    pccm.setMaxTotal(100);

    httpClient = new DefaultHttpClient(pccm);
    try {

      httpClient
          .getParams()
          .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httppost = new HttpPost(url);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      if (requestBody != null) {
        StringEntity entity = new StringEntity(requestBody.toString(), Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httppost.setEntity(entity);
      }

      logger.info(httppost.toString());
      response = httpClient.execute(httppost);
    } catch (Exception e) {
      e.printStackTrace();

      return null;
    }
    return response;
  }

  /** constructor. */
  public static HttpResponse getJsonRpc(String jsonRpcNode, JsonObject jsonRpcObject) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + jsonRpcNode + "/jsonrpc";
      response = createConnect(requestUrl, jsonRpcObject);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return response;
  }

  @Test(enabled = true, description = "Subscribe event client")
  public void testEnergyCostDetail() {
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);

    req.subscribe("blockTrigger");
    req.subscribe("transactionTrigger");
    req.subscribe("contractLogTrigger");
    req.subscribe("contractEventTrigger");
    req.monitor("inproc://reqmoniter", ZMQ.EVENT_CONNECTED | ZMQ.EVENT_DISCONNECTED);
    final ZMQ.Socket moniter = context.socket(ZMQ.PAIR);
    moniter.connect("inproc://reqmoniter");
    new Thread(new Runnable() {
      public void run() {
        while (true) {
          Event event = Event.read(moniter.base());
          System.out.println(event.event + "  " + event.addr);
        }
      }

    }).start();
    req.connect("tcp://47.94.197.215:55555");
    req.setReceiveTimeOut(10000);

    while (true) {
      byte[] message = req.recv();
      if (message != null) {
        System.out.println("receive : " + new String(message));
      }
    }
  }

  @Test(enabled = true)
  public void testSingForHex() {
    try {
      SignInterface cryptoEngine =
          SignUtils.fromPrivate(
              ByteArray.fromHexString(
                  "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC"),
              true);
      /*      ByteString sig = ByteString.copyFrom(cryptoEngine.Base64toBytes(cryptoEngine
      .signHash(Sha256Hash.of(DBConfig.isECKeyCryptoEngine(),
          ByteArray.fromHexString(
              "ba989430c392dedef66a259a1f1112b178dbe7f2793975d8cf80f9b31ecd33ff"))
              .getBytes())));*/
      //
      ByteString sig =
          ByteString.copyFrom(
              cryptoEngine.Base64toBytes(
                  cryptoEngine.signHash(
                      ByteArray.fromHexString(
                          "4f2a4c136f56a41714b42e14d497e38dcbe0f9c4ca2e5957cf3a340c62d133f8"))));
      logger.info(ByteArray.toHexString(sig.toByteArray()));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
