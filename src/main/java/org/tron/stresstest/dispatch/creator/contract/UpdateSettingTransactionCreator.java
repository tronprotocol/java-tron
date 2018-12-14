package org.tron.stresstest.dispatch.creator.contract;

import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Contract.UpdateSettingContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.dispatch.creator.transfer.AbstractTransferTransactionCreator;

public class UpdateSettingTransactionCreator extends AbstractTransferTransactionCreator implements
    GoodCaseTransactonCreator {
  @Override
  protected Protocol.Transaction create() {
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    UpdateSettingContract contract = createUpdateSettingContract(ownerAddressBytes,
        Wallet.decodeFromBase58Check("TNp65uzyaBeHikaCLa13Ub5pQkoqx9WVZw"), 100L);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.UpdateSettingContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
