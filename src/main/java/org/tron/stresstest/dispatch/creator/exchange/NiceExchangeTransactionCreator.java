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

public class NiceExchangeTransactionCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
  AtomicInteger integer = new AtomicInteger(0);
  @Override
  protected Protocol.Transaction create() {
    byte[] tokenId = firstTokeID;
    if (integer.incrementAndGet() % 2 == 0) {
      tokenId = secondTokeID;
    }
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    Contract.ExchangeTransactionContract contract = createExchangeTransactionContract(ownerAddressBytes,
        exchangeId, tokenId, quant, expected);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.ExchangeTransactionContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
