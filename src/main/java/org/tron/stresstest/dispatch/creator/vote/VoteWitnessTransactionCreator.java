package org.tron.stresstest.dispatch.creator.vote;

import java.util.HashMap;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.dispatch.creator.transfer.AbstractTransferTransactionCreator;

public class VoteWitnessTransactionCreator extends AbstractTransferTransactionCreator implements GoodCaseTransactonCreator {
  @Override
  protected Protocol.Transaction create() {
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    HashMap<String, String> witness = new HashMap<>();
    witness.put(voteWitnessAddress, voteCount);

    Contract.VoteWitnessContract contract = createVoteWitnessContract(ownerAddressBytes, witness);
    Protocol.Transaction transaction = createTransaction(contract, ContractType.VoteWitnessContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
