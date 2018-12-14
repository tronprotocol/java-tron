package org.tron.stresstest.dispatch.creator.exchange;

import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract.ExchangeCreateContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.dispatch.creator.transfer.AbstractTransferTransactionCreator;

public class ExchangeCreateCreator extends AbstractTransferTransactionCreator implements
    GoodCaseTransactonCreator {
  @Override
  protected Protocol.Transaction create() {
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    ExchangeCreateContract contract = createExchangeCreateContract(ownerAddressBytes, "1000001".getBytes(), 1L, "_".getBytes(), 1L);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.ExchangeCreateContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
