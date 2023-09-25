package org.tron.program;

import java.io.File;
import java.io.IOException;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.client.DatabaseGrpcClient;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.http.solidity.SolidityNodeHttpApiService;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.DynamicProperties;

@Slf4j
public class SolidityNodeTest extends BaseTest {

  @Resource
  RpcApiService rpcApiService;
  @Resource
  SolidityNodeHttpApiService solidityNodeHttpApiService;

  static {
    try {
      Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
    } catch (IOException e) {
      Assert.fail("create temp directory failed.");
    }
    Args.getInstance().setSolidityNode(true);
  }

  /**
   * init db.
   */
  @BeforeClass
  public static void init() {
  }

  /**
   * remo db when after test.
   */
  @AfterClass
  public static void removeDb() {
    Args.clearParam();
  }

  private static Boolean deleteFolder(File index) {
    if (!index.isDirectory() || index.listFiles().length <= 0) {
      return index.delete();
    }
    for (File file : index.listFiles()) {
      if (null != file && !deleteFolder(file)) {
        return false;
      }
    }
    return index.delete();
  }

  @Test
  public void testSolidityArgs() {
    Assert.assertNotNull(Args.getInstance().getTrustNodeAddr());
    Assert.assertTrue(Args.getInstance().isSolidityNode());
  }

  @Test
  public void testSolidityGrpcCall() {
    rpcApiService.init(Args.getInstance());
    rpcApiService.start();
    DatabaseGrpcClient databaseGrpcClient = null;
    String address = Args.getInstance().getTrustNodeAddr();
    try {
      databaseGrpcClient = new DatabaseGrpcClient(address);
    } catch (Exception e) {
      logger.error("Failed to create database grpc client {}", address);
    }

    Assert.assertNotNull(databaseGrpcClient);
    DynamicProperties dynamicProperties = databaseGrpcClient.getDynamicProperties();
    Assert.assertNotNull(dynamicProperties);

    Block genesisBlock = databaseGrpcClient.getBlock(0);
    Assert.assertNotNull(genesisBlock);
    Assert.assertFalse(genesisBlock.getTransactionsList().isEmpty());
    Block invalidBlock = databaseGrpcClient.getBlock(-1);
    Assert.assertNotNull(invalidBlock);
    try {
      databaseGrpcClient = new DatabaseGrpcClient(address, -1);
    } catch (Exception e) {
      logger.error("Failed to create database grpc client {}", address);
    }
    databaseGrpcClient.shutdown();
    rpcApiService.stop();
  }

  @Test
  public void testSolidityNodeHttpApiService() {
    solidityNodeHttpApiService.init(Args.getInstance());
    solidityNodeHttpApiService.start();
    solidityNodeHttpApiService.stop();
    Assert.assertTrue(true);
  }
}
