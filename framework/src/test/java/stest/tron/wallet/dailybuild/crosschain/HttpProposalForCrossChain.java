package stest.tron.wallet.dailybuild.crosschain;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.common.utils.ByteArray;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.CrossChainBase;
import stest.tron.wallet.common.client.utils.HttpMethed;


@Slf4j
public class HttpProposalForCrossChain extends CrossChainBase {
  String slotCount = "";
  String endTime = "";
  String duration = "";
  private HttpResponse response;
  private JSONObject responseContent;
  int registerNum = 202;

  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(0);
  private String crossChainHttpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(1);

  @Test(enabled = true, description = "Create proposal for cross chain")
  public void test01CreateProposalForGetAllowCrossChain() {
    //Create proposal for first chain
    response = HttpMethed.createProposal(httpnode, witness001Address, 54L, 1L, witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.listProposals(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("proposals"));
    org.junit.Assert.assertTrue(jsonArray.size() >= 1);
    final Integer proposalId = jsonArray.size();
    response = HttpMethed
        .approvalProposal(httpnode, witness001Address, proposalId, true, witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed
        .approvalProposal(httpnode, witness002Address, proposalId, true, witnessKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    //Create proposal for second chain
    response = HttpMethed.createProposal(crossChainHttpnode, witness001Address,
        54L, 1L, witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(crossChainHttpnode);

    response = HttpMethed.listProposals(crossChainHttpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    jsonArray = JSONArray.parseArray(responseContent.getString("proposals"));
    org.junit.Assert.assertTrue(jsonArray.size() >= 1);
    final Integer crossProposalId = jsonArray.size();
    response = HttpMethed
        .approvalProposal(crossChainHttpnode, witness001Address,
            crossProposalId, true, witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed
        .approvalProposal(crossChainHttpnode, witness002Address,
            crossProposalId, true, witnessKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));

  }


  @Test(enabled = true, description = "Create proposal for auction config")
  public void test02CreateProposalForGetAuctionConfig() {
    slotCount = "88";
    endTime = String.valueOf(System.currentTimeMillis() / 1000L + 300);
    logger.info("endTime:" + endTime);
    duration = "088";
    long slotVaule = Long.valueOf(crossRound + slotCount + endTime + duration);
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(55L, slotVaule);
    proposalMap.put(56L, 100000000L);
    response = HttpMethed.createProposals(httpnode, witness001Address,
        proposalMap, witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    System.out.println("-------------");

    slotVaule = Long.valueOf(round + slotCount + endTime + duration);
    proposalMap.put(55L, slotVaule);
    response = HttpMethed.createProposals(crossChainHttpnode, witness001Address,
        proposalMap, witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(crossChainHttpnode);

    //Approval proposal for first chain
    response = HttpMethed.listProposals(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("proposals"));
    org.junit.Assert.assertTrue(jsonArray.size() >= 1);
    final Integer proposalId = jsonArray.size();
    response = HttpMethed
        .approvalProposal(httpnode, witness001Address, proposalId, true, witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed
        .approvalProposal(httpnode, witness002Address, proposalId, true, witnessKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    //Approval proposal for second chain
    response = HttpMethed.listProposals(crossChainHttpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    jsonArray = JSONArray.parseArray(responseContent.getString("proposals"));
    org.junit.Assert.assertTrue(jsonArray.size() >= 1);
    final Integer crossProposalId = jsonArray.size();
    response = HttpMethed
        .approvalProposal(crossChainHttpnode, witness001Address,
            crossProposalId, true, witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    response = HttpMethed
        .approvalProposal(crossChainHttpnode, witness002Address,
            crossProposalId, true, witnessKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));

  }

  @Test(enabled = true, description = "Register cross chain")
  public void test03RegisterCrossChain() {
    HttpMethed.sendCoin(httpnode, foundationAddress, registerAccountAddress, 500000000L,
        foundationKey);
    HttpMethed.sendCoin(crossChainHttpnode, foundationAddress, registerAccountAddress, 500000000L,
        foundationKey);
    HttpMethed.waitToProduceOneBlock(httpnode);
    List<String> srList = new ArrayList<>();
    srList.add(ByteArray.toHexString(witness001Address));
    srList.add(ByteArray.toHexString(witness002Address));
    response =
        httpRegisterCrossChainGetTxid(httpnode, ByteArray.toHexString(registerAccountAddress),
            ByteArray.toHexString(registerAccountAddress),
            ByteArray.toHexString(crossChainId.toByteArray()), srList, startSynBlockNum, 300000L,
            ByteArray.toHexString(crossParentHash.toByteArray()),
            crossStartSynTimeStamp, registerNum, registerAccountKey);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    HttpMethed.waitToProduceOneBlock(httpnode);
    response =
        httpRegisterCrossChainGetTxid(httpnode, ByteArray.toHexString(registerAccountAddress),
            ByteArray.toHexString(registerAccountAddress),
            ByteArray.toHexString(crossChainId.toByteArray()), srList, startSynBlockNum, 300000L,
            ByteArray.toHexString(crossParentHash.toByteArray()),
            crossStartSynTimeStamp, registerNum, registerAccountKey);
    Assert.assertFalse(HttpMethed.verificationResult(response));

    response =
        httpRegisterCrossChainGetTxid(crossChainHttpnode,
            ByteArray.toHexString(registerAccountAddress),
        ByteArray.toHexString(registerAccountAddress),
        ByteArray.toHexString(crossChainId.toByteArray()), srList, startSynBlockNum, 300000L,
        ByteArray.toHexString(crossParentHash.toByteArray()),
            crossStartSynTimeStamp, registerNum, registerAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));

  }

  @Test(enabled = true, description = "Get register cross chain list")
  public void test04GetRegisterCrossChainList() {
    response = httpGetRegisterCrossChainList(httpnode, 0, 10);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    JSONArray array = responseContent.getJSONArray("crossChainInfo");

    Assert.assertTrue(array.size() >= 1);
  }

  @Test(enabled = true, description = "Vote cross chain")
  public void test05VoteCrossChain() {
    response = httpVoteCrossChain(httpnode, ByteArray.toHexString(registerAccountAddress),
        registerNum, voteAmount, Integer.valueOf(crossRound), registerAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    //vote cross chain for second chain
    response = httpVoteCrossChain(crossChainHttpnode,
        ByteArray.toHexString(registerAccountAddress),
        registerNum, voteAmount, Integer.valueOf(crossRound), registerAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(crossChainHttpnode);

  }


  @Test(enabled = true, description = "Unvote cross chain")
  public void test06UnVoteCrossChain() {

    response = httpUnvoteCrossChain(httpnode, ByteArray.toHexString(registerAccountAddress),
        registerNum, Integer.valueOf(crossRound), registerAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = httpUnvoteCrossChain(crossChainHttpnode,
        ByteArray.toHexString(registerAccountAddress),
        registerNum, Integer.valueOf(crossRound), registerAccountKey);
    HttpMethed.waitToProduceOneBlock(crossChainHttpnode);

  }

  @Test(enabled = true, description = "Update cross chain")
  public void test07UpdateCrossChain() {
    List<String> srList = new ArrayList<>();
    srList.add(ByteArray.toHexString(witness001Address));
    srList.add(ByteArray.toHexString(witness002Address));
    String updateParentHash = "0000000000000000fd45f1e9a38283a5555dd5616efd8691c8a736e91ce9f918";
    response = httpUpdateCrossChain(httpnode, ByteArray.toHexString(registerAccountAddress),
        ByteArray.toHexString(registerAccountAddress),
        ByteArray.toHexString(crossChainId.toByteArray()), srList, 2L, 30000L,
        updateParentHash, 1621491901000L, 1, registerAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

  }

  @Test(enabled = true, description = "Get cross chain vote summary list")
  public void test08GetCrossChainVoteSummaryList() {
    response = httpGetcrosschainvotesummarylist(httpnode, 0, 10, Integer.valueOf(crossRound));
    Assert.assertTrue(HttpMethed.verificationResult(response));

  }

  @Test(enabled = true, description = "Get cross chain parachain list")
  public void test09GetCrossChainParachainList() throws Exception {
    int waitTimes = 30;
    List<String> paraChainList = null;
    while (waitTimes-- >= 0) {
      response = httpGetparachainlist(httpnode, Integer.valueOf(crossRound));
      responseContent = HttpMethed.parseResponseContent(response);
      paraChainList = (List<String>) responseContent.get("paraChainIds");
      if (paraChainList.size() != 0) {
        break;
      }
      Thread.sleep(30000);
    }

    Assert.assertTrue(paraChainList.size() >= 1);


  }

  @Test(enabled = true, description = "Get cross chain vote detail list")
  public void test10GetCrossChainVoteDetailList() {
    response = httpGetcrosschainvotedetaillist(httpnode, registerNum,
        0, 10, Integer.valueOf(crossRound));
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    System.out.println(responseContent);
    JSONArray voteCrossChainContract = responseContent.getJSONArray("voteCrossChainContract");
    Assert.assertTrue(voteCrossChainContract.size() >= 1);
  }


  @Test(enabled = true, description = "get auction config list")
  public void test11GetAuctionConfigList() {
    response = httpGetauctionconfiglist(httpnode);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    JSONArray voteCrossChainContract = responseContent.getJSONArray("auctionConfigDetail");
    Assert.assertTrue(voteCrossChainContract.size() >= 1);

  }

}
