package org.tron.program.generate;

import lombok.Setter;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.program.design.factory.Creator;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.contract.AccountContract.AccountCreateContract;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author liukai
 * @since 2022/9/9.
 */
@Setter
@Creator(type = "account")
public class AccountTransactionCreator extends AbstractTransactionCreator implements TransactionCreator {

  private String ownerAddress;
  private String privateKey;
  private CountDownLatch countDownLatch;

  public AccountTransactionCreator(String ownerAddress, String privateKey, CountDownLatch countDownLatch) {
    this.ownerAddress = ownerAddress;
    this.privateKey = privateKey;
    this.countDownLatch = countDownLatch;
  }

  @Override
  public Protocol.Transaction create() {
    byte[] ownerAddressBytes = Commons.decodeFromBase58Check(ownerAddress);
    ECKey newAccountKey = new ECKey();
    byte[] newAccountAddressBytes = newAccountKey.getAddress();
    AccountCreateContract contract = createAccountCreateContract(ownerAddressBytes, newAccountAddressBytes);
    Transaction transaction = createTransaction(contract, Contract.ContractType.AccountCreateContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    countDownLatch.countDown();
    return transaction;
  }

  public String create(Protocol.Transaction transaction) {
    return Hex.toHexString(transaction.toByteArray());
  }

  @Override
  public List<String> createTransactions() {
    return null;
  }
}
