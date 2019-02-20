package org.tron.stresstest.dispatch.creator.asset;

import com.google.protobuf.ByteString;
import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.core.Wallet;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Setter
public class NiceTransferAssetTransactionCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

  private String assetName = "1002113";
  private String ownerAddress = "TDZdB4ogHSgU1CGrun8WXaMb2QDDkvAKQm";
  private String toAddress = commonToAddress;
  private long amount = 1L;
  private String privateKey = "549c7797b351e48ab1c6bb5857138b418012d97526fc2acba022357d49c93ac0";

  @Override
  protected Protocol.Transaction create() {


    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    Contract.TransferAssetContract contract = Contract.TransferAssetContract.newBuilder()
            .setAssetName(ByteString.copyFrom(assetName.getBytes()))
            .setOwnerAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(ownerAddress)))
            .setToAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(toAddress)))
            .setAmount(amount)
            .build();
    Protocol.Transaction transaction = createTransaction(contract, ContractType.TransferAssetContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
