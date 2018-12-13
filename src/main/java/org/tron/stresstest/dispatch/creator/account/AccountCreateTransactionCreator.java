package org.tron.stresstest.dispatch.creator.account;

import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.dispatch.creator.transfer.AbstractTransferTransactionCreator;

public class AccountCreateTransactionCreator extends AbstractTransferTransactionCreator implements GoodCaseTransactonCreator {
  @Override
  protected Protocol.Transaction create() {
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    ECKey newAccountKey = new ECKey();
    byte[] newAccountAddressBytes = newAccountKey.getAddress();
    Contract.AccountCreateContract contract = createAccountCreateContract(ownerAddressBytes, newAccountAddressBytes);
    Protocol.Transaction transaction = createTransaction(contract, ContractType.AccountCreateContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
