package org.tron.stresstest.dispatch.creator.transfer;

import com.google.protobuf.ByteString;
import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.protos.Protocol.DeferredStage;

@Setter
public class DelaySendCoin extends AbstractTransferTransactionCreator implements GoodCaseTransactonCreator {

  private String ownerAddress = commonOwnerAddress;
  private String toAddress = commonToAddress;
  private long amount = 1L;
  private String privateKey = commonOwnerPrivateKey;
  private Long delaySeconds = 20L;
  public static final int UNEXECUTEDDEFERREDTRANSACTION = 1;

  @Override
  protected Protocol.Transaction create() {

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    Contract.TransferContract contract = Contract.TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(ownerAddress)))
        .setToAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(toAddress)))
        .setAmount(amount)
        .build();
    Protocol.Transaction transaction = createTransaction(contract, ContractType.TransferContract);
    transaction = setDelaySeconds(transaction, delaySeconds);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }

  public static Transaction setDelaySeconds(Transaction transaction, long delaySeconds) {
    DeferredStage deferredStage = transaction.getRawData().toBuilder()
        .getDeferredStage().toBuilder().setDelaySeconds(delaySeconds)
        .setStage(UNEXECUTEDDEFERREDTRANSACTION).build();
    Transaction.raw rawData = transaction.toBuilder().getRawData()
        .toBuilder().setDeferredStage(deferredStage).build();
    return transaction.toBuilder().setRawData(rawData).build();
  }


}
