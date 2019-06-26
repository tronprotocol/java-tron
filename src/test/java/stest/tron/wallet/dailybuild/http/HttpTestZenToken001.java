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

  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(0);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethed.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private String tokenId = zenTokenId;
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");

  List<Note> shieldOutList = new ArrayList<>();
  ShieldAddressInfo addressInfo = new ShieldAddressInfo();
  Optional<ShieldAddressInfo> receiverAddressInfo;
  String assetIssueId;
  ShieldNoteInfo receiverNote;
  String memo;
  String sk;
  String d;
  String ask;
  String nsk;
  String ovk;
  String ak;
  String nk;
  String ivk;
  String pkD;
  String paymentAddress;
  String rcm;

  private Long costTokenAmount = 10 * zenTokenFee;
  private Long sendTokenAmount = 8 * zenTokenFee;
  private JSONObject responseContent;
  private HttpResponse response;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(foundationZenTokenKey);
    PublicMethed.printAddress(zenTokenOwnerKey);

    response = HttpMethed
        .transferAsset(httpnode, foundationZenTokenAddress, zenTokenOwnerAddress, tokenId,
            costTokenAmount, foundationZenTokenKey);
    org.junit.Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
  }

  @Test(enabled = true, description = "Get spending key by http")
  public void test01GetSpendingKey() {
    response = HttpMethed.getSpendingKey(httpnode);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    sk = responseContent.getString("value");
    logger.info("sk: " + sk);

  }

  @Test(enabled = true, description = "Get diversifier by http")
  public void test02GetDiversifier() {
    response = HttpMethed.getDiversifier(httpnode);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    d = responseContent.getString("d");
    logger.info("d: " + d);

  }

  @Test(enabled = true, description = "Get expanded spending key by http")
  public void test03GetExpandedSpendingKey() {
    response = HttpMethed.getExpandedSpendingKey(httpnode, sk);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    ask = responseContent.getString("ask");
    nsk = responseContent.getString("nsk");
    ovk = responseContent.getString("ovk");
    logger.info("ask: " + ask);
    logger.info("nsk: " + nsk);
    logger.info("ovk: " + ovk);
  }

  @Test(enabled = true, description = "Get AK from ASK by http")
  public void test04GetAkFromAsk() {
    response = HttpMethed.getAkFromAsk(httpnode, ask);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    ak = responseContent.getString("value");
    logger.info("ak: " + ak);
  }

  @Test(enabled = true, description = "Get Nk from Nsk by http")
  public void test05GetNkFromNsk() {
    response = HttpMethed.getNkFromNsk(httpnode, nsk);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    nk = responseContent.getString("value");
    logger.info("nk: " + nk);
  }

  @Test(enabled = true, description = "Get incoming viewing Key by http")
  public void test06GetIncomingViewingKey() {
    response = HttpMethed.getIncomingViewingKey(httpnode, ak, nk);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    ivk = responseContent.getString("ivk");
    logger.info("ivk: " + ivk);
  }

  @Test(enabled = true, description = "Get Zen Payment Address by http")
  public void test07GetZenPaymentAddress() {
    response = HttpMethed.getZenPaymentAddress(httpnode, ivk, d);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    pkD = responseContent.getString("pkD");
    paymentAddress = responseContent.getString("payment_address");
    System.out.println("pkd: " + responseContent.getString("pkD"));
    System.out.println("address: " + responseContent.getString("payment_address"));
    /*addressInfo.setSk(sk.getBytes());
    addressInfo.setD(new DiversifierT(d.getBytes()));
    addressInfo.setIvk(ivk.getBytes());
    addressInfo.setOvk(ovk.getBytes());
    addressInfo.setPkD(pkD.getBytes());*/

    addressInfo.setSk(ByteArray.fromHexString(sk));
    addressInfo.setD(new DiversifierT(ByteArray.fromHexString(d)));
    addressInfo.setIvk(ByteArray.fromHexString(ivk));
    addressInfo.setOvk(ByteArray.fromHexString(ovk));
    addressInfo.setPkD(ByteArray.fromHexString(pkD));
    receiverAddressInfo = Optional.of(addressInfo);
  }

  @Test(enabled = true, description = "Get rcm by http")
  public void test08GetRcm() {
    response = HttpMethed.getRcm(httpnode);
    org.junit.Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    rcm = responseContent.getString("value");
    logger.info("rcm: " + rcm);
  }

  @Test(enabled = true, description = "Public to shield transaction by http")
  public void test09PublicToShieldTransaction() {
    Args.getInstance().setFullNodeAllowShieldedTransaction(true);
    response = HttpMethed.getAccount(httpnode, foundationZenTokenAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    assetIssueId = responseContent.getString("asset_issued_ID");
    final Long beforeAssetBalance = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);

    response = HttpMethed.getAccountReource(httpnode, zenTokenOwnerAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    final Long beforeNetUsed = responseContent.getLong("freeNetUsed");

    memo = "aaaaaaa";

    shieldOutList = HttpMethed.addShieldOutputList(httpnode, shieldOutList, paymentAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);

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

    receiverNote = HttpMethed.scanNoteByIvk(httpnode, receiverAddressInfo.get());
    Assert.assertTrue(receiverNote.getValue() == sendTokenAmount - zenTokenFee);
    Assert.assertEquals(memo.getBytes(), receiverNote.getMemo());
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    response = HttpMethed.getAccount(httpnode, foundationZenTokenAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    assetIssueId = responseContent.getString("asset_issued_ID");
    final Long assetBalance = HttpMethed
        .getAssetIssueValue(httpnode, zenTokenOwnerAddress, assetIssueId);
    HttpMethed
        .transferAsset(httpnode, zenTokenOwnerAddress, foundationZenTokenAddress, assetIssueId,
            assetBalance, zenTokenOwnerKey);
  }
}