package org.tron.stresstest.dispatch.creator.transfer;

import com.google.protobuf.ByteString;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.program.FullNode;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import java.io.File;

@Setter
public class NiceTransferTransactionCreator extends AbstractTransferTransactionCreator implements GoodCaseTransactonCreator {

  private String ownerAddress = commonOwnerAddress;
  private String toAddress = commonToAddress;
  private long amount = 1L;
  private String privateKey = commonOwnerPrivateKey;


  @Override
  protected Protocol.Transaction create() {

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    //ECKey ecKey = new ECKey(Utils.getRandom());
    //byte[] toAddress = ecKey.getAddress();

    Contract.TransferContract contract = Contract.TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(ownerAddress)))
        //.setToAddress(ByteString.copyFrom(toAddress))
        .setToAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(FullNode.accountQueue.poll())))
        .setAmount(amount)
        .build();

    Protocol.Transaction transaction = createTransaction(contract, ContractType.TransferContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
