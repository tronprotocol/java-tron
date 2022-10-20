package org.tron.core.services.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.services.interfaceOnPBFT.http.PBFT.HttpApiOnPBFTService;
import org.tron.core.services.interfaceOnSolidity.http.solidity.HttpApiOnSolidityService;

public class HttpApiAccessFilterTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  private static TronApplicationContext context;
  private static Application appTest;
  private static CloseableHttpClient httpClient = HttpClients.createDefault();
  private static String dbPath = "output_http_api_access_filter_test";

  /**
   * init dependencies.
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    Args.getInstance().setFullNodeAllowShieldedTransactionArgs(false);
    context = new TronApplicationContext(DefaultConfig.class);
    appTest = ApplicationFactory.create(context);

    FullNodeHttpApiService httpApiService = context
            .getBean(FullNodeHttpApiService.class);
    HttpApiOnSolidityService httpApiOnSolidityService = context
            .getBean(HttpApiOnSolidityService.class);
    HttpApiOnPBFTService httpApiOnPBFTService = context
            .getBean(HttpApiOnPBFTService.class);

    appTest.addService(httpApiService);
    appTest.addService(httpApiOnSolidityService);
    appTest.addService(httpApiOnPBFTService);
    appTest.initServices(Args.getInstance());
    appTest.startServices();
    appTest.startup();
  }

  /**
   * destroy the context.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    appTest.shutdownServices();
    appTest.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
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
        int statusCode = getReuqestCode(url);
        Assert.assertEquals(HttpStatus.SC_OK, statusCode);
      }
    }
  }

  private String sendGetRequest(String url) {
    HttpGet request = new HttpGet(url);
    request.setHeader("User-Agent", "Java client");
    HttpResponse response = null;
    try {
      response = httpClient.execute(request);
      BufferedReader rd = new BufferedReader(
              new InputStreamReader(response.getEntity().getContent()));
      StringBuffer result = new StringBuffer();
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

  private int getReuqestCode(String url) {
    HttpGet request = new HttpGet(url);
    request.setHeader("User-Agent", "Java client");
    HttpResponse response = null;

    try {
      response = httpClient.execute(request);
      return response.getStatusLine().getStatusCode();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return 0;
  }
}
