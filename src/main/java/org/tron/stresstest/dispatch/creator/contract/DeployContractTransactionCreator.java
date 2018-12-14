package org.tron.stresstest.dispatch.creator.contract;

import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.dispatch.creator.transfer.AbstractTransferTransactionCreator;

public class DeployContractTransactionCreator extends AbstractTransferTransactionCreator implements
    GoodCaseTransactonCreator {
  @Override
  protected Protocol.Transaction create() {
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    CreateSmartContract contract = org.tron.stresstest.dispatch.CreateSmartContract.createContractDeployContract(contractName, ownerAddress.toByteArray(),
        ABI, code, value, consumeUserResourcePercent, libraryAddress);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.CreateSmartContract);

    transaction = transaction.toBuilder().setRawData(transaction.getRawData().toBuilder().setFeeLimit(10000000).build()).build();

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
