package org.tron.core.services.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.entity.Dec;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.JsonUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.interfaceOnPBFT.http.PBFT.HttpApiOnPBFTService;
import org.tron.core.services.interfaceOnSolidity.http.solidity.HttpApiOnSolidityService;

public class GetBasePoolServletTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  private TronApplicationContext context;
  private String ip = "127.0.0.1";
  private int fullHttpPort;
  private Application appTest;
  private CloseableHttpClient httpClient = HttpClients.createDefault();

  private String dbPath = "output_base_pool_test";

  private ChainBaseManager chainBaseManager;

  /**
   * init dependencies.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"-d", dbPath}, "config-localtest.conf");
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

    chainBaseManager = context.getBean(ChainBaseManager.class);
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
  public void testHttp() {
    chainBaseManager.getStableMarketStore().setBasePool(Dec.newDec(100));
    fullHttpPort = Args.getInstance().getFullNodeHttpPort();
    String urlPath = "/wallet/getbasepool";
    String url = String.format("http://%s:%d%s", ip, fullHttpPort, urlPath);
    String response = sendGetRequest(url);
    Dec basePool = chainBaseManager.getStableMarketStore().getBasePool();
    Map<String, Object> result = JsonUtil.json2Obj(response, Map.class);
    Assert.assertTrue(
        basePool.eq(
            Dec.newDec(String.valueOf(result.get("basepool")))));
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
}
