package org.tron.core.services;

import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;
import org.tron.common.utils.JsonUtilTest;
import org.tron.common.utils.PublicMethod;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;


@Slf4j
public class WalletApiTest {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static TronApplicationContext context;
  private static Application appT;


  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[]{ "-d", temporaryFolder.newFolder().toString(),
        "--p2p-disable", "true"}, Constant.TEST_CONF);
    int rpcPort = PublicMethod.chooseRandomPort();
    Args.getInstance().setRpcPort(rpcPort);
    Args.getInstance().setRpcEnable(true);

    // http request for /wallet/xx on FullNode
    Args.getInstance().setFullNodeHttpPort(rpcPort + 1);
    Args.getInstance().setFullNodeHttpEnable(true);

    // http request for /walletsolidity/xx on FullNode
    Args.getInstance().setSolidityHttpPort(rpcPort + 2);
    Args.getInstance().setSolidityNodeHttpEnable(true);

    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    appT.startup();
  }

  @Test
  public void listNodesTest() {
    String fullNode = String.format("%s:%d", "127.0.0.1",
        Args.getInstance().getRpcPort());
    WalletGrpc.WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullNode)
            .usePlaintext()
            .build());
    Assert.assertTrue(walletStub.listNodes(EmptyMessage.getDefaultInstance())
        .getNodesList().isEmpty());
  }

  @Test
  public void getPaginatedNowWitnessListTest() {
    try {
      String url = "http://127.0.0.1:" + Args.getInstance().getFullNodeHttpPort() +
          "/wallet/getpaginatednowwitnesslist";
      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setDoOutput(true);

      // Create JSON payload
      String jsonPayload = "{\"offset\": 0, \"limit\": 1000, \"visible\": true}";
      connection.getOutputStream().write(jsonPayload.getBytes());
      connection.getOutputStream().flush();

      Assert.assertEquals("HTTP response code should be 200", 200, connection.getResponseCode());
    } catch (Exception e) {
      Assert.fail("HTTP request failed: " + e.getMessage());
    }
  }

  @Test
  public void getPaginatedNowWitnessListSolidityTest() {
    try {
      String url = "http://127.0.0.1:" + Args.getInstance().getSolidityHttpPort() +
          "/walletsolidity/getpaginatednowwitnesslist";
      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setDoOutput(true);

      // Create JSON payload
      String jsonPayload = "{\"offset\": 0, \"limit\": 1000, \"visible\": true}";
      connection.getOutputStream().write(jsonPayload.getBytes());
      connection.getOutputStream().flush();

      Assert.assertEquals("HTTP response code should be 200", 200, connection.getResponseCode());
    } catch (Exception e) {
      Assert.fail("HTTP solidity request failed: " + e.getMessage());
    }
  }


  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
  }

}
