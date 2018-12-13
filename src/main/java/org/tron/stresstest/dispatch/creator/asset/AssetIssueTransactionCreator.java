package org.tron.stresstest.dispatch.creator.asset;

import java.util.HashMap;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

public class AssetIssueTransactionCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
  @Override
  protected Protocol.Transaction create() {
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    Contract.AssetIssueContract contract = createAssetIssueContract(
        ownerAddressBytes,
        "create",
        1000,
        1,
        1,
        0,
        System.currentTimeMillis() + 60 * 60 * 24 * 30,
        System.currentTimeMillis() + 60 * 60 * 24 * 60,
        0,
        "create asset issue",
        "www.create.com",
        100000,
        1000000,
        new HashMap<>()
    );
    Protocol.Transaction transaction = createTransaction(contract, ContractType.AssetIssueContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
