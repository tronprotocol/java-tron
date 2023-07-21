package org.tron.core.services.filter;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PublicMethod;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.interfaceOnPBFT.RpcApiServiceOnPBFT;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;

@Slf4j
public class RpcApiAccessInterceptorTest {

  private static TronApplicationContext context;

  private static WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private static WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private static WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPBFT = null;
  private static Application appTest;

  private static String dbPath = "output_rpc_api_access_interceptor_test";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  /**
   * init logic.
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[] {"-d", dbPath}, Constant.TEST_CONF);
    Args.getInstance().setRpcPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setRpcOnSolidityPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setRpcOnPBFTPort(PublicMethod.chooseRandomPort());
    String fullNode = String.format("%s:%d", Args.getInstance().getNodeDiscoveryBindIp(),
        Args.getInstance().getRpcPort());
    String solidityNode = String.format("%s:%d", Args.getInstance().getNodeDiscoveryBindIp(),
        Args.getInstance().getRpcOnSolidityPort());
    String pBFTNode = String.format("%s:%d", Args.getInstance().getNodeDiscoveryBindIp(),
        Args.getInstance().getRpcOnPBFTPort());

    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullNode)
        .usePlaintext()
        .build();
    ManagedChannel channelpBFT = ManagedChannelBuilder.forTarget(pBFTNode)
        .usePlaintext()
        .build();
    ManagedChannel channelSolidity = ManagedChannelBuilder.forTarget(solidityNode)
        .usePlaintext()
        .build();

    context = new TronApplicationContext(DefaultConfig.class);

    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    blockingStubPBFT = WalletSolidityGrpc.newBlockingStub(channelpBFT);

    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    RpcApiServiceOnSolidity rpcApiServiceOnSolidity =
        context.getBean(RpcApiServiceOnSolidity.class);
    RpcApiServiceOnPBFT rpcApiServiceOnPBFT = context.getBean(RpcApiServiceOnPBFT.class);

    appTest = ApplicationFactory.create(context);
    appTest.addService(rpcApiService);
    appTest.addService(rpcApiServiceOnSolidity);
    appTest.addService(rpcApiServiceOnPBFT);
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
  public void testAccessDisabledFullNode() {
    List<String> disabledApiList = new ArrayList<>();
    disabledApiList.add("getaccount");
    disabledApiList.add("getblockbynum");
    Args.getInstance().setDisabledApiList(disabledApiList);

    final GrpcAPI.NumberMessage message = GrpcAPI.NumberMessage.newBuilder().setNum(0).build();
    thrown.expect(StatusRuntimeException.class);
    thrown.expectMessage("this API is unavailable due to config");
    blockingStubFull.getBlockByNum(message);
  }

  @Test
  public void testRpcApiService() {
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    ServerCallStreamObserverTest serverCallStreamObserverTest = new ServerCallStreamObserverTest();
    rpcApiService.getBlockCommon(GrpcAPI.BlockReq.getDefaultInstance(),
        serverCallStreamObserverTest);
    Assert.assertTrue("Get block Common failed!", serverCallStreamObserverTest.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getBrokerageInfoCommon(GrpcAPI.BytesMessage.newBuilder().build(),
        serverCallStreamObserverTest);
    Assert.assertTrue("Get brokerage info Common failed!",
        serverCallStreamObserverTest.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getBurnTrxCommon(GrpcAPI.EmptyMessage.newBuilder().build(),
        serverCallStreamObserverTest);
    Assert.assertTrue("Get burn trx common failed!",
        serverCallStreamObserverTest.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getPendingSizeCommon(GrpcAPI.EmptyMessage.getDefaultInstance(),
        serverCallStreamObserverTest);
    Assert.assertTrue("Get pending size common failed!",
        serverCallStreamObserverTest.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getRewardInfoCommon(GrpcAPI.BytesMessage.newBuilder().build(),
        serverCallStreamObserverTest);
    Assert.assertTrue("Get reward info common failed!",
        serverCallStreamObserverTest.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getTransactionCountByBlockNumCommon(
        GrpcAPI.NumberMessage.newBuilder().getDefaultInstanceForType(),
        serverCallStreamObserverTest);
    Assert.assertTrue("Get transaction count by block num failed!",
        serverCallStreamObserverTest.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getTransactionFromPendingCommon(GrpcAPI.BytesMessage.newBuilder().build(),
        serverCallStreamObserverTest);
    Assert.assertTrue("Get transaction from pending failed!",
        serverCallStreamObserverTest.isReady() == false);
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getTransactionListFromPendingCommon(GrpcAPI.EmptyMessage.newBuilder()
        .getDefaultInstanceForType(), serverCallStreamObserverTest);
    Assert.assertTrue("Get transaction list from pending failed!",
        serverCallStreamObserverTest.isReady());
  }


  class ServerCallStreamObserverTest extends ServerCallStreamObserver {

    Object ret;

    @Override
    public boolean isCancelled() {
      ret = null;
      return true;
    }

    @Override
    public void setOnCancelHandler(Runnable onCancelHandler) {
    }

    @Override
    public void setCompression(String compression) {
    }

    @Override
    public boolean isReady() {
      return Objects.nonNull(ret);
    }

    @Override
    public void setOnReadyHandler(Runnable onReadyHandler) {
    }

    @Override
    public void disableAutoInboundFlowControl() {
    }

    @Override
    public void request(int count) {
    }

    @Override
    public void setMessageCompression(boolean enable) {
    }

    @Override
    public void onNext(Object value) {
      ret = value;
    }

    @Override
    public void onError(Throwable t) {
    }

    @Override
    public void onCompleted() {
    }
  }


  @Test
  public void testAccessDisabledSolidityNode() {
    List<String> disabledApiList = new ArrayList<>();
    disabledApiList.add("getaccount");
    disabledApiList.add("getblockbynum");
    Args.getInstance().setDisabledApiList(disabledApiList);

    final GrpcAPI.NumberMessage message = GrpcAPI.NumberMessage.newBuilder().setNum(0).build();
    thrown.expect(StatusRuntimeException.class);
    thrown.expectMessage("this API is unavailable due to config");
    blockingStubSolidity.getBlockByNum(message);
  }

  @Test
  public void testAccessDisabledPBFTNode() {
    List<String> disabledApiList = new ArrayList<>();
    disabledApiList.add("getaccount");
    disabledApiList.add("getblockbynum");
    Args.getInstance().setDisabledApiList(disabledApiList);

    final GrpcAPI.NumberMessage message = GrpcAPI.NumberMessage.newBuilder().setNum(0).build();
    thrown.expect(StatusRuntimeException.class);
    thrown.expectMessage("this API is unavailable due to config");
    blockingStubPBFT.getBlockByNum(message);
  }

  @Test
  public void testAccessNoDisabled() {
    Args.getInstance().setDisabledApiList(Collections.emptyList());

    final GrpcAPI.NumberMessage message = GrpcAPI.NumberMessage.newBuilder().setNum(0).build();
    Assert.assertNotNull(blockingStubFull.getBlockByNum(message));
    Assert.assertNotNull(blockingStubSolidity.getBlockByNum(message));
    Assert.assertNotNull(blockingStubPBFT.getBlockByNum(message));
  }

  @Test
  public void testAccessDisabledNotIncluded() {
    List<String> disabledApiList = new ArrayList<>();
    disabledApiList.add("getaccount");
    Args.getInstance().setDisabledApiList(disabledApiList);

    final GrpcAPI.NumberMessage message = GrpcAPI.NumberMessage.newBuilder().setNum(0).build();
    Assert.assertNotNull(blockingStubFull.getBlockByNum(message));
    Assert.assertNotNull(blockingStubSolidity.getBlockByNum(message));
    Assert.assertNotNull(blockingStubPBFT.getBlockByNum(message));
  }

}

