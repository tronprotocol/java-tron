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
public class HttpTestZenToken004 {

  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo1;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo2;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo3;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo4;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo5;
  String sendShieldAddress;
  String receiverShieldAddress1;
  String receiverShieldAddress2;
  String receiverShieldAddress3;
  String receiverShieldAddress4;
  String receiverShieldAddress5;
  List<Note> shieldOutList = new ArrayList<>();
  String memo1;
  String memo2;
  String memo3;
  String memo4;
  String memo5;
  ShieldNoteInfo sendNote;
  ShieldNoteInfo receiverNote1;
  ShieldNoteInfo receiverNote2;
  ShieldNoteInfo receiverNote3;
  ShieldNoteInfo receiverNote4;
  ShieldNoteInfo receiverNote5;
  String assetIssueId;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] receiverPublicAddress = ecKey1.getAddress();
  String receiverPublicKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethed.getFinalAddress(foundationZenTokenKey);
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long zenTokenWhenCreateNewAddress = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenWhenCreateNewAddress");
  private Long sendTokenAmount = 18 * zenTokenFee;
  private JSONObject responseContent;
  private HttpResponse response;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(foundationZenTokenKey);
    Args.setFullNodeAllowShieldedTransaction(true);
  }

  @Test(enabled = false, description = "Shield to two shield transaction by http")
  public void test01ShieldToTwoShieldTransaction() {
    sendShieldAddressInfo = HttpMethed.generateShieldAddress(httpnode);
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddress:" + sendShieldAddress);
    String memo = "Shield memo in " + System.currentTimeMillis();
    shieldOutList = HttpMethed
        .addShieldOutputList(httpnode, shieldOutList, sendShieldAddress, "" + sendTokenAmount,
            memo);
    response = HttpMethed
        .sendShieldCoin(httpnode, foundationZenTokenAddress, sendTokenAmount + zenTokenFee, null,
            null, shieldOutList, null, 0, foundationZenTokenKey);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    HttpMethed.waitToProduceOneBlock(httpnode);
    sendNote = HttpMethed.scanNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0);

    receiverShieldAddressInfo1 = HttpMethed.generateShieldAddress(httpnode);
    receiverShieldAddressInfo2 = HttpMethed.generateShieldAddress(httpnode);
    receiverShieldAddress1 = receiverShieldAddressInfo1.get().getAddress();
    receiverShieldAddress2 = receiverShieldAddressInfo2.get().getAddress();
    logger.info("receiverShieldAddress1:" + receiverShieldAddress1);
    logger.info("receiverShieldAddress2:" + receiverShieldAddress2);
    memo1 = "Shield memo1 in " + System.currentTimeMillis();
    memo2 = "Shield memo2 in " + System.currentTimeMillis();
    Long sendToShiledAddress1Amount = 1 * zenTokenFee;
    Long sendToShiledAddress2Amount = sendTokenAmount - sendToShiledAddress1Amount - zenTokenFee;
    shieldOutList.clear();
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);

    response = HttpMethed
        .sendShieldCoin(httpnode, null, 0, sendShieldAddressInfo.get(), sendNote, shieldOutList,
            null, 0, null);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    HttpMethed.waitToProduceOneBlock(httpnode);

    receiverNote1 = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo1.get()).get(0);
    receiverNote2 = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo2.get()).get(0);
    Assert.assertTrue(receiverNote1.getValue() == sendToShiledAddress1Amount);
    Assert.assertTrue(receiverNote2.getValue() == sendToShiledAddress2Amount);
    Assert.assertEquals(memo1.getBytes(), receiverNote1.getMemo());
    Assert.assertEquals(memo2.getBytes(), receiverNote2.getMemo());

    Assert.assertTrue(HttpMethed.getSpendResult(httpnode, sendShieldAddressInfo.get(), sendNote));
  }

  @Test(enabled = false, description = "Shield to one public and one shield transaction by http")
  public void test02ShieldToOnePublicAndOneShieldTransaction() {
    sendShieldAddressInfo = HttpMethed.generateShieldAddress(httpnode);
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddress:" + sendShieldAddress);
    String memo = "Shield memo in " + System.currentTimeMillis();
    shieldOutList.clear();
    shieldOutList = HttpMethed
        .addShieldOutputList(httpnode, shieldOutList, sendShieldAddress, "" + sendTokenAmount,
            memo);
    response = HttpMethed
        .sendShieldCoin(httpnode, foundationZenTokenAddress, sendTokenAmount + zenTokenFee, null,
            null, shieldOutList, null, 0, foundationZenTokenKey);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    HttpMethed.waitToProduceOneBlock(httpnode);
    sendNote = HttpMethed.scanNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0);

    receiverShieldAddressInfo3 = HttpMethed.generateShieldAddress(httpnode);
    receiverShieldAddress3 = receiverShieldAddressInfo3.get().getAddress();

    response = HttpMethed.getAccount(httpnode, foundationZenTokenAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    assetIssueId = responseContent.getString("asset_issued_ID");

    final Long beforeAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long beforeNetUsed = responseContent.getLong("freeNetUsed");

    shieldOutList.clear();
    Long sendToPublicAddressAmount = 1 * zenTokenFee;
    Long sendToShiledAddressAmount =
        sendTokenAmount - sendToPublicAddressAmount - zenTokenWhenCreateNewAddress;
    memo3 = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress3,
        "" + sendToShiledAddressAmount, memo3);

    PublicMethed.printAddress(receiverPublicKey);
    response = HttpMethed
        .sendShieldCoin(httpnode, null, 0, sendShieldAddressInfo.get(), sendNote, shieldOutList,
            receiverPublicAddress, sendToPublicAddressAmount, null);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    HttpMethed.waitToProduceOneBlock(httpnode);

    final Long afterAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long afterNetUsed = responseContent.getLong("freeNetUsed");

    Assert.assertTrue(afterAssetBalance - beforeAssetBalance == sendToPublicAddressAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);

    receiverNote3 = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo3.get()).get(0);
    Assert.assertTrue(receiverNote3.getValue() == sendToShiledAddressAmount);
    Assert.assertEquals(memo3.getBytes(), receiverNote3.getMemo());

    Assert.assertTrue(HttpMethed.getSpendResult(httpnode, sendShieldAddressInfo.get(), sendNote));

    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    Assert.assertTrue(HttpMethed
        .getSpendResultFromSolidity(httpnode, httpSolidityNode, sendShieldAddressInfo.get(),
            sendNote));
    Assert.assertFalse(HttpMethed
        .getSpendResultFromSolidity(httpnode, httpSolidityNode, receiverShieldAddressInfo3.get(),
            receiverNote3));

    Assert.assertTrue(
        HttpMethed.scanAndMarkNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0).getIsSpend());
    Assert.assertFalse(
        HttpMethed.scanAndMarkNoteByIvk(httpnode, receiverShieldAddressInfo3.get()).get(0)
            .getIsSpend());
  }

  @Test(enabled = false, description = "Shield to one public and two shield transaction by http")
  public void test03ShieldToOnePublicAndTwoShieldTransaction() {
    sendShieldAddressInfo = HttpMethed.generateShieldAddress(httpnode);
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddress:" + sendShieldAddress);
    String memo = "Shield memo in " + System.currentTimeMillis();
    shieldOutList.clear();
    shieldOutList = HttpMethed
        .addShieldOutputList(httpnode, shieldOutList, sendShieldAddress, "" + sendTokenAmount,
            memo);
    response = HttpMethed
        .sendShieldCoin(httpnode, foundationZenTokenAddress, sendTokenAmount + zenTokenFee, null,
            null, shieldOutList, null, 0, foundationZenTokenKey);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    HttpMethed.waitToProduceOneBlock(httpnode);
    sendNote = HttpMethed.scanNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0);

    receiverShieldAddressInfo4 = HttpMethed.generateShieldAddress(httpnode);
    receiverShieldAddress4 = receiverShieldAddressInfo4.get().getAddress();
    receiverShieldAddressInfo5 = HttpMethed.generateShieldAddress(httpnode);
    receiverShieldAddress5 = receiverShieldAddressInfo5.get().getAddress();

    final Long beforeAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long beforeNetUsed = responseContent.getLong("freeNetUsed");

    shieldOutList.clear();
    Long sendToPublicAddressAmount = 1 * zenTokenFee;
    Long sendToShiledAddress1Amount = 2 * zenTokenFee;
    Long sendToShiledAddress2Amount =
        sendTokenAmount - sendToPublicAddressAmount - sendToShiledAddress1Amount - zenTokenFee;
    memo4 = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    memo5 = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress4,
        "" + sendToShiledAddress1Amount, memo4);
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress5,
        "" + sendToShiledAddress2Amount, memo5);

    PublicMethed.printAddress(receiverPublicKey);
    response = HttpMethed
        .sendShieldCoin(httpnode, null, 0, sendShieldAddressInfo.get(), sendNote, shieldOutList,
            receiverPublicAddress, sendToPublicAddressAmount, null);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    HttpMethed.waitToProduceOneBlock(httpnode);

    final Long afterAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long afterNetUsed = responseContent.getLong("freeNetUsed");

    Assert.assertTrue(afterAssetBalance - beforeAssetBalance == sendToPublicAddressAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);

    receiverNote4 = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo4.get()).get(0);
    Assert.assertTrue(receiverNote4.getValue() == sendToShiledAddress1Amount);
    Assert.assertEquals(memo4.getBytes(), receiverNote4.getMemo());

    receiverNote5 = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo5.get()).get(0);
    Assert.assertTrue(receiverNote5.getValue() == sendToShiledAddress2Amount);
    Assert.assertEquals(memo5.getBytes(), receiverNote5.getMemo());

    Assert.assertTrue(HttpMethed.getSpendResult(httpnode, sendShieldAddressInfo.get(), sendNote));
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    final Long assetBalance = HttpMethed
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    HttpMethed
        .transferAsset(httpnode, receiverPublicAddress, foundationZenTokenAddress, assetIssueId,
            assetBalance, receiverPublicKey);
  }
}