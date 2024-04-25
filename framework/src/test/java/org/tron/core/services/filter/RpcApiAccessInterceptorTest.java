package org.tron.core.services.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.api.GrpcAPI.BlockExtention;
import org.tron.api.GrpcAPI.BlockReq;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.TransactionIdList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.PublicMethod;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.interfaceOnPBFT.RpcApiServiceOnPBFT;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class RpcApiAccessInterceptorTest {

  private static TronApplicationContext context;
  private static WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private static WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private static WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPBFT = null;
  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  /**
   * init logic.
   */
  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[] {"-d", temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
    Args.getInstance().setRpcPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setRpcOnSolidityPort(PublicMethod.chooseRandomPort());
    Args.getInstance().setRpcOnPBFTPort(PublicMethod.chooseRandomPort());
    String fullNode = String.format("%s:%d", Args.getInstance().getNodeLanIp(),
        Args.getInstance().getRpcPort());
    String solidityNode = String.format("%s:%d", Args.getInstance().getNodeLanIp(),
        Args.getInstance().getRpcOnSolidityPort());
    String pBFTNode = String.format("%s:%d", Args.getInstance().getNodeLanIp(),
        Args.getInstance().getRpcOnPBFTPort());

    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullNode)
        .usePlaintext()
        .build();
    ManagedChannel channelPBFT = ManagedChannelBuilder.forTarget(pBFTNode)
        .usePlaintext()
        .build();
    ManagedChannel channelSolidity = ManagedChannelBuilder.forTarget(solidityNode)
        .usePlaintext()
        .build();

    context = new TronApplicationContext(DefaultConfig.class);

    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    blockingStubPBFT = WalletSolidityGrpc.newBlockingStub(channelPBFT);

    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    RpcApiServiceOnSolidity rpcApiServiceOnSolidity =
        context.getBean(RpcApiServiceOnSolidity.class);
    RpcApiServiceOnPBFT rpcApiServiceOnPBFT = context.getBean(RpcApiServiceOnPBFT.class);

    Application appTest = ApplicationFactory.create(context);
    appTest.addService(rpcApiService);
    appTest.addService(rpcApiServiceOnSolidity);
    appTest.addService(rpcApiServiceOnPBFT);
    appTest.startup();
  }

  /**
   * destroy the context.
   */
  @AfterClass
  public static void destroy() {
    context.close();
    Args.clearParam();
  }

  @Test
  public void testAccessDisabledFullNode() {
    List<String> disabledApiList = new ArrayList<>();
    disabledApiList.add("getaccount");
    disabledApiList.add("getblockbynum");
    Args.getInstance().setDisabledApiList(disabledApiList);

    final NumberMessage message = NumberMessage.newBuilder().setNum(0).build();
    assertThrows("this API is unavailable due to config", StatusRuntimeException.class,
        () -> blockingStubFull.getBlockByNum(message));
  }

  @Test
  public void testRpcApiService() {
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    ServerCallStreamObserverTest<BlockExtention> serverCallStreamObserverTest =
        new ServerCallStreamObserverTest<>();
    ServerCallStreamObserverTest<NumberMessage> serverCallStreamObserverTest1 =
        new ServerCallStreamObserverTest<>();
    ServerCallStreamObserverTest<Transaction> serverCallStreamObserverTest2 =
        new ServerCallStreamObserverTest<>();
    ServerCallStreamObserverTest<TransactionIdList> serverCallStreamObserverTest3 =
        new ServerCallStreamObserverTest<>();
    rpcApiService.getBlockCommon(BlockReq.getDefaultInstance(), serverCallStreamObserverTest);
    assertTrue("Get block Common failed!", serverCallStreamObserverTest.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getBrokerageInfoCommon(BytesMessage.newBuilder().build(),
        serverCallStreamObserverTest1);
    assertTrue("Get brokerage info Common failed!",
        serverCallStreamObserverTest1.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getBurnTrxCommon(EmptyMessage.newBuilder().build(),
        serverCallStreamObserverTest1);
    assertTrue("Get burn trx common failed!",
        serverCallStreamObserverTest1.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getPendingSizeCommon(EmptyMessage.getDefaultInstance(),
        serverCallStreamObserverTest1);
    assertTrue("Get pending size common failed!",
        serverCallStreamObserverTest1.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getRewardInfoCommon(BytesMessage.newBuilder().build(),
        serverCallStreamObserverTest1);
    assertTrue("Get reward info common failed!",
        serverCallStreamObserverTest1.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getTransactionCountByBlockNumCommon(
        NumberMessage.newBuilder().getDefaultInstanceForType(),
        serverCallStreamObserverTest1);
    assertTrue("Get transaction count by block num failed!",
        serverCallStreamObserverTest1.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getTransactionFromPendingCommon(BytesMessage.newBuilder().build(),
        serverCallStreamObserverTest2);
    assertFalse("Get transaction from pending failed!",
        serverCallStreamObserverTest2.isReady());
    serverCallStreamObserverTest.isCancelled();
    rpcApiService.getTransactionListFromPendingCommon(EmptyMessage.newBuilder()
        .getDefaultInstanceForType(), serverCallStreamObserverTest3);
    assertTrue("Get transaction list from pending failed!",
        serverCallStreamObserverTest3.isReady());
  }

  static class ServerCallStreamObserverTest<RespT> extends ServerCallStreamObserver<RespT> {

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

    final NumberMessage message = NumberMessage.newBuilder().setNum(0).build();
    assertThrows("this API is unavailable due to config", StatusRuntimeException.class,
        () -> blockingStubSolidity.getBlockByNum(message));
  }

  @Test
  public void testAccessDisabledPBFTNode() {
    List<String> disabledApiList = new ArrayList<>();
    disabledApiList.add("getaccount");
    disabledApiList.add("getblockbynum");
    Args.getInstance().setDisabledApiList(disabledApiList);

    final NumberMessage message = NumberMessage.newBuilder().setNum(0).build();
    assertThrows("this API is unavailable due to config", StatusRuntimeException.class,
        () -> blockingStubPBFT.getBlockByNum(message));
  }

  @Test
  public void testAccessNoDisabled() {
    Args.getInstance().setDisabledApiList(Collections.emptyList());

    final NumberMessage message = NumberMessage.newBuilder().setNum(0).build();
    assertNotNull(blockingStubFull.getBlockByNum(message));
    assertNotNull(blockingStubSolidity.getBlockByNum(message));
    assertNotNull(blockingStubPBFT.getBlockByNum(message));
  }

  @Test
  public void testAccessDisabledNotIncluded() {
    List<String> disabledApiList = new ArrayList<>();
    disabledApiList.add("getaccount");
    Args.getInstance().setDisabledApiList(disabledApiList);

    final NumberMessage message = NumberMessage.newBuilder().setNum(0).build();
    assertNotNull(blockingStubFull.getBlockByNum(message));
    assertNotNull(blockingStubSolidity.getBlockByNum(message));
    assertNotNull(blockingStubPBFT.getBlockByNum(message));
  }

  @Test
  public void testGetBandwidthPrices() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getBandwidthPrices(message));
    assertNotNull(blockingStubSolidity.getBandwidthPrices(message));
    assertNotNull(blockingStubPBFT.getBandwidthPrices(message));
  }

  @Test
  public void testGetEnergyPrices() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getEnergyPrices(message));
    assertNotNull(blockingStubSolidity.getEnergyPrices(message));
    assertNotNull(blockingStubPBFT.getEnergyPrices(message));
  }

  @Test
  public void testGetMemoFee() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getMemoFee(message));
  }

}

