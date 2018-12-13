package org.tron.stresstest.dispatch.creator.witness;

import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.dispatch.creator.transfer.AbstractTransferTransactionCreator;

public class WitnessUpdateTransactionCreator extends AbstractTransferTransactionCreator implements GoodCaseTransactonCreator {
  @Override
  protected Protocol.Transaction create() {
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    Contract.WitnessUpdateContract contract = createWitnessUpdateContract(witnessUpdateAddressBytes, "http://Mercury.org".getBytes());
    Protocol.Transaction transaction = createTransaction(contract, ContractType.WitnessUpdateContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(witnessUpdatePrivateKey)));
    return transaction;
  }
}
