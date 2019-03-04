package org.tron.stresstest.dispatch.creator.contract;

import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract.UpdateSettingContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

@Setter
public class UpdateSettingTransactionCreator extends AbstractTransactionCreator implements
        GoodCaseTransactonCreator {

  private String ownerAddress = triggerOwnerAddress;
  private String contractAddress = commonContractAddress1;
  private long consumeUserResourcePercent = 100L;
  private String privateKey = triggerOwnerKey;

  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    UpdateSettingContract contract = createUpdateSettingContract(ownerAddressBytes,
            Wallet.decodeFromBase58Check(contractAddress), consumeUserResourcePercent);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.UpdateSettingContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
