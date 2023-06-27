package org.tron.core.services.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.application.Application;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.services.interfaceOnPBFT.http.PBFT.HttpApiOnPBFTService;
import org.tron.core.services.interfaceOnSolidity.http.solidity.HttpApiOnSolidityService;

public class HttpApiAccessFilterTest extends BaseTest {

  @Resource
  private Application appTest;
  @Resource
  private FullNodeHttpApiService httpApiService;
  @Resource
  private HttpApiOnSolidityService httpApiOnSolidityService;
  @Resource
  private HttpApiOnPBFTService httpApiOnPBFTService;
  @Resource
  private HttpApiAccessFilter httpApiAccessFilter;
  private static final CloseableHttpClient httpClient = HttpClients.createDefault();

  static {
    dbPath = "output_http_api_access_filter_test";
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    Args.getInstance().setFullNodeAllowShieldedTransactionArgs(false);
  }

  /**
   * init dependencies.
   */
  @Before
  public void init() {
    appTest.addService(httpApiService);
    appTest.addService(httpApiOnSolidityService);
    appTest.addService(httpApiOnPBFTService);
    appTest.initServices(Args.getInstance());
    appTest.startServices();
    appTest.startup();
  }

  @Test
  public void testHttpFilter() {
    List<String> disabledApiList = new ArrayList<>();
    disabledApiList.add("getaccount");
    disabledApiList.add("getnowblock");

    List<String> emptyList = Collections.emptyList();

    List<String> patterns = new ArrayList<>();
    patterns.add("/walletsolidity/");
    patterns.add("/walletpbft/");
    patterns.add("/wallet/");

    int httpPort;
    String ip = "127.0.0.1";
    for (String api : disabledApiList) {
      for (String pattern : patterns) {
        String urlPath = pattern + api;
        if (urlPath.contains("/walletsolidity")) {
          httpPort = Args.getInstance().getSolidityHttpPort();
        } else if (urlPath.contains("/walletpbft")) {
          httpPort = Args.getInstance().getPBFTHttpPort();
        } else {
          httpPort = Args.getInstance().getFullNodeHttpPort();
        }

        String url = String.format("http://%s:%d%s", ip, httpPort, urlPath);

        Args.getInstance().setDisabledApiList(disabledApiList);
        String response = sendGetRequest(url);
        Assert.assertEquals("{\"Error\":\"this API is unavailable due to config\"}",
            response);

        Args.getInstance().setDisabledApiList(emptyList);
        int statusCode = getRequestCode(url);
        Assert.assertEquals(HttpStatus.SC_OK, statusCode);
      }
    }
  }

  private String sendGetRequest(String url) {
    HttpGet request = new HttpGet(url);
    request.setHeader("User-Agent", "Java client");
    HttpResponse response;
    try {
      response = httpClient.execute(request);
      BufferedReader rd = new BufferedReader(
              new InputStreamReader(response.getEntity().getContent()));
      StringBuilder result = new StringBuilder();
      String line;
      while ((line = rd.readLine()) != null) {
        result.append(line);
      }
      return result.toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private int getRequestCode(String url) {
    HttpGet request = new HttpGet(url);
    request.setHeader("User-Agent", "Java client");
    HttpResponse response;

    try {
      response = httpClient.execute(request);
      return response.getStatusLine().getStatusCode();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return 0;
  }

  @Test
  public void testIsDisabled() throws Exception {
    List<String> list = new ArrayList<>();
    list.add("getnowblock");
    CommonParameter.getInstance().setDisabledApiList(list);
    Method privateMethod = httpApiAccessFilter.getClass()
            .getDeclaredMethod("isDisabled", String.class);
    privateMethod.setAccessible(true);

    String url = "/wallet/getnowblock";
    boolean f = (boolean) privateMethod.invoke(httpApiAccessFilter,url);
    Assert.assertTrue(f);

    url = "/wallet/a/../b/../getnowblock";
    f = (boolean) privateMethod.invoke(httpApiAccessFilter,url);
    Assert.assertTrue(f);

    url = "/wallet/a/b/../getnowblock";
    f = (boolean) privateMethod.invoke(httpApiAccessFilter,url);
    Assert.assertFalse(f);

    url = "/wallet/getblock";
    f = (boolean) privateMethod.invoke(httpApiAccessFilter,url);
    Assert.assertFalse(f);
  }

}
