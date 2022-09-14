package org.tron.program.generate;

import com.google.protobuf.ByteString;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.program.GenerateTransaction;
import org.tron.program.design.factory.Creator;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.TransferContract;

import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * @author liukai
 * @since 2022/9/14.
 */
@Creator(type = "transfer")
public class TransferCreator extends AbstractTransactionCreator implements TransactionCreator {

  private static String ownerAddress = "TXtrbmfwZ2LxtoCveEhZT86fTss1w8rwJE";
  private static String privateKey = "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";
  private long amount = 1L;

  @Override
  public Protocol.Transaction create() {
    TransferContract contract = TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(Commons.decodeFromBase58Check(ownerAddress)))
            .setToAddress(ByteString.copyFrom(Commons.decodeFromBase58Check(GenerateTransaction.accountQueue.poll())))
            .setAmount(amount)
            .build();
    Protocol.Transaction transaction = createTransaction(contract, ContractType.TransferContract);
    return sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
  }

}
