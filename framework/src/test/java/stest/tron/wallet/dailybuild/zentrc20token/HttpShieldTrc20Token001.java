package stest.tron.wallet.dailybuild.zentrc20token;

import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.Note;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.zen.address.DiversifierT;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.ShieldNoteInfo;
import stest.tron.wallet.common.client.utils.ZenTrc20Base;

@Slf4j
public class HttpShieldTrc20Token001 extends ZenTrc20Base {

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
  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(0);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private JSONObject responseContent;
  private HttpResponse response;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    //Args.getInstance().setFullNodeAllowShieldedTransaction(true);
    //PublicMethed.printAddress(foundationZenTokenKey);
    //PublicMethed.printAddress(zenTokenOwnerKey);
  }

  @Test(enabled = true, description = "Get spending key by http")
  public void test01GetSpendingKey() {
    response = HttpMethed.getSpendingKey(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    sk = responseContent.getString("value");
    logger.info("sk: " + sk);

  }

  @Test(enabled = true, description = "Get diversifier by http")
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

  @Test(enabled = true, description = "Get expanded spending key by http")
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

  @Test(enabled = true, description = "Get AK from ASK by http")
  public void test04GetAkFromAsk() {
    response = HttpMethed.getAkFromAsk(httpnode, ask);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    ak = responseContent.getString("value");
    logger.info("ak: " + ak);
  }

  @Test(enabled = true, description = "Get Nk from Nsk by http")
  public void test05GetNkFromNsk() {
    response = HttpMethed.getNkFromNsk(httpnode, nsk);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    nk = responseContent.getString("value");
    logger.info("nk: " + nk);
  }

  @Test(enabled = true, description = "Get incoming viewing Key by http")
  public void test06GetIncomingViewingKey() {
    response = HttpMethed.getIncomingViewingKey(httpnode, ak, nk);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    ivk = responseContent.getString("ivk");
    logger.info("ivk: " + ivk);
  }

  @Test(enabled = true, description = "Get Zen Payment Address by http")
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

  @Test(enabled = true, description = "Get rcm by http")
  public void test08GetRcm() {
    response = HttpMethed.getRcm(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    rcm = responseContent.getString("value");
    logger.info("rcm: " + rcm);
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
  }
}