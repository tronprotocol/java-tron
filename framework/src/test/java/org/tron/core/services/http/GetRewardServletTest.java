package org.tron.core.services.http;

import static org.tron.common.utils.Commons.decodeFromBase58Check;

import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.service.MortgageService;
import org.tron.core.store.DelegationStore;
import org.tron.json.JSONObject;

@Slf4j
public class GetRewardServletTest extends AddressQueryServletTestBase {

  @Resource
  private Manager manager;

  @Resource
  private MortgageService mortgageService;

  @Resource
  private DelegationStore delegationStore;

  @Resource
  private GetRewardServlet getRewardServlet;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
  }

  @Override
  protected void invokeGet(MockHttpServletRequest request, MockHttpServletResponse response) {
    getRewardServlet.doGet(request, response);
  }

  @Override
  protected void invokePost(MockHttpServletRequest request, MockHttpServletResponse response) {
    getRewardServlet.doPost(request, response);
  }

  @Override
  protected String valueKey() {
    return "reward";
  }

  @Override
  protected int valueWithoutState() {
    return 0;
  }

  @Before
  public void init() {
    manager.getDynamicPropertiesStore().saveChangeDelegation(1);
    byte[] sr = decodeFromBase58Check("TNboetpFgv9SqMoHvaVt626NLXETnbdW1K");
    delegationStore.setBrokerage(0, sr, 10);
    delegationStore.setWitnessVote(0, sr, 100000000);
  }

  @Test
  public void getRewardValueByJsonTest() throws Exception {
    MockHttpServletRequest request = jsonRequest("TNboetpFgv9SqMoHvaVt626NLXETnbdW1K");

    JSONObject result = JSONObject.parseObject(invoke(request));
    Assert.assertEquals(138181, (int) result.get("reward"));
  }

  @Test
  public void getRewardValueTest() throws Exception {
    mortgageService.payStandbyWitness();
    MockHttpServletRequest request = formRequest("TNboetpFgv9SqMoHvaVt626NLXETnbdW1K");

    JSONObject result = JSONObject.parseObject(invoke(request));
    Assert.assertEquals(138181, (int) result.get("reward"));
  }
}
