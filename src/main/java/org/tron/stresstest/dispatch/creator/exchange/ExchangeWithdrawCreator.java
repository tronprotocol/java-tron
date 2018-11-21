package org.tron.stresstest.dispatch.creator.exchange;

import java.util.concurrent.atomic.AtomicInteger;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

public class ExchangeWithdrawCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
  AtomicInteger integer = new AtomicInteger(0);
  @Override
  protected Protocol.Transaction create() {
    byte[] tokenId = firstTokeID2;
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    Contract.ExchangeWithdrawContract contract = createExchangeWithdrawContract(ownerAddressBytes,
        exchangeId, tokenId, quant2);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.ExchangeWithdrawContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
