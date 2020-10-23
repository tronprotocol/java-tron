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
public class HttpTestZenToken002 {

  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo;
  String sendShieldAddress;
  String receiverShieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  String memo1;
  String memo2;
  ShieldNoteInfo sendNote;
  ShieldNoteInfo receiverNote;
  ShieldNoteInfo noteByOvk;
  ShieldNoteInfo noteByIvk;
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
  @BeforeClass(enabled = false)
  public void beforeClass() {
    PublicMethed.printAddress(foundationZenTokenKey);
    PublicMethed.printAddress(zenTokenOwnerKey);
    response = HttpMethed
        .transferAsset(httpnode, foundationZenTokenAddress, zenTokenOwnerAddress, zenTokenId,
            sendTokenAmount, foundationZenTokenKey);
    org.junit.Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    Args.setFullNodeAllowShieldedTransaction(true);

  }

  @Test(enabled = false, description = "Public to shield transaction by http")
  public void test01PublicToShieldTransaction() {
    response = HttpMethed.getAccount(httpnode, foundationZenTokenAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    assetIssueId = responseContent.getString("asset_issued_ID");

    final Long beforeAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long beforeNetUsed = responseContent.getLong("freeNetUsed");

    sendShieldAddressInfo = HttpMethed.generateShieldAddress(httpnode);
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddress:" + sendShieldAddress);
    memo1 = "Shield memo1 in " + System.currentTimeMillis();
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, sendShieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo1);

    response = HttpMethed
        .sendShieldCoin(httpnode, zenTokenOwnerAddress, sendTokenAmount, null, null, shieldOutList,
            null, 0, zenTokenOwnerKey);
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

    sendNote = HttpMethed.scanNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0);
    Assert.assertTrue(sendNote.getValue() == sendTokenAmount - zenTokenFee);
    Assert.assertEquals(memo1.getBytes(), sendNote.getMemo());

