package org.tron.core.services.filter;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.PublicMethod;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;

@Slf4j
public class LiteFnQueryGrpcInterceptorTest {

  private static TronApplicationContext context;
  private static ManagedChannel channelFull = null;
  private static ManagedChannel channelSolidity = null;
  private static ManagedChannel channelpBFT = null;
  private static WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private static WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private static WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubpBFT = null;
  private static ChainBaseManager chainBaseManager;
  private static final String ERROR_MSG =
      "UNAVAILABLE: this API is closed because this node is a lite fullnode";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  /**
   * init logic.
   */
  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
    Args.getInstance().setRpcEnable(true);
    Args.getInstance().setRpcPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setRpcSolidityEnable(true);
    Args.getInstance().setRpcOnSolidityPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setRpcPBFTEnable(true);
    Args.getInstance().setRpcOnPBFTPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setP2pDisable(true);
    String fullnode = String.format("%s:%d", Constant.LOCAL_HOST,
            Args.getInstance().getRpcPort());
    String solidityNode = String.format("%s:%d", Constant.LOCAL_HOST,
            Args.getInstance().getRpcOnSolidityPort());
    String pBFTNode = String.format("%s:%d", Constant.LOCAL_HOST,
        Args.getInstance().getRpcOnPBFTPort());
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext()
            .build();
    channelSolidity = ManagedChannelBuilder.forTarget(solidityNode)
        .usePlaintext()
        .build();
    channelpBFT = ManagedChannelBuilder.forTarget(pBFTNode)
            .usePlaintext()
            .build();
    context = new TronApplicationContext(DefaultConfig.class);
    context.registerShutdownHook();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    blockingStubpBFT = WalletSolidityGrpc.newBlockingStub(channelpBFT);
    chainBaseManager = context.getBean(ChainBaseManager.class);
    Application appTest = ApplicationFactory.create(context);
    appTest.startup();
  }

  /**
   * destroy the context.
   */
  @AfterClass
  public static void destroy() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelpBFT != null) {
      channelpBFT.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    context.close();
    Args.clearParam();
  }

  @Test
  public void testGrpcApiThrowStatusRuntimeException() {
    final GrpcAPI.NumberMessage message = GrpcAPI.NumberMessage.newBuilder().setNum(0).build();
    chainBaseManager.setNodeType(ChainBaseManager.NodeType.LITE);
    thrown.expect(StatusRuntimeException.class);
    thrown.expectMessage(ERROR_MSG);
    blockingStubFull.getBlockByNum(message);
  }

  @Test
  public void testGrpcSolidityThrowStatusRuntimeException() {
    final GrpcAPI.NumberMessage message = GrpcAPI.NumberMessage.newBuilder().setNum(0).build();
    chainBaseManager.setNodeType(ChainBaseManager.NodeType.LITE);
    thrown.expect(StatusRuntimeException.class);
    thrown.expectMessage(ERROR_MSG);
    blockingStubSolidity.getBlockByNum(message);
  }

  @Test
  public void testpBFTGrpcApiThrowStatusRuntimeException() {
    final GrpcAPI.NumberMessage message = GrpcAPI.NumberMessage.newBuilder().setNum(0).build();
    chainBaseManager.setNodeType(ChainBaseManager.NodeType.LITE);
    thrown.expect(StatusRuntimeException.class);
    thrown.expectMessage(ERROR_MSG);
    blockingStubpBFT.getBlockByNum(message);
  }

  @Test
  public void testGrpcInterceptor() {
    GrpcAPI.NumberMessage message = GrpcAPI.NumberMessage.newBuilder().setNum(0).build();
    chainBaseManager.setNodeType(ChainBaseManager.NodeType.FULL);
    Assert.assertNotNull(blockingStubFull.getBlockByNum(message));
  }
}
