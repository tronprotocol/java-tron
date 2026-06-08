package org.tron.core.services.http;

import static org.tron.common.utils.Commons.decodeFromBase58Check;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.service.MortgageService;
import org.tron.core.store.DelegationStore;
import org.tron.json.JSONObject;

@Slf4j
public class GetRewardServletTest extends BaseTest {

  @Resource
  private Manager manager;

  @Resource
  private MortgageService mortgageService;

  @Resource
  private DelegationStore delegationStore;

  @Resource
  GetRewardServlet getRewardServlet;

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, TestConstants.TEST_CONF
    );
  }

  public MockHttpServletRequest createRequest(String contentType) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setContentType(contentType);
    request.setCharacterEncoding("UTF-8");
    return request;
  }

  @Before
  public void init() {
    manager.getDynamicPropertiesStore().saveChangeDelegation(1);
    byte[] sr = decodeFromBase58Check("TNboetpFgv9SqMoHvaVt626NLXETnbdW1K");
    delegationStore.setBrokerage(0, sr, 10);
    delegationStore.setWitnessVote(0, sr, 100000000);
  }

  @Test
  public void getRewardValueByJsonTest() {
    int expect = 138181;
    String jsonParam = "{\"address\": \"TNboetpFgv9SqMoHvaVt626NLXETnbdW1K\"}";
    MockHttpServletRequest request = createRequest("application/json");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setContent(jsonParam.getBytes());
    try {
      getRewardServlet.doPost(request, response);
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      int reward = (int)result.get("reward");
      Assert.assertEquals(expect, reward);
    } catch (UnsupportedEncodingException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void getRewardByJsonUTF8Test() {
    int expect = 138181;
    String jsonParam = "{\"address\": \"TNboetpFgv9SqMoHvaVt626NLXETnbdW1K\"}";
    MockHttpServletRequest request = createRequest("application/json; charset=utf-8");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setContent(jsonParam.getBytes());
    try {
      getRewardServlet.doPost(request, response);
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      int reward = (int)result.get("reward");
      Assert.assertEquals(expect, reward);
    } catch (UnsupportedEncodingException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void getRewardValueTest() {
    int expect = 138181;
    MockHttpServletRequest request = createRequest("application/x-www-form-urlencoded");
    MockHttpServletResponse response = new MockHttpServletResponse();
    mortgageService.payStandbyWitness();
    request.addParameter("address", "TNboetpFgv9SqMoHvaVt626NLXETnbdW1K");
    getRewardServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      int reward = (int)result.get("reward");
      Assert.assertEquals(expect, reward);
    } catch (UnsupportedEncodingException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void getByBlankParamTest() {
    MockHttpServletRequest request = createRequest("application/x-www-form-urlencoded");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addParameter("address", "");
    GetRewardServlet getRewardServlet = new GetRewardServlet();
    getRewardServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      JSONObject result = JSONObject.parseObject(contentAsString);
      int reward = (int)result.get("reward");
      Assert.assertEquals(0, reward);
      String content = (String) result.get("Error");
      Assert.assertNull(content);
    } catch (UnsupportedEncodingException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void getRewardByOversizedValidCharAddressTest() {
    // 41-char, all-valid-Base58 address: the length guard returns null -> reward 0.
    MockHttpServletRequest request = createRequest("application/x-www-form-urlencoded");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addParameter("address", "T" + new String(new char[40]).replace('\0', 'a'));
    new GetRewardServlet().doPost(request, response);
    try {
      JSONObject result = JSONObject.parseObject(response.getContentAsString());
      Assert.assertEquals(0, (int) result.get("reward"));
      Assert.assertNull(result.get("Error"));
    } catch (UnsupportedEncodingException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void getRewardByOversizedIllegalCharAddressTest() {
    MockHttpServletRequest request = createRequest("application/x-www-form-urlencoded");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addParameter("address", "T" + new String(new char[40]).replace('\0', '0'));
    new GetRewardServlet().doPost(request, response);
    try {
      JSONObject result = JSONObject.parseObject(response.getContentAsString());
      Assert.assertEquals(0, (int) result.get("reward"));
      Assert.assertNull(result.get("Error"));
    } catch (UnsupportedEncodingException e) {
      Assert.fail(e.getMessage());
    }
  }

}