    ShieldNoteInfo scanAndMarkNoteSendNote = HttpMethed
        .scanAndMarkNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0);
    Assert.assertFalse(scanAndMarkNoteSendNote.getIsSpend());
  }

  @Test(enabled = false, description = "Shield to shield transaction by http")
  public void test02ShieldToShieldTransaction() {
    receiverShieldAddressInfo = HttpMethed.generateShieldAddress(httpnode);
    receiverShieldAddress = receiverShieldAddressInfo.get().getAddress();

    shieldOutList.clear();
    memo2 = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress,
        "" + (sendNote.getValue() - zenTokenFee), memo2);

    response = HttpMethed
        .sendShieldCoin(httpnode, null, 0, sendShieldAddressInfo.get(), sendNote, shieldOutList,
            null, 0, null);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    Long afterAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);

    receiverNote = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo.get()).get(0);

    Assert.assertTrue(receiverNote.getValue() == sendNote.getValue() - zenTokenFee);
    Assert.assertEquals(ByteArray.toHexString(memo2.getBytes()),
        ByteArray.toHexString(receiverNote.getMemo()));

    Assert.assertTrue(HttpMethed.getSpendResult(httpnode, sendShieldAddressInfo.get(), sendNote));
    Assert.assertFalse(
        HttpMethed.getSpendResult(httpnode, receiverShieldAddressInfo.get(), receiverNote));
  }

  @Test(enabled = false, description = "Scan note by ivk and scan not by ivk on FullNode by http")
  public void test03ScanNoteByIvkAndOvk() {
    //Scan sender note by ovk equals scan receiver note by ivk on FullNode
    noteByOvk = HttpMethed.scanNoteByOvk(httpnode, sendShieldAddressInfo.get()).get(0);
    noteByIvk = HttpMethed.scanNoteByIvk(httpnode, receiverShieldAddressInfo.get()).get(0);
    Assert.assertEquals(noteByIvk.getValue(), noteByOvk.getValue());
    Assert.assertEquals(noteByIvk.getMemo(), noteByOvk.getMemo());
    Assert.assertEquals(noteByIvk.getR(), noteByOvk.getR());
    Assert.assertEquals(noteByIvk.getPaymentAddress(), noteByOvk.getPaymentAddress());
  }

  @Test(enabled = false, description = "Scan note by ivk and scan not by ivk on Solidity by http")
  public void test04ScanNoteByIvkAndOvkFromSolidity() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    //Scan sender note by ovk equals scan receiver note by ivk on Solidity
    noteByOvk = HttpMethed.scanNoteByOvkFromSolidity(httpSolidityNode, sendShieldAddressInfo.get())
        .get(0);
    noteByIvk = HttpMethed
        .scanNoteByIvkFromSolidity(httpSolidityNode, receiverShieldAddressInfo.get()).get(0);
    Assert.assertEquals(noteByIvk.getValue(), noteByOvk.getValue());
    Assert.assertEquals(noteByIvk.getMemo(), noteByOvk.getMemo());
    Assert.assertEquals(noteByIvk.getR(), noteByOvk.getR());
    Assert.assertEquals(noteByIvk.getPaymentAddress(), noteByOvk.getPaymentAddress());
  }

  @Test(enabled = false, description = "Scan note by ivk and scan not by ivk on PBFT by http")
  public void test05ScanNoteByIvkAndOvkFromPbft() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    //Scan sender note by ovk equals scan receiver note by ivk on Solidity
    noteByOvk = HttpMethed.scanNoteByOvkFromPbft(httpPbftNode, sendShieldAddressInfo.get()).get(0);
    noteByIvk = HttpMethed.scanNoteByIvkFromPbft(httpPbftNode, receiverShieldAddressInfo.get())
        .get(0);
    Assert.assertEquals(noteByIvk.getValue(), noteByOvk.getValue());
    Assert.assertEquals(noteByIvk.getMemo(), noteByOvk.getMemo());
    Assert.assertEquals(noteByIvk.getR(), noteByOvk.getR());
    Assert.assertEquals(noteByIvk.getPaymentAddress(), noteByOvk.getPaymentAddress());
  }


  /**
   * constructor.
   */
  @Test(enabled = false, description = "Query whether note is spend on solidity by http")
  public void test06QueryNoteIsSpendOnSolidity() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    Assert.assertTrue(HttpMethed
        .getSpendResultFromSolidity(httpnode, httpSolidityNode, sendShieldAddressInfo.get(),
            sendNote));
    Assert.assertFalse(HttpMethed
        .getSpendResultFromSolidity(httpnode, httpSolidityNode, receiverShieldAddressInfo.get(),
            receiverNote));
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Query whether note is spend on PBFT by http")
  public void test07QueryNoteIsSpendOnSolidity() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    Assert.assertTrue(HttpMethed
        .getSpendResultFromPbft(httpnode, httpPbftNode, sendShieldAddressInfo.get(), sendNote));
    Assert.assertFalse(HttpMethed
        .getSpendResultFromPbft(httpnode, httpPbftNode, receiverShieldAddressInfo.get(),
            receiverNote));
  }


  /**
   * constructor.
   */
  @Test(enabled = false, description = "Query note and spend status on fullnode")
  public void test08QueryNoteAndSpendStatusOnFullnode() {
    ShieldNoteInfo scanAndMarkNoteSendNote = HttpMethed
        .scanAndMarkNoteByIvk(httpnode, sendShieldAddressInfo.get()).get(0);
    Assert.assertTrue(scanAndMarkNoteSendNote.isSpend);
    Assert.assertEquals(scanAndMarkNoteSendNote.getValue(), sendNote.getValue());
    Assert.assertEquals(scanAndMarkNoteSendNote.getMemo(), sendNote.getMemo());
    Assert.assertEquals(scanAndMarkNoteSendNote.getR(), sendNote.getR());
    Assert.assertEquals(scanAndMarkNoteSendNote.getPaymentAddress(), sendNote.getPaymentAddress());

    ShieldNoteInfo scanAndMarkNoteReceiverNote = HttpMethed
        .scanAndMarkNoteByIvk(httpnode, receiverShieldAddressInfo.get()).get(0);
    Assert.assertFalse(scanAndMarkNoteReceiverNote.getIsSpend());
    Assert.assertEquals(scanAndMarkNoteReceiverNote.getValue(), receiverNote.getValue());
    Assert.assertEquals(scanAndMarkNoteReceiverNote.getMemo(), receiverNote.getMemo());
    Assert.assertEquals(scanAndMarkNoteReceiverNote.getR(), receiverNote.getR());
    Assert.assertEquals(scanAndMarkNoteReceiverNote.getPaymentAddress(),
        receiverNote.getPaymentAddress());
  }

  @Test(enabled = false, description = "Query note and spend status on solidity")
  public void test09QueryNoteAndSpendStatusOnSolidity() {
    ShieldNoteInfo scanAndMarkNoteSendNote = HttpMethed
        .scanAndMarkNoteByIvkFromSolidity(httpnode, httpSolidityNode, sendShieldAddressInfo.get())
        .get(0);
    Assert.assertTrue(scanAndMarkNoteSendNote.isSpend);
    Assert.assertEquals(scanAndMarkNoteSendNote.getValue(), sendNote.getValue());
    Assert.assertEquals(scanAndMarkNoteSendNote.getMemo(), sendNote.getMemo());
    Assert.assertEquals(scanAndMarkNoteSendNote.getR(), sendNote.getR());
    Assert.assertEquals(scanAndMarkNoteSendNote.getPaymentAddress(), sendNote.getPaymentAddress());

    ShieldNoteInfo scanAndMarkNoteReceiverNote = HttpMethed
        .scanAndMarkNoteByIvkFromSolidity(httpnode, httpSolidityNode,
            receiverShieldAddressInfo.get()).get(0);
    Assert.assertFalse(scanAndMarkNoteReceiverNote.getIsSpend());
    Assert.assertEquals(scanAndMarkNoteReceiverNote.getValue(), receiverNote.getValue());
    Assert.assertEquals(scanAndMarkNoteReceiverNote.getMemo(), receiverNote.getMemo());
    Assert.assertEquals(scanAndMarkNoteReceiverNote.getR(), receiverNote.getR());
    Assert.assertEquals(scanAndMarkNoteReceiverNote.getPaymentAddress(),
        receiverNote.getPaymentAddress());

  }

  @Test(enabled = false, description = "Query note and spend status on PBFT")
  public void test10QueryNoteAndSpendStatusOnPbft() {
    ShieldNoteInfo scanAndMarkNoteSendNote = HttpMethed
        .scanAndMarkNoteByIvkFromPbft(httpnode, httpPbftNode, sendShieldAddressInfo.get()).get(0);
    Assert.assertTrue(scanAndMarkNoteSendNote.isSpend);
    Assert.assertEquals(scanAndMarkNoteSendNote.getValue(), sendNote.getValue());
    Assert.assertEquals(scanAndMarkNoteSendNote.getMemo(), sendNote.getMemo());
    Assert.assertEquals(scanAndMarkNoteSendNote.getR(), sendNote.getR());
    Assert.assertEquals(scanAndMarkNoteSendNote.getPaymentAddress(), sendNote.getPaymentAddress());

    ShieldNoteInfo scanAndMarkNoteReceiverNote = HttpMethed
        .scanAndMarkNoteByIvkFromPbft(httpnode, httpPbftNode, receiverShieldAddressInfo.get())
        .get(0);
    Assert.assertFalse(scanAndMarkNoteReceiverNote.getIsSpend());
    Assert.assertEquals(scanAndMarkNoteReceiverNote.getValue(), receiverNote.getValue());
    Assert.assertEquals(scanAndMarkNoteReceiverNote.getMemo(), receiverNote.getMemo());
    Assert.assertEquals(scanAndMarkNoteReceiverNote.getR(), receiverNote.getR());
    Assert.assertEquals(scanAndMarkNoteReceiverNote.getPaymentAddress(),
        receiverNote.getPaymentAddress());

  }


  @Test(enabled = false, description = "Shield to public transaction by http")
  public void test11ShieldToPublicTransaction() {
    final Long beforeAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long beforeNetUsed = responseContent.getLong("freeNetUsed");

    shieldOutList.clear();
    response = HttpMethed
        .sendShieldCoin(httpnode, null, 0, receiverShieldAddressInfo.get(), receiverNote,
            shieldOutList, zenTokenOwnerAddress, receiverNote.getValue() - zenTokenFee, null);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    HttpMethed.waitToProduceOneBlock(httpnode);

    Long afterAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    Long afterNetUsed = responseContent.getLong("freeNetUsed");

    logger.info("beforeAssetBalance:" + beforeAssetBalance);
    logger.info("afterAssetBalance:" + afterAssetBalance);
    Assert.assertTrue(
        afterAssetBalance - beforeAssetBalance == receiverNote.getValue() - zenTokenFee);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);

    Assert.assertTrue(
        HttpMethed.getSpendResult(httpnode, receiverShieldAddressInfo.get(), receiverNote));
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    final Long assetBalance = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    HttpMethed
        .transferAsset(httpnode, zenTokenOwnerAddress, foundationZenTokenAddress, assetIssueId,
            assetBalance, zenTokenOwnerKey);
  }
}