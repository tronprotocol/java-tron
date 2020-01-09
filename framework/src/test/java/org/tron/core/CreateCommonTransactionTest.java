package org.tron.core;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletGrpc.WalletBlockingStub;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.raw;
import org.tron.protos.contract.BalanceContract.CrossContract;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;
import stest.tron.wallet.common.client.utils.PublicMethed;

public class CreateCommonTransactionTest {

  private static String fullnode = "127.0.0.1:50051";

  private static ByteString owner = ByteString
      .copyFrom(Wallet.decodeFromBase58Check("TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW"));
  private static String pk = "D95611A9AF2A2A45359106222ED1AFED48853D9A44DEFF8DC7913F5CBA727366";

  /**
   * for example create UpdateBrokerageContract
   */
  public static void testCreateUpdateBrokerageContract() {
    WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build());
    UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
    updateBrokerageContract.setOwnerAddress(
        ByteString.copyFrom(Wallet.decodeFromBase58Check("TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz")))
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
  }

  public static void testCrossTx() {
    WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build());
    CrossContract.Builder builder = CrossContract.newBuilder();
    builder.setOwnerAddress(owner)
        .setOwnerChainId(Sha256Hash.wrap(ByteArray
            .fromHexString("000000000000000019b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString())
        .setToAddress(owner).setToChainId(Sha256Hash.wrap(
        ByteArray.fromHexString("0000000000000000bff8ab4242b00fac071a0035cb8e98d6351c87f0f1a753dd"))
        .getByteString());
    Transaction.Builder transaction = Transaction.newBuilder();
    raw.Builder raw = Transaction.raw.newBuilder();
    Contract.Builder contract = Contract.newBuilder();
    contract.setType(ContractType.CrossContract)
        .setParameter(Any.pack(builder.build()));
    raw.addContract(contract.build());
    transaction.setRawData(raw.build());
    TransactionExtention transactionExtention = walletStub
        .createCommonTransaction(transaction.build());
    System.out.println("Common CrossContract: " + transactionExtention);
    Transaction tx = PublicMethed
        .addTransactionSign(transactionExtention.getTransaction(), pk, walletStub);
    System.out.println(walletStub.broadcastTransaction(tx));
  }

  public static void query() {
    WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build());
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(0);
    System.out.println(new Sha256Hash
        (0, Sha256Hash.of(walletStub.getBlockByNum(builder.build()).getBlockHeader().getRawData()
            .toByteArray())));
  }

  public static void main(String[] args) {
//    testCreateUpdateBrokerageContract();
    testCrossTx();
//    query();
  }

}
