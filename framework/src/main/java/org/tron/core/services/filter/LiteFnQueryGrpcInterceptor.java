package org.tron.core.services.filter;

import com.beust.jcommander.internal.Sets;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;

@Component
public class LiteFnQueryGrpcInterceptor implements ServerInterceptor {

  private static final Set<String> filterMethods = Sets.newHashSet();

  // for test
  public static Set<String> getFilterMethods() {
    return filterMethods;
  }

  static {
    // wallet
    filterMethods.add("protocol.Wallet/GetBlockById");
    filterMethods.add("protocol.Wallet/GetBlockByLatestNum");
    filterMethods.add("protocol.Wallet/GetBlockByLatestNum2");
    filterMethods.add("protocol.Wallet/GetBlockByLimitNext");
    filterMethods.add("protocol.Wallet/GetBlockByLimitNext2");
    filterMethods.add("protocol.Wallet/GetBlockByNum");
    filterMethods.add("protocol.Wallet/GetBlockByNum2");
    filterMethods.add("protocol.Wallet/GetMerkleTreeVoucherInfo");
    filterMethods.add("protocol.Wallet/GetTransactionById");
    filterMethods.add("protocol.Wallet/GetTransactionCountByBlockNum");
    filterMethods.add("protocol.Wallet/GetTransactionInfoById");
    filterMethods.add("protocol.Wallet/IsSpend");
    filterMethods.add("protocol.Wallet/ScanAndMarkNoteByIvk");
    filterMethods.add("protocol.Wallet/ScanNoteByIvk");
    filterMethods.add("protocol.Wallet/ScanNoteByOvk");
    filterMethods.add("protocol.Wallet/TotalTransaction");
    filterMethods.add("protocol.Wallet/GetMarketOrderByAccount");
    filterMethods.add("protocol.Wallet/GetMarketOrderById");
    filterMethods.add("protocol.Wallet/GetMarketPriceByPair");
    filterMethods.add("protocol.Wallet/GetMarketOrderListByPair");
    filterMethods.add("protocol.Wallet/GetMarketPairList");
    filterMethods.add("protocol.Wallet/ScanShieldedTRC20NotesByIvk");
    filterMethods.add("protocol.Wallet/ScanShieldedTRC20NotesByOvk");
    filterMethods.add("protocol.Wallet/IsShieldedTRC20ContractNoteSpent");

    // walletSolidity
    filterMethods.add("protocol.WalletSolidity/GetBlockByNum");
    filterMethods.add("protocol.WalletSolidity/GetBlockByNum2");
    filterMethods.add("protocol.WalletSolidity/GetMerkleTreeVoucherInfo");
    filterMethods.add("protocol.WalletSolidity/GetTransactionById");
    filterMethods.add("protocol.WalletSolidity/GetTransactionCountByBlockNum");
    filterMethods.add("protocol.WalletSolidity/GetTransactionInfoById");
    filterMethods.add("protocol.WalletSolidity/IsSpend");
    filterMethods.add("protocol.WalletSolidity/ScanAndMarkNoteByIvk");
    filterMethods.add("protocol.WalletSolidity/ScanNoteByIvk");
    filterMethods.add("protocol.WalletSolidity/ScanNoteByOvk");
    filterMethods.add("protocol.WalletSolidity/GetMarketOrderByAccount");
    filterMethods.add("protocol.WalletSolidity/GetMarketOrderById");
    filterMethods.add("protocol.WalletSolidity/GetMarketPriceByPair");
    filterMethods.add("protocol.WalletSolidity/GetMarketOrderListByPair");
    filterMethods.add("protocol.WalletSolidity/GetMarketPairList");
    filterMethods.add("protocol.WalletSolidity/ScanShieldedTRC20NotesByIvk");
    filterMethods.add("protocol.WalletSolidity/ScanShieldedTRC20NotesByOvk");
    filterMethods.add("protocol.WalletSolidity/IsShieldedTRC20ContractNoteSpent");

    // database
    filterMethods.add("protocol.Database/GetBlockByNum");
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
      Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    boolean shouldBeFiltered = false;
    if (CommonParameter.getInstance().isLiteFullNode
            && !CommonParameter.getInstance().openHistoryQueryWhenLiteFN
            && filterMethods.contains(call.getMethodDescriptor().getFullMethodName())) {
      shouldBeFiltered = true;
    }
    if (shouldBeFiltered) {
      call.close(Status.UNAVAILABLE
              .withDescription("this API is closed because this node is a lite fullnode"), headers);
      return new ServerCall.Listener<ReqT>() {};
    } else {
      return next.startCall(call, headers);
    }
  }
}
