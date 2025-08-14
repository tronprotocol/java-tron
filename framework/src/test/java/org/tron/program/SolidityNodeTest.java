package org.tron.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.client.DatabaseGrpcClient;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.exception.TronError;
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
    Args.setParam(new String[]{"-d", dbPath(), "--solidity"}, Constant.TEST_CONF);
  }

  @Test
  public void testSolidityArgs() {
    Assert.assertNotNull(Args.getInstance().getTrustNodeAddr());
    Assert.assertTrue(Args.getInstance().isSolidityNode());
    String trustNodeAddr = Args.getInstance().getTrustNodeAddr();
    Args.getInstance().setTrustNodeAddr(null);
    TronError thrown = assertThrows(TronError.class,
        SolidityNode::start);
    assertEquals(TronError.ErrCode.SOLID_NODE_INIT, thrown.getErrCode());
    Args.getInstance().setTrustNodeAddr(trustNodeAddr);
  }

  @Test
  public void testSolidityGrpcCall() {
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
    solidityNodeHttpApiService.start();
    // start again
    solidityNodeHttpApiService.start();
    solidityNodeHttpApiService.stop();
    Assert.assertTrue(true);
  }
}
