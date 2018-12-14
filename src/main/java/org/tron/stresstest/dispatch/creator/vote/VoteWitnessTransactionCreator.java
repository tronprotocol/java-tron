package org.tron.stresstest.dispatch.creator.vote;

import java.util.HashMap;
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
public class VoteWitnessTransactionCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

  private String voteWitnessAddress = commonWitnessAddress;
  private String voteCount = "1";
  private String ownerAddress = commonOwnerAddress;
  private String privateKey = commonOwnerPrivateKey;

  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    HashMap<String, String> witness = new HashMap<>();
    witness.put(voteWitnessAddress, voteCount);

    Contract.VoteWitnessContract contract = createVoteWitnessContract(ownerAddressBytes, witness);
    Protocol.Transaction transaction = createTransaction(contract, ContractType.VoteWitnessContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
