package org.tron.stresstest.dispatch.creator.witness;

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
public class WitnessUpdateTransactionCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

  private String witnessAddress = commonWitnessAddress;
  private String witnessPrivateKey = commonWitnessPrivateKey;
  private String url = "http://Mercury.org";

  @Override
  protected Protocol.Transaction create() {
    byte[] witnessAddressBytes = Wallet.decodeFromBase58Check(witnessAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    Contract.WitnessUpdateContract contract = createWitnessUpdateContract(witnessAddressBytes, url.getBytes());
    Protocol.Transaction transaction = createTransaction(contract, ContractType.WitnessUpdateContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(witnessPrivateKey)));
    return transaction;
  }
}
