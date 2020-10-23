package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.List;
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

@Slf4j
public class HttpTestZenToken006 {

  List<Note> shieldOutList = new ArrayList<>();
  String assetIssueId;
  String sk;
  String d1;
  String ask;
  String nsk;
  String ovk;
  String ak;
  String nk;
  String ivk;
  String pkD1;
  String paymentAddress1;
  String rcm;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
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
  @BeforeClass(enabled = true)
  public void beforeClass() {
    Args.setFullNodeAllowShieldedTransaction(true);
    PublicMethed.printAddress(foundationZenTokenKey);
    PublicMethed.printAddress(zenTokenOwnerKey);
  }

  @Test(enabled = false, description = "Get new shielded address by http")
  public void test01GetNewShieldedAddress() {
    response = HttpMethed.getNewShieldedAddress(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.size() == 10);
    Assert.assertTrue(responseContent.containsKey("d"));
    Assert.assertTrue(responseContent.containsKey("ovk"));
    Assert.assertTrue(responseContent.containsKey("nsk"));
    Assert.assertTrue(responseContent.containsKey("payment_address"));
    Assert.assertTrue(responseContent.containsKey("sk"));
    Assert.assertTrue(responseContent.containsKey("ask"));
    Assert.assertTrue(responseContent.containsKey("pkD"));
    Assert.assertTrue(responseContent.containsKey("ak"));
    Assert.assertTrue(responseContent.containsKey("nk"));
    Assert.assertTrue(responseContent.containsKey("ivk"));

    sk = responseContent.getString("sk");
    d1 = responseContent.getString("d");
    ask = responseContent.getString("ask");
    nsk = responseContent.getString("nsk");
    ovk = responseContent.getString("ovk");
    ak = responseContent.getString("ak");
    nk = responseContent.getString("nk");
    ivk = responseContent.getString("ivk");
    pkD1 = responseContent.getString("pkD");
    paymentAddress1 = responseContent.getString("payment_address");
  }

  @Test(enabled = false, description = "Get expanded spending key by http")
  public void test02GetExpandedSpendingKey() {
    response = HttpMethed.getExpandedSpendingKey(httpnode, sk);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    String askFromSk = responseContent.getString("ask");
    String nskFromSk = responseContent.getString("nsk");
    String ovkFromSk = responseContent.getString("ovk");
    Assert.assertEquals(ask, askFromSk);
    Assert.assertEquals(nsk, nskFromSk);
    Assert.assertEquals(ovk, ovkFromSk);

  }


  @Test(enabled = false, description = "Get rcm by http")
  public void test03GetRcm() {
    response = HttpMethed.getRcm(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    rcm = responseContent.getString("value");
    logger.info("rcm: " + rcm);
  }

  @Test(enabled = false, description = "Public to shield transaction withoutask by http")
  public void test04PublicToShieldTransactionWithoutAsk() {
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
    Long sendSheldAddressAmount1 = zenTokenFee * 2;
    Long sendAmount = sendSheldAddressAmount1 + zenTokenFee;
    shieldOutList.clear();
    shieldOutList = HttpMethed
        .addShieldOutputList(httpnode, shieldOutList, paymentAddress1, "" + sendSheldAddressAmount1,
            memo1);
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethed
        .sendShieldCoinWithoutAsk(httpnode, httpSolidityNode, httpnode, zenTokenOwnerAddress,
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