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
import org.tron.core.zen.address.DiversifierT;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.ShieldNoteInfo;

@Slf4j
public class HttpTestZenToken001 {

  List<Note> shieldOutList = new ArrayList<>();
  Optional<ShieldAddressInfo> shieldAddressOptionalInfo1;
  Optional<ShieldAddressInfo> shieldAddressOptionalInfo2;
  Optional<ShieldAddressInfo> shieldAddressOptionalInfo3;
  ShieldAddressInfo shieldAddressInfo1 = new ShieldAddressInfo();
  ShieldAddressInfo shieldAddressInfo2 = new ShieldAddressInfo();
  ShieldAddressInfo shieldAddressInfo3 = new ShieldAddressInfo();
  String assetIssueId;
  ShieldNoteInfo shieldNote1;
  ShieldNoteInfo shieldNote2;
  ShieldNoteInfo shieldNote3;
  String memo;
  String sk;
  String d1;
  String d2;
  String d3;
  String ask;
  String nsk;
  String ovk;
  String ak;
  String nk;
  String ivk;
  String pkD1;
  String pkD2;
  String pkD3;
  String paymentAddress1;
  String paymentAddress2;
  String paymentAddress3;
  String rcm;
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
  private String tokenId = zenTokenId;
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long costTokenAmount = 20 * zenTokenFee;
  private Long sendTokenAmount = 8 * zenTokenFee;
  private JSONObject responseContent;
  private HttpResponse response;

  /**
   * constructor.
   */
  @BeforeClass(enabled = false)
  public void beforeClass() {
    Args.setFullNodeAllowShieldedTransaction(true);
    PublicMethed.printAddress(foundationZenTokenKey);
    PublicMethed.printAddress(zenTokenOwnerKey);
  }

