package org.tron.stresstest.dispatch.creator.transfer;

import com.google.protobuf.ByteString;
import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.core.Wallet;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Setter
public class NiceTransferTransactionCreator extends AbstractTransferTransactionCreator implements GoodCaseTransactonCreator {

  private String ownerAddress = commonOwnerAddress;
  private String toAddress = commonToAddress;
  private long amount = 1L;
  private String privateKey = commonOwnerPrivateKey;

  @Override
  protected Protocol.Transaction create() {

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    Contract.TransferContract contract = Contract.TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(ownerAddress)))
        .setToAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(toAddress)))
        .setAmount(amount)
        .build();
    Protocol.Transaction transaction = createTransaction(contract, ContractType.TransferContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
