package org.tron.stresstest.dispatch.creator.asset;

import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Configuration;
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

  private String assetOwnerAddress = Configuration.getByPath("stress.conf").getString("address.assetIssueOwnerAddress");
  private String assetName = commontokenid;
  private String participateOwnerAddress = Configuration.getByPath("stress.conf").getString("address.participateOwnerAddress");
  private long amount = 1L;
  private String participateOwnerPrivateKey = Configuration.getByPath("stress.conf").getString("privateKey.participateOwnerPrivateKey");

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
