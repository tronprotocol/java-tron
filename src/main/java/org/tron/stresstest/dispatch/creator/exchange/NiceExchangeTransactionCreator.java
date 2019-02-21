package org.tron.stresstest.dispatch.creator.exchange;

import java.util.concurrent.atomic.AtomicInteger;
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
public class NiceExchangeTransactionCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
  AtomicInteger integer = new AtomicInteger(0);

  private String firstTokenID = "_";
  private String secondTokenID = "1002126";
  private String ownerAddress = "TDZdB4ogHSgU1CGrun8WXaMb2QDDkvAKQm";
  private long exchangeID = 160;
  private long quant = 5L;
  private long expected = 1L;
  private String privateKey = "549c7797b351e48ab1c6bb5857138b418012d97526fc2acba022357d49c93ac0";

  @Override
  protected Protocol.Transaction create() {
    byte[] tokenId = firstTokenID.getBytes();
    if (integer.incrementAndGet() % 2 == 0) {
      tokenId = secondTokenID.getBytes();
    }
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    Contract.ExchangeTransactionContract contract = createExchangeTransactionContract(ownerAddressBytes,
            exchangeID, tokenId, quant, expected);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.ExchangeTransactionContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}