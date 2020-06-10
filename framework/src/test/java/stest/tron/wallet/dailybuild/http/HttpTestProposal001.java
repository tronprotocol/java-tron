package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.tron.common.utils.ByteArray;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpTestProposal001 {

  private static Integer proposalId;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witness1Address = PublicMethed.getFinalAddress(witnessKey001);
  private final String witnessKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  private final byte[] witness2Address = PublicMethed.getFinalAddress(witnessKey002);
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private JSONObject responseContent;
  private HttpResponse response;

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Create proposal by http")
  public void test1CreateProposal() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.createProposal(httpnode, witness1Address, 21L, 1L, witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
  }

  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "List proposals by http")
  public void test2ListProposals() {
    response = HttpMethed.listProposals(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("proposals"));
    Assert.assertTrue(jsonArray.size() >= 1);
    proposalId = jsonArray.size();
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetProposalById by http")
  public void test3GetExchangeById() {
    response = HttpMethed.getProposalById(httpnode, proposalId);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getInteger("proposal_id") == proposalId);
    Assert.assertEquals(responseContent.getString("proposer_address"),
        ByteArray.toHexString(witness1Address));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Approval proposal by http")
  public void test4ApprovalProposal() {
    response = HttpMethed
        .approvalProposal(httpnode, witness1Address, proposalId, true, witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed
        .approvalProposal(httpnode, witness2Address, proposalId, true, witnessKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getProposalById(httpnode, proposalId);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("approvals"));
    Assert.assertTrue(jsonArray.size() == 2);
  }


  /**
   * * constructor. *
   */
  @Test(enabled = true, description = "Get paginated proposal list by http")
  public void test5GetPaginatedProposalList() {

    response = HttpMethed.getPaginatedProposalList(httpnode, 0, 1);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("proposals"));
    Assert.assertTrue(jsonArray.size() == 1);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Delete proposal by http")
  public void test6DeleteProposal() {
    response = HttpMethed.deleteProposal(httpnode, witness1Address, proposalId, witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getProposalById(httpnode, proposalId);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(responseContent.getString("state"), "CANCELED");
  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get chain parameters by http")
  public void test7GetChainParameters() {
    response = HttpMethed.getChainParameters(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals("getMaintenanceTimeInterval",
        responseContent.getJSONArray("chainParameter").getJSONObject(0).get("key"));
    Assert.assertEquals(300000,
        responseContent.getJSONArray("chainParameter").getJSONObject(0).get("value"));
    Assert.assertEquals("getCreateAccountFee",
        responseContent.getJSONArray("chainParameter").getJSONObject(2).get("key"));
    Assert.assertEquals(100000,
        responseContent.getJSONArray("chainParameter").getJSONObject(2).get("value"));
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}

