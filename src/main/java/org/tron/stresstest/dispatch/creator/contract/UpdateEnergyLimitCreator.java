package org.tron.stresstest.dispatch.creator.contract;

import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract.UpdateEnergyLimitContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.dispatch.creator.transfer.AbstractTransferTransactionCreator;

public class UpdateEnergyLimitCreator extends AbstractTransferTransactionCreator implements
    GoodCaseTransactonCreator {
  @Override
  protected Protocol.Transaction create() {
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    UpdateEnergyLimitContract contract = createUpdateEnergyLimitContract(ownerAddressBytes,
        Wallet.decodeFromBase58Check("TNp65uzyaBeHikaCLa13Ub5pQkoqx9WVZw"), 11111111111111L);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.UpdateEnergyLimitContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
