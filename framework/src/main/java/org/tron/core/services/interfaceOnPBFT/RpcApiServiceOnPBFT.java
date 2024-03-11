package org.tron.core.services.interfaceOnPBFT;

import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.api.DatabaseGrpc.DatabaseImplBase;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockExtention;
import org.tron.api.GrpcAPI.BlockReference;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.CanDelegatedMaxSizeRequestMessage;
import org.tron.api.GrpcAPI.CanDelegatedMaxSizeResponseMessage;
import org.tron.api.GrpcAPI.CanWithdrawUnfreezeAmountRequestMessage;
import org.tron.api.GrpcAPI.CanWithdrawUnfreezeAmountResponseMessage;
import org.tron.api.GrpcAPI.DecryptNotesTRC20;
import org.tron.api.GrpcAPI.DelegatedResourceList;
import org.tron.api.GrpcAPI.DelegatedResourceMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.GetAvailableUnfreezeCountRequestMessage;
import org.tron.api.GrpcAPI.GetAvailableUnfreezeCountResponseMessage;
import org.tron.api.GrpcAPI.IvkDecryptTRC20Parameters;
import org.tron.api.GrpcAPI.NfTRC20Parameters;
import org.tron.api.GrpcAPI.NoteParameters;
import org.tron.api.GrpcAPI.NullifierResult;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.OvkDecryptTRC20Parameters;
import org.tron.api.GrpcAPI.PaginatedMessage;
import org.tron.api.GrpcAPI.PricesResponseMessage;
import org.tron.api.GrpcAPI.SpendResult;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.api.WalletSolidityGrpc.WalletSolidityImplBase;
import org.tron.common.application.RpcService;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.filter.LiteFnQueryGrpcInterceptor;
import org.tron.core.services.ratelimiter.RateLimiterInterceptor;
import org.tron.core.services.ratelimiter.RpcApiAccessInterceptor;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.DelegatedResourceAccountIndex;
import org.tron.protos.Protocol.DynamicProperties;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.Protocol.MarketOrder;
import org.tron.protos.Protocol.MarketOrderList;
import org.tron.protos.Protocol.MarketOrderPair;
import org.tron.protos.Protocol.MarketOrderPairList;
import org.tron.protos.Protocol.MarketPriceList;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucherInfo;
import org.tron.protos.contract.ShieldContract.OutputPointInfo;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;


@Slf4j(topic = "API")
public class RpcApiServiceOnPBFT extends RpcService {

  @Autowired
  private WalletOnPBFT walletOnPBFT;

  @Autowired
  private RpcApiService rpcApiService;

  @Autowired
  private RateLimiterInterceptor rateLimiterInterceptor;

  @Autowired
  private LiteFnQueryGrpcInterceptor liteFnQueryGrpcInterceptor;

  @Autowired
  private RpcApiAccessInterceptor apiAccessInterceptor;

  private final String executorName = "rpc-pbft-executor";

  @Override
  public void init() {
  }

  @Override
  public void init(CommonParameter parameter) {
    port = Args.getInstance().getRpcOnPBFTPort();
  }

  @Override
  public void start() {
    try {
      NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(port)
          .addService(new DatabaseApi());

      CommonParameter args = CommonParameter.getInstance();

      if (args.getRpcThreadNum() > 0) {
        serverBuilder = serverBuilder
            .executor(ExecutorServiceManager.newFixedThreadPool(
                executorName, args.getRpcThreadNum()));
      }

      serverBuilder = serverBuilder.addService(new WalletPBFTApi());

      // Set configs from config.conf or default value
      serverBuilder
          .maxConcurrentCallsPerConnection(args.getMaxConcurrentCallsPerConnection())
          .flowControlWindow(args.getFlowControlWindow())
          .maxConnectionIdle(args.getMaxConnectionIdleInMillis(), TimeUnit.MILLISECONDS)
          .maxConnectionAge(args.getMaxConnectionAgeInMillis(), TimeUnit.MILLISECONDS)
          .maxInboundMessageSize(args.getMaxMessageSize())
          .maxHeaderListSize(args.getMaxHeaderListSize());

      // add a ratelimiter interceptor
      serverBuilder.intercept(rateLimiterInterceptor);

      // add api access interceptor
      serverBuilder.intercept(apiAccessInterceptor);

      // add lite fullnode query interceptor
      serverBuilder.intercept(liteFnQueryGrpcInterceptor);

      if (args.isRpcReflectionServiceEnable()) {
        serverBuilder.addService(ProtoReflectionService.newInstance());
      }

      apiServer = serverBuilder.build();
      rateLimiterInterceptor.init(apiServer);
      super.start();
    } catch (Exception e) {
      logger.debug(e.getMessage(), e);
    }
  }

