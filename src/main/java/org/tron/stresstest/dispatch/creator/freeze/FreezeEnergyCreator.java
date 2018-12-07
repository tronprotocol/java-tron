package org.tron.stresstest.dispatch.creator.freeze;

import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

public class FreezeEnergyCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
  @Override
  protected Protocol.Transaction create() {
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    Contract.FreezeBalanceContract contract = createFreezeBalanceContract(ownerAddressBytes, 1_000_000L, 0, 1, toAddressStr);
    Protocol.Transaction transaction = createTransaction(contract, ContractType.FreezeBalanceContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
