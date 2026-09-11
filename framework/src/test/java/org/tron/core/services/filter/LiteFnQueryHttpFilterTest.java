package org.tron.core.services.filter;

import static org.tron.core.ChainBaseManager.NodeType.FULL;
import static org.tron.core.ChainBaseManager.NodeType.LITE;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.PublicMethod;
import org.tron.core.config.args.Args;

@Slf4j
public class LiteFnQueryHttpFilterTest extends BaseTest {

  private final String ip = "127.0.0.1";
  private int fullHttpPort;
  private final CloseableHttpClient httpClient = HttpClients.createDefault();

  static {
    Args.setParam(new String[]{"-d", dbPath()}, TestConstants.TEST_CONF);
    Args.getInstance().setAllowShieldedTransactionApi(false);
    Args.getInstance().setRpcEnable(false);
    Args.getInstance().setRpcSolidityEnable(false);
    Args.getInstance().setRpcPBFTEnable(false);
    Args.getInstance().setFullNodeHttpEnable(true);
    Args.getInstance().setFullNodeHttpPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setPBFTHttpEnable(true);
    Args.getInstance().setPBFTHttpPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setSolidityNodeHttpEnable(true);
    Args.getInstance().setSolidityHttpPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setJsonRpcHttpFullNodeEnable(false);
    Args.getInstance().setJsonRpcHttpSolidityNodeEnable(false);
    Args.getInstance().setJsonRpcHttpPBFTNodeEnable(false);
    Args.getInstance().setP2pDisable(true);
  }

  /**
   * init dependencies.
   */
  @Before
  public void init() {
    appT.startup();
  }

  @Test
  public void testHttpFilter() throws IOException {
    Set<String> urlPathSets = LiteFnQueryHttpFilter.getFilterPaths();
    for (String urlPath : urlPathSets) {
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
      Assert.assertEquals("this API is closed because this node is a lite fullnode", response);

      // test lite fullnode with history query opened
      chainBaseManager.setNodeType(LITE);
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
    }

  }

  @After
  public void closeHttpClient() throws IOException {
    httpClient.close();
  }

  private String sendGetRequest(String url) throws IOException {
    HttpGet request = new HttpGet(url);
    request.setHeader("User-Agent", "Java client");
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    }
  }
}
