package org.tron.core;

import static org.tron.common.utils.client.WalletClient.decodeFromBase58Check;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletGrpc.WalletBlockingStub;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.raw;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;

public class CreateCommonTransactionTest {

  private static final String FULL_NODE = "127.0.0.1:50051";

  /**
   * for example create UpdateBrokerageContract
   */
  public static void testCreateUpdateBrokerageContract() {
    io.grpc.ManagedChannel channel = ManagedChannelBuilder.forTarget(FULL_NODE).usePlaintext().build();
    try {
      WalletBlockingStub walletStub = WalletGrpc.newBlockingStub(channel);
      UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
      updateBrokerageContract.setOwnerAddress(
          ByteString.copyFrom(decodeFromBase58Check("TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz")))
          .setBrokerage(10);
      Transaction.Builder transaction = Transaction.newBuilder();
      raw.Builder raw = Transaction.raw.newBuilder();
      Contract.Builder contract = Contract.newBuilder();
      contract.setType(ContractType.UpdateBrokerageContract)
          .setParameter(Any.pack(updateBrokerageContract.build()));
      raw.addContract(contract.build());
      transaction.setRawData(raw.build());
      TransactionExtention transactionExtention = walletStub
          .createCommonTransaction(transaction.build());
      System.out.println("Common UpdateBrokerage: " + transactionExtention);
    } finally {
      // Properly shutdown the gRPC channel to prevent resource leaks
      channel.shutdown();
      try {
        if (!channel.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
          channel.shutdownNow();
        }
      } catch (InterruptedException e) {
        channel.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  public static void main(String[] args) {
    testCreateUpdateBrokerageContract();
  }

}
