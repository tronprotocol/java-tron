package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;

import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;

import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionStoreTest;

public class GetTransactionInfoByBlockNumServletTest extends BaseTest {

  @Resource
  private GetTransactionInfoByBlockNumServlet getTransactionInfoByBlockNumServlet;
  private static final byte[] transactionId = TransactionStoreTest.randomBytes(32);
  private static TransactionRetCapsule transactionRetCapsule;

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, Constant.TEST_CONF
    );
  }

  @Before
  public void init() {
    byte[] blockNum = ByteArray.fromLong(100);
    TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();

    transactionInfoCapsule.setId(transactionId);
    transactionInfoCapsule.setFee(1000L);
    transactionInfoCapsule.setBlockNumber(100L);
    transactionInfoCapsule.setBlockTimeStamp(200L);

    transactionRetCapsule = new TransactionRetCapsule();
    transactionRetCapsule.addTransactionInfo(transactionInfoCapsule.getInstance());
    chainBaseManager.getTransactionRetStore()
            .put(blockNum, transactionRetCapsule);
  }

  @Test
  public void testGetTransactionInfoByBlockNum() {
    String jsonParam = "{\"num\" : 100}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();

    getTransactionInfoByBlockNumServlet.doPost(request, response);
    Assert.assertEquals(200, response.getStatus());
    try {
      String contentAsString = response.getContentAsString();
      JSONArray array = JSONArray.parseArray(contentAsString);
      Assert.assertEquals(1, array.size());
      JSONObject object = (JSONObject) array.get(0);
      Assert.assertEquals(1000, object.get("fee"));
      Assert.assertEquals(100, object.get("blockNumber"));
    } catch (UnsupportedEncodingException e) {
      Assert.fail(e.getMessage());
    }
  }
}
