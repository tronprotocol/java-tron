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

  private String ownerAddress = "TDZdB4ogHSgU1CGrun8WXaMb2QDDkvAKQm";
  private String firstTokenID = "1002093";
  private long firstTokenBalance = 1L;
  private String secondTokenID = "_";
  private long secondTokenBalance = 1L;
  private String privateKey = "549c7797b351e48ab1c6bb5857138b418012d97526fc2acba022357d49c93ac0";

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