  @Test(enabled = false, description = "Get spending key by http")
  public void test01GetSpendingKey() {
    response = HttpMethed.getSpendingKey(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    sk = responseContent.getString("value");
    logger.info("sk: " + sk);

  }

  @Test(enabled = false, description = "Get diversifier by http")
  public void test02GetDiversifier() {
    response = HttpMethed.getDiversifier(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    d1 = responseContent.getString("d");
    logger.info("d1: " + d1);

    response = HttpMethed.getDiversifier(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    d2 = responseContent.getString("d");
    logger.info("d2: " + d2);

    response = HttpMethed.getDiversifier(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    d3 = responseContent.getString("d");
    logger.info("d3: " + d3);
  }

  @Test(enabled = false, description = "Get expanded spending key by http")
  public void test03GetExpandedSpendingKey() {
    response = HttpMethed.getExpandedSpendingKey(httpnode, sk);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    ask = responseContent.getString("ask");
    nsk = responseContent.getString("nsk");
    ovk = responseContent.getString("ovk");
    logger.info("ask: " + ask);
    logger.info("nsk: " + nsk);
    logger.info("ovk: " + ovk);
  }

  @Test(enabled = false, description = "Get AK from ASK by http")
  public void test04GetAkFromAsk() {
    response = HttpMethed.getAkFromAsk(httpnode, ask);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    ak = responseContent.getString("value");
    logger.info("ak: " + ak);
  }

  @Test(enabled = false, description = "Get Nk from Nsk by http")
  public void test05GetNkFromNsk() {
    response = HttpMethed.getNkFromNsk(httpnode, nsk);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    nk = responseContent.getString("value");
    logger.info("nk: " + nk);
  }

  @Test(enabled = false, description = "Get incoming viewing Key by http")
  public void test06GetIncomingViewingKey() {
    response = HttpMethed.getIncomingViewingKey(httpnode, ak, nk);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    ivk = responseContent.getString("ivk");
    logger.info("ivk: " + ivk);
  }

  @Test(enabled = false, description = "Get Zen Payment Address by http")
  public void test07GetZenPaymentAddress() {
    response = HttpMethed.getZenPaymentAddress(httpnode, ivk, d1);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    pkD1 = responseContent.getString("pkD");
    paymentAddress1 = responseContent.getString("payment_address");
    System.out.println("pkd1: " + pkD1);
    System.out.println("address1: " + paymentAddress1);
    shieldAddressInfo1.setSk(ByteArray.fromHexString(sk));
    shieldAddressInfo1.setD(new DiversifierT(ByteArray.fromHexString(d1)));
    shieldAddressInfo1.setIvk(ByteArray.fromHexString(ivk));
    shieldAddressInfo1.setOvk(ByteArray.fromHexString(ovk));
    shieldAddressInfo1.setPkD(ByteArray.fromHexString(pkD1));
    shieldAddressOptionalInfo1 = Optional.of(shieldAddressInfo1);

    response = HttpMethed.getZenPaymentAddress(httpnode, ivk, d2);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    pkD2 = responseContent.getString("pkD");
    paymentAddress2 = responseContent.getString("payment_address");
    System.out.println("pkd2: " + pkD2);
    System.out.println("address2: " + paymentAddress2);
    shieldAddressInfo2.setSk(ByteArray.fromHexString(sk));
    shieldAddressInfo2.setD(new DiversifierT(ByteArray.fromHexString(d2)));
    shieldAddressInfo2.setIvk(ByteArray.fromHexString(ivk));
    shieldAddressInfo2.setOvk(ByteArray.fromHexString(ovk));
    shieldAddressInfo2.setPkD(ByteArray.fromHexString(pkD2));
    shieldAddressOptionalInfo2 = Optional.of(shieldAddressInfo2);

    response = HttpMethed.getZenPaymentAddress(httpnode, ivk, d3);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    pkD3 = responseContent.getString("pkD");
    paymentAddress3 = responseContent.getString("payment_address");
    System.out.println("pkd3: " + pkD3);
    System.out.println("address3: " + paymentAddress3);
    shieldAddressInfo3.setSk(ByteArray.fromHexString(sk));
    shieldAddressInfo3.setD(new DiversifierT(ByteArray.fromHexString(d3)));
    shieldAddressInfo3.setIvk(ByteArray.fromHexString(ivk));
    shieldAddressInfo3.setOvk(ByteArray.fromHexString(ovk));
    shieldAddressInfo3.setPkD(ByteArray.fromHexString(pkD3));
    shieldAddressOptionalInfo3 = Optional.of(shieldAddressInfo3);
  }

  @Test(enabled = false, description = "Get rcm by http")
  public void test08GetRcm() {
    response = HttpMethed.getRcm(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    rcm = responseContent.getString("value");
    logger.info("rcm: " + rcm);
  }

  @Test(enabled = false, description = "Public to shield transaction withoutask by http")
  public void test09PublicToShieldTransactionWithoutAsk() {
    response = HttpMethed
        .transferAsset(httpnode, foundationZenTokenAddress, zenTokenOwnerAddress, tokenId,
            costTokenAmount, foundationZenTokenKey);
    org.junit.Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, foundationZenTokenAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    assetIssueId = responseContent.getString("asset_issued_ID");

    final Long beforeAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    response = HttpMethed.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long beforeNetUsed = responseContent.getLong("freeNetUsed");

    String memo1 = "Shield memo11 in " + System.currentTimeMillis();
    String memo2 = "Shield memo22 in " + System.currentTimeMillis();
    Long sendSheldAddressAmount1 = zenTokenFee * 2;
    Long sendSheldAddressAmount2 = zenTokenFee * 3;
    Long sendAmount = sendSheldAddressAmount1 + sendSheldAddressAmount2 + zenTokenFee;
    shieldOutList.clear();
    shieldOutList = HttpMethed
        .addShieldOutputList(httpnode, shieldOutList, shieldAddressOptionalInfo1.get().getAddress(),
            "" + sendSheldAddressAmount1, memo1);
    shieldOutList = HttpMethed
        .addShieldOutputList(httpnode, shieldOutList, shieldAddressOptionalInfo2.get().getAddress(),
            "" + sendSheldAddressAmount2, memo2);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethed
        .sendShieldCoinWithoutAsk(httpnode, httpSolidityNode, httpPbftNode, zenTokenOwnerAddress,
            sendAmount, null, null, shieldOutList, null, 0, zenTokenOwnerKey);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    HttpMethed.waitToProduceOneBlock(httpnode);
    Long afterAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);

    response = HttpMethed.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    Long afterNetUsed = responseContent.getLong("freeNetUsed");

    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == sendAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);

    String memo3 = "Shield memo33 in " + System.currentTimeMillis();
    Long sendSheldAddressAmount3 = costTokenAmount - sendAmount - zenTokenFee;
    shieldOutList.clear();
    shieldOutList = HttpMethed
        .addShieldOutputList(httpnode, shieldOutList, shieldAddressOptionalInfo3.get().getAddress(),
            "" + sendSheldAddressAmount3, memo3);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethed
        .sendShieldCoinWithoutAsk(httpnode, httpSolidityNode, httpPbftNode, zenTokenOwnerAddress,
            sendSheldAddressAmount3 + zenTokenFee, null, null, shieldOutList, null, 0,
            zenTokenOwnerKey);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);

    List<ShieldNoteInfo> shieldNoteInfoByIvkList = HttpMethed
        .scanNoteByIvk(httpnode, shieldAddressOptionalInfo1.get());
    logger.info("size are:" + shieldNoteInfoByIvkList.size());
    Assert.assertTrue(shieldNoteInfoByIvkList.size() == 3);
    List<ShieldNoteInfo> shieldNoteInfoByMarkList = HttpMethed
        .scanAndMarkNoteByIvk(httpnode, shieldAddressOptionalInfo2.get());
    Assert.assertTrue(shieldNoteInfoByMarkList.size() == 3);

    shieldNote1 = shieldNoteInfoByIvkList.get(0);
    shieldNote2 = shieldNoteInfoByIvkList.get(1);
    shieldNote3 = shieldNoteInfoByIvkList.get(2);
    Assert.assertTrue(shieldNote1.getValue() == sendSheldAddressAmount1);
    Assert.assertEquals(memo1.getBytes(), shieldNote1.getMemo());
    Assert.assertTrue(shieldNote2.getValue() == sendSheldAddressAmount2);
    Assert.assertEquals(memo2.getBytes(), shieldNote2.getMemo());
    Assert.assertTrue(shieldNote3.getValue() == sendSheldAddressAmount3);
    Assert.assertEquals(memo3.getBytes(), shieldNote3.getMemo());
    Assert.assertFalse(shieldNoteInfoByMarkList.get(0).getIsSpend());
    Assert.assertFalse(shieldNoteInfoByMarkList.get(1).getIsSpend());
    Assert.assertFalse(shieldNoteInfoByMarkList.get(2).getIsSpend());
  }

  @Test(enabled = false, description = "Shield to shield transaction withoutask by http")
  public void test10ShieldToShieldTransactionWithoutAsk() {
    Optional<ShieldAddressInfo> receiverShieldAddressInfo1 = HttpMethed
        .generateShieldAddress(httpnode);
    String receiverShieldAddress1 = receiverShieldAddressInfo1.get().getAddress();
    logger.info("receiverShieldAddress1:" + receiverShieldAddress1);
    Optional<ShieldAddressInfo> receiverShieldAddressInfo2 = HttpMethed
        .generateShieldAddress(httpnode);
    String receiverShieldAddress2 = receiverShieldAddressInfo2.get().getAddress();
    logger.info("receiverShieldAddress2:" + receiverShieldAddress2);
    Optional<ShieldAddressInfo> receiverShieldAddressInfo3 = HttpMethed
        .generateShieldAddress(httpnode);
    String receiverShieldAddress3 = receiverShieldAddressInfo3.get().getAddress();
    logger.info("receiverShieldAddress3:" + receiverShieldAddress3);

    shieldOutList.clear();
    String receiverMemo1 = "Shield memo1 in " + System.currentTimeMillis();
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress1,
        "" + (shieldNote1.getValue() - zenTokenFee), receiverMemo1);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethed
        .sendShieldCoinWithoutAsk(httpnode, httpSolidityNode, httpPbftNode, null, 0,
            shieldAddressOptionalInfo1.get(), shieldNote1, shieldOutList, null, 0, null);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    /*shieldOutList.clear();
    String receiverMemo2 = "Shield memo2 in " + System.currentTimeMillis();
    shieldOutList = HttpMethed
        .addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress2,
            "" + (shieldNote2.getValue() - zenTokenFee), receiverMemo2);
    response = HttpMethed
        .sendShieldCoinWithoutAsk(httpnode, null, 0, shieldAddressOptionalInfo2.get(), shieldNote2,
            shieldOutList,
            null, 0, null);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);*/
    shieldOutList.clear();
    String receiverMemo3 = "Shield memo3 in " + System.currentTimeMillis();
    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, receiverShieldAddress3,
        "" + (shieldNote3.getValue() - zenTokenFee), receiverMemo3);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethed
        .sendShieldCoinWithoutAsk(httpnode, httpSolidityNode, httpPbftNode, null, 0,
            shieldAddressOptionalInfo3.get(), shieldNote3, shieldOutList, null, 0, null);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    HttpMethed.waitToProduceOneBlock(httpnode);
    HttpMethed.waitToProduceOneBlock(httpnode);

    List<ShieldNoteInfo> shieldNoteInfoByOvkList = HttpMethed
        .scanNoteByOvk(httpnode, shieldAddressOptionalInfo3.get());
    Assert.assertTrue(shieldNoteInfoByOvkList.size() == 2);
    List<ShieldNoteInfo> shieldNoteInfoByMarkList = HttpMethed
        .scanAndMarkNoteByIvk(httpnode, shieldAddressOptionalInfo2.get());
    Assert.assertTrue(shieldNoteInfoByMarkList.size() == 3);

    Assert.assertTrue(
        shieldNoteInfoByOvkList.get(0).getValue() == shieldNote1.getValue() - zenTokenFee);
    Assert.assertEquals(receiverMemo1.getBytes(), shieldNoteInfoByOvkList.get(0).getMemo());
    /*Assert.assertTrue(
        shieldNoteInfoByOvkList.get(1).getValue() == shieldNote2.getValue() - zenTokenFee);
    Assert.assertEquals(receiverMemo2.getBytes(), shieldNoteInfoByOvkList.get(1).getMemo());*/
    Assert.assertTrue(
        shieldNoteInfoByOvkList.get(1).getValue() == shieldNote3.getValue() - zenTokenFee);
    Assert.assertEquals(receiverMemo3.getBytes(), shieldNoteInfoByOvkList.get(1).getMemo());
    Assert.assertTrue(shieldNoteInfoByMarkList.get(0).getIsSpend());
    Assert.assertFalse(shieldNoteInfoByMarkList.get(1).getIsSpend());
    Assert.assertTrue(shieldNoteInfoByMarkList.get(2).getIsSpend());
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