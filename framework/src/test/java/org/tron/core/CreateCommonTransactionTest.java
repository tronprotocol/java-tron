package org.tron.core;

import static stest.tron.wallet.common.client.WalletClient.decodeFromBase58Check;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletGrpc.WalletBlockingStub;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.raw;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.BalanceContract.CrossContract;
import org.tron.protos.contract.BalanceContract.CrossContract.CrossDataType;
import org.tron.protos.contract.BalanceContract.CrossToken;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;
import stest.tron.wallet.common.client.utils.PublicMethed;

public class CreateCommonTransactionTest {

  private static String fullnode = "127.0.0.1:50051";

  private static ByteString owner = ByteString
      .copyFrom(Commons.decodeFromBase58Check("TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW"));
  private static String pk = "D95611A9AF2A2A45359106222ED1AFED48853D9A44DEFF8DC7913F5CBA727366";
  private static final String URL = "https://tron.network";

  /**
   * for example create UpdateBrokerageContract
   */
  public static void testCreateUpdateBrokerageContract() {
    WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build());
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
  }

  public static void testCrossTx() {
    WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build());
    CrossToken.Builder crossToken = CrossToken.newBuilder();
    crossToken.setAmount(100).setTokenId(ByteString.copyFrom(ByteArray.fromString("1000001")))
        .setTokenName(ByteString.copyFrom(ByteArray.fromString("testCross"))).setPrecision(0)
        .setChainId(Sha256Hash.wrap(ByteArray
            .fromHexString("000000000000000019b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString());
    CrossContract.Builder builder = CrossContract.newBuilder();
    builder.setOwnerAddress(owner)
        .setOwnerChainId(Sha256Hash.wrap(ByteArray
            .fromHexString("000000000000000019b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString())
        .setToAddress(owner).setToChainId(Sha256Hash.wrap(
        ByteArray.fromHexString("0000000000000000d4b7cf850c78c1c779d19446edeafdfeb30875060e5dcee8"))
        .getByteString()).setType(CrossDataType.TOKEN).setData(crossToken.build().toByteString());
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

  public static void testFalseCrossTxBack() {
    WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build());
    CrossToken.Builder crossToken = CrossToken.newBuilder();
    crossToken.setAmount(100).setTokenId(ByteString.copyFrom(ByteArray.fromString("1000001")))
        .setTokenName(ByteString.copyFrom(ByteArray.fromString("testCross2"))).setPrecision(0)
        .setChainId(Sha256Hash.wrap(ByteArray
            .fromHexString("0000000000000000d4b7cf850c78c1c779d19446edeafdfeb30875060e5dcee8"))
            .getByteString());
    CrossContract.Builder builder = CrossContract.newBuilder();
    builder.setOwnerAddress(owner)
        .setToChainId(Sha256Hash.wrap(ByteArray
            .fromHexString("000000000000000019b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString())
        .setToAddress(owner).setOwnerChainId(Sha256Hash.wrap(
        ByteArray.fromHexString("0000000000000000d4b7cf850c78c1c779d19446edeafdfeb30875060e5dcee8"))
        .getByteString()).setType(CrossDataType.TOKEN).setData(crossToken.build().toByteString());
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

  public static void testRightCrossTxBack() {
    WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build());
    CrossToken.Builder crossToken = CrossToken.newBuilder();
    crossToken.setAmount(100).setTokenId(ByteString.copyFrom(ByteArray.fromString("1000001")))
        .setTokenName(ByteString.copyFrom(ByteArray.fromString("testCross2"))).setPrecision(0)
        .setChainId(Sha256Hash.wrap(ByteArray
            .fromHexString("000000000000000019b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString());
    CrossContract.Builder builder = CrossContract.newBuilder();
    builder.setOwnerAddress(owner)
        .setToChainId(Sha256Hash.wrap(ByteArray
            .fromHexString("000000000000000019b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString())
        .setToAddress(owner).setOwnerChainId(Sha256Hash.wrap(
        ByteArray.fromHexString("0000000000000000d4b7cf850c78c1c779d19446edeafdfeb30875060e5dcee8"))
        .getByteString()).setType(CrossDataType.TOKEN).setData(crossToken.build().toByteString());
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
    //NumberMessage.Builder builder = NumberMessage.newBuilder();
    //builder.setNum(0);
    //System.out.println(new Sha256Hash
    //    (0, Sha256Hash.of(walletStub.getBlockByNum(builder.build()).getBlockHeader().getRawData()
    //        .toByteArray())));
    Account.Builder account = Account.newBuilder();
    account.setAddress(owner);
    System.out.println(walletStub.getAccount(account.build()));
  }

  public static void createAsset(String tokenName) {
    WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build());
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(owner)
            .setName(ByteString.copyFrom(ByteArray.fromString(tokenName)))
            .setTotalSupply(100000000)
            .setPrecision(0)
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .setStartTime(1595805931000L)
            .setEndTime(1596905931000L)
            .setTrxNum(1).setNum(1)
            .build();

    Transaction.Builder transaction = Transaction.newBuilder();
    raw.Builder raw = Transaction.raw.newBuilder();
    Contract.Builder contract = Contract.newBuilder();
    contract.setType(ContractType.AssetIssueContract)
        .setParameter(Any.pack(assetIssueContract));
    raw.addContract(contract.build());
    transaction.setRawData(raw.build());
    TransactionExtention transactionExtention = walletStub
        .createCommonTransaction(transaction.build());
    System.out.println("Common AssetIssueContract: " + transactionExtention);
    Transaction tx = PublicMethed
        .addTransactionSign(transactionExtention.getTransaction(), pk, walletStub);
    System.out.println(walletStub.broadcastTransaction(tx));
  }

  public static void main(String[] args) {
    //testCreateUpdateBrokerageContract();
    //testCrossTx();
    //testFalseCrossTxBack();
    //testRightCrossTxBack();
    query();
    //createAsset("testCross111");
  }

}
