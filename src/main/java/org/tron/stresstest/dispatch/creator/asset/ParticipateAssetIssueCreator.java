package org.tron.stresstest.dispatch.creator.asset;

import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

@Setter
public class ParticipateAssetIssueCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

  private String assetOwnerAddress = "TDZdB4ogHSgU1CGrun8WXaMb2QDDkvAKQm";
  private String assetName = "1002093";
  private String participateOwnerAddress = "TXtrbmfwZ2LxtoCveEhZT86fTss1w8rwJE";
  private long amount = 1L;
  private String participateOwnerPrivateKey = "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";

  @Override
  protected Protocol.Transaction create() {
    byte[] assetOwnerAddressBytes = Wallet.decodeFromBase58Check(assetOwnerAddress);
    byte[] participateOwnerAddressBytes = Wallet.decodeFromBase58Check(participateOwnerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    Contract.ParticipateAssetIssueContract contract = createParticipateAssetIssueContract(assetOwnerAddressBytes, assetName.getBytes(), participateOwnerAddressBytes, amount);
    Protocol.Transaction transaction = createTransaction(contract, ContractType.ParticipateAssetIssueContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(participateOwnerPrivateKey)));
    return transaction;
  }
}
