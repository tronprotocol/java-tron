package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.TransferContract;

public class GetBlockByNumServletTest extends BaseTest {

  @Resource
  private GetBlockByNumServlet getBlockByNumServlet;

  private BlockCapsule block;
  private TransactionCapsule transaction;

  static {
    Args.setParam(
          new String[]{
              "--output-directory", dbPath(),
          }, TestConstants.TEST_CONF
    );
  }

  @Before
  public void initBlock() {
    block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 123L,
        ByteString.copyFrom(new byte[21]));
    transaction = new TransactionCapsule(TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(
            "410000000000000000000000000000000000000001")))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            "410000000000000000000000000000000000000002")))
        .setAmount(1000L)
        .build(), ContractType.TransferContract);
    block.addTransaction(transaction);
    chainBaseManager.getBlockIndexStore().put(block.getBlockId());
    chainBaseManager.getBlockStore().put(block.getBlockId().getBytes(), block);
  }

  @Test
  public void testGetBlockByNum() throws Exception {
    String jsonParam = "{\"num\": 1}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();

    getBlockByNumServlet.doPost(request, response);
    assertRequestedBlock(response);
  }

  @Test
  public void testGet() throws Exception {
    MockHttpServletRequest request = createRequest(HttpGet.METHOD_NAME);
    request.addParameter("num", "1");
    MockHttpServletResponse response = new MockHttpServletResponse();

    getBlockByNumServlet.doGet(request, response);
    assertRequestedBlock(response);
  }

  private void assertRequestedBlock(MockHttpServletResponse response) throws Exception {
    assertEquals(200, response.getStatus());
    JSONObject result = JSONObject.parseObject(response.getContentAsString());
    assertEquals(block.getBlockId().toString(), result.getString("blockID"));
    assertEquals(1L, result.getJSONObject("block_header").getJSONObject("raw_data")
        .getLongValue("number"));
    assertEquals(1, result.getJSONArray("transactions").size());
    assertEquals(transaction.getTransactionId().toString(),
        result.getJSONArray("transactions").getJSONObject(0).getString("txID"));
  }
}
