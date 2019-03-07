package org.tron.stresstest.dispatch.creator.contract;

import java.util.Random;
import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract.UpdateEnergyLimitContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

@Setter
public class UpdateEnergyLimitCreator extends AbstractTransactionCreator implements
    GoodCaseTransactonCreator {

  private String ownerAddress = triggerOwnerAddress;
  private String commonContractAddress = commonContractAddress1;
  private long originEnergyLimit = new Random().nextInt(1000000000) + 1;
  private String privateKey = triggerOwnerKey;

  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    UpdateEnergyLimitContract contract = createUpdateEnergyLimitContract(ownerAddressBytes,
        Wallet.decodeFromBase58Check(commonContractAddress), originEnergyLimit);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.UpdateEnergyLimitContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
