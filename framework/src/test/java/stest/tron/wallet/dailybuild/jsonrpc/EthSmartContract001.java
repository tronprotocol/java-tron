package stest.tron.wallet.dailybuild.jsonrpc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.JsonRpcBase;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ZenTrc20Base;


@Slf4j

public class EthSmartContract001 extends JsonRpcBase {
  private JSONObject responseContent;
  private HttpResponse response;

  @Test(enabled = true, description = "Json rpc api of eth_call")
  public void test01JsonRpcApiTestForEthCall() throws Exception {
    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(jsonRpcOwnerAddress));
    param.addProperty("to", trc20AddressHex);
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    param.addProperty("data", "0x06fdde03");
    JsonArray params = new JsonArray();
    params.add(param);
    params.add("latest");
    JsonObject requestBody = getJsonRpcBody("eth_call",params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String dataResult = responseContent.getString("result");
    Assert.assertEquals(dataResult,"0x000000000000000000000000000000000000000000000000000"
        + "0000000000020000000000000000000000000000000000000000000000000000000000000000a546f6b65"
        + "6e545243323000000000000000000000000000000000000000000000");
  }


  @Test(enabled = true, description = "Json rpc api of eth_estimateGas")
  public void test02JsonRpcApiTestForEthEstimateGas() throws Exception {
    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(jsonRpcOwnerAddress));
    param.addProperty("to", trc20AddressHex);
    param.addProperty("gas", "0x0");
    param.addProperty("gasPrice", "0x0");
    param.addProperty("value", "0x0");
    param.addProperty("data", "0x1249c58b");
    JsonArray params = new JsonArray();
    params.add(param);
    JsonObject requestBody = getJsonRpcBody("eth_estimateGas",params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String dataResult = responseContent.getString("result");
    Assert.assertEquals(dataResult,"0x147");
  }


  @Test(enabled = true, description = "Json rpc api of eth_getCode")
  public void test03JsonRpcApiTestForEthGetCode() throws Exception {
    JsonArray params = new JsonArray();
    params.add(trc20AddressHex);
    params.add("latest");
    JsonObject requestBody = getJsonRpcBody("eth_getCode",params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    String dataResult = responseContent.getString("result");
    Assert.assertTrue(dataResult.length() > 1000L);
  }

}
