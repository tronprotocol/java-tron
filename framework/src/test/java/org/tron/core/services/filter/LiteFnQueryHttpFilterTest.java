package org.tron.core.services.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

public class LiteFnQueryHttpFilterTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  private TronApplicationContext context;
  private String ip = "127.0.0.1";
  private int fullHttpPort;
  private Application appTest;
  private CloseableHttpClient httpClient = HttpClients.createDefault();

  private String dbPath = "output_grpc_filter_test";

  /**
   * init dependencies.
   */
  @Before
  public void init() {
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
  @After
  public void destroy() {
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
    Set<String> urlPathSets = LiteFnQueryHttpFilter.getFilterPaths();
    urlPathSets.forEach(urlPath -> {
      if (urlPath.contains("/walletsolidity")) {
        fullHttpPort = Args.getInstance().getSolidityHttpPort();
      } else if (urlPath.contains("/walletpbft")) {
        fullHttpPort = Args.getInstance().getPBFTHttpPort();
      } else {
        fullHttpPort = Args.getInstance().getFullNodeHttpPort();
      }
      String url = String.format("http://%s:%d%s", ip, fullHttpPort, urlPath);
      // test lite fullnode with history query closed
      Args.getInstance().setLiteFullNode(true);
      Args.getInstance().setOpenHistoryQueryWhenLiteFN(false);
      String response = sendGetRequest(url);
      Assert.assertEquals("this API is closed because this node is a lite fullnode",
              response);

      // test lite fullnode with history query opened
      Args.getInstance().setLiteFullNode(false);
      Args.getInstance().setOpenHistoryQueryWhenLiteFN(true);
      response = sendGetRequest(url);
      Assert.assertNotEquals("this API is closed because this node is a lite fullnode",
              response);

      // test normal fullnode
      Args.getInstance().setLiteFullNode(false);
      Args.getInstance().setOpenHistoryQueryWhenLiteFN(true);
      response = sendGetRequest(url);
      Assert.assertNotEquals("this API is closed because this node is a lite fullnode",
              response);
    });

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

  private String sendPostRequest(String url, String body) throws IOException {
    HttpPost request = new HttpPost(url);
    request.setHeader("User-Agent", "Java client");
    StringEntity entity = new StringEntity(body);
    request.setEntity(entity);
    HttpResponse response = httpClient.execute(request);
    BufferedReader rd = new BufferedReader(
            new InputStreamReader(response.getEntity().getContent()));
    StringBuffer result = new StringBuffer();
    String line;
    while ((line = rd.readLine()) != null) {
      result.append(line);
    }
    return result.toString();
  }
}
