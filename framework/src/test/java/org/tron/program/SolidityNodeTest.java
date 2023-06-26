package org.tron.program;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.client.DatabaseGrpcClient;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.DynamicProperties;

@Slf4j
public class SolidityNodeTest {

  private static TronApplicationContext context;

  private static RpcApiService rpcApiService;
  private static String dbPath = "output_sn_test";

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    Args.getInstance().setSolidityNode(true);
    rpcApiService = context.getBean(RpcApiService.class);
  }

  /**
   * init db.
   */
  @BeforeClass
  public static void init() {
    rpcApiService.start();
  }

  /**
   * remo db when after test.
   */
  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    rpcApiService.stop();
    context.destroy();
    File dbFolder = new File(dbPath);
    if (deleteFolder(dbFolder)) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
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
  }

}
