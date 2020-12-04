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
public class HttpTestZenToken003 {

  Optional<ShieldAddressInfo> receiverShieldAddressInfo1;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo2;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo3;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo4;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo5;
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
  ShieldNoteInfo receiverNote1;
  ShieldNoteInfo receiverNote2;
  ShieldNoteInfo receiverNote3;
  ShieldNoteInfo receiverNote4;
  ShieldNoteInfo receiverNote5;
  String assetIssueId;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverPublicAddress = ecKey2.getAddress();
  String receiverPublicKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethed.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
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
    PublicMethed.printAddress(zenTokenOwnerKey);
    Args.setFullNodeAllowShieldedTransaction(true);

  }

  @Test(enabled = false, description = "Public to two shield transaction by http")
  public void test01PublicToTwoShieldTransaction() {
    response = HttpMethed
        .transferAsset(httpnode, foundationZenTokenAddress, zenTokenOwnerAddress, zenTokenId,
            sendTokenAmount, foundationZenTokenKey);
    HttpMethed.waitToProduceOneBlock(httpnode);

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
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);

    response = HttpMethed.getAccount(httpnode, foundationZenTokenAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    assetIssueId = responseContent.getString("asset_issued_ID");
    final Long beforeAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);

    response = HttpMethed.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long beforeNetUsed = responseContent.getLong("freeNetUsed");

    response = HttpMethed
        .sendShieldCoin(httpnode, zenTokenOwnerAddress, sendTokenAmount, null, null, shieldOutList,
            null, 0, zenTokenOwnerKey);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    HttpMethed.waitToProduceOneBlock(httpnode);
    Long afterAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    Long afterNetUsed = responseContent.getLong("freeNetUsed");
    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == sendTokenAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);

    receiverNote1 = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo1.get()).get(0);
    receiverNote2 = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo2.get()).get(0);
    Assert.assertTrue(receiverNote1.getValue() == sendToShiledAddress1Amount);
    Assert.assertTrue(receiverNote2.getValue() == sendToShiledAddress2Amount);
    Assert.assertEquals(memo1.getBytes(), receiverNote1.getMemo());
    Assert.assertEquals(memo2.getBytes(), receiverNote2.getMemo());
  }

  @Test(enabled = false, description = "Public to one public and one shield transaction by http")
  public void test02ShieldToOneShieldAndOnePublicTransaction() {
    response = HttpMethed
        .transferAsset(httpnode, foundationZenTokenAddress, zenTokenOwnerAddress, zenTokenId,
            sendTokenAmount, foundationZenTokenKey);
    HttpMethed.waitToProduceOneBlock(httpnode);

    receiverShieldAddressInfo3 = HttpMethed.generateShieldAddress(httpnode);
    receiverShieldAddress3 = receiverShieldAddressInfo3.get().getAddress();

    final Long beforeAssetBalanceSendAddress = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long beforeNetUsedSendAddress = responseContent.getLong("freeNetUsed");
    response = HttpMethed.getAccount(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    Long beforeBalanceSendAddress = responseContent.getLong("balance");

    final Long beforeAssetBalanceReceiverAddress = HttpMethed
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long beforeNetUsedReceiverAddress = responseContent.getLong("freeNetUsed");

    shieldOutList.clear();
    Long sendToPublicAddressAmount = 1 * zenTokenFee;
    Long sendToShiledAddressAmount =
        sendTokenAmount - sendToPublicAddressAmount - zenTokenWhenCreateNewAddress;
    memo3 = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress3,
        "" + sendToShiledAddressAmount, memo3);

    PublicMethed.printAddress(receiverPublicKey);
    response = HttpMethed
        .sendShieldCoin(httpnode, zenTokenOwnerAddress, sendTokenAmount, null, null, shieldOutList,
            receiverPublicAddress, sendToPublicAddressAmount, zenTokenOwnerKey);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    HttpMethed.waitToProduceOneBlock(httpnode);

    Long afterAssetBalanceSendAddress = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    Long afterNetUsedSendAddress = responseContent.getLong("freeNetUsed");
    response = HttpMethed.getAccount(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    Long afterBalanceSendAddress = responseContent.getLong("balance");

    final Long afterAssetBalanceReceiverAddress = HttpMethed
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long afterNetUsedReceiverAddress = responseContent.getLong("freeNetUsed");

    Assert.assertTrue(
        beforeAssetBalanceSendAddress - afterAssetBalanceSendAddress == sendTokenAmount);
    Assert.assertTrue(beforeNetUsedSendAddress == afterNetUsedSendAddress);
    Assert.assertTrue(beforeBalanceSendAddress == afterBalanceSendAddress);

    Assert.assertTrue(afterAssetBalanceReceiverAddress - beforeAssetBalanceReceiverAddress
        == sendToPublicAddressAmount);
    Assert.assertTrue(beforeNetUsedReceiverAddress == afterNetUsedReceiverAddress);

    receiverNote3 = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo3.get()).get(0);

    Assert.assertTrue(receiverNote3.getValue() == sendToShiledAddressAmount);
    Assert.assertEquals(memo3.getBytes(), receiverNote3.getMemo());
  }

  @Test(enabled = false, description = "Public to one public and two shield transaction by http")
  public void test03ShieldToOneShieldAndTwoPublicTransaction() {
    response = HttpMethed
        .transferAsset(httpnode, foundationZenTokenAddress, zenTokenOwnerAddress, zenTokenId,
            sendTokenAmount, foundationZenTokenKey);
    HttpMethed.waitToProduceOneBlock(httpnode);

    receiverShieldAddressInfo4 = HttpMethed.generateShieldAddress(httpnode);
    receiverShieldAddress4 = receiverShieldAddressInfo4.get().getAddress();
    receiverShieldAddressInfo5 = HttpMethed.generateShieldAddress(httpnode);
    receiverShieldAddress5 = receiverShieldAddressInfo5.get().getAddress();

    final Long beforeAssetBalanceSendAddress = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long beforeNetUsedSendAddress = responseContent.getLong("freeNetUsed");
    response = HttpMethed.getAccount(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    Long beforeBalanceSendAddress = responseContent.getLong("balance");

    final Long beforeAssetBalanceReceiverAddress = HttpMethed
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long beforeNetUsedReceiverAddress = responseContent.getLong("freeNetUsed");

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
        .sendShieldCoin(httpnode, zenTokenOwnerAddress, sendTokenAmount, null, null, shieldOutList,
            receiverPublicAddress, sendToPublicAddressAmount, zenTokenOwnerKey);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    HttpMethed.waitToProduceOneBlock(httpnode);

    Long afterAssetBalanceSendAddress = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    Long afterNetUsedSendAddress = responseContent.getLong("freeNetUsed");
    response = HttpMethed.getAccount(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    Long afterBalanceSendAddress = responseContent.getLong("balance");

    final Long afterAssetBalanceReceiverAddress = HttpMethed
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, receiverPublicAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long afterNetUsedReceiverAddress = responseContent.getLong("freeNetUsed");

    Assert.assertTrue(
        beforeAssetBalanceSendAddress - afterAssetBalanceSendAddress == sendTokenAmount);
    Assert.assertTrue(beforeNetUsedSendAddress == afterNetUsedSendAddress);
    Assert.assertTrue(beforeBalanceSendAddress == afterBalanceSendAddress);

    Assert.assertTrue(afterAssetBalanceReceiverAddress - beforeAssetBalanceReceiverAddress
        == sendToPublicAddressAmount);
    Assert.assertTrue(beforeNetUsedReceiverAddress == afterNetUsedReceiverAddress);

    receiverNote4 = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo4.get()).get(0);
    Assert.assertTrue(receiverNote4.getValue() == sendToShiledAddress1Amount);
    Assert.assertEquals(memo4.getBytes(), receiverNote4.getMemo());

    receiverNote5 = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo5.get()).get(0);
    Assert.assertTrue(receiverNote5.getValue() == sendToShiledAddress2Amount);
    Assert.assertEquals(memo5.getBytes(), receiverNote5.getMemo());
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    final Long assetBalance1 = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    HttpMethed
        .transferAsset(httpnode, zenTokenOwnerAddress, foundationZenTokenAddress, assetIssueId,
            assetBalance1, zenTokenOwnerKey);

    final Long assetBalance2 = HttpMethed
        .getAssetIssueValue(httpnode, receiverPublicAddress, assetIssueId);
    HttpMethed
        .transferAsset(httpnode, receiverPublicAddress, foundationZenTokenAddress, assetIssueId,
            assetBalance2, receiverPublicKey);
  }
}