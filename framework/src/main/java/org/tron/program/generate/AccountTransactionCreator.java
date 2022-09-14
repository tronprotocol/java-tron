package org.tron.program.generate;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.program.design.factory.Creator;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.contract.AccountContract.AccountCreateContract;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author liukai
 * @since 2022/9/9.
 */
@Setter
@Creator(type = "transfer")
@Slf4j
public class AccountTransactionCreator extends AbstractTransactionCreator implements TransactionCreator {

  private static String ownerAddress = "TXtrbmfwZ2LxtoCveEhZT86fTss1w8rwJE";
  private static String privateKey = "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";
  private ExecutorService generatePool = Executors.newFixedThreadPool(4, r -> new Thread(r, "create-transaction"));

  @Override
  public Protocol.Transaction create() {
    byte[] ownerAddressBytes = Commons.decodeFromBase58Check(ownerAddress);
    ECKey newAccountKey = new ECKey();
    byte[] newAccountAddressBytes = newAccountKey.getAddress();
    AccountCreateContract contract = createAccountCreateContract(ownerAddressBytes, newAccountAddressBytes);
    Transaction transaction = createTransaction(contract, Contract.ContractType.AccountCreateContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }

  public String create(Protocol.Transaction transaction) {
    return Hex.toHexString(transaction.toByteArray());
  }

  @Override
  public List<String> createTransactions(int count) {
    CountDownLatch countDownLatch = new CountDownLatch(count);
    List<String> transactions = new ArrayList<>(count * 2);
    for (int i = 0; i < count; i++) {
      generatePool.execute(() -> {
        // test account
        Transaction transaction = create();
        transactions.add(Hex.toHexString(transaction.toByteArray()));
        countDownLatch.countDown();
      });
    }
    try {
      countDownLatch.await();
      generatePool.shutdown();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return transactions;
  }
}