  /**
   * DatabaseApi.
   */
  private class DatabaseApi extends DatabaseImplBase {

    @Override
    public void getBlockReference(EmptyMessage request,
        StreamObserver<BlockReference> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getDatabaseApi().getBlockReference(request, responseObserver)
      );
    }

    @Override
    public void getNowBlock(EmptyMessage request, StreamObserver<Block> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getDatabaseApi().getNowBlock(request, responseObserver));
    }

    @Override
    public void getBlockByNum(NumberMessage request, StreamObserver<Block> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getDatabaseApi().getBlockByNum(request, responseObserver)
      );
    }

    @Override
    public void getDynamicProperties(EmptyMessage request,
        StreamObserver<DynamicProperties> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getDatabaseApi().getDynamicProperties(request, responseObserver)
      );
    }
  }

  /**
   * WalletPBFTApi.
   */
  private class WalletPBFTApi extends WalletSolidityImplBase {

    @Override
    public void getAccount(Account request, StreamObserver<Account> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getAccount(request, responseObserver)
      );
    }

    @Override
    public void getAccountById(Account request, StreamObserver<Account> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getAccountById(request, responseObserver)
      );
    }

    @Override
    public void listWitnesses(EmptyMessage request, StreamObserver<WitnessList> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().listWitnesses(request, responseObserver)
      );
    }

    @Override
    public void getAssetIssueById(BytesMessage request,
        StreamObserver<AssetIssueContract> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getAssetIssueById(request, responseObserver)
      );
    }

    @Override
    public void getAssetIssueByName(BytesMessage request,
        StreamObserver<AssetIssueContract> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getAssetIssueByName(request, responseObserver)
      );
    }

    @Override
    public void getAssetIssueList(EmptyMessage request,
        StreamObserver<AssetIssueList> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getAssetIssueList(request, responseObserver)
      );
    }

    @Override
    public void getAssetIssueListByName(BytesMessage request,
        StreamObserver<AssetIssueList> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getAssetIssueListByName(request, responseObserver)
      );
    }

    @Override
    public void getPaginatedAssetIssueList(PaginatedMessage request,
        StreamObserver<AssetIssueList> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getPaginatedAssetIssueList(request, responseObserver)
      );
    }

    @Override
    public void getExchangeById(BytesMessage request,
        StreamObserver<Exchange> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getExchangeById(
              request, responseObserver
          )
      );
    }

    @Override
    public void getNowBlock(EmptyMessage request, StreamObserver<Block> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getNowBlock(request, responseObserver)
      );
    }

    @Override
    public void getNowBlock2(EmptyMessage request,
        StreamObserver<BlockExtention> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getNowBlock2(request, responseObserver)
      );

    }

    @Override
    public void getBlockByNum(NumberMessage request, StreamObserver<Block> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getBlockByNum(request, responseObserver)
      );
    }

    @Override
    public void getBlockByNum2(NumberMessage request,
        StreamObserver<BlockExtention> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getBlockByNum2(request, responseObserver)
      );
    }

    @Override
    public void getDelegatedResource(DelegatedResourceMessage request,
        StreamObserver<DelegatedResourceList> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getDelegatedResource(request, responseObserver)
      );
    }

    @Override
    public void getDelegatedResourceV2(DelegatedResourceMessage request,
        StreamObserver<DelegatedResourceList> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getDelegatedResourceV2(request, responseObserver)
      );
    }

    @Override
    public void getDelegatedResourceAccountIndex(BytesMessage request,
        StreamObserver<DelegatedResourceAccountIndex> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getDelegatedResourceAccountIndex(request, responseObserver)
      );
    }

    @Override
    public void getDelegatedResourceAccountIndexV2(BytesMessage request,
        StreamObserver<DelegatedResourceAccountIndex> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getDelegatedResourceAccountIndexV2(request, responseObserver)
      );
    }

    @Override
    public void getCanDelegatedMaxSize(CanDelegatedMaxSizeRequestMessage request,
         StreamObserver<CanDelegatedMaxSizeResponseMessage> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getCanDelegatedMaxSize(request, responseObserver)
      );
    }

    @Override
    public void getAvailableUnfreezeCount(GetAvailableUnfreezeCountRequestMessage request,
          StreamObserver<GetAvailableUnfreezeCountResponseMessage> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getAvailableUnfreezeCount(request, responseObserver)
      );
    }

    @Override
    public void getCanWithdrawUnfreezeAmount(CanWithdrawUnfreezeAmountRequestMessage request,
         StreamObserver<CanWithdrawUnfreezeAmountResponseMessage> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getCanWithdrawUnfreezeAmount(request, responseObserver)
      );
    }

    @Override
    public void getTransactionCountByBlockNum(NumberMessage request,
        StreamObserver<NumberMessage> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getTransactionCountByBlockNum(request, responseObserver)
      );
    }

    @Override
    public void getTransactionById(BytesMessage request,
        StreamObserver<Transaction> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getTransactionById(request, responseObserver)
      );

    }

    @Override
    public void getTransactionInfoById(BytesMessage request,
        StreamObserver<TransactionInfo> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getTransactionInfoById(request, responseObserver)
      );

    }

    @Override
    public void listExchanges(EmptyMessage request,
        StreamObserver<ExchangeList> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().listExchanges(request, responseObserver)
      );
    }

    @Override
    public void triggerConstantContract(TriggerSmartContract request,
        StreamObserver<TransactionExtention> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .triggerConstantContract(request, responseObserver)
      );
    }

    @Override
    public void estimateEnergy(TriggerSmartContract request,
        StreamObserver<GrpcAPI.EstimateEnergyMessage> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .estimateEnergy(request, responseObserver)
      );
    }

    @Override
    public void getRewardInfo(BytesMessage request,
        StreamObserver<NumberMessage> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getRewardInfo(request, responseObserver)
      );
    }

    @Override
    public void getBrokerageInfo(BytesMessage request,
        StreamObserver<NumberMessage> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getBrokerageInfo(request, responseObserver)
      );
    }

    @Override
    public void getMerkleTreeVoucherInfo(OutputPointInfo request,
        StreamObserver<IncrementalMerkleVoucherInfo> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getMerkleTreeVoucherInfo(request, responseObserver)
      );
    }

    @Override
    public void scanNoteByIvk(GrpcAPI.IvkDecryptParameters request,
        StreamObserver<GrpcAPI.DecryptNotes> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().scanNoteByIvk(request, responseObserver)
      );
    }

    @Override
    public void scanAndMarkNoteByIvk(GrpcAPI.IvkDecryptAndMarkParameters request,
        StreamObserver<GrpcAPI.DecryptNotesMarked> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().scanAndMarkNoteByIvk(request, responseObserver)
      );
    }

    @Override
    public void scanNoteByOvk(GrpcAPI.OvkDecryptParameters request,
        StreamObserver<GrpcAPI.DecryptNotes> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().scanNoteByOvk(request, responseObserver)
      );
    }

    @Override
    public void isSpend(NoteParameters request, StreamObserver<SpendResult> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().isSpend(request, responseObserver)
      );
    }

    @Override
    public void getMarketOrderByAccount(BytesMessage request,
        StreamObserver<MarketOrderList> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getMarketOrderByAccount(request, responseObserver)
      );
    }

    @Override
    public void getMarketOrderById(BytesMessage request,
        StreamObserver<MarketOrder> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getMarketOrderById(request, responseObserver)
      );
    }

    @Override
    public void getMarketPriceByPair(MarketOrderPair request,
        StreamObserver<MarketPriceList> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getMarketPriceByPair(request, responseObserver)
      );
    }

    @Override
    public void getMarketOrderListByPair(MarketOrderPair request,
        StreamObserver<MarketOrderList> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getMarketOrderListByPair(request, responseObserver)
      );
    }

    @Override
    public void getMarketPairList(EmptyMessage request,
        StreamObserver<MarketOrderPairList> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .getMarketPairList(request, responseObserver)
      );
    }

    @Override
    public void scanShieldedTRC20NotesByIvk(IvkDecryptTRC20Parameters request,
        StreamObserver<DecryptNotesTRC20> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .scanShieldedTRC20NotesByIvk(request, responseObserver)
      );
    }

    @Override
    public void scanShieldedTRC20NotesByOvk(OvkDecryptTRC20Parameters request,
        StreamObserver<DecryptNotesTRC20> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .scanShieldedTRC20NotesByOvk(request, responseObserver)
      );
    }

    @Override
    public void isShieldedTRC20ContractNoteSpent(NfTRC20Parameters request,
        StreamObserver<NullifierResult> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi()
              .isShieldedTRC20ContractNoteSpent(request, responseObserver)
      );
    }

    @Override
    public void getBurnTrx(EmptyMessage request, StreamObserver<NumberMessage> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getBurnTrx(request, responseObserver)
      );
    }

    @Override
    public void getBlock(GrpcAPI.BlockReq  request,
                         StreamObserver<BlockExtention> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getBlock(request, responseObserver));
    }

    @Override
    public void getBandwidthPrices(EmptyMessage request,
        StreamObserver<PricesResponseMessage> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getBandwidthPrices(request, responseObserver));
    }

    @Override
    public void getEnergyPrices(EmptyMessage request,
        StreamObserver<PricesResponseMessage> responseObserver) {
      walletOnPBFT.futureGet(
          () -> rpcApiService.getWalletSolidityApi().getEnergyPrices(request, responseObserver));
    }

  }
}
