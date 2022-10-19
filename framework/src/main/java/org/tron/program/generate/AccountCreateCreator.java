package org.tron.program.generate;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.program.design.factory.Creator;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.contract.AccountContract.AccountCreateContract;

/**
 * @author liukai
 * @since 2022/9/9.
 */
@Creator(type = "account")
@Slf4j
public class AccountCreateCreator extends AbstractTransactionCreator implements TransactionCreator {
  // 有钱的账户
  private static String ownerAddress = "TXtrbmfwZ2LxtoCveEhZT86fTss1w8rwJE";
  private static String privateKey = "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";

  @Override
  public Protocol.Transaction create() {
    byte[] ownerAddressBytes = Commons.decodeFromBase58Check(ownerAddress);
    ECKey newAccountKey = new ECKey();
    byte[] newAccountAddressBytes = newAccountKey.getAddress();
    AccountCreateContract contract = createAccountCreateContract(ownerAddressBytes, newAccountAddressBytes);
    Transaction transaction = createTransaction(contract, Contract.ContractType.AccountCreateContract);
    return sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
  }

}
