package org.tron.core.services.filter;

import static org.tron.core.ChainBaseManager.NodeType.FULL;
import static org.tron.core.ChainBaseManager.NodeType.LITE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.application.Application;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.services.interfaceOnPBFT.http.PBFT.HttpApiOnPBFTService;
import org.tron.core.services.interfaceOnSolidity.http.solidity.HttpApiOnSolidityService;

@Slf4j
public class LiteFnQueryHttpFilterTest extends BaseTest {

  private final String ip = "127.0.0.1";
  private int fullHttpPort;
  @Resource
  private Application appTest;
  @Resource
  private FullNodeHttpApiService httpApiService;
  @Resource
  private HttpApiOnSolidityService httpApiOnSolidityService;
  @Resource
  private HttpApiOnPBFTService httpApiOnPBFTService;
  private final CloseableHttpClient httpClient = HttpClients.createDefault();

  static {
    dbPath = "output_http_filter_test";
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
      chainBaseManager.setNodeType(LITE);
      Args.getInstance().setOpenHistoryQueryWhenLiteFN(false);
      String response = sendGetRequest(url);
      logger.info("response:{}", response);

      // test lite fullnode with history query opened
      chainBaseManager.setNodeType(FULL);
      Args.getInstance().setOpenHistoryQueryWhenLiteFN(true);
      response = sendGetRequest(url);
      Assert.assertNotEquals("this API is closed because this node is a lite fullnode",
              response);

      // test normal fullnode
      chainBaseManager.setNodeType(FULL);
      Args.getInstance().setOpenHistoryQueryWhenLiteFN(true);
      response = sendGetRequest(url);
      Assert.assertNotEquals("this API is closed because this node is a lite fullnode",
              response);
    });

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

  private String sendPostRequest(String url, String body) throws IOException {
    HttpPost request = new HttpPost(url);
    request.setHeader("User-Agent", "Java client");
    StringEntity entity = new StringEntity(body);
    request.setEntity(entity);
    HttpResponse response = httpClient.execute(request);
    BufferedReader rd = new BufferedReader(
            new InputStreamReader(response.getEntity().getContent()));
    StringBuilder result = new StringBuilder();
    String line;
    while ((line = rd.readLine()) != null) {
      result.append(line);
    }
    return result.toString();
  }
}
