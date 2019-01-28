package org.tron.stresstest.dispatch.creator.exchange;

import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract.ExchangeCreateContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

@Setter
public class ExchangeCreateCreator extends AbstractTransactionCreator implements
    GoodCaseTransactonCreator {

  private String ownerAddress = commonOwnerAddress;
  private String firstTokenID = "1002033";
  private long firstTokenBalance = 1L;
  private String secondTokenID = "_";
  private long secondTokenBalance = 1L;
  private String privateKey = commonOwnerPrivateKey;

  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    ExchangeCreateContract contract = createExchangeCreateContract(ownerAddressBytes, firstTokenID.getBytes(), firstTokenBalance, secondTokenID.getBytes(), secondTokenBalance);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.ExchangeCreateContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
