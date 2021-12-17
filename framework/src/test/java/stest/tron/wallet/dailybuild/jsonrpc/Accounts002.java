package stest.tron.wallet.dailybuild.jsonrpc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.JsonRpcBase;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class Accounts002 extends JsonRpcBase {
  private JSONObject responseContent;
  private HttpResponse response;
  String realGasPrice;
  String bid = null;
  int indexNum = 0;
  String indexHex = null;
  JSONObject result = null;
  String transacionHash = null;
  String blockHash = null;
  String blockNumHex = null;
  String parentHash = null;
  String txTrieRoot = null;
  String witnessAddress = null;
  String feeLimit = null;
  String accountStateRoot = null;
  String energyUsed = "0x135c6";

  List<String> transactionIdList = null;
  long size = 0;
  long gas = 0;
  long blockTimeStamp = 0;
  long gasPriceFromHttp = 0;

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Json rpc api of eth_accounts from solidity")
  public void test01JsonRpcApiTestForEthAccounts() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("eth_accounts", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    List result = new ArrayList();
    logger.info(String.valueOf(result));
    Assert.assertEquals(responseContent.get("result"), result);
  }

  @Test(enabled = true, description = "Json rpc api of eth_blockNumber from solidity")
  public void test02JsonRpcApiTestForEthBlockNumber() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("eth_blockNumber", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    responseContent.get("result");
    String blockNum = responseContent.getString("result").substring(2);
    long blockNumFromjsonRpcNodeForSolidity = Long.parseLong(blockNum, 16);
    response = HttpMethed.getNowBlockFromSolidity(httpsolidityNode);
    responseContent = HttpMethed.parseResponseContent(response);
    long blockNumFromHttp =
        responseContent.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    logger.info("blocknumFromjsonRpcNodeForSolidity：" + blockNumFromjsonRpcNodeForSolidity);
    logger.info("blocknumFromHttp:" + blockNumFromHttp);
    Assert.assertTrue(Math.abs(blockNumFromjsonRpcNodeForSolidity - blockNumFromHttp) <= 3);
  }

  @Test(enabled = true, description = "Json rpc api of eth_call from solidity")
  public void test03JsonRpcApiTestForEthCall() throws Exception {
    JsonObject param = new JsonObject();
    HttpMethed.waitToProduceOneBlock(httpFullNode);
    param.addProperty("from", ByteArray.toHexString(jsonRpcOwnerAddress));
    param.addProperty("to", trc20AddressHex);
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    param.addProperty("data", "0x06fdde03");
    JsonArray params = new JsonArray();
    params.add(param);
    params.add("latest");
    JsonObject requestBody = getJsonRpcBody("eth_call", params);
    logger.info("03params:" + params);
    logger.info("requestBody:" + requestBody);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String dataResult = responseContent.getString("result");
    Assert.assertEquals(
        "0x000000000000000000000000000000000000000000000000000"
            + "00000000000200000000000000000000000000000000000000000"
            + "00000000000000000000000a546f6b656e5452433230000000000"
            + "00000000000000000000000000000000000",
        dataResult);
  }

  @Test(enabled = true, description = "Json rpc api of eth_chainId from solidity")
  public void test04JsonRpcApiTestForEthChainId() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("eth_chainId", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    responseContent.get("result");
    String blockIdFromjsonRpcNodeForSolidity =
        responseContent.get("result").toString().substring(2);
    response = HttpMethed.getBlockByNumFromSolidity(httpsolidityNode, 0);
    responseContent = HttpMethed.parseResponseContent(response);
    String blockIdFromHttp = responseContent.getString("blockID").substring(56);
    logger.info("blockIdFromjsonRpcNodeForSolidity:" + blockIdFromjsonRpcNodeForSolidity);
    logger.info("blockIdFromHttp:" + blockIdFromHttp);
    Assert.assertEquals(blockIdFromjsonRpcNodeForSolidity, blockIdFromHttp);
  }

  @Test(enabled = true, description = "Json rpc api of eth_coinbase from solidity")
  public void test05JsonRpcApiTestForEthCoinbase() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("eth_coinbase", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);

    Assert.assertEquals(
        "0x410be88a918d74d0dfd71dc84bd4abf036d0562991", responseContent.getString("result"));
  }

  @Test(enabled = true, description = "Json rpc api of eth_estimateGas from solidity")
  public void test06JsonRpcApiTestForEthEstimateGas() throws Exception {
    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(jsonRpcOwnerAddress));
    param.addProperty("to", trc20AddressHex);
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    param.addProperty("data", "0x1249c58b");
    JsonArray params = new JsonArray();
    params.add(param);
    JsonObject requestBody = getJsonRpcBody("eth_estimateGas", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    logger.info("test06requestBody:" + requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String dataResult = responseContent.getString("result");
    Assert.assertEquals("0x147", dataResult);
  }

  @Test(enabled = true, description = "Json rpc api of eth_estimateGasHasPayable")
  public void test07JsonRpcApiTestForEthEstimateGasHasPayable() throws Exception {
    response = HttpMethed.getTransactionInfoByIdFromSolidity(httpsolidityNode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    Long realEnergyUsed = responseContent.getJSONObject("receipt").getLong("energy_usage_total");
    logger.info("realEnergyUsed:" + realEnergyUsed);
    JsonObject param = new JsonObject();
    param.addProperty("from", "0x" + ByteArray.toHexString(jsonRpcOwnerAddress).substring(2));
    param.addProperty("to", "0x" + contractAddressFrom58);
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x1389");
    param.addProperty("data", data);
    JsonArray params = new JsonArray();
    params.add(param);
    JsonObject requestBody = getJsonRpcBody("eth_estimateGas", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    logger.info("test07requestBody:" + requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String dataResult = responseContent.getString("result");
    Assert.assertEquals((long) realEnergyUsed, Long.parseLong(dataResult.substring(2), 16));
  }

  @Test(enabled = true, description = "Json rpc api of eth_estimateGasWithoutTo from solidity")
  public void test08JsonRpcApiTestForEthEstimateGasWithoutTo() throws Exception {
    JsonObject param = new JsonObject();
    param.addProperty("from", "0x6C0214C9995C6F3A61AB23F0EB84B0CDE7FD9C7C");
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    param.addProperty(
        "data",
        "0x6080604052d3600055d2600155346002556101418061001f6000396000f30060806040"
            + "52600436106100565763ffffffff7c010000000000000000000000000000000000000000"
            + "000000000000000060003504166305c24200811461005b5780633be9ece7146100815780"
            + "6371dc08ce146100aa575b600080fd5b6100636100b2565b6040805193845260208401929"
            + "0925282820152519081900360600190f35b6100a873ffffffffffffffffffffffffffffff"
            + "ffffffffff600435166024356044356100c0565b005b61006361010d565b60005460015460"
            + "0254909192565b60405173ffffffffffffffffffffffffffffffffffffffff841690821561"
            + "08fc029083908590600081818185878a8ad0945050505050158015610107573d6000803e3d"
            + "6000fd5b50505050565bd3d2349091925600a165627a7a72305820a2fb39541e90eda9a2f5"
            + "f9e7905ef98e66e60dd4b38e00b05de418da3154e757002900000000000000000000000000"
            + "00000000000000000000000000000090fa17bb");
    JsonArray params = new JsonArray();
    params.add(param);
    JsonObject requestBody = getJsonRpcBody("eth_estimateGas", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    logger.info("test08requestBody:" + requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String dataResult = responseContent.getString("result");
    logger.info("dataResult:" + dataResult);
    Assert.assertEquals(energyUsed, dataResult);
  }

  @Test(enabled = true, description = "Json rpc api of eth_estimateGasSendTrx from solidity")
  public void test09JsonRpcApiTestForEthEstimateGasSendTrx() throws Exception {
    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(jsonRpcOwnerAddress));
    param.addProperty("to", "0xC1A74CD01732542093F5A87910A398AD70F04BD7");
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x1");
    param.addProperty("data", "0x0");
    JsonArray params = new JsonArray();
    params.add(param);
    JsonObject requestBody = getJsonRpcBody("eth_estimateGas", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    logger.info("test09requestBody:" + requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String dataResult = responseContent.getString("result");
    Assert.assertEquals("0x0", dataResult);
  }

  @Test(enabled = true, description = "Json rpc api of eth_gasPrice from solidity")
  public void test10JsonRpcApiTestForEthGasPrice() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("eth_gasPrice", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    responseContent.get("result");
    String gasPrice = responseContent.get("result").toString().substring(2);
    long gasPriceFromJsonrpc = Long.parseLong(gasPrice, 16);
    logger.info(String.valueOf(gasPriceFromJsonrpc));
    response = HttpMethed.getChainParameters(httpFullNode);
    responseContent = HttpMethed.parseResponseContent(response);
    JSONArray temp;
    temp = responseContent.getJSONArray("chainParameter");
    for (int i = 0; i < temp.size(); i++) {
      if (temp.getJSONObject(i).get("key").equals("getEnergyFee")) {
        gasPriceFromHttp = temp.getJSONObject(i).getLong("value");
      }
    }
    logger.info("gasPriceFromHttp:" + gasPriceFromHttp);
    Assert.assertEquals(gasPriceFromJsonrpc, gasPriceFromHttp);
  }

  @Test(enabled = true, description = "Json rpc api of eth_getBalance from solidity")
  public void test11JsonRpcApiTestForEthGetBalance() throws Exception {
    JsonArray params = new JsonArray();
    params.add("0x" + ByteArray.toHexString(foundationAccountAddress).substring(2));
    params.add("latest");
    JsonObject requestBody = getJsonRpcBody("eth_getBalance", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String balance = responseContent.getString("result").substring(2);
    Long balance1 = Long.parseLong(balance, 16);
    logger.info("balance1:" + balance1);
    response = HttpMethed.getAccountFromSolidity(httpsolidityNode, foundationAccountAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    Long balance2 = responseContent.getLong("balance");
    logger.info(balance1.toString());
    logger.info(balance2.toString());
    Assert.assertEquals(balance1, balance2);
  }

  @Test(
      enabled = true,
      description = "Json rpc api of eth_getBlockTransactionCountByNumber from solidity")
  public void test12JsonRpcApiTestForEthGetBlockTransactionCountByNum() throws Exception {
    response = HttpMethed.getNowBlockFromSolidity(httpsolidityNode);
    responseContent = HttpMethed.parseResponseContent(response);
    JsonArray params = new JsonArray();
    params.add("earliest");
    JsonObject requestBody = getJsonRpcBody("eth_getBlockTransactionCountByNumber", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String transactionNum = responseContent.getString("result").substring(2);
    int transactionNum1 = Integer.parseInt(transactionNum, 16);
    logger.info(String.valueOf(transactionNum1));
    response = HttpMethed.getTransactionCountByBlocknumFromSolidity(httpsolidityNode, 0);
    responseContent = HttpMethed.parseResponseContent(response);
    int transactionNum2 = responseContent.getInteger("count");
    logger.info(String.valueOf(transactionNum2));
    Assert.assertEquals(transactionNum1, transactionNum2);
  }

  @Test(enabled = true, description = "Json rpc api of eth_getCode from solidity")
  public void test13JsonRpcApiTestForEthGetCode() throws Exception {

    JsonArray params = new JsonArray();
    params.add(contractAddressFrom58);
    params.add("latest");
    JsonObject requestBody = getJsonRpcBody("eth_getCode", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String codeFromJsonRpc = responseContent.getString("result").substring(2);
    logger.info(codeFromJsonRpc);
    response = HttpMethed.getContractInfo(httpFullNode, contractAddressFrom58);
    logger.info("13contractAddressFrom58:" + contractAddressFrom58);
    responseContent = HttpMethed.parseResponseContent(response);
    String codeFromHttp = responseContent.getString("runtimecode");
    logger.info(codeFromHttp);
    Assert.assertEquals(codeFromJsonRpc, codeFromHttp);
  }

  @Test(enabled = true, description = "Json rpc api of eth_getStorageAt from solidity")
  public void test14JsonRpcApiTestForEthGetStorageAt01() throws Exception {

    JsonArray params = new JsonArray();
    params.add(contractAddressFrom58);
    params.add("0x0");
    params.add("latest");
    JsonObject requestBody = getJsonRpcBody("eth_getStorageAt", params);
    logger.info("requestBody:" + requestBody);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("14responseContent:" + responseContent);
    String result = responseContent.getString("result").substring(2);
    long resultExpect = Long.parseLong(result, 16);
    logger.info("result:" + resultExpect);
    Assert.assertEquals("1234", String.valueOf(resultExpect));
  }

  @Test(enabled = true, description = "Json rpc api of eth_getStorageAt from solidity")
  public void test15JsonRpcApiTestForEthGetStorageAt02() throws Exception {

    String address =
        "000000000000000000000000" + ByteArray.toHexString(jsonRpcOwnerAddress).substring(2);
    String str = address + "0000000000000000000000000000000000000000000000000000000000000001";
    logger.info("str:" + str);
    JsonArray paramsForSha3 = new JsonArray();
    paramsForSha3.add(str);
    JsonObject requestBodyForSha3 = getJsonRpcBody("web3_sha3", paramsForSha3);
    logger.info("requestBodyForSha3:" + requestBodyForSha3);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBodyForSha3);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("responseContent:" + responseContent);
    String resultForSha3 = responseContent.getString("result");
    logger.info("resultForSha3:" + resultForSha3);
    JsonArray params = new JsonArray();
    params.add(contractAddressFrom58);
    params.add(resultForSha3);
    params.add("latest");
    JsonObject requestBody = getJsonRpcBody("eth_getStorageAt", params);
    logger.info("requestBody:" + requestBody);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("15responseContent:" + responseContent);
    String result = responseContent.getString("result").substring(2);
    logger.info("15result:" + result);
    logger.info("mapResult:" + Integer.parseInt(result, 16));
    Assert.assertEquals("5678", String.valueOf(Integer.parseInt(result, 16)));
  }

  @Test(
      enabled = true,
      description = "Json rpc api of eth_getTransactionByBlockNumberAndIndex from solidity")
  public void test16JsonRpcApiTestForEthGetTransactionByBlockNumberAndIndex() throws Exception {
    logger.info("16blockNum:" + blockNum);
    blockNumHex = "0x" + Integer.toHexString(blockNum);
    logger.info("blockNumHex:" + blockNumHex);
    JsonArray params = new JsonArray();
    params.add(blockNumHex);
    indexNum = 0;
    response = HttpMethed.getBlockByNumFromSolidity(httpsolidityNode, blockNum);
    responseContent = HttpMethed.parseResponseContent(response);
    parentHash =
        responseContent
            .getJSONObject("block_header")
            .getJSONObject("raw_data")
            .getString("parentHash");
    txTrieRoot =
        responseContent
            .getJSONObject("block_header")
            .getJSONObject("raw_data")
            .getString("txTrieRoot");
    witnessAddress =
        responseContent
            .getJSONObject("block_header")
            .getJSONObject("raw_data")
            .getString("witness_address");
    feeLimit =
        responseContent
            .getJSONArray("transactions")
            .getJSONObject(0)
            .getJSONObject("raw_data")
            .getString("fee_limit");
    logger.info(feeLimit);

    JSONObject getBlockByNumFromSolidityResult = null;
    for (int i = 0; i < responseContent.getJSONArray("transactions").size(); i++) {
      if (txid.equals(
          responseContent.getJSONArray("transactions").getJSONObject(i).getString("txID"))) {
        indexNum = i;
        getBlockByNumFromSolidityResult =
            responseContent.getJSONArray("transactions").getJSONObject(i);
        bid = responseContent.getString("blockID");
        break;
      }
    }
    transactionIdList = new ArrayList<>();
    if (responseContent.getJSONArray("transactions").size() > 0) {
      for (int i = 0; i < responseContent.getJSONArray("transactions").size(); i++) {
        transactionIdList.add(
            "0x" + responseContent.getJSONArray("transactions").getJSONObject(i).getString("txID"));
      }
    }
    logger.info("16transactionIdList:" + transactionIdList);
    logger.info(String.valueOf(indexNum));
    indexHex = "0x" + Integer.toHexString(indexNum);
    logger.info("indexHex：" + indexHex);
    params.add(indexHex);
    JsonObject requestBody = getJsonRpcBody("eth_getTransactionByBlockNumberAndIndex", params);
    logger.info("13requestBody:" + requestBody);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    result = responseContent.getJSONObject("result");
    logger.info("16 result" + result);
    Map<String, Object> jsonrpcResult = new HashMap();
    for (Map.Entry<String, Object> entry : result.entrySet()) {
      jsonrpcResult.put(entry.getKey(), entry.getValue());
    }
    transacionHash = jsonrpcResult.get("hash").toString();
    logger.info("transactionHash：" + transacionHash);
    blockHash = jsonrpcResult.get("blockHash").toString();
    logger.info("jsonrpcResult:" + jsonrpcResult);
    response = HttpMethed.getTransactionInfoByBlocknumFromSolidity(httpsolidityNode, blockNum);
    logger.info("response:" + response);
    List<JSONObject> responseContent1 = HttpMethed.parseResponseContentArray(response);
    logger.info("responseContent1:" + responseContent1);
    blockTimeStamp = responseContent1.get(0).getLong("blockTimeStamp");

    for (int i = 0; i < responseContent1.size(); i++) {
      if (responseContent1.get(i).getString("id").equals(transactionIdList.get(0).substring(2))) {
        gas = responseContent1.get(i).getJSONObject("receipt").getLong("energy_usage_total");
        logger.info("gas:" + gas);
        break;
      }
    }

    Assert.assertEquals(jsonrpcResult.get("gas").toString(), "0x" + Long.toHexString(gas));
    Assert.assertNull(jsonrpcResult.get("nonce"));
    Assert.assertEquals(
        jsonrpcResult.get("hash").toString(),
        "0x" + getBlockByNumFromSolidityResult.getString("txID"));
    Assert.assertEquals(jsonrpcResult.get("blockHash").toString(), "0x" + bid);
    Assert.assertEquals(jsonrpcResult.get("blockNumber").toString(), blockNumHex);
    Assert.assertEquals(jsonrpcResult.get("transactionIndex").toString(), indexHex);
    Assert.assertEquals(
        jsonrpcResult.get("from").toString(),
        "0x"
            + getBlockByNumFromSolidityResult
                .getJSONObject("raw_data")
                .getJSONArray("contract")
                .getJSONObject(0)
                .getJSONObject("parameter")
                .getJSONObject("value")
                .getString("owner_address")
                .substring(2));
    Assert.assertEquals(
        jsonrpcResult.get("to").toString(),
        "0x"
            + getBlockByNumFromSolidityResult
                .getJSONObject("raw_data")
                .getJSONArray("contract")
                .getJSONObject(0)
                .getJSONObject("parameter")
                .getJSONObject("value")
                .getString("contract_address")
                .substring(2));

    Assert.assertEquals(jsonrpcResult.get("value").toString(), "0x1389");
    String data;
    if (getBlockByNumFromSolidityResult.getJSONObject("raw_data").getString("data") == null) {
      data = "0x";
    } else {
      data =
          getBlockByNumFromSolidityResult.getJSONObject("raw_data").getString("data").substring(2);
    }
    Assert.assertEquals(jsonrpcResult.get("input").toString(), data);

    long temp =
        Long.parseLong(
            getBlockByNumFromSolidityResult.getString("signature").substring(130, 131), 16);
    long v =
        Long.parseLong(
            getBlockByNumFromSolidityResult.getString("signature").substring(130, 132), 16);
    if (temp < 27) {
      v += 27;
    }
    Assert.assertEquals(Long.parseLong(jsonrpcResult.get("v").toString().substring(2), 16), v);
    Assert.assertEquals(
        jsonrpcResult.get("r").toString().substring(2),
        getBlockByNumFromSolidityResult.getString("signature").substring(2, 66));
    Assert.assertEquals(
        jsonrpcResult.get("s").toString().substring(2),
        getBlockByNumFromSolidityResult.getString("signature").substring(66, 130));
  }

  @Test(
      enabled = true,
      description = "Json rpc api of eth_getBlockTransactionCountByHash from solidity")
  public void test17JsonRpcApiTestForEthGetBlockTransactionCountByHash() throws Exception {
    logger.info("blockNum:" + blockNum);
    JsonArray params = new JsonArray();
    params.add(blockHash);
    logger.info("blockHash:" + blockHash);
    JsonObject requestBody = getJsonRpcBody("eth_getBlockTransactionCountByHash", params);
    logger.info("requestBody:" + requestBody);
    HttpMethed.waitToProduceOneBlock(httpFullNode);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("responseContent:" + responseContent);
    String transactionNum = responseContent.getString("result").substring(2);
    int transactionNumFromjsonRpcNodeForSolidity = Integer.parseInt(transactionNum, 16);
    logger.info(
        "transactionNumFromjsonRpcNodeForSolidity:" + transactionNumFromjsonRpcNodeForSolidity);
    response = HttpMethed.getTransactionCountByBlocknumFromSolidity(httpsolidityNode, blockNum);
    responseContent = HttpMethed.parseResponseContent(response);
    int transactionNumFromHttp = responseContent.getInteger("count");
    logger.info("transactionNumFromHttp:" + transactionNumFromHttp);
    Assert.assertEquals(transactionNumFromHttp, transactionNumFromjsonRpcNodeForSolidity);
  }

  @Test(
      enabled = true,
      description = "Json rpc api of eth_getBlockTransactionCountByNumber from solidity")
  public void test18JsonRpcApiTestForEthGetBlockTransactionCountByNum() throws Exception {
    JsonArray params = new JsonArray();
    params.add(blockNum);
    logger.info(String.valueOf(blockNum));
    JsonObject requestBody = getJsonRpcBody("eth_getBlockTransactionCountByNumber", params);
    logger.info("requestBody:" + requestBody);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    logger.info("response:" + response);
    HttpMethed.waitToProduceOneBlock(httpFullNode);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("responseContent:" + responseContent);
    String transactionNum = responseContent.getString("result").substring(2);
    int transactionNum1 = Integer.parseInt(transactionNum, 16);
    logger.info(String.valueOf(transactionNum1));
    response = HttpMethed.getTransactionCountByBlocknumFromSolidity(httpsolidityNode, blockNum);
    responseContent = HttpMethed.parseResponseContent(response);
    int transactionNum2 = responseContent.getInteger("count");
    logger.info(String.valueOf(transactionNum2));
    Assert.assertEquals(transactionNum1, transactionNum2);
  }

  @Test(
      enabled = true,
      description = "Json rpc api of eth_getTransactionByBlockHashAndIndex from solidity")
  public void test19JsonRpcApiTestForEthGetTransactionByBlockHashAndIndex() throws Exception {
    JsonArray params = new JsonArray();
    params.add("0x" + bid);
    params.add(indexHex);
    logger.info("indexHex:" + indexHex);
    JsonObject requestBody = getJsonRpcBody("eth_getTransactionByBlockHashAndIndex", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    JSONObject resultForGetTransactionByBlockHashAndIndex = responseContent.getJSONObject("result");
    Assert.assertEquals(result, resultForGetTransactionByBlockHashAndIndex);
  }

  @Test(enabled = true, description = "Json rpc api of eth_getTransactionByHash from solidity")
  public void test20JsonRpcApiTestForEthGetTransactionByHash() throws Exception {
    JsonArray params = new JsonArray();
    params.add(transacionHash);
    JsonObject requestBody = getJsonRpcBody("eth_getTransactionByHash", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    JSONObject result1 = responseContent.getJSONObject("result");
    Assert.assertEquals(result, result1);
  }

  @Test(enabled = true, description = "Json rpc api of eth_getTransactionReceipt from solidity")
  public void test21JsonRpcApiTestForEthGetTransactionReceipt() throws Exception {
    JsonArray params = new JsonArray();
    Thread.sleep(6000);
    params.add(trc20Txid);
    JsonObject requestBody = getJsonRpcBody("eth_getTransactionReceipt", params);
    logger.info("requestBody:" + requestBody);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    logger.info("response:" + response);
    responseContent = HttpMethed.parseResponseContent(response);
    JSONObject resultFromTransactionReceipt = responseContent.getJSONObject("result");
    logger.info("resultFromTransactionReceipt:" + resultFromTransactionReceipt);
    JSONArray logs = resultFromTransactionReceipt.getJSONArray("logs");
    logger.info("logs:" + logs);
    logger.info("result:" + resultFromTransactionReceipt.toString());
    response = HttpMethed.getBlockByNumFromSolidity(httpsolidityNode, blockNumForTrc20);
    responseContent = HttpMethed.parseResponseContent(response);
    int index = 0;
    for (int i = 0; i < responseContent.getJSONArray("transactions").size(); i++) {
      if (trc20Txid.equals(
          responseContent.getJSONArray("transactions").getJSONObject(i).getString("txID"))) {
        index = i;
        break;
      }
    }

    JsonArray paramsForTransactionByBlockNumberAndIndex = new JsonArray();
    paramsForTransactionByBlockNumberAndIndex.add("0x" + Integer.toHexString(blockNumForTrc20));
    paramsForTransactionByBlockNumberAndIndex.add("0x" + Integer.toHexString(index));
    JsonObject requestBody1 =
        getJsonRpcBody(
            "eth_getTransactionByBlockNumberAndIndex", paramsForTransactionByBlockNumberAndIndex);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody1);
    logger.info("requestBody1:" + requestBody1);
    responseContent = HttpMethed.parseResponseContent(response);
    JSONObject resultFromTransactionByBlockNumberAndIndex = responseContent.getJSONObject("result");
    logger.info(
        "resultFromTransactionByBlockNumberAndIndex:" + resultFromTransactionByBlockNumberAndIndex);
    Assert.assertEquals(
        resultFromTransactionReceipt.getString("blockHash"),
        resultFromTransactionByBlockNumberAndIndex.getString("blockHash"));
    Assert.assertEquals(
        resultFromTransactionReceipt.getString("blockNumber"),
        resultFromTransactionByBlockNumberAndIndex.getString("blockNumber"));
    Assert.assertEquals(
        resultFromTransactionReceipt.getString("transactionIndex"),
        resultFromTransactionByBlockNumberAndIndex.getString("transactionIndex"));
    Assert.assertEquals(
        resultFromTransactionReceipt.getString("transactionHash"), "0x" + trc20Txid);
    Assert.assertEquals(
        resultFromTransactionReceipt.getString("from"),
        resultFromTransactionByBlockNumberAndIndex.getString("from"));
    Assert.assertEquals(
        resultFromTransactionReceipt.getString("to"),
        resultFromTransactionByBlockNumberAndIndex.getString("to"));
    logger.info("effectiveGasPrice:" + resultFromTransactionReceipt.getString("effectiveGasPrice"));
    logger.info("gasPriceFromHttp:" + Long.toHexString(gasPriceFromHttp));
    Assert.assertEquals(
        resultFromTransactionReceipt.getString("effectiveGasPrice"),
        "0x" + Long.toHexString(gasPriceFromHttp));
    /* Assert.assertEquals(
    resultFromTransactionReceipt.getString("contractAddress").substring(2),
    trc20AddressHex.substring(2));*/
    Assert.assertNull(resultFromTransactionReceipt.getString("contractAddress"));
    Assert.assertEquals(
        resultFromTransactionReceipt.getString("logsBloom"),
        "0x000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000");
    Assert.assertEquals("0x1", resultFromTransactionReceipt.getString("status"));
    Assert.assertEquals("0x0", resultFromTransactionReceipt.getString("type"));
    logger.info("gas:" + resultFromTransactionByBlockNumberAndIndex.getString("gas"));
    Assert.assertEquals(
        resultFromTransactionReceipt.getString("gasUsed"),
        resultFromTransactionByBlockNumberAndIndex.getString("gas"));
    Assert.assertEquals(
        resultFromTransactionReceipt.getString("cumulativeGasUsed"),
        resultFromTransactionByBlockNumberAndIndex.getString("gas"));
    Assert.assertEquals(
        logs.getJSONObject(0).getString("logIndex"), "0x" + Integer.toHexString(index));
    Assert.assertEquals(logs.getJSONObject(0).getString("removed"), "false");
    Assert.assertEquals(
        logs.getJSONObject(0).getString("blockHash"),
        resultFromTransactionReceipt.getString("blockHash"));
    Assert.assertEquals(
        logs.getJSONObject(0).getString("blockNumber"),
        resultFromTransactionReceipt.getString("blockNumber"));
    Assert.assertEquals(
        logs.getJSONObject(0).getString("transactionIndex"),
        resultFromTransactionReceipt.getString("transactionIndex"));
    Assert.assertEquals(
        logs.getJSONObject(0).getString("transactionHash"),
        resultFromTransactionReceipt.getString("transactionHash"));
    Assert.assertEquals(
        logs.getJSONObject(0).getString("address"), resultFromTransactionReceipt.getString("to"));
    response =
        HttpMethed.getTransactionInfoByBlocknumFromSolidity(httpsolidityNode, blockNumForTrc20);
    List<JSONObject> responseContent1 = HttpMethed.parseResponseContentArray(response);
    logger.info("responseContent1:" + responseContent1);

    response = HttpMethed.getBlockByNumFromSolidity(httpsolidityNode, blockNumForTrc20);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertEquals(
        logs.getJSONObject(0).getString("data").substring(2),
        responseContent1.get(index).getJSONArray("log").getJSONObject(0).getString("data"));

    Assert.assertEquals(
        logs.getJSONObject(0).getString("topics").replace("0x", ""),
        responseContent1.get(index).getJSONArray("log").getJSONObject(0).getString("topics"));
  }

  @Test(
      enabled = true,
      description = "Json rpc api of eth_getUncleByBlockHashAndIndex from solidity")
  public void test22JsonRpcApiTestForEthGetUncleByBlockHashAndIndex() throws Exception {
    JsonArray params = new JsonArray();
    params.add("0x0000000000f9cc56243898cbe88685678855e07f51c5af91322c225ce3693868");
    params.add("0x");
    JsonObject requestBody = getJsonRpcBody("eth_getUncleByBlockHashAndIndex", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String result = responseContent.getString("result");
    logger.info(result);
    Assert.assertNull(result);
  }

  @Test(
      enabled = true,
      description = "Json rpc api of eth_getUncleByBlockNumberAndIndex from solidity")
  public void test23JsonRpcApiTestForEthGetUncleByBlockNumberAndIndex() throws Exception {
    JsonArray params = new JsonArray();
    params.add("0xeb82f0");
    params.add("0x");
    JsonObject requestBody = getJsonRpcBody("eth_getUncleByBlockNumberAndIndex", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String result = responseContent.getString("result");
    logger.info(result);
    Assert.assertNull(result);
  }

  @Test(enabled = true, description = "Json rpc api of eth_getUncleCountByBlockHash from solidity")
  public void test24JsonRpcApiTestForEthGetUncleCountByBlockHash() throws Exception {
    JsonArray params = new JsonArray();
    params.add("0x0000000000f9cc56243898cbe88685678855e07f51c5af91322c225ce3693868");
    JsonObject requestBody = getJsonRpcBody("eth_getUncleCountByBlockHash", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String result = responseContent.getString("result");
    logger.info(result);
    Assert.assertEquals(result, "0x0");
  }

  @Test(
      enabled = true,
      description = "Json rpc api of eth_getUncleCountByBlockNumber from solidity")
  public void test25JsonRpcApiTestForEthGetUncleCountByBlockNumber() throws Exception {
    JsonArray params = new JsonArray();
    params.add("eth_getUncleCountByBlockNumber");
    JsonObject requestBody = getJsonRpcBody("eth_getUncleCountByBlockNumber", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String result = responseContent.getString("result");
    logger.info(result);
    Assert.assertEquals(result, "0x0");
  }

  @Test(enabled = true, description = "Json rpc api of eth_getWork from solidity")
  public void test26JsonRpcApiTestForEthGetWork() throws Exception {
    String resultFromjsonRpcNodeForSolidity = "";
    String resultFromHttp = "";
    for (int i = 0; i < 5; i++) {
      JsonArray params = new JsonArray();
      JsonObject requestBody = getJsonRpcBody("eth_getWork", params);
      response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
      responseContent = HttpMethed.parseResponseContent(response);
      String result = responseContent.getString("result");
      int resultLen = result.length();
      resultFromjsonRpcNodeForSolidity = result.substring(4, resultLen - 12);
      response = HttpMethed.getNowBlockFromSolidity(httpsolidityNode);
      responseContent = HttpMethed.parseResponseContent(response);
      resultFromHttp = responseContent.getString("blockID");
      logger.info("resultFromjsonRpcNodeForSolidity" + i + ":" + resultFromjsonRpcNodeForSolidity);
      logger.info("resultFromHttp" + i + ":" + resultFromHttp);
      if (resultFromjsonRpcNodeForSolidity.equals(resultFromHttp)) {
        break;
      }
      HttpMethed.waitToProduceOneBlock(httpFullNode);
    }
    Assert.assertEquals(resultFromjsonRpcNodeForSolidity, resultFromHttp);
  }

  @Test(enabled = true, description = "Json rpc api of eth_hashrate from solidity ")
  public void test27JsonRpcApiTestForEthHashRate() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("eth_hashrate", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String result = responseContent.getString("result");
    logger.info(result);
    Assert.assertEquals("0x0", result);
  }

  @Test(enabled = true, description = "Json rpc api of eth_mining from solidity")
  public void test28JsonRpcApiTestForEthMining() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("eth_mining", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String result = responseContent.getString("result");
    logger.info(result);
    Assert.assertEquals(result, "true");
  }

  @Test(enabled = true, description = "Json rpc api of eth_protocolVersion from solidity")
  public void test29JsonRpcApiTestForEthProtocolVersion() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("eth_protocolVersion", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String protocolVersion = responseContent.getString("result").substring(2);
    Long protocolVersion1 = Long.parseLong(protocolVersion, 16);
    response = HttpMethed.getNowBlockFromSolidity(httpsolidityNode);
    responseContent = HttpMethed.parseResponseContent(response);
    Long protocolVersion2 =
        responseContent.getJSONObject("block_header").getJSONObject("raw_data").getLong("version");
    logger.info(protocolVersion1.toString());
    logger.info(protocolVersion2.toString());
    Assert.assertEquals(protocolVersion1, protocolVersion2);
  }

  @Test(enabled = true, description = "Json rpc api of eth_syncing from solidity")
  public void test30JsonRpcApiTestForEthSyncing() throws Exception {
    long currentNumFromHttp = 0;
    long currentNumFromJsonRpc = 0;
    JSONObject temp = responseContent;
    for (int i = 0; i < 5; i++) {
      JsonArray params = new JsonArray();
      JsonObject requestBody = getJsonRpcBody("eth_syncing", params);
      response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
      responseContent = HttpMethed.parseResponseContent(response);
      temp = responseContent.getJSONObject("result");
      String currentNumFromRpc = temp.getString("currentBlock");
      logger.info(currentNumFromRpc);
      logger.info(temp.toString());
      response = HttpMethed.getNowBlockFromSolidity(httpsolidityNode);
      responseContent = HttpMethed.parseResponseContent(response);
      currentNumFromHttp =
          responseContent.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
      currentNumFromJsonRpc = Long.parseLong(currentNumFromRpc.substring(2), 16);
      logger.info("currentNumFromHttp" + i + ":" + currentNumFromHttp);
      logger.info("currentNumFromJsonRpc:" + currentNumFromJsonRpc);
      if (currentNumFromHttp == currentNumFromJsonRpc) {
        break;
      }
      HttpMethed.waitToProduceOneBlock(httpFullNode);
    }
    Assert.assertEquals(currentNumFromHttp, currentNumFromJsonRpc);
    Assert.assertTrue(temp.containsKey("startingBlock"));
    Assert.assertTrue(temp.containsKey("currentBlock"));
    Assert.assertTrue(temp.containsKey("highestBlock"));
  }

  @Test(enabled = true, description = "Json rpc api of net_listening from solidity")
  public void test31JsonRpcApiTestForNetListening() throws Exception {
    boolean temp = false;
    boolean expect = false;
    for (int i = 0; i < 5; i++) {
      JsonArray params = new JsonArray();
      JsonObject requestBody = getJsonRpcBody("net_listening", params);
      response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
      responseContent = HttpMethed.parseResponseContent(response);
      temp = responseContent.getBoolean("result");
      response = HttpMethed.getNodeInfo(httpsolidityNode);
      responseContent = HttpMethed.parseResponseContent(response);
      int num = responseContent.getInteger("activeConnectCount");
      logger.info("num" + i + ":" + num);
      if (num >= 1) {
        expect = true;
      }
      logger.info("temp" + i + ":" + temp);
      logger.info("expect" + i + ":" + expect);
      if (temp == true && expect == true) {
        break;
      }
      HttpMethed.waitToProduceOneBlock(httpFullNode);
    }
    Assert.assertEquals(expect, temp);
  }

  @Test(enabled = true, description = "Json rpc api of net_peerCount from solidity")
  public void test32JsonRpcApiTestForNetPeerCount() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("net_peerCount", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String result = responseContent.getString("result");
    logger.info(result);
    Assert.assertNotNull(result);
  }

  @Test(enabled = true, description = "Json rpc api of net_version from solidity")
  public void test33JsonRpcApiTestForEthVersion() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("net_version", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String firstBlockHashFromJsonRpc = responseContent.getString("result").substring(2);
    response = HttpMethed.getBlockByNumFromSolidity(httpsolidityNode, 0);
    responseContent = HttpMethed.parseResponseContent(response);
    String firstBlockHashFromHttp = responseContent.getString("blockID").substring(56);
    logger.info("firstBlockHashFromJsonRpc" + firstBlockHashFromJsonRpc);
    logger.info("firstBlockHashFromHttp" + firstBlockHashFromHttp);
    Assert.assertEquals(firstBlockHashFromJsonRpc, firstBlockHashFromHttp);
  }

  @Test(enabled = true, description = "Json rpc api of web3_clientVersion from solidity")
  public void test34JsonRpcApiTestForWeb3ClientVersion() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("web3_clientVersion", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String result = responseContent.getString("result");
    List<String> resultList = new ArrayList<>();
    for (String str : result.split("/")) {
      resultList.add(str);
    }
    Assert.assertEquals(resultList.size(), 5);
    Assert.assertEquals(resultList.get(0), "TRON");
    Assert.assertEquals(resultList.get(1).substring(0, 1), "v");
    Assert.assertEquals(resultList.get(2), "Linux");
    Assert.assertEquals(resultList.get(3), "Java1.8");
    Assert.assertEquals(resultList.get(4).substring(0, 11), "GreatVoyage");
  }

  @Test(enabled = true, description = "Json rpc api of web3_sha3 from solidity")
  public void test35JsonRpcApiTestForWeb3Sha3() throws Exception {
    JsonArray params = new JsonArray();
    params.add("0x08");
    JsonObject requestBody1 = getJsonRpcBody("web3_sha3", params);
    response = getEthHttps(ethHttpsNode, requestBody1);
    responseContent = HttpMethed.parseResponseContent(response);
    String result1 = responseContent.getString("result");
    JsonObject requestBody2 = getJsonRpcBody("web3_sha3", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody2);
    responseContent = HttpMethed.parseResponseContent(response);
    String result2 = responseContent.getString("result");
    Assert.assertEquals(result1, result2);
  }

  @Test(enabled = true, description = "Json rpc api of eth_compileLLL from solidity")
  public void test36JsonRpcApiTestForEthCompileLll() throws Exception {
    JsonArray params = new JsonArray();
    params.add("(returnlll (suicide (caller)))");
    JsonObject requestBody1 = getJsonRpcBody("eth_compileLLL", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody1);
    responseContent = HttpMethed.parseResponseContent(response);
    String errorMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(errorMessage, "the method eth_compileLLL does not exist/is not available");
  }

  @Test(enabled = true, description = "Json rpc api of eth_compileSerpent from solidity")
  public void test37JsonRpcApiTestForEthCompileSerpent() throws Exception {
    JsonArray params = new JsonArray();
    params.add("/* some serpent */");
    JsonObject requestBody = getJsonRpcBody("eth_compileSerpent", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String errorMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(
        errorMessage, "the method eth_compileSerpent does not exist/is not available");
  }

  @Test(enabled = true, description = "Json rpc api of eth_compileSolidity from solidity")
  public void test38JsonRpcApiTestForEthCompileSolidity() throws Exception {
    JsonArray params = new JsonArray();
    params.add("contract test { function multiply(uint a) returns(uint d) {   return a * 7;   } }");
    JsonObject requestBody = getJsonRpcBody("eth_compileSolidity", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String errorMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(
        errorMessage, "the method eth_compileSolidity does not exist/is not available");
  }

  @Test(enabled = true, description = "Json rpc api of eth_getCompilers from solidity")
  public void test39JsonRpcApiTestForEthCompileSolidity() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("eth_getCompilers", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String errorMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(
        errorMessage, "the method eth_getCompilers does not exist/is not available");
  }

  @Test(enabled = true, description = "Json rpc api of eth_getTransactionCount from solidity")
  public void test40JsonRpcApiTestForEthGetTransactionCount() throws Exception {
    JsonArray params = new JsonArray();
    params.add("0x407d73d8a49eeb85d32cf465507dd71d507100c1");
    params.add("latest");
    JsonObject requestBody = getJsonRpcBody("eth_getTransactionCount", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String errorMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(
        errorMessage, "the method eth_getTransactionCount does not exist/is not available");
  }

  @Test(enabled = true, description = "Json rpc api of eth_sendRawTransaction from solidity")
  public void test41JsonRpcApiTestForEthSendRawTransaction() throws Exception {
    JsonArray params = new JsonArray();
    params.add("0x234");
    JsonObject requestBody = getJsonRpcBody("eth_sendRawTransaction", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String errorMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(
        errorMessage, "the method eth_sendRawTransaction does not exist/is not available");
  }

  @Test(enabled = true, description = "Json rpc api of eth_sendTransaction")
  public void test42JsonRpcApiTestForEthSendTransaction() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject temp = new JsonObject();
    params.add(temp);
    temp.addProperty("from", "0xb60e8dd61c5d32be8058bb8eb970870f07233155");
    temp.addProperty("to", "0xd46e8dd67c5d32be8058bb8eb970870f07244567");
    temp.addProperty("gas", "0x76c0");
    temp.addProperty("gasPrice", "0x9184e72a000");
    temp.addProperty(
        "data",
        "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");
    temp.addProperty("value", "0x9184e72a");

    JsonObject requestBody = getJsonRpcBody("eth_sendTransaction", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String errorMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(
        errorMessage, "the method eth_sendTransaction does not exist/is not available");
  }

  @Test(enabled = true, description = "Json rpc api of eth_sign from solidity")
  public void test43JsonRpcApiTestForEthSign() throws Exception {
    JsonArray params = new JsonArray();
    params.add("0x9b2055d370f73ec7d8a03e965129118dc8f5bf83");
    params.add("0xdeadbeaf");
    JsonObject requestBody = getJsonRpcBody("eth_sign", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String errorMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(errorMessage, "the method eth_sign does not exist/is not available");
  }

  @Test(enabled = true, description = "Json rpc api of eth_signTransaction from solidity")
  public void test44JsonRpcApiTestForEthSignTransaction() throws Exception {
    JsonArray params = new JsonArray();
    JsonObject temp = new JsonObject();
    params.add(temp);
    temp.addProperty(
        "data",
        "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");
    temp.addProperty("from", "0xb60e8dd61c5d32be8058bb8eb970870f07233155");
    temp.addProperty("gas", "0x76c0");
    temp.addProperty("gasPrice", "0x9184e72a000");
    temp.addProperty("to", "0xd46e8dd67c5d32be8058bb8eb970870f07244567");
    temp.addProperty("value", "0x9184e72a");

    JsonObject requestBody = getJsonRpcBody("eth_signTransaction", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String errorMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(
        errorMessage, "the method eth_signTransaction does not exist/is not available");
  }

  @Test(enabled = true, description = "Json rpc api of eth_submitWork from solidity")
  public void test45JsonRpcApiTestForEthSubmitWork() throws Exception {
    JsonArray params = new JsonArray();
    params.add("0x0000000000000001");
    params.add("0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
    params.add("0xD1GE5700000000000000000000000000D1GE5700000000000000000000000000");
    JsonObject requestBody = getJsonRpcBody("eth_submitWork", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String errorMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(errorMessage, "the method eth_submitWork does not exist/is not available");
  }

  @Test(enabled = true, description = "Json rpc api of parity_nextNonce from solidity")
  public void test46JsonRpcApiTestForParityNextNonce() throws Exception {
    JsonArray params = new JsonArray();
    params.add("0x9b2055d370f73ec7d8a03e965129118dc8f5bf83");
    JsonObject requestBody = getJsonRpcBody("parity_nextNonce", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String errorMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(
        errorMessage, "the method parity_nextNonce does not exist/is not available");
  }

  @Test(enabled = true, description = "Json rpc api of eth_submitHashrate from solidity")
  public void test47JsonRpcApiTestForEthSubmitHashrate() throws Exception {
    JsonArray params = new JsonArray();
    params.add("0x0000000000000000000000000000000000000000000000000000000000500000");
    params.add("0x59daa26581d0acd1fce254fb7e85952f4c09d0915afd33d3886cd914bc7d283c");
    JsonObject requestBody = getJsonRpcBody("eth_submitHashrate", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String errorMessage = responseContent.getJSONObject("error").getString("message");
    Assert.assertEquals(
        errorMessage, "the method eth_submitHashrate does not exist/is not available");
  }

  @Test(
      enabled = true,
      description = "Json rpc api of eth_getBlockByHash params is false from solidity")
  public void test48JsonRpcApiTestForEthGetBlockByHash() throws Exception {
    response = HttpMethed.getBlockByNumFromSolidity(httpsolidityNode, blockNum);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("45getBlockByNumFromSolidityFromHttp:" + responseContent);
    accountStateRoot =
        responseContent
            .getJSONObject("block_header")
            .getJSONObject("raw_data")
            .getString("accountStateRoot");
    JsonArray params = new JsonArray();
    params.add(blockHash);
    params.add(false);
    JsonObject requestBody = getJsonRpcBody("eth_getBlockByHash", params);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    JSONObject getBlockByHashResult = responseContent.getJSONObject("result");

    Assert.assertNull(getBlockByHashResult.getString("nonce"));
    Assert.assertNull(getBlockByHashResult.getString("sha3Uncles"));
    Assert.assertNull(getBlockByHashResult.getString("receiptsRoot"));
    Assert.assertNull(getBlockByHashResult.getString("difficulty"));
    Assert.assertNull(getBlockByHashResult.getString("totalDifficulty"));
    Assert.assertNull(getBlockByHashResult.getString("extraData"));
    Assert.assertNull(getBlockByHashResult.getString("baseFeePerGas"));
    Assert.assertNull(getBlockByHashResult.getString("mixHash"));
    Assert.assertEquals(getBlockByHashResult.getString("uncles"), new ArrayList<>().toString());
    Assert.assertEquals(getBlockByHashResult.getString("stateRoot"), "0x" + accountStateRoot);

    Assert.assertEquals(
        getBlockByHashResult.getString("logsBloom"),
        "0x00000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000");
    Assert.assertEquals(getBlockByHashResult.getString("number"), blockNumHex);
    Assert.assertEquals(getBlockByHashResult.getString("hash"), "0x" + bid);
    Assert.assertEquals(getBlockByHashResult.getString("parentHash"), "0x" + parentHash);
    Assert.assertEquals(getBlockByHashResult.getString("transactionsRoot"), "0x" + txTrieRoot);
    Assert.assertEquals(
        getBlockByHashResult.getString("miner"), "0x" + witnessAddress.substring(2));
    Assert.assertEquals(getBlockByHashResult.getString("gasUsed"), "0x" + Long.toHexString(gas));
    Assert.assertEquals(
        String.valueOf(Long.parseLong(getBlockByHashResult.getString("gasLimit").substring(2), 16)),
        feeLimit);
    Assert.assertEquals(
        Long.parseLong(getBlockByHashResult.getString("timestamp").substring(2), 16),
        blockTimeStamp);
    final GrpcAPI.NumberMessage message =
        GrpcAPI.NumberMessage.newBuilder().setNum(blockNum).build();
    HttpMethed.waitToProduceOneBlock(httpFullNode);
    Block block = blockingStubFull.getBlockByNum(message);
    logger.info("sizeFromJrpc:" + block.getSerializedSize());
    logger.info(
        "sizeFromJsonRPc:"
            + Long.parseLong(getBlockByHashResult.getString("size").substring(2), 16));
    size = block.getSerializedSize();
    Assert.assertEquals(
        Long.parseLong(getBlockByHashResult.getString("size").substring(2), 16),
        block.getSerializedSize());

    Long.parseLong(getBlockByHashResult.getString("timestamp").substring(2), 16);
    JSONArray transactionId = getBlockByHashResult.getJSONArray("transactions");
    List<String> transactionIdListFromGetBlockByHash = new ArrayList<>();
    if (transactionId.size() > 0) {
      for (int i = 0; i < transactionId.size(); i++) {
        transactionIdListFromGetBlockByHash.add(transactionId.get(i).toString());
      }
    }
    Assert.assertEquals(transactionIdListFromGetBlockByHash, transactionIdList);
  }

  @Test(
      enabled = true,
      description = "Json rpc api of eth_getBlockByNumFromSolidityber params is true from solidity")
  public void test49JsonRpcApiTestForEthgetBlockByNumFromSolidityber() throws Exception {

    JsonArray params = new JsonArray();
    params.add(blockNumHex);
    logger.info("46blockNumHex:" + blockNumHex);
    params.add(true);
    JsonObject requestBody = getJsonRpcBody("eth_getBlockByNumber", params);
    logger.info("requestBody:" + requestBody);
    response = getJsonRpc(jsonRpcNodeForSolidity, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("responseContent:" + responseContent);
    JSONObject getBlockByNumFromSolidityberResult = responseContent.getJSONObject("result");
    logger.info("getBlockByHashResult:" + getBlockByNumFromSolidityberResult);

    Assert.assertNull(getBlockByNumFromSolidityberResult.getString("nonce"));
    Assert.assertNull(getBlockByNumFromSolidityberResult.getString("sha3Uncles"));
    Assert.assertNull(getBlockByNumFromSolidityberResult.getString("receiptsRoot"));
    Assert.assertNull(getBlockByNumFromSolidityberResult.getString("difficulty"));
    Assert.assertNull(getBlockByNumFromSolidityberResult.getString("totalDifficulty"));
    Assert.assertNull(getBlockByNumFromSolidityberResult.getString("extraData"));
    Assert.assertNull(getBlockByNumFromSolidityberResult.getString("baseFeePerGas"));
    Assert.assertNull(getBlockByNumFromSolidityberResult.getString("mixHash"));
    Assert.assertEquals(
        getBlockByNumFromSolidityberResult.getString("uncles"), new ArrayList<>().toString());
    Assert.assertEquals(
        getBlockByNumFromSolidityberResult.getString("stateRoot"), "0x" + accountStateRoot);
    Assert.assertEquals(
        getBlockByNumFromSolidityberResult.getString("logsBloom"),
        "0x00000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000");
    Assert.assertEquals(getBlockByNumFromSolidityberResult.getString("number"), blockNumHex);
    Assert.assertEquals(getBlockByNumFromSolidityberResult.getString("hash"), "0x" + bid);
    Assert.assertEquals(
        getBlockByNumFromSolidityberResult.getString("parentHash"), "0x" + parentHash);
    Assert.assertEquals(
        getBlockByNumFromSolidityberResult.getString("transactionsRoot"), "0x" + txTrieRoot);
    Assert.assertEquals(
        getBlockByNumFromSolidityberResult.getString("miner"), "0x" + witnessAddress.substring(2));
    Assert.assertEquals(
        getBlockByNumFromSolidityberResult.getString("gasUsed"), "0x" + Long.toHexString(gas));
    Assert.assertEquals(
        String.valueOf(
            Long.parseLong(
                getBlockByNumFromSolidityberResult.getString("gasLimit").substring(2), 16)),
        feeLimit);
    Assert.assertEquals(
        Long.parseLong(getBlockByNumFromSolidityberResult.getString("timestamp").substring(2), 16),
        blockTimeStamp);
    logger.info("size:" + size);
    Assert.assertEquals(
        Long.parseLong(getBlockByNumFromSolidityberResult.getString("size").substring(2), 16),
        size);

    JSONArray transactionsList = getBlockByNumFromSolidityberResult.getJSONArray("transactions");
    logger.info("transactionsList:" + transactionsList);
    List<String> transactionInfoListFromGetBlockByHash = new ArrayList<>();
    if (transactionsList.size() > 0) {
      for (int i = 0; i < transactionsList.size(); i++) {
        transactionInfoListFromGetBlockByHash.add(transactionsList.get(i).toString());
      }
    }
    List<String> transactionInfoListFromTransactionByBlockNumberAndIndex = new ArrayList<>();
    for (int i = 0; i < transactionsList.size(); i++) {
      JsonArray paramsForEthGetTransactionByBlockNumberAndIndex = new JsonArray();
      paramsForEthGetTransactionByBlockNumberAndIndex.add(blockNumHex);
      String index = "0x" + Integer.toHexString(i);
      logger.info("index:" + index);
      paramsForEthGetTransactionByBlockNumberAndIndex.add(index);
      logger.info(
          "paramsForEthGetTransactionByBlockNumberAndIndex:"
              + paramsForEthGetTransactionByBlockNumberAndIndex);
      JsonObject requestBodyForTransactionByBlockNumberAndIndex =
          getJsonRpcBody(
              "eth_getTransactionByBlockNumberAndIndex",
              paramsForEthGetTransactionByBlockNumberAndIndex);
      response = getJsonRpc(jsonRpcNodeForSolidity, requestBodyForTransactionByBlockNumberAndIndex);
      responseContent = HttpMethed.parseResponseContent(response);
      logger.info("responseContent:" + responseContent);
      result = responseContent.getJSONObject("result");
      logger.info("result:" + result);
      transactionInfoListFromTransactionByBlockNumberAndIndex.add(result.toString());
    }
    Assert.assertEquals(
        transactionInfoListFromGetBlockByHash,
        transactionInfoListFromTransactionByBlockNumberAndIndex);
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
