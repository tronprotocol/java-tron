package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.Note;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.config.args.Args;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.ShieldNoteInfo;

@Slf4j
public class HttpTestZenToken005 {

  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo;
  String sendShieldAddress;
  String receiverShieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  String memo1;
  String memo2;
  ShieldNoteInfo sendNote;
  ShieldNoteInfo receiveNote;
  String assetIssueId;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethed.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long sendTokenAmount = 7 * zenTokenFee;
  private JSONObject responseContent;
  private HttpResponse response;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(foundationZenTokenKey);
    PublicMethed.printAddress(zenTokenOwnerKey);
    response = HttpMethed
        .transferAsset(httpnode, foundationZenTokenAddress, zenTokenOwnerAddress, zenTokenId,
            sendTokenAmount, foundationZenTokenKey);
    org.junit.Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    Args.setFullNodeAllowShieldedTransaction(true);
    sendShieldAddressInfo = HttpMethed.generateShieldAddress(httpnode);
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddress:" + sendShieldAddress);
    memo1 = "Shield memo1 in " + System.currentTimeMillis();
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, sendShieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo1);

    response = HttpMethed
        .sendShieldCoin(httpnode, zenTokenOwnerAddress, sendTokenAmount, null, null, shieldOutList,
            null, 0, zenTokenOwnerKey);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    shieldOutList.clear();
    HttpMethed.waitToProduceOneBlock(httpnode);
    sendNote = HttpMethed.scanNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0);
  }

  @Test(enabled = false, description = "Shield to shield transaction without ask by http")
  public void test01ShieldToShieldWithoutAskTransaction() {
    receiverShieldAddressInfo = HttpMethed.generateShieldAddress(httpnode);
    receiverShieldAddress = receiverShieldAddressInfo.get().getAddress();

    shieldOutList.clear();
    memo2 = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress,
        "" + (sendNote.getValue() - zenTokenFee), memo2);

    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethed
        .sendShieldCoinWithoutAsk(httpnode, httpSolidityNode, httpPbftNode, null, 0,
            sendShieldAddressInfo.get(), sendNote, shieldOutList, null, 0, null);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    logger.info("response:" + response);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("responseContent:" + responseContent);
    HttpMethed.printJsonContent(responseContent);
    HttpMethed.waitToProduceOneBlock(httpnode);

    receiveNote = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo.get()).get(0);

    Assert.assertTrue(receiveNote.getValue() == sendNote.getValue() - zenTokenFee);
    Assert.assertEquals(ByteArray.toHexString(memo2.getBytes()),
        ByteArray.toHexString(receiveNote.getMemo()));

    Assert.assertTrue(HttpMethed.getSpendResult(httpnode, sendShieldAddressInfo.get(), sendNote));
  }

  @Test(enabled = false, description = "Get merkle tree voucher info by http")
  public void test02GetMerkleTreeVoucherInfo() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed
        .getMerkleTreeVoucherInfo(httpnode, sendNote.getTrxId(), sendNote.getIndex(), 1);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.toJSONString().contains("tree"));
    Assert.assertTrue(responseContent.toJSONString().contains("rt"));
    Assert.assertTrue(responseContent.toJSONString().contains("paths"));

    response = HttpMethed
        .getMerkleTreeVoucherInfo(httpnode, receiveNote.getTrxId(), receiveNote.getIndex(), 1000);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.toJSONString().contains(
        "synBlockNum is too large, cmBlockNum plus synBlockNum must be <= latestBlockNumber"));
  }

  @Test(enabled = false, description = "Get merkle tree voucher info by http from solidity")
  public void test03GetMerkleTreeVoucherInfoFromSolidity() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed
        .getMerkleTreeVoucherInfoFromSolidity(httpSolidityNode, sendNote.getTrxId(),
            sendNote.getIndex(), 1);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.toJSONString().contains("tree"));
    Assert.assertTrue(responseContent.toJSONString().contains("rt"));
    Assert.assertTrue(responseContent.toJSONString().contains("paths"));

    response = HttpMethed
        .getMerkleTreeVoucherInfoFromSolidity(httpSolidityNode, receiveNote.getTrxId(),
            receiveNote.getIndex(), 1000);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.toJSONString().contains(
        "synBlockNum is too large, cmBlockNum plus synBlockNum must be <= latestBlockNumber"));
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    response = HttpMethed.getAccount(httpnode, foundationZenTokenAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    assetIssueId = responseContent.getString("asset_issued_ID");
    final Long assetBalance = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    HttpMethed
        .transferAsset(httpnode, zenTokenOwnerAddress, foundationZenTokenAddress, assetIssueId,
            assetBalance, zenTokenOwnerKey);
  }
}