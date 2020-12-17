package org.tron.stresstest.dispatch.creator.vote;

import java.util.HashMap;
import java.util.Random;
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

  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(voteOwnerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    Contract.VoteWitnessContract contract = createVoteWitnessContract(ownerAddressBytes, getRandomVoteHashMap());
    Protocol.Transaction transaction = createTransaction(contract, ContractType.VoteWitnessContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(voteOwnerKey)));
    return transaction;
  }

  public HashMap<String, String> getRandomVoteHashMap() {
    String[] witnessList = {
        "TRx32uh7TQjdnLFKyWVPKJBfEn1XWjJtcm",
        "TRxF8fZERk4XzQZe1SzvkS5nyNJ7x6tGZ5",
        "TRxUztFKWdXy42MSdiHQoef5VLaXADMJp3",
        "TRxh1GnspMRadaU37UzrRRpkME2EkwCHg4",
        "TRxsiQ2vugWqY2JGr39NHqysAw5zHfWhpU",
        "TRx3MZDxWzTBW3HYX3ZWGEBrvAC8upGA8C",
        "TRxFANjAvztBibiqPRWgG841fVP12BCH7d",
        "TRxVs5MRUy2yHn2kqwev81VjYwXBdYdXrD",
        "TRxhePptGctYfCpxFCsLLAHUr1iShFkGC1",
        "TRxtaoGBJeSwQJu5551cBhaw5sW3vaazuF",
        "TRx3cWa892UxbCaoqCjidp3r946SLZ6U72",
        "TRxFZ7TDgQGF8MfLxnjQ9EqL5WtEiUmTmH",
        "TRxVyqGWNwiPCetP7EQTnukdsVGgMpXzwj",
        "TRxinhH2wZa4zPCqgcUgEZTx3uYs9bFKuM",
        "TRxtfixDf8e4MnZw6zRAVbL3isVnnaiq2o",
        "TRx4sTiyZuDN8whJUyovHZNTk6UYdsqqwg",
        "TRxFiLJp8i5YMQyG2rJFzNA9htaTc7wLcf",
        "TRxXnVabXh8QzdPvAGigmyuYuC391hzmwL",
        "TRxiyR3cJPwyMMpq3WQQF7xiRkNDLkyd9X",
        "TRxu36iquybaSti8ZhVzZ2tPgK7NiXTrSn",
        "TRx4znAxu5FWxb5ccVUX89TtZ8qWF2PM2b",
        "TRxYCcQNn7U7RtN7ZqF36GQYhfMTKnoarw"
    };

    HashMap<String, String> voteWitness = new HashMap<>();
    for (String witnessAddress : witnessList) {
      voteWitness.put(witnessAddress,String.valueOf(new Random().nextInt(1000) + 1));
    }

    return voteWitness;

  }
}
