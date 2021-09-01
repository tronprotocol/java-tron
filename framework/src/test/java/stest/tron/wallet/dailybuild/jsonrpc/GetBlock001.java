package stest.tron.wallet.dailybuild.jsonrpc;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.Test;
import org.tron.common.utils.ByteArray;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.JsonRpcBase;


@Slf4j

public class GetBlock001 extends JsonRpcBase {
  private JSONObject responseContent;
  private HttpResponse response;

  @Test(enabled = true, description = "Json rpc api of eth_getBlockByHash")
  public void test01JsonRpcApiTestForEthGetBlockByHash() throws Exception {
    JsonArray params = new JsonArray();
    params.add(blockId);
    params.add("true");
    JsonObject requestBody = getJsonRpcBody("eth_getBlockByHash",params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);

    Assert.assertEquals(Integer.toHexString(blockNum), responseContent.getJSONObject("result")
        .getString("number").substring(2));
    Assert.assertEquals(blockId, responseContent.getJSONObject("result").getString("hash")
        .substring(2));
    Assert.assertTrue(responseContent.getJSONObject("result")
        .getJSONArray("transactions").size() >= 1);
  }

}
