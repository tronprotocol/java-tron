package stest.tron.wallet.dailybuild.jsonrpc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.JsonRpcBase;
import stest.tron.wallet.common.client.utils.PublicMethed;



@Slf4j

public class BuildTransaction001 extends JsonRpcBase {

  JSONArray jsonRpcReceives = new JSONArray();
  //String txid;
  private JSONObject responseContent;
  private HttpResponse response;
  String transactionString;
  String transactionSignString;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey1.getAddress();
  final String receiverKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }



  @Test(enabled = true, description = "Json rpc api of buildTransaction for transfer trx")
  public void test01JsonRpcApiTestOfBuildTransactionForTransferTrx() throws Exception {
    final Long beforeRecevierBalance = HttpMethed.getBalance(httpFullNode, receiverAddress);


    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(jsonRpcOwnerAddress));
    param.addProperty("to", ByteArray.toHexString(receiverAddress));
    param.addProperty("value", "0x1");
    JsonArray params = new JsonArray();
    params.add(param);
    JsonObject requestBody = getJsonRpcBody("buildTransaction",params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    transactionString = responseContent.getJSONObject("result").getString("transaction");
    transactionSignString = HttpMethed.gettransactionsign(httpFullNode, transactionString,
        jsonRpcOwnerKey);
    response = HttpMethed.broadcastTransaction(httpFullNode, transactionSignString);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlock(httpFullNode);
    Long afterRecevierBalance = HttpMethed.getBalance(httpFullNode, receiverAddress);

    Assert.assertEquals(afterRecevierBalance - beforeRecevierBalance,1L);

  }

  @Test(enabled = true, description = "Json rpc api of buildTransaction for transfer trc10")
  public void test02JsonRpcApiTestOfBuildTransactionForTransferTrc10() throws Exception {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    final Long beforeTokenBalance = PublicMethed.getAssetBalanceByAssetId(ByteString
        .copyFromUtf8(jsonRpcAssetId), receiverKey, blockingStubFull);
    JsonObject param = new JsonObject();
    param.addProperty("from", ByteArray.toHexString(jsonRpcOwnerAddress));
    param.addProperty("to", ByteArray.toHexString(receiverAddress));
    param.addProperty("tokenId", Long.valueOf(jsonRpcAssetId));
    param.addProperty("tokenValue", 1);
    JsonArray params = new JsonArray();
    params.add(param);
    JsonObject requestBody = getJsonRpcBody("buildTransaction",params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    transactionString = responseContent.getJSONObject("result").getString("transaction");
    transactionSignString = HttpMethed.gettransactionsign(httpFullNode, transactionString,
        jsonRpcOwnerKey);
    response = HttpMethed.broadcastTransaction(httpFullNode, transactionSignString);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlock(httpFullNode);
    Long afterTokenBalance = PublicMethed.getAssetBalanceByAssetId(ByteString
        .copyFromUtf8(jsonRpcAssetId), receiverKey, blockingStubFull);

    Assert.assertEquals(afterTokenBalance - beforeTokenBalance,1L);

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
