package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpTestMutiSign001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey1.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  Long amount = 1000000000L;
  JsonArray keys = new JsonArray();
  JsonObject manager1Wight = new JsonObject();
  JsonObject manager2Wight = new JsonObject();
  JsonObject ownerObject = new JsonObject();
  JsonObject witnessObject = new JsonObject();
  JsonObject activeObject = new JsonObject();

  private final String manager1Key = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] manager1Address = PublicMethed.getFinalAddress(manager1Key);

  private final String manager2Key = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] manager2Address = PublicMethed.getFinalAddress(manager2Key);

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Account Permission Up Date by http")
  public void test1AccountPermissionUpDate() {
    PublicMethed.printAddress(ownerKey);
    response = HttpMethed.sendCoin(httpnode,fromAddress,ownerAddress,amount,testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    manager1Wight.addProperty("address",ByteArray.toHexString(manager1Address));
    manager1Wight.addProperty("weight",1);

    logger.info(manager1Wight.toString());
    manager2Wight.addProperty("address",ByteArray.toHexString(manager2Address));
    manager2Wight.addProperty("weight",1);

    logger.info(manager2Wight.toString());

    keys.add(manager1Wight);
    keys.add(manager2Wight);

    ownerObject.addProperty("type", 0);
    ownerObject.addProperty("permission_name", "owner");
    ownerObject.addProperty("threshold", 2);
    ownerObject.add("keys", keys);

    activeObject.addProperty("type", 2);
    activeObject.addProperty("permission_name", "active0");
    activeObject.addProperty("threshold", 2);
    activeObject.addProperty("operations",
        "7fff1fc0037e0000000000000000000000000000000000000000000000000000");
    activeObject.add("keys", keys);

    response = HttpMethed.accountPermissionUpdate(httpnode,ownerAddress,ownerObject,
        witnessObject,activeObject,ownerKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Add transaction sign by http")
  public void test2AddTransactionSign() {

    HttpMethed.waitToProduceOneBlock(httpnode);
    String[] permissionKeyString = new String[2];
    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;

    response = HttpMethed.sendCoin(httpnode,ownerAddress,fromAddress,10L,permissionKeyString);
    Assert.assertTrue(HttpMethed.verificationResult(response));


  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
